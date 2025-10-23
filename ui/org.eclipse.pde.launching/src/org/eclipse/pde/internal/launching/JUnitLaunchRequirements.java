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
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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
				IPluginModelBase model = findRequiredPluginInTargetOrHost(PluginRegistry.findModel(id), plugins -> plugins.max(PDECore.VERSION), id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
				addedRequirements.add(model.getBundleDescription());
			}
		}
		return addedRequirements;
	}

	private static void addAbsentRequirements(Set<BundleDescription> requirements, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		for (BundleRevision bundle : requirements) {
			String id = bundle.getSymbolicName();
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().map(IPluginModelBase::getBundleDescription).noneMatch(b -> b.isResolved() && b.getVersion().equals(bundle.getVersion()))) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(PluginRegistry.findModel(bundle), plgs -> plgs.filter(p -> p.getBundleDescription() == bundle).findFirst(), id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
			}
		}
	}

	private static IPluginModelBase findRequiredPluginInTargetOrHost(IPluginModelBase model, Function<Stream<IPluginModelBase>, Optional<IPluginModelBase>> pluginSelector, String id) throws CoreException {
		if (model == null || !model.getBundleDescription().isResolved()) {
			// prefer bundle from host over unresolved bundle from target
			model = pluginSelector.apply(PDECore.getDefault().findPluginsInHost(id)) //
					.orElseThrow(() -> new CoreException(Status.error(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id))));
		}
		return model;
	}

}
