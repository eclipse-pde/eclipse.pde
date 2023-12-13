/*******************************************************************************
 * Copyright (c) 2010, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576860 - Specify all launch-type requirements in RequirementHelper
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * Centralizes code for validating the contents of a launch and finding missing requirements.
 *
 * @since 3.6
 * @see EclipsePluginValidationOperation
 */
public class RequirementHelper {

	private RequirementHelper() {
	} // static use only

	@FunctionalInterface // like java.util.function.Function but can throw CoreException
	public static interface ILaunchRequirementsFunction {
		List<String> getRequiredBundleIds(ILaunchConfiguration lc) throws CoreException;
	}

	private static final ConcurrentMap<String, ILaunchRequirementsFunction> APPLICATION_REQUIREMENTS = new ConcurrentHashMap<>();

	public static void registerLaunchTypeRequirements(String launchTypeId, ILaunchRequirementsFunction requirementsFunction) {
		APPLICATION_REQUIREMENTS.put(launchTypeId, Objects.requireNonNull(requirementsFunction));
	}

	public static void registerSameRequirementsAsFor(String launchTypeId, String sourceLaunchTypeId) {
		// enforce  initialization of source class to register required plug-ins
		ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(sourceLaunchTypeId);
		for (Set<String> modes : type.getSupportedModeCombinations()) {
			try {
				for (ILaunchDelegate delegate : type.getDelegates(modes)) {
					delegate.getDelegate();
				}
			} catch (CoreException e) {
			}
		}
		APPLICATION_REQUIREMENTS.put(launchTypeId, APPLICATION_REQUIREMENTS.get(sourceLaunchTypeId));
	}

	/**
	 * Returns a list of string plug-in ids that are required to launch the product, application
	 * or application to test that the given launch configuration specifies.  Which attributes are
	 * checked will depend on whether a product, an application or a junit application is being launched.
	 *
	 * @param config launch configuration to get attributes from
	 * @return list of string plug-in IDs that are required by the config's application/product settings
	 * @throws CoreException if there is a problem reading the launch config
	 */
	public static List<String> getApplicationLaunchRequirements(ILaunchConfiguration config) throws CoreException {
		ILaunchRequirementsFunction requirementsFunction = APPLICATION_REQUIREMENTS.get(config.getType().getIdentifier());
		return requirementsFunction != null ? Objects.requireNonNull(requirementsFunction.getRequiredBundleIds(config)) : Collections.emptyList();
	}

	public static boolean addApplicationLaunchRequirements(List<String> appRequirements, ILaunchConfiguration configuration, Map<IPluginModelBase, String> bundle2startLevel) throws CoreException {
		Consumer<IPluginModelBase> addPlugin = b -> BundleLauncherHelper.addDefaultStartingBundle(bundle2startLevel, b);
		return addApplicationLaunchRequirements(appRequirements, configuration, bundle2startLevel.keySet(), addPlugin);
	}

	public static boolean addApplicationLaunchRequirements(List<String> appRequirements, ILaunchConfiguration configuration, Set<IPluginModelBase> containedPlugins, Consumer<IPluginModelBase> addPlugin) throws CoreException {
		boolean isFeatureBasedLaunch = configuration.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false);
		String pluginResolution = isFeatureBasedLaunch ? configuration.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE) : IPDELauncherConstants.LOCATION_WORKSPACE;

		boolean allRequirementsSatisfied = true;
		for (String requiredBundleId : appRequirements) {
			ModelEntry entry = PluginRegistry.findEntry(requiredBundleId);
			if (entry != null) {
				// add required plug-in if not yet already included
				var allPluginsWithId = Stream.of(entry.getWorkspaceModels(), entry.getExternalModels()).flatMap(Arrays::stream);
				if (allPluginsWithId.noneMatch(containedPlugins::contains)) {
					IPluginModelBase plugin = BundleLauncherHelper.getLatestPlugin(requiredBundleId, pluginResolution);
					addPlugin.accept(plugin);
				}
			} else {
				allRequirementsSatisfied = false;
			}
		}
		return allRequirementsSatisfied;
	}

	public static List<String> getProductRequirements(ILaunchConfiguration config) throws CoreException {
		String product = config.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
		if (product == null) {
			return Collections.emptyList();
		}
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IExtension[] extensions = registry.findExtensions("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		for (IExtension extension : extensions) {

			if (product.equals(extension.getUniqueIdentifier()) || product.equals(extension.getSimpleIdentifier())) {
				Set<String> requiredIds = new LinkedHashSet<>();
				requiredIds.add(extension.getContributor().getName());

				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					String application = element.getAttribute(IPDELauncherConstants.APPLICATION);
					if (application != null && !application.isEmpty()) {
						requiredIds.addAll(getApplicationRequirements(application));
					}
				}
				// Only one extension should match the product so break out of the loop
				return List.copyOf(requiredIds);
			}
		}
		return Collections.emptyList();
	}

	public static List<String> getApplicationRequirements(String application) {
		PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
		IExtension[] extensions = registry.findExtensions("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		for (IExtension extension : extensions) {
			if (application.equals(extension.getUniqueIdentifier()) || application.equals(extension.getSimpleIdentifier())) {
				return List.of(extension.getContributor().getName());
				// Only one extension should match the application so break out of the loop
			}
		}
		return Collections.emptyList();
	}
}
