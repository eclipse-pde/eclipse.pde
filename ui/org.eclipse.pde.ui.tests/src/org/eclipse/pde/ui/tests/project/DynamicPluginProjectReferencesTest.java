/*******************************************************************************
 *  Copyright (c) 2019, 2021 Julian Honnen
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


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class DynamicPluginProjectReferencesTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

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
		assertThat(referenced).isEmpty();
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
			if (reference.getProject() == osgiProject) {
				return;
			}
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
			if (reference.getProject() == osgiProject) {
				return;
			}
		}

		Assert.fail("references should include local osgi project: " + Arrays.toString(referencedBuildConfigs));
	}

	@Test
	public void testIgnoreCapabilities() throws Exception {
		ProjectUtils.importTestProject("tests/projects/capabilities/capabilities.provider");
		IProject consumer = ProjectUtils.importTestProject("tests/projects/capabilities/capabilities.consumer");

		IBuildConfiguration[] referenced = consumer.getReferencedBuildConfigs(consumer.getActiveBuildConfig().getName(),
				false);
		assertThat(referenced).isEmpty();
	}

}
