/*******************************************************************************
 *  Copyright (c) 2019, 2022 Julian Honnen and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *     Andras Peteri <apeteri@b2international.com> - extracted common superclass
 *     Hannes Wellmann - Bug 577116 - Improve test utility method reusability
 *     Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static java.util.Comparator.comparing;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.presentation.StandardRepresentation;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.osgi.framework.Version;

public abstract class AbstractLaunchTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static IProject launchConfigsProject;

	@BeforeClass
	public static void setupTargetPlatform() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();
		launchConfigsProject = ProjectUtils.importTestProject("tests/launch");
	}

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	protected static ILaunchConfiguration getLaunchConfiguration(String name) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		return launchManager.getLaunchConfiguration(launchConfigsProject.getFile(name));
	}

	public static IPluginModelBase findWorkspaceModel(String id, String version) {
		return getModel(id, version, ModelEntry::getWorkspaceModels, "workspace");
	}

	public static IPluginModelBase findTargetModel(String id, String version) {
		return getModel(id, version, ModelEntry::getExternalModels, "target");
	}

	private static IPluginModelBase getModel(String id, String versionStr,
			Function<ModelEntry, IPluginModelBase[]> modelsGetter, String type) {

		ModelEntry entry = PluginRegistry.findEntry(id);
		assertNotNull("entry '" + id + "' should be present in PluginRegistry", entry);
		IPluginModelBase[] models = modelsGetter.apply(entry);
		assertTrue("entry '" + id + "' should have " + type + " models", models.length > 0);

		if (versionStr == null) {
			return models[0];
		}
		Version version = Version.parseVersion(versionStr);
		Stream<IPluginModelBase> candiates = Arrays.stream(models);
		return candiates.filter(model -> version.equals(Version.parseVersion(model.getPluginBase().getVersion())))
				.findFirst() // always take first like BundleLaunchHelper
				.orElseThrow(
						() -> new NoSuchElementException("No " + type + " model " + id + "-" + version + " found"));
	}

	static BundleLocationDescriptor workspaceBundle(String id, String version) {
		Objects.requireNonNull(version);
		return () -> findWorkspaceModel(id, version);
	}

	static BundleLocationDescriptor targetBundle(String id, String version) {
		Objects.requireNonNull(version);
		// PluginRegistry.findModel does not consider external models when
		// workspace models are present and returns the 'last' plug-in if
		// multiple with the same version exist
		return () -> findTargetModel(id, version);
	}

	static interface BundleLocationDescriptor {
		IPluginModelBase findModel();
	}

	static void assertPluginMapsEquals(String message, Map<IPluginModelBase, String> expected,
			Map<IPluginModelBase, String> actual) {
		// Like Assert.assertEquals() but with more expressive and easier to
		// compare failure message
		Assertions.assertThat(actual).withRepresentation(new StandardRepresentation() {
			@Override
			public String toStringOf(Object object) {
				if (object instanceof IPluginModelBase plugin) {
					String location = plugin.getUnderlyingResource() != null ? "w" : "e";
					IPluginBase p = plugin.getPluginBase();
					return p.getId() + "-" + p.getVersion() + "(" + location + ")";
				}
				if (object instanceof Map) {
					@SuppressWarnings("unchecked")
					var entries = ((Map<IPluginModelBase, String>) object).entrySet().stream();
					return entries.sorted(PLUGIN_COMPARATOR).map(super::toStringOf)
							.collect(Collectors.joining(",\n", "{\n", "\n}"));
				}
				return super.toStringOf(object);
			}
		}).as(message).isEqualTo(expected);
	}

	private static final Comparator<Entry<IPluginModelBase, String>> PLUGIN_COMPARATOR = comparing(Entry::getKey,
			comparing((IPluginModelBase p) -> p.getPluginBase(),
					comparing(IPluginBase::getId).thenComparing(IPluginBase::getVersion))
			.thenComparing((IPluginModelBase p) -> p.getUnderlyingResource() == null));
}
