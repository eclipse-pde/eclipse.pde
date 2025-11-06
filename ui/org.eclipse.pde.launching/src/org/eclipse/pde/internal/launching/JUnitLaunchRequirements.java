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
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

public class JUnitLaunchRequirements {

	private static final StateObjectFactory FACTORY = BundleHelper.getPlatformAdmin().getFactory();
	private static final String PDE_JUNIT_RUNTIME = "org.eclipse.pde.junit.runtime"; //$NON-NLS-1$
	private static final String JUNIT4_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit4.runtime"; //$NON-NLS-1$
	private static final String JUNIT5_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit5.runtime"; //$NON-NLS-1$

	public static void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> collectedModels, Map<IPluginModelBase, String> startLevelMap) throws CoreException {
		Collection<IPluginModelBase> runtimeBundles = getEclipseJunitRuntimePlugins(configuration, collectedModels, startLevelMap);
		List<BundleDescription> roots = modelsAsDescriptions(runtimeBundles).toList();
		List<BundleDescription> bundles = Stream.concat(DependencyManager.findRequirementsClosure(roots).stream(), collectedModels.values().stream().flatMap(pl -> modelsAsDescriptions(pl))).distinct().toList();
		Collection<BundleDescription> runtimeRequirements = filterRequirementsByState(bundles, runtimeBundles, configuration);
		addAbsentRequirements(runtimeRequirements, collectedModels, startLevelMap);
	}

	private static Collection<BundleDescription> filterRequirementsByState(Collection<BundleDescription> bundles, Collection<IPluginModelBase> rootBundles, ILaunchConfiguration configuration) throws CoreException {
		//lookup that maps a copy to the original description from the bundles parameter
		Map<BundleRevision, BundleDescription> descriptionnMap = new IdentityHashMap<>();
		Set<BundleRevision> rootSet = modelsAsDescriptions(rootBundles).collect(Collectors.toSet());
		State state = FACTORY.createState(true);
		State targetState = PDECore.getDefault().getModelManager().getState().getState();
		List<BundleDescription> resolveRoots = new ArrayList<>();
		long id = 1;
		for (BundleDescription bundle : bundles) {
			BundleDescription copy = FACTORY.createBundleDescription(id++, bundle);
			descriptionnMap.put(copy, bundle);
			state.addBundle(copy);
			if (rootSet.contains(bundle)) {
				resolveRoots.add(copy);
			}
		}
		state.setPlatformProperties(targetState.getPlatformProperties());
		state.setResolverHookFactory(new ResolverHookFactory() {

			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				return new ResolverHook() {

					@Override
					public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
					}

					@Override
					public void filterResolvable(Collection<BundleRevision> candidates) {
					}

					@Override
					public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
						boolean optional = isOptional(requirement);
						if (candidates.size() == 1 && !optional) {
							//We only have one candidate and requirement is not optional, so keep it or we get an error!
							return;
						}
						List<BundleCapability> list = candidates.stream().filter(cp -> isFromDifferentState(cp)).toList();
						if (list.isEmpty()) {
							//nothing to do here...
							return;
						}
						//iterate in reverse order so we remove lower ranked candidates first ...
						for (int i = list.size() - 1; i >= 0 && (optional || candidates.size() > 1); i--) {
							BundleCapability capability = list.get(i);
							candidates.remove(capability);
						}
					}

					private boolean isFromDifferentState(BundleCapability capability) {
						BundleRevision resource = capability.getResource();
						BundleDescription original = descriptionnMap.get(resource);
						if (original != null) {
							return original.getContainingState() != targetState;
						}
						return false;
					}

					@Override
					public void end() {
					}
				};
			}
		});
		state.resolve(false);
		for (BundleDescription rootBundle : resolveRoots) {
			ResolverError[] errors = state.getResolverErrors(rootBundle);
			if (errors.length > 0) {
				throw new CoreException(Status.error(String.format("%s can not be resolved: %s", rootBundle, Arrays.toString(errors)))); //$NON-NLS-1$
			}
		}

		Collection<BundleDescription> closure = DependencyManager.findRequirementsClosure(resolveRoots);
		// map back to the originals!
		return closure.stream().map(bd -> descriptionnMap.get(bd)).filter(Objects::nonNull).toList();
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

	private static Collection<IPluginModelBase> getEclipseJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> collectedModels, Map<IPluginModelBase, String> startLevelMap) throws CoreException {
		Set<IPluginModelBase> descriptions = new LinkedHashSet<>();
		for (String id : getRequiredJunitRuntimeEclipsePlugins(configuration)) {
			addIfAbsent(id, collectedModels, startLevelMap).ifPresent(descriptions::add);
		}
		return descriptions;
	}

	private static Optional<IPluginModelBase> addIfAbsent(String id, Map<String, List<IPluginModelBase>> collectedModels, Map<IPluginModelBase, String> startLevelMap) throws CoreException {
		List<IPluginModelBase> models = collectedModels.computeIfAbsent(id, k -> new ArrayList<>());
		if (models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
			IPluginModelBase model = findRequiredPluginInTargetOrHost(PluginRegistry.findModel(id), plugins -> plugins.max(PDECore.VERSION), id);
			models.add(model);
			BundleLauncherHelper.addDefaultStartingBundle(startLevelMap, model);
			return Optional.of(model);
		}

		return models.stream().filter(m -> m.getBundleDescription().isResolved()).findFirst();
	}

	private static void addAbsentRequirements(Collection<BundleDescription> requirements, Map<String, List<IPluginModelBase>> collectedModels, Map<IPluginModelBase, String> startLevelMap) throws CoreException {
		for (BundleRevision bundle : requirements) {
			String id = bundle.getSymbolicName();
			List<IPluginModelBase> models = collectedModels.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().map(IPluginModelBase::getBundleDescription).noneMatch(b -> b.isResolved() && b.getVersion().equals(bundle.getVersion()))) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(PluginRegistry.findModel(bundle), plgs -> plgs.filter(p -> p.getBundleDescription() == bundle).findFirst(), id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(startLevelMap, model);
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

	private static Stream<BundleDescription> modelsAsDescriptions(Collection<IPluginModelBase> runtimeBundles) {
		return runtimeBundles.stream().map(p -> p.getBundleDescription()).filter(Objects::nonNull);
	}

	private static boolean isOptional(Requirement req) {
		String resolution = req.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		return Namespace.RESOLUTION_OPTIONAL.equalsIgnoreCase(resolution);
	}

}
