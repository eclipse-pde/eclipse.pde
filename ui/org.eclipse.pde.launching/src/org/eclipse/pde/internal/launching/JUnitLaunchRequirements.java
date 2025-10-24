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
import java.util.HashSet;
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
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

public class JUnitLaunchRequirements {

	private static final String PDE_JUNIT_RUNTIME = "org.eclipse.pde.junit.runtime"; //$NON-NLS-1$
	private static final String JUNIT4_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit4.runtime"; //$NON-NLS-1$
	private static final String JUNIT5_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit5.runtime"; //$NON-NLS-1$

	public static void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> collectedModels, Map<IPluginModelBase, String> startLevelMap) throws CoreException {
		Collection<String> runtimePlugins = getRequiredJunitRuntimeEclipsePlugins(configuration);
		//We first need to collect the runtime, either by adding it or take it from the already selected bundles
		Collection<IPluginModelBase> collected = new HashSet<>();
		for (String id : runtimePlugins) {
			addIfAbsent(id, collectedModels, startLevelMap).or(() -> collectedModels.getOrDefault(id, List.of()).stream().filter(m -> m.getBundleDescription().isResolved()).findFirst()).ifPresent(collected::add);
		}
		//now compute the closure and add them to the collection
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(collected.stream().map(IPluginModelBase::getBundleDescription).toList());
		for (BundleDescription description : closure) {
			collected.add(addIfAbsent(description, collectedModels, startLevelMap));
		}
	}

	@SuppressWarnings("restriction")
	public static Collection<String> getRequiredJunitRuntimeEclipsePlugins(ILaunchConfiguration configuration) {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return List.of();
		}
		switch (testKind.getId()) {
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT3_TEST_KIND_ID -> {
				return List.of(PDE_JUNIT_RUNTIME);
			} // Nothing to add for JUnit-3
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT4_TEST_KIND_ID -> {
				return List.of(PDE_JUNIT_RUNTIME, JUNIT4_JDT_RUNTIME_PLUGIN);
			}
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID -> {
				return List.of(PDE_JUNIT_RUNTIME, JUNIT5_JDT_RUNTIME_PLUGIN);
			}
			default -> throw new IllegalArgumentException("Unsupported junit test kind: " + testKind.getId()); //$NON-NLS-1$
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

	private static Optional<IPluginModelBase> addIfAbsent(String id, Map<String, List<IPluginModelBase>> fAllBundles, Map<IPluginModelBase, String> fModels) throws CoreException {
		List<IPluginModelBase> models = fAllBundles.computeIfAbsent(id, k -> new ArrayList<>());
		if (models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
			IPluginModelBase model = findRequiredPluginInTargetOrHost(PluginRegistry.findModel(id), plugins -> plugins.max(PDECore.VERSION), id);
			models.add(model);
			BundleLauncherHelper.addDefaultStartingBundle(fModels, model);
			return Optional.of(model);
		}
		return Optional.empty();
	}

	private static IPluginModelBase addIfAbsent(BundleDescription description, Map<String, List<IPluginModelBase>> fAllBundles, Map<IPluginModelBase, String> fModels) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel((Resource) description);
		if (model == null) {
			Version version = description.getVersion();
			model = PDECore.getDefault().findPluginsInHost(description.getSymbolicName()).filter(m -> version.equals(PDECore.getOSGiVersion(m))).findFirst().orElseThrow(() -> new CoreException(Status.error("Resolved bundle description " + description + " not found in target or host!"))); //$NON-NLS-1$//$NON-NLS-2$
		}
		List<IPluginModelBase> models = fAllBundles.computeIfAbsent(description.getSymbolicName(), k -> new ArrayList<>());
		if (!models.contains(model)) {
			models.add(model);
			BundleLauncherHelper.addDefaultStartingBundle(fModels, model);
		}
		return model;
	}

}
