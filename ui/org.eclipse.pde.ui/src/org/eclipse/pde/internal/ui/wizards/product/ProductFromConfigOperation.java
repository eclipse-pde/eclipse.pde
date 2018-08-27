/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.util.*;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * This operation generates a product configuration filling in fields based on information
 * stored a launch configuration. Product, application, JRE, and config information is
 * collected from the launch config.
 */
public class ProductFromConfigOperation extends BaseProductCreationOperation {

	private ILaunchConfiguration fLaunchConfiguration;

	public ProductFromConfigOperation(IFile file, ILaunchConfiguration config) {
		super(file);
		fLaunchConfiguration = config;
	}

	@Override
	protected void initializeProduct(IProduct product) {
		if (fLaunchConfiguration == null)
			return;
		try {
			IProductModelFactory factory = product.getModel().getFactory();
			boolean useProduct = fLaunchConfiguration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false);
			if (useProduct) {
				String id = fLaunchConfiguration.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
				if (id != null) {
					initializeProductInfo(factory, product, id);
				}
			} else {
				String appName = fLaunchConfiguration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
				product.setApplication(appName);
			}

			// Set JRE info from information from the launch configuration
			String jreString = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
			if (jreString != null) {
				IPath jreContainerPath = new Path(jreString);
				IJREInfo jreInfo = product.getJREInfo();
				if (jreInfo == null) {
					jreInfo = product.getModel().getFactory().createJVMInfo();
				}
				jreInfo.setJREContainerPath(TargetPlatform.getOS(), jreContainerPath);
				product.setJREInfo(jreInfo);
			}

			// fetch the plug-ins models
			String workspaceId = IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS;
			String targetId = IPDELauncherConstants.SELECTED_TARGET_PLUGINS;
			if (fLaunchConfiguration.getType().getIdentifier().equals(IPDELauncherConstants.OSGI_CONFIGURATION_TYPE)) {
				workspaceId = IPDELauncherConstants.WORKSPACE_BUNDLES;
				targetId = IPDELauncherConstants.TARGET_BUNDLES;
			}
			Set<String> set = new HashSet<>();
			Map<IPluginModelBase, String> map = BundleLauncherHelper.getWorkspaceBundleMap(fLaunchConfiguration, set, workspaceId);
			map.putAll(BundleLauncherHelper.getTargetBundleMap(fLaunchConfiguration, set, targetId));

			addPlugins(factory, product, map);

			if (fLaunchConfiguration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
				super.initializeProduct(product);
			} else {
				String path = fLaunchConfiguration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, "/"); //$NON-NLS-1$
				IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(new Path(path));
				if (container != null) {
					IConfigurationFileInfo info = factory.createConfigFileInfo();
					info.setUse(null, "custom"); //$NON-NLS-1$
					info.setPath(null, container.getFullPath().toString());
					product.setConfigurationFileInfo(info);
				} else {
					super.initializeProduct(product);
				}
			}

			// set vm and program arguments from the launch configuration
			String vmargs = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
			String programArgs = fLaunchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
			if (vmargs != null || programArgs != null) {
				IArgumentsInfo arguments = product.getLauncherArguments();
				if (arguments == null)
					arguments = factory.createLauncherArguments();
				if (vmargs != null)
					arguments.setVMArguments(vmargs, IArgumentsInfo.L_ARGS_ALL);
				if (programArgs != null) {
					String[] parsedArgs = DebugPlugin.splitArguments(programArgs);
					List<String> unwantedArgs = Arrays.asList(new String[] {'-' + IEnvironment.P_ARCH, '-' + IEnvironment.P_NL, '-' + IEnvironment.P_OS, '-' + IEnvironment.P_WS});
					StringBuilder filteredArgs = new StringBuilder();
					for (int i = 0; i < parsedArgs.length; i++) {
						if (unwantedArgs.contains(parsedArgs[i].toLowerCase())) {
							if (!parsedArgs[i + 1].startsWith("-")) { //$NON-NLS-1$
								i++; // skip its value too
								continue;
							}
						}
						filteredArgs.append(parsedArgs[i] + ' ');
					}
					programArgs = filteredArgs.toString().trim();
					if (programArgs.length() > 0)
						arguments.setProgramArguments(programArgs, IArgumentsInfo.L_ARGS_ALL);
				}
				product.setLauncherArguments(arguments);
			}

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
