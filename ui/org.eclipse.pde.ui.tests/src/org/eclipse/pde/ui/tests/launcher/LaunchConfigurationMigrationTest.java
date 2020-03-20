/**************************************************************************************
 *  Copyright (c) 2019 Andras Peteri
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andras Peteri <apeteri@b2international.com> - initial API and implementation
 **************************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.project.ProjectCreationTests;
import org.junit.Before;
import org.junit.Test;

public class LaunchConfigurationMigrationTest extends AbstractLaunchTest {

	@Before
	public void setupPluginProjects() throws Exception {
		createProject("org.eclipse.pde.plugin1");
		createProject("org.eclipse.pde.plugin2");
	}

	private void createProject(String bundleName) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(bundleName);
		if (project.exists()) {
			project.delete(true, null);
		}

		IBundleProjectDescription description = ProjectCreationTests.getBundleProjectService().getDescription(project);
		description.setSymbolicName(project.getName());
		description.apply(null);
	}

	@Test
	public void testPluginBasedWithoutAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-without-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> workspaceBundles = BundleLauncherHelper.getWorkspaceBundleMap(wc);
		assertEquals("default:true", workspaceBundles.get(findWorkspaceModel("org.eclipse.pde.plugin1")));
		assertEquals("3:false", workspaceBundles.get(findWorkspaceModel("org.eclipse.pde.plugin2")));

		Map<IPluginModelBase, String> targetBundles = BundleLauncherHelper.getTargetBundleMap(wc);
		assertEquals("default:true", targetBundles.get(findTargetModel("org.eclipse.core.runtime")));
		assertEquals("2:false", targetBundles.get(findTargetModel("org.eclipse.ui")));
	}

	@Test
	public void testPluginBasedWithAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-with-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> workspaceBundles = BundleLauncherHelper.getWorkspaceBundleMap(wc);
		assertEquals("default:default", workspaceBundles.get(findWorkspaceModel("org.eclipse.pde.plugin1")));

		Map<IPluginModelBase, String> targetBundles = BundleLauncherHelper.getTargetBundleMap(wc);
		assertEquals("default:true", targetBundles.get(findTargetModel("org.eclipse.core.runtime")));
		assertEquals("2:false", targetBundles.get(findTargetModel("org.eclipse.ui")));
	}

	@Test
	public void testBundleBased() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("bundle-based.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateOsgiLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldOsgiPropertiesRemoved(wc);

		Map<IPluginModelBase, String> workspaceBundles = BundleLauncherHelper.getWorkspaceBundleMap(wc);
		assertEquals("default:true", workspaceBundles.get(findWorkspaceModel("org.eclipse.pde.plugin1")));
		assertEquals("3:false", workspaceBundles.get(findWorkspaceModel("org.eclipse.pde.plugin2")));

		Map<IPluginModelBase, String> targetBundles = BundleLauncherHelper.getTargetBundleMap(wc);
		assertEquals("default:true", targetBundles.get(findTargetModel("org.eclipse.core.runtime")));
		assertEquals("2:false", targetBundles.get(findTargetModel("org.eclipse.ui")));
	}

	@SuppressWarnings("deprecation")
	private void assertOldPropertiesRemoved(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		assertFalse("selected_workspace_plugins should not be present",
				wc.hasAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS));
		assertFalse("selected_target_plugins should not be present",
				wc.hasAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS));
	}

	@SuppressWarnings("deprecation")
	private void assertOldOsgiPropertiesRemoved(ILaunchConfiguration wc) throws CoreException {
		assertFalse("workspace_bundles should not be present",
				wc.hasAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES));
		assertFalse("target_bundles should not be present", wc.hasAttribute(IPDELauncherConstants.TARGET_BUNDLES));
	}

	private IPluginModelBase findWorkspaceModel(String id) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		assertNotNull("entry '" + id + "' should be present in PluginRegistry", entry);
		assertTrue("entry '" + id + "' should have workspace models", entry.hasWorkspaceModels());
		return entry.getWorkspaceModels()[0];
	}

	private IPluginModelBase findTargetModel(String id) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		assertNotNull("entry '" + id + "' should be present in PluginRegistry", entry);
		assertTrue("entry '" + id + "' should have external models", entry.hasExternalModels());
		return entry.getExternalModels()[0];
	}
}
