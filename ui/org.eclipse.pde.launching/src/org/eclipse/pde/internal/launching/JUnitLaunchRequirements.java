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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

public class JUnitLaunchRequirements {

	public static final String JUNIT4_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit4.runtime"; //$NON-NLS-1$
	public static final String JUNIT5_JDT_RUNTIME_PLUGIN = "org.eclipse.jdt.junit5.runtime"; //$NON-NLS-1$

	public static void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		Set<String> requiredPlugins = new LinkedHashSet<>(getRequiredJunitRuntimeEclipsePlugins(configuration));

		if (allBundles.containsKey("junit-platform-runner")) { //$NON-NLS-1$
			// add launcher and jupiter.engine to support @RunWith(JUnitPlatform.class)
			requiredPlugins.add("junit-platform-launcher"); //$NON-NLS-1$
			requiredPlugins.add("junit-jupiter-engine"); //$NON-NLS-1$
		}

		Set<BundleDescription> addedRequirements = new HashSet<>();
		addAbsentRequirements(requiredPlugins, addedRequirements, allBundles, allModels);

		Set<BundleDescription> requirementsOfRequirements = DependencyManager.findRequirementsClosure(addedRequirements);
		Set<String> rorIds = requirementsOfRequirements.stream().map(BundleDescription::getSymbolicName).collect(Collectors.toSet());
		addAbsentRequirements(rorIds, null, allBundles, allModels);
	}

	@SuppressWarnings("restriction")
	public static Collection<String> getRequiredJunitRuntimeEclipsePlugins(ILaunchConfiguration configuration) {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return Collections.emptyList();
		}
		List<String> plugins = new ArrayList<>();
		plugins.add("org.eclipse.pde.junit.runtime"); //$NON-NLS-1$

		if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add("org.eclipse.jdt.junit4.runtime"); //$NON-NLS-1$
		} else if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add("org.eclipse.jdt.junit5.runtime"); //$NON-NLS-1$
		}
		return plugins;
	}

	private static void addAbsentRequirements(Collection<String> requirements, Set<BundleDescription> addedRequirements, Map<String, List<IPluginModelBase>> allBundles, Map<IPluginModelBase, String> allModels) throws CoreException {
		for (String id : requirements) {
			List<IPluginModelBase> models = allBundles.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(allModels, model);
				if (addedRequirements != null) {
					addedRequirements.add(model.getBundleDescription());
				}
			}
		}
	}

	private static IPluginModelBase findRequiredPluginInTargetOrHost(String id) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model == null || !model.getBundleDescription().isResolved()) {
			// prefer bundle from host over unresolved bundle from target
			model = PDECore.getDefault().findPluginInHost(id).max(Comparator.comparing(p -> p.getBundleDescription().getVersion())).orElse(null);
		}
		if (model == null) {
			throw new CoreException(Status.error(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id)));
		}
		return model;
	}
}
