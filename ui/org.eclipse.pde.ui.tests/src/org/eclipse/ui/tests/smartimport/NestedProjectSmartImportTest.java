/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * A .project below a bundle or feature is a test fixture and must not be
 * proposed by the Smart Import wizard, while nested projects below a plain
 * project are still found.
 */
@RunWith(Parameterized.class)
public class NestedProjectSmartImportTest {

	@Parameters(name = "{0}")
	public static Object[][] projects() {
		return new Object[][] { //
			{ "BundleWithNestedProject", List.of(""), List.of("BundleWithNestedProject") }, //
			{ "FeatureWithNestedProject", List.of(""), List.of("FeatureWithNestedProject") }, //
			{ "PlainProjectWithNestedProject", List.of("", "nested/NestedProject"),
					List.of("PlainProjectWithNestedProject", "NestedProject") }, //
		};
	}

	@ClassRule
	public static TemporaryFolder workingDirectory = new TemporaryFolder();

	@Parameter(0)
	public String rootName;
	@Parameter(1)
	public List<String> expectedProposals;
	@Parameter(2)
	public List<String> expectedProjects;

	@BeforeClass
	public static void setupClass() throws Exception {
		PDETestCase.copyFromThisBundleInto("tests/smartImport", workingDirectory.getRoot().toPath());
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@After
	public void cleanup() throws Exception {
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@Test
	public void testNestedProjectsBelowBundlesAreNotImported() throws Exception {
		File root = new File(workingDirectory.getRoot(), rootName);
		SmartImportJob job = new SmartImportJob(root, null, true, true);

		Map<File, List<ProjectConfigurator>> proposals = job.getImportProposals(new NullProgressMonitor());

		Path rootPath = root.toPath().toAbsolutePath().normalize();
		List<Path> proposedPaths = proposals.keySet().stream().map(file -> file.toPath().toAbsolutePath().normalize())
				.toList();
		assertThat(proposedPaths).containsExactlyInAnyOrderElementsOf(expectedProposals.stream().map(rootPath::resolve).toList());

		job.setDirectoriesToImport(proposals.keySet());
		job.run(new NullProgressMonitor());
		job.join();

		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		List<String> projectNames = Arrays.stream(workspace.getProjects()).map(IProject::getName).toList();
		assertThat(projectNames).containsExactlyInAnyOrderElementsOf(expectedProjects);
	}

}
