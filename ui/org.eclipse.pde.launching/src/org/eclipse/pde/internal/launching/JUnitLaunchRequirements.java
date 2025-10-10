/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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
 *     Advantest - GH issue 2006 - JUnit 5 bundle clashes with JUnit 6
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleRevision;

public class JUnitLaunchRequirements {

	public static final String JUNIT4_RUNTIME_PLUGIN = "org.eclipse.jdt.junit4.runtime"; //$NON-NLS-1$
	public static final String JUNIT5_RUNTIME_PLUGIN = "org.eclipse.jdt.junit5.runtime"; //$NON-NLS-1$

	private static final VersionRange JUNIT5_VERSIONS = new VersionRange("[1, 5)"); //$NON-NLS-1$

	// we add launcher and jupiter.engine to support @RunWith(JUnitPlatform.class)
	private static final String[] JUNIT5_RUN_WITH_PLUGINS = {"junit-platform-launcher", //$NON-NLS-1$
			"junit-jupiter-engine", //$NON-NLS-1$
	};

	public static void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		Collection<String> plugins = getRequiredJunitRuntimePlugins(configuration);
		addPlugins(plugins, allBundles, allModels);
		if (plugins.contains(JUNIT5_RUNTIME_PLUGIN) && (allBundles.containsKey("junit-platform-runner") || allBundles.containsKey("org.junit.platform.runner"))) { //$NON-NLS-1$ //$NON-NLS-2$
			Set<BundleDescription> descriptions = JUnitLaunchRequirements.junit5PlatformRequirements();
			Set<BundleDescription> junitRequirements = DependencyManager.findRequirementsClosure(descriptions);
			addAbsentRequirements(junitRequirements, allBundles, allModels);
		}
	}

	@SuppressWarnings("restriction")
	public static Collection<String> getRequiredJunitRuntimePlugins(ILaunchConfiguration configuration) {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return Collections.emptyList();
		}
		List<String> plugins = new ArrayList<>();
		plugins.add("org.eclipse.pde.junit.runtime"); //$NON-NLS-1$

		if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add(JUNIT4_RUNTIME_PLUGIN);
		} else if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add(JUNIT5_RUNTIME_PLUGIN);
		}
		return plugins;
	}

	private static void addPlugins(Collection<String> plugins, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		Set<String> requiredPlugins = new LinkedHashSet<>(plugins);

		Set<BundleDescription> addedRequirements = new LinkedHashSet<>();
		addAbsentRequirements(requiredPlugins, addedRequirements, allBundles, allModels);

		Set<BundleDescription> requirementsOfRequirements = DependencyManager.findRequirementsClosure(addedRequirements);
		addAbsentRequirements(requirementsOfRequirements, allBundles, allModels);
	}

	private static void addAbsentRequirements(Collection<String> requirements, Set<BundleDescription> addedRequirements, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		for (String id : requirements) {
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
				IPluginModelBase model = JUnitLaunchRequirements.findRequiredPluginInTargetOrHost(id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
				if (addedRequirements != null) {
					addedRequirements.add(model.getBundleDescription());
				}
			}
		}
	}

	private static void addAbsentRequirements(Set<BundleDescription> toAdd, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		for (BundleDescription requirement : toAdd) {
			String id = requirement.getSymbolicName();
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			boolean replace = !models.isEmpty() && models.stream().anyMatch(m -> !m.getBundleDescription().getVersion().equals(requirement.getVersion()));
			if (replace || models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
				IPluginModelBase model = JUnitLaunchRequirements.findRequiredPluginInTargetOrHost(requirement);
				if (replace) {
					String startLevel = null;
					for (IPluginModelBase m : models) {
						startLevel = allModels.remove(m);
					}
					models.clear();
					allModels.put(model, startLevel);
				}
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
			}
		}
	}

	public static Set<BundleDescription> junit5PlatformRequirements() throws CoreException {
		Set<BundleDescription> descriptions = new LinkedHashSet<>();
		for (String id : JUNIT5_RUN_WITH_PLUGINS) {
			IPluginModelBase model = findRequiredPluginInTargetOrHost(id, JUNIT5_VERSIONS);
			if (model != null) {
				BundleDescription description = model.getBundleDescription();
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	public static IPluginModelBase findRequiredPluginInTargetOrHost(String id) throws CoreException {
		return findRequiredPluginInTargetOrHost(id, PluginRegistry.findModel(id));
	}

	private static IPluginModelBase findRequiredPluginInTargetOrHost(String id, VersionRange versionRange) throws CoreException {
		return findRequiredPluginInTargetOrHost(id, PluginRegistry.findModel(id, versionRange));
	}

	public static IPluginModelBase findRequiredPluginInTargetOrHost(BundleRevision bundleRevision) throws CoreException {
		String id = bundleRevision.getSymbolicName();
		return findRequiredPluginInTargetOrHost(id, PluginRegistry.findModel(bundleRevision));
	}

	private static IPluginModelBase findRequiredPluginInTargetOrHost(String id, IPluginModelBase model) throws CoreException {
		if (model == null || !model.getBundleDescription().isResolved()) {
			// prefer bundle from host over unresolved bundle from target
			model = PDECore.getDefault().findPluginInHost(id);
		}
		if (model == null) {
			String message = NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id);
			Status error = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, IStatus.OK, message, null);
			throw new CoreException(error);
		}
		return model;
	}

}
