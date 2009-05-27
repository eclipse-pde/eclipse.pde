/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gamil.com> - bug 205361
 *******************************************************************************/
package org.eclipse.pde.ui.tests.wizards;

import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.tools.ConvertProjectToPluginOperation;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Test case to test the conversion of projects to plug-in projects
 */
public class ConvertProjectToPluginTestCase extends PDETestCase {

	private static String PROJECT_NAME_1 = "Foo";
	private static String PROJECT_NAME_2 = "Bar";

	public static Test suite() {
		return new TestSuite(ConvertProjectToPluginTestCase.class);
	}

	/**
	 * Test the conversion of a single simple project.
	 * 
	 * @throws Exception
	 *             If there's a problem.
	 */
	public void testSingleProject() throws Exception {

		IProject project = createProject(PROJECT_NAME_1);

		assertNotNull(project);
		assertTrue(project.exists());
		assertFalse(project.hasNature(PDE.PLUGIN_NATURE));

		convertProject(project);

		assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
		assertTrue(project.getFile(ICoreConstants.MANIFEST_PATH).exists());
		assertTrue(project.getFile(ICoreConstants.BUILD_PROPERTIES_PATH).exists());
	}

	/**
	 * Test the conversion of a couple of simple projects.
	 * 
	 * @throws Exception
	 *             If there's a problem.
	 */
	public void testMultipleProjects() throws Exception {

		IProject project1 = createProject(PROJECT_NAME_1);
		IProject project2 = createProject(PROJECT_NAME_2);

		IProject[] projects = new IProject[] {project1, project2};

		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			assertNotNull(project);
			assertTrue(project.exists());
			assertFalse(project.hasNature(PDE.PLUGIN_NATURE));
		}

		convertProjects(projects);

		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertTrue(project.getFile(ICoreConstants.MANIFEST_PATH).exists());
			assertTrue(project.getFile(ICoreConstants.BUILD_PROPERTIES_PATH).exists());
		}
	}

	/**
	 * Convert a project to a plugin project
	 * 
	 * @param project
	 *            The project to convert
	 */
	private void convertProject(IProject project) {
		convertProjects(new IProject[] {project});
	}

	/**
	 * Convert projects to a plugin projects
	 * 
	 * @param projects
	 *            The projects to convert
	 */
	private void convertProjects(IProject[] projects) {
		IRunnableWithProgress convertOperation;
		convertOperation = new ConvertProjectToPluginOperation(projects);

		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		try {
			progressService.runInUI(progressService, convertOperation, null);
		} catch (InvocationTargetException e) {
			fail("Plug-in project conversion failed...");
		} catch (InterruptedException e) {
			fail("Plug-in project conversion failed...");
		}

	}

	/**
	 * Create a simple project of the specified name
	 * 
	 * @param name
	 *            The name of the project to be created
	 * @return The project instance created
	 * @throws CoreException
	 *             thrown if there was a problem during creation
	 */
	private IProject createProject(String name) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		return project;

	}

}
