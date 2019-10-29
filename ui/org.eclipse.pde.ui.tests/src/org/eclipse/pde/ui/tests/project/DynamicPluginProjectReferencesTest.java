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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;

public class DynamicPluginProjectReferencesTest {

	@After
	public void clearWorkspace() throws Exception {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(false, true, null);
		}
	}

	@Test
	public void testFragmentHost_required() throws Exception {
		checkHostToFragmentDependencies("required");
	}

	@Test
	public void testFragmentHost_imported() throws Exception {
		checkHostToFragmentDependencies("imported");
	}

	private void checkHostToFragmentDependencies(String type) throws IOException, CoreException {
		IProject host = ProjectUtils.importTestProject("tests/projects/host-fragment-" + type + "/bundle");
		ProjectUtils.importTestProject("tests/projects/host-fragment-" + type + "/testfragment");
		ProjectUtils.importTestProject("tests/projects/host-fragment-" + type + "/testframework");

		IBuildConfiguration[] referenced = host.getReferencedBuildConfigs(host.getActiveBuildConfig().getName(), false);
		assertThat(referenced, is(emptyArray()));
	}

	@Test
	public void testHopViaTargetPlatform_required() throws Exception {
		IPluginModelBase osgiModel = PluginRegistry.findModel("org.eclipse.osgi");
		IPluginModelBase coreCommandsModel = PluginRegistry.findModel("org.eclipse.core.commands");

		PluginImportOperation operation = new PluginImportOperation(
				new IPluginModelBase[] { osgiModel, coreCommandsModel }, PluginImportOperation.IMPORT_BINARY, false);
		operation.schedule();
		operation.join();

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject coreCommandsProject = root.getProject(coreCommandsModel.getPluginBase().getId());
		IProject osgiProject = root.getProject(osgiModel.getPluginBase().getId());

		IBuildConfiguration[] referencedBuildConfigs = coreCommandsProject
				.getReferencedBuildConfigs(coreCommandsProject.getActiveBuildConfig().getName(), false);
		for (IBuildConfiguration reference : referencedBuildConfigs) {
			if (reference.getProject() == osgiProject)
				return;
		}

		Assert.fail("references should include local osgi project: " + Arrays.toString(referencedBuildConfigs));
	}

	@Test
	public void testHopViaTargetPlatform_imported() throws Exception {
		IPluginModelBase osgiModel = PluginRegistry.findModel("org.eclipse.osgi");

		PluginImportOperation operation = new PluginImportOperation(new IPluginModelBase[] { osgiModel },
				PluginImportOperation.IMPORT_BINARY, false);
		operation.schedule();
		operation.join();

		IProject importingTargetBundle = ProjectUtils.importTestProject("tests/projects/importing.targetbundle");

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject osgiProject = root.getProject(osgiModel.getPluginBase().getId());

		IBuildConfiguration[] referencedBuildConfigs = importingTargetBundle
				.getReferencedBuildConfigs(importingTargetBundle.getActiveBuildConfig().getName(), false);
		for (IBuildConfiguration reference : referencedBuildConfigs) {
			if (reference.getProject() == osgiProject)
				return;
		}

		Assert.fail("references should include local osgi project: " + Arrays.toString(referencedBuildConfigs));
	}

	@Test
	public void testIgnoreCapabilities() throws Exception {
		ProjectUtils.importTestProject("tests/projects/capabilities/capabilities.provider");
		IProject consumer = ProjectUtils.importTestProject("tests/projects/capabilities/capabilities.consumer");

		IBuildConfiguration[] referenced = consumer.getReferencedBuildConfigs(consumer.getActiveBuildConfig().getName(),
				false);
		assertThat(referenced, is(emptyArray()));
	}

}
