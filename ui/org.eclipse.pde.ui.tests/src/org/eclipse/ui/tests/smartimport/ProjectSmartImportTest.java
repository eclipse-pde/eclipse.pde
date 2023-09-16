/*******************************************************************************
 * Copyright (c) 2018, 2023 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProjectSmartImportTest {

	@Parameters(name = "{0}")
	public static Object[][] projects() {
		return new Object[][] { //
				{ "JavaEclipseProject", List.of("org.eclipse.jdt.core.javanature") }, //
				{ "FeatureProject", List.of("org.eclipse.pde.FeatureNature") }, //
				{ "PlainEclipseProject", List.of() }, //
				{ "PlainJavaProject", List.of("org.eclipse.jdt.core.javanature") }, //
		};
	}

	@ClassRule
	public static TemporaryFolder workingDirectory = new TemporaryFolder();

	@Parameter(0)
	public String projectName;
	@Parameter(1)
	public List<String> expectedNatures;

	@BeforeClass
	public static void setupClass() throws Exception {
		// Copy imported projects to temp-directory to not pollute this project
		// and have it unzipped for I-build tests
		PDETestCase.copyFromThisBundleInto("tests/smartImport", workingDirectory.getRoot().toPath());
		Files.writeString(getErrorLogFile(), ""); // empty error log
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@After
	public void cleanup() throws IOException, CoreException {
		Files.writeString(getErrorLogFile(), ""); // empty error log
		ProjectUtils.deleteAllWorkspaceProjects();
	}

	private static Path getErrorLogFile() {
		return Platform.getLogFileLocation().toFile().toPath();
	}

	@Test
	public void testImport() throws CoreException, InterruptedException {
		File projectPath = new File(workingDirectory.getRoot(), projectName);

		var job = new org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob(projectPath, null, true, false);
		job.run(new NullProgressMonitor());
		job.join();

		// check imported project
		assertThat(getErrorLogFile()).isEmptyFile();
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		assertThat(workspace.getProjects()).hasSize(1).allMatch(p -> p.getName().equals(projectName));
		IProject project = workspace.getProject(projectName);
		checkProblemsView(project);

		assertThat(project).satisfies(new Condition<>(IProject::isOpen, "is open"));

		assertThat(project.getDescription().getNatureIds()).containsExactlyElementsOf(expectedNatures);
	}

	private void checkProblemsView(IProject project) throws CoreException {
		IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		List<IMarker> errorMarkers = Arrays.asList(problems).stream().filter(m -> {
			try {
				return m.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_ERROR);
			} catch (CoreException e) {
				return false;
			}
		}).toList();
		assertTrue(
				"There should be no errors in imported project: " + System.lineSeparator() + errorMarkers.stream()
						.map(String::valueOf).collect(Collectors.joining(System.lineSeparator())),
				errorMarkers.isEmpty());

	}

}
