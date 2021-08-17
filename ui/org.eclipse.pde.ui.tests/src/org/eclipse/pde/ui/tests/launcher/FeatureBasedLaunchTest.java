/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.wizards.feature.CreateFeatureProjectOperation;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureData;
import org.junit.*;

public class FeatureBasedLaunchTest extends AbstractLaunchTest {

	private static IProject featureProject;

	private ILaunchConfiguration fFeatureBasedWithStartLevels;

	@BeforeClass
	public static void createTestFeature() throws Exception {

		FeatureData featureData = new FeatureData();
		featureData.id = FeatureBasedLaunchTest.class.getName() + "-feature";
		featureData.version = "1.0.0";

		IPluginBase[] contents = Stream.of("javax.inject", "org.eclipse.core.runtime", "org.eclipse.ui") //
				.map(id -> PluginRegistry.findModel(id).getPluginBase()) //
				.toArray(IPluginBase[]::new);

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		featureProject = workspaceRoot.getProject(featureData.id);
		IPath location = workspaceRoot.getLocation().append(featureProject.getName());
		CreateFeatureProjectOperation operation = new CreateFeatureProjectOperation(featureProject, location,
				featureData, contents, null) {
			@Override
			protected void openFeatureEditor(IFile manifestFile) {
				// don't open in headless tests
			}
		};
		operation.run(null);
	}

	@AfterClass
	public static void cleanup() throws Exception {
		featureProject.delete(true, null);
	}

	@Before
	public void setupLaunchConfig() throws Exception {
		fFeatureBasedWithStartLevels = getLaunchConfiguration("feature-based-with-startlevels.launch");
	}

	@Test
	public void testOldEntryWithoutConfigurationHasDefaults() throws Exception {
		checkStartLevels("javax.inject", "default:default");
	}

	@Test
	public void testUseConfiguredStartLevels() throws Exception {
		checkStartLevels("org.eclipse.core.runtime", "1:true");
	}

	@Test
	public void testIgnoreConfiguredStartLevelsOfUncheckedPlugin() throws Exception {
		checkStartLevels("org.eclipse.ui", "default:default");
	}

	private void checkStartLevels(String pluginId, String expectedStartLevels) throws CoreException {
		Map<IPluginModelBase, String> bundleMap = BundleLauncherHelper.getMergedBundleMap(fFeatureBasedWithStartLevels,
				false);

		Map<String, String> byId = new LinkedHashMap<>();
		for (Entry<IPluginModelBase, String> entry : bundleMap.entrySet()) {
			byId.put(entry.getKey().getPluginBase().getId(), entry.getValue());
		}

		assertThat(byId).containsEntry(pluginId, expectedStartLevels);
	}
}
