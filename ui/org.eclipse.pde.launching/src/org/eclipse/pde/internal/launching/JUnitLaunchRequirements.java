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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleRevision;

public class JUnitLaunchRequirements {

	public static final String JUNIT4_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit4.runtime"; //$NON-NLS-1$
	public static final String JUNIT5_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit5.runtime"; //$NON-NLS-1$

	public static void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		Collection<String> runtimePlugins = getRequiredJunitRuntimeEclipsePlugins(configuration);
		Set<BundleDescription> addedRuntimeBundles = addAbsentRequirements(runtimePlugins, allBundles, allModels);
		Set<BundleDescription> runtimeRequirements = DependencyManager.findRequirementsClosure(addedRuntimeBundles);
		addAbsentRequirements(runtimeRequirements, allBundles, allModels);
	}

	@SuppressWarnings("restriction")
	public static Collection<String> getRequiredJunitRuntimeEclipsePlugins(ILaunchConfiguration configuration) {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return List.of();
		}
		List<String> plugins = new ArrayList<>();
		plugins.add("org.eclipse.pde.junit.runtime"); //$NON-NLS-1$
		switch (testKind.getId()) {
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT3_TEST_KIND_ID -> {
			} // Nothing to add for JUnit-3
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT4_TEST_KIND_ID -> plugins.add(JUNIT4_JDT_RUNTIME_PLUGIN);
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID -> plugins.add(JUNIT5_JDT_RUNTIME_PLUGIN);
			default -> throw new IllegalArgumentException("Unsupported junit test kind: " + testKind.getId()); //$NON-NLS-1$
		}
		return plugins;
	}

	private static Set<BundleDescription> addAbsentRequirements(Collection<String> requirements, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		Set<BundleDescription> addedRequirements = new LinkedHashSet<>();
		for (String id : requirements) {
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().noneMatch(p -> p.getBundleDescription().isResolved())) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(id, null);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
				addedRequirements.add(model.getBundleDescription());
			}
		}
		return addedRequirements;
	}

	private static void addAbsentRequirements(Set<BundleDescription> requirements, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		for (BundleRevision requirement : requirements) {
			String id = requirement.getSymbolicName();
			Version version = requirement.getVersion();
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			boolean replace = !models.isEmpty() && models.stream().anyMatch(m -> !m.getBundleDescription().getVersion().equals(version));
			if (replace || models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(requirement.getSymbolicName(), new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED));
				if (replace) {
					String startLevel = null;
					for (IPluginModelBase m : models) {
						startLevel = allModels.remove(m);
					}
					models.clear();
					allModels.put(model, startLevel); //TODO: isn't this overwritten? Should it be retained or just ignored? 
				}
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
			}
		}
	}

	private static IPluginModelBase findRequiredPluginInTargetOrHost(String id, VersionRange version) throws CoreException {
		IPluginModelBase model = version != null ? PluginRegistry.findModel(id, version) : PluginRegistry.findModel(id);
		if (model == null || !model.getBundleDescription().isResolved()) {
			// prefer bundle from host over unresolved bundle from target
			model = PDECore.getDefault().findPluginInHost(id, version);
		}
		if (model == null) {
			throw new CoreException(Status.error(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id)));
		}
		return model;
	}

}
