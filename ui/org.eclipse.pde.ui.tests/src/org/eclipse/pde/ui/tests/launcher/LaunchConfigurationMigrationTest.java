/**************************************************************************************
 *  Copyright (c) 2019, 2021 Andras Peteri and others.
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
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 **************************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class LaunchConfigurationMigrationTest extends AbstractLaunchTest {

	@BeforeClass
	public static void setupPluginProjects() throws Exception {
		ProjectUtils.createPluginProject("org.eclipse.pde.plugin1", "org.eclipse.pde.plugin1", null);
		ProjectUtils.createPluginProject("org.eclipse.pde.plugin2", "org.eclipse.pde.plugin2", null);
	}

	@Test
	public void testPluginBasedWithoutAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-without-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals("default:true", bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)));
		assertEquals("3:false", bundles.get(findWorkspaceModel("org.eclipse.pde.plugin2", null)));

		assertEquals("default:true", bundles.get(findTargetModel("org.eclipse.core.runtime", null)));
		assertEquals("2:false", bundles.get(findTargetModel("org.eclipse.ui", null)));
	}

	@Test
	public void testPluginBasedWithAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-with-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals("default:default", bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)));

		assertEquals("default:true", bundles.get(findTargetModel("org.eclipse.core.runtime", null)));
		assertEquals("2:false", bundles.get(findTargetModel("org.eclipse.ui", null)));
	}

	@Test
	public void testBundleBased() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("bundle-based.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateOsgiLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldOsgiPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals("default:true", bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)));
		assertEquals("3:false", bundles.get(findWorkspaceModel("org.eclipse.pde.plugin2", null)));

		assertEquals("default:true", bundles.get(findTargetModel("org.eclipse.core.runtime", null)));
		assertEquals("2:false", bundles.get(findTargetModel("org.eclipse.ui", null)));
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
}
