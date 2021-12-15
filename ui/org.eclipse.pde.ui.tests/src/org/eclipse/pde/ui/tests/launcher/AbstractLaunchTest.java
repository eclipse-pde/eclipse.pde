/*******************************************************************************
 *  Copyright (c) 2019, 2021 Julian Honnen and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
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

	protected ILaunchConfiguration getLaunchConfiguration(String name) {
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
				.orElseThrow(() -> new NoSuchElementException("No " + type + " model " + id + "-" + version + "found"));
	}
}
