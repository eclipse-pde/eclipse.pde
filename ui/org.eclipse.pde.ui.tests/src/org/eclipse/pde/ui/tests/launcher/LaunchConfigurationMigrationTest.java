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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LaunchConfigurationMigrationTest extends AbstractLaunchTest {

	@BeforeAll
	public static void setupPluginProjects() throws Exception {
		ProjectUtils.createPluginProject("org.eclipse.pde.plugin1", "org.eclipse.pde.plugin1", "0.0.0");
		ProjectUtils.createPluginProject("org.eclipse.pde.plugin2", "org.eclipse.pde.plugin2", "0.0.0");
	}

	@Test
	public void testPluginBasedWithoutAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-without-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals(bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)), "default:true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(bundles.get(findWorkspaceModel("org.eclipse.pde.plugin2", null)), "3:false"); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(bundles.get(findTargetModel("org.eclipse.core.runtime", null)), "default:true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(bundles.get(findTargetModel("org.eclipse.ui", null)), "2:false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPluginBasedWithAutomaticAdd() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("plugin-based-with-automatic-add.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals(bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)), "default:default"); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(bundles.get(findTargetModel("org.eclipse.core.runtime", null)), "default:true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(bundles.get(findTargetModel("org.eclipse.ui", null)), "2:false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBundleBased() throws Exception {
		ILaunchConfiguration configuration = getLaunchConfiguration("bundle-based.launch");

		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		BundleLauncherHelper.migrateOsgiLaunchConfiguration(wc);
		assertTrue(wc.isDirty());

		assertOldOsgiPropertiesRemoved(wc);

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getAllSelectedPluginBundles(wc);
		assertEquals(bundles.get(findWorkspaceModel("org.eclipse.pde.plugin1", null)), "default:true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(bundles.get(findWorkspaceModel("org.eclipse.pde.plugin2", null)), "3:false"); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(bundles.get(findTargetModel("org.eclipse.core.runtime", null)), "default:true"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(bundles.get(findTargetModel("org.eclipse.ui", null)), "2:false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@SuppressWarnings("deprecation")
	private void assertOldPropertiesRemoved(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		assertFalse(wc.hasAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS), "selected_workspace_plugins should not be present"); //$NON-NLS-1$
		assertFalse(wc.hasAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS), "selected_target_plugins should not be present"); //$NON-NLS-1$
	}

	@SuppressWarnings("deprecation")
	private void assertOldOsgiPropertiesRemoved(ILaunchConfiguration wc) throws CoreException {
		assertFalse(wc.hasAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES), "workspace_bundles should not be present"); //$NON-NLS-1$
		assertFalse(wc.hasAttribute(IPDELauncherConstants.TARGET_BUNDLES), "target_bundles should not be present"); //$NON-NLS-1$
	}
}
