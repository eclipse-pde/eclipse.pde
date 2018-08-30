/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.api.tools.util.tests.ResourceEventWaiter;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;

/**
 * Abstract class with commonly used methods for API Tools tests
 *
 * @since 1.0.0
 */
public class AbstractApiTest {

	/**
	 * Constant representing the name of the testing project created for plugin
	 * tests. Value is: <code>APITests</code>.
	 */
	protected static final String TESTING_PROJECT_NAME = "APITests"; //$NON-NLS-1$

	/**
	 * Constant representing the name of the testing plugin project created for
	 * plugin tests. Value is: <code>APIPluginTests</code>.
	 */
	protected static final String TESTING_PLUGIN_PROJECT_NAME = "APIPluginTests"; //$NON-NLS-1$

	/**
	 * Returns the {@link IJavaProject} with the given name. If this method is
	 * called from a non-plugin unit test, <code>null</code> is always returned.
	 *
	 * @return the {@link IJavaProject} with the given name or <code>null</code>
	 */
	protected IJavaProject getTestingJavaProject(String name) {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		if (ws != null) {
			IProject pro = ws.getRoot().getProject(name);
			if (pro.exists()) {
				return JavaCore.create(pro);
			}
		}
		return null;
	}

	/**
	 * Returns the {@link IApiComponent} for the given project name or
	 * <code>null</code> if it does not exist
	 *
	 * @param projectname the name of the project
	 * @return the {@link IApiComponent} for the given project name or
	 *         <code>null</code>
	 */
	protected IApiComponent getProjectApiComponent(String projectname) {
		IJavaProject project = getTestingJavaProject(projectname);
		assertNotNull("the project " + projectname + " must exist", project); //$NON-NLS-1$ //$NON-NLS-2$
		IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
		assertNotNull("the workspace profile must exist", profile); //$NON-NLS-1$
		return profile.getApiComponent(project.getElementName());
	}

	/**
	 * Performs the given refactoring
	 *
	 * @param refactoring
	 * @throws Exception
	 */
	protected void performRefactoring(final Refactoring refactoring) throws CoreException {
		if (refactoring == null) {
			return;
		}
		NullProgressMonitor monitor = new NullProgressMonitor();
		CreateChangeOperation create = new CreateChangeOperation(refactoring);
		refactoring.checkFinalConditions(monitor);
		PerformChangeOperation perform = new PerformChangeOperation(create);
		ResourcesPlugin.getWorkspace().run(perform, monitor);
	}

	/**
	 * Wait for autobuild notification to occur
	 */
	public static void waitForAutoBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}

	/**
	 * Creates a project with the given name and adds the default 'src' folder
	 *
	 * @param name
	 * @param packages an optional list of packages to add to the project when
	 *            it is created
	 * @throws Exception
	 */
	protected void createProject(final String name, String[] packages) throws Exception {
		if (name == null) {
			return;
		}

		// create project and import source
		IJavaProject jproject = ProjectUtils.createPluginProject(name, new String[] {
				PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID });
		assertNotNull("The java project must have been created", jproject); //$NON-NLS-1$

		IPackageFragmentRoot root = jproject.getPackageFragmentRoot(jproject.getProject().getFolder(ProjectUtils.SRC_FOLDER));
		assertTrue("the src root must have been created", root.exists()); //$NON-NLS-1$
		if (packages != null) {
			IPackageFragment fragment = null;
			for (String package1 : packages) {
				fragment = root.createPackageFragment(package1, true, new NullProgressMonitor());
				assertNotNull("the package fragment " + package1 + " cannot be null", fragment); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		PluginRegistry.getWorkspaceModels();

		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline cannot be null", baseline); //$NON-NLS-1$

		// This assertion caused intermittant failures, skipping it hasn't
		// caused any problems in the tests (Bug 368458)
		// IApiComponent component = baseline.getApiComponent(name);
		// assertNotNull("the test project api component must exist in the workspace baseline",
		// component);

	}

	/**
	 * Deletes a project with the given name
	 *
	 * @param name
	 * @throws CoreException
	 */
	protected void deleteProject(String name) throws CoreException {
		if (name == null) {
			return;
		}
		getWorkspaceBaseline().dispose();
		IProject pro = getProject(name);
		if (pro.exists()) {
			ResourceEventWaiter waiter = new ResourceEventWaiter(new Path(name), IResourceChangeEvent.POST_CHANGE,
					IResourceDelta.CHANGED, 0);
			pro.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the project delete event did not arrive", obj); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a the project with the given name. The returned project has not
	 * been checked for existence.
	 *
	 * @param name
	 * @return the handle to the project with the given name.
	 */
	protected IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	/**
	 * Returns the workspace baseline.
	 *
	 * @return workspace baseline
	 */
	protected IApiBaseline getWorkspaceBaseline() {
		return ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}
}
