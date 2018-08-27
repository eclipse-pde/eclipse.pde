/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gamil.com> - bug 205361
 *******************************************************************************/
package org.eclipse.pde.ui.tests.wizards;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
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

	private final static String API_TOOLS_NATURE = "org.eclipse.pde.api.tools.apiAnalysisNature";

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
		assertFalse(project.hasNature(API_TOOLS_NATURE));

		convertProjects(new IProject[]{project}, false);

		assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
		assertFalse(project.hasNature(API_TOOLS_NATURE));
		assertTrue(PDEProject.getManifest(project).exists());
		assertTrue(PDEProject.getBuildProperties(project).exists());
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

		for (IProject project : projects) {
			assertNotNull(project);
			assertTrue(project.exists());
			assertFalse(project.hasNature(PDE.PLUGIN_NATURE));
			assertFalse(project.hasNature(API_TOOLS_NATURE));
		}

		convertProjects(projects, false);

		for (IProject project : projects) {
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertFalse(project.hasNature(API_TOOLS_NATURE));
			assertTrue(PDEProject.getManifest(project).exists());
			assertTrue(PDEProject.getBuildProperties(project).exists());
		}
	}

	/**
	 * Test the conversion of a project can add the api tools nature correctly
	 *
	 * @throws Exception
	 *             If there's a problem.
	 */
	public void testApiToolsSetup() throws Exception {

		IProject project1 = createProject(PROJECT_NAME_1);
		IProject project2 = createProject(PROJECT_NAME_2);

		IProject[] projects = new IProject[] {project1, project2};

		for (IProject project : projects) {
			assertNotNull(project);
			assertTrue(project.exists());
			assertFalse(project.hasNature(PDE.PLUGIN_NATURE));
			assertFalse(project.hasNature(API_TOOLS_NATURE));
		}

		convertProjects(projects, true);

		for (IProject project : projects) {
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertTrue(project.hasNature(API_TOOLS_NATURE));
			assertTrue(PDEProject.getManifest(project).exists());
			assertTrue(PDEProject.getBuildProperties(project).exists());
		}
	}

	/**
	 * Convert projects to a plugin projects
	 *
	 * @param projects
	 *            The projects to convert
	 * @param enableApiTools whether to enable the api tools nature on the projects
	 */
	private void convertProjects(IProject[] projects, boolean enableApiTools) {
		IRunnableWithProgress convertOperation;
		convertOperation = new ConvertProjectToPluginOperation(projects,enableApiTools);

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
