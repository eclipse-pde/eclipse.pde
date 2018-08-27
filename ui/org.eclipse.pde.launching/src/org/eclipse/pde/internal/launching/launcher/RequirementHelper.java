/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Karsten Thoms (itemis) - Bug 530406
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * Centralizes code for validating the contents of a launch and finding missing requirements.
 *
 * @since 3.6
 * @see EclipsePluginValidationOperation
 */
public class RequirementHelper {

	/**
	 * Returns a list of string plug-in ids that are required to launch the product, application
	 * or application to test that the given launch configuration specifies.  Which attributes are
	 * checked will depend on whether a product, an application or a junit application is being launched.
	 *
	 * @param config launch configuration to get attributes from
	 * @param plugins list of plugin models to look for product extensions in
	 * @return list of string plug-in IDs that are required by the config's application/product settings
	 * @throws CoreException if there is a problem reading the launch config
	 */
	public static String[] getApplicationRequirements(ILaunchConfiguration config) throws CoreException {
		Set<String> requiredIds = new HashSet<>();
		if (config.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			String product = config.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
			if (product != null) {
				getProductRequirements(product, requiredIds);
			}
		} else {
			String configType = config.getType().getIdentifier();
			if (configType.equals(IPDELauncherConstants.ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE)) {
				String application = config.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
				if (!IPDEConstants.CORE_TEST_APPLICATION.equals(application)) {
					getApplicationRequirements(application, requiredIds);
				}
			} else {
				// Junit launch configs can have the core test application set in either the 'app to test' or the 'application' attribute
				String application = config.getAttribute(IPDELauncherConstants.APP_TO_TEST, (String) null);
				if (application == null) {
					application = config.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
				}
				if (application == null) {
					application = TargetPlatform.getDefaultApplication();
				}
				if (!IPDEConstants.CORE_TEST_APPLICATION.equals(application)) {
					getApplicationRequirements(application, requiredIds);
				}
			}
		}
		return requiredIds.toArray(new String[requiredIds.size()]);
	}

	private static void getProductRequirements(String product, Collection<String> requiredIds) {
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IExtension[] extensions = registry.findExtensions("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		for (IExtension extension : extensions) {

			if (product.equals(extension.getUniqueIdentifier()) || product.equals(extension.getSimpleIdentifier())) {
				requiredIds.add(extension.getContributor().getName());

				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					String application = element.getAttribute("application"); //$NON-NLS-1$
					if (application != null && application.length() > 0) {
						getApplicationRequirements(application, requiredIds);
					}
				}
				// Only one extension should match the product so break out of the looop
				break;
			}
		}
	}

	private static void getApplicationRequirements(String application, Collection<String> requiredIds) {
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IExtension[] extensions = registry.findExtensions("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		for (IExtension extension : extensions) {
			if (application.equals(extension.getUniqueIdentifier()) || application.equals(extension.getSimpleIdentifier())) {
				requiredIds.add(extension.getContributor().getName());
				// Only one extension should match the application so break out of the looop
				break;
			}
		}
	}
}
