/*******************************************************************************
 *  Copyright (c) 2026 vogella GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.natures.SiteProject;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Builds a project that lists a PDE builder without having a PDE nature, in a
 * workspace without PDE projects, under the workspace root rule that auto-builds
 * and build jobs hold.
 */
public class PDEBuilderSchedulingRuleTest {

	@Rule
	public final TestRule clearWorkspace = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Test
	public void testManifestBuilder() throws CoreException {
		buildUnderRootRule(PluginProject.MANIFEST_BUILDER_ID);
	}

	/** A preceding builder makes the build rule non-empty, so the PDE builder's own rule is what fails. */
	@Test
	public void testManifestBuilderAfterJavaBuilder() throws CoreException {
		buildUnderRootRule(JavaCore.BUILDER_ID, PluginProject.MANIFEST_BUILDER_ID);
	}

	@Test
	public void testSchemaBuilder() throws CoreException {
		buildUnderRootRule(PluginProject.SCHEMA_BUILDER_ID);
	}

	@Test
	public void testFeatureBuilder() throws CoreException {
		buildUnderRootRule(FeatureProject.BUILDER_ID);
	}

	@Test
	public void testUpdateSiteBuilder() throws CoreException {
		buildUnderRootRule(SiteProject.BUILDER_ID);
	}

	private static void buildUnderRootRule(String... builderIds) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject("noPdeNature");
		project.create(null);
		project.open(null);
		IProjectDescription description = project.getDescription();
		ICommand[] commands = new ICommand[builderIds.length];
		for (int i = 0; i < builderIds.length; i++) {
			commands[i] = description.newCommand();
			commands[i].setBuilderName(builderIds[i]);
		}
		description.setBuildSpec(commands);
		project.setDescription(description, IResource.FORCE, null);

		workspace.run(monitor -> project.build(IncrementalProjectBuilder.FULL_BUILD, monitor), workspace.getRoot(),
				IWorkspace.AVOID_UPDATE, null);
	}
}
