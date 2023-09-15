/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat, Inc. and others.
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class ProjectTestTemplate {

	public ProjectTestTemplate() {
	}

	@After
	public void cleanup() throws CoreException, FileNotFoundException, IOException {
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			p.delete(true, new NullProgressMonitor());
		}
		// empty error log
		new FileWriter(Platform.getLogFileLocation().toFile(), false).close();
	}

	@BeforeClass
	public static void setupClass() {
		// empty error log
		try {
			new FileWriter(Platform.getLogFileLocation().toFile(), false).close();
		} catch (IOException e) {
			// ignore
		}
	}

	@Test
	@SuppressWarnings("restriction")
	public void testImport() throws CoreException, InterruptedException, IOException {
		SmartImportJob job = new SmartImportJob(getProjectPath(), null, true, false);
		job.run(new NullProgressMonitor());
		job.join();

		// check imported project
		checkErrorLog();
		checkProblemsView();

		checkImportedProject();
	}

	private void checkErrorLog() throws IOException {
		String log = Files.readString(Platform.getLogFileLocation().toFile().toPath());
		assertTrue(log.isEmpty());
	}

	private void checkProblemsView() throws CoreException {
		IProject project = getProject();
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

	abstract File getProjectPath();

	abstract IProject getProject();

	/**
	 * Checks whether the project was imported correctly.
	 *
	 */
	abstract void checkImportedProject() throws CoreException;

}
