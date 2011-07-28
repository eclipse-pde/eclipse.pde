/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import junit.framework.TestCase;

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
import org.eclipse.pde.internal.core.natures.PDE;

/**
 * Abstract class with commonly used methods for API Tools tests
 * 
 * @since 1.0.0 
 */
public class AbstractApiTest extends TestCase {	
	
	/**
	 * Constant representing the name of the testing project created for plugin tests.
	 * Value is: <code>APITests</code>.
	 */
	protected static final String TESTING_PROJECT_NAME = "APITests";
	
	/**
	 * Constant representing the name of the testing plugin project created for plugin tests.
	 * Value is: <code>APIPluginTests</code>.
	 */
	protected static final String TESTING_PLUGIN_PROJECT_NAME = "APIPluginTests";
	
	/**
	 * Returns the {@link IJavaProject} with the given name. If this method
	 * is called from a non-plugin unit test, <code>null</code> is always returned.
	 * @return the {@link IJavaProject} with the given name or <code>null</code>
	 */
	protected IJavaProject getTestingJavaProject(String name) {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		if(ws != null) {
			IProject pro = ws.getRoot().getProject(name);
			if(pro.exists()) {
				return JavaCore.create(pro);
			}
		}
		return null;
	}
	
	/**
	 * Returns the {@link IApiComponent} for the given project name or <code>null</code> if it does not exist
	 * @param projectname the name of the project
	 * @return the {@link IApiComponent} for the given project name or <code>null</code>
	 */
	protected IApiComponent getProjectApiComponent(String projectname) {
		IJavaProject project = getTestingJavaProject(projectname);
		assertNotNull("the project " + projectname + " must exist", project);
		IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
		assertNotNull("the workspace profile must exist", profile);
		return profile.getApiComponent(project.getElementName());
	}
	
	/**
	 * Performs the given refactoring
	 * @param refactoring
	 * @throws Exception
	 */
	protected void performRefactoring(final Refactoring refactoring) throws CoreException {
		if(refactoring == null) {
			return;
		}
		NullProgressMonitor monitor = new NullProgressMonitor();
		CreateChangeOperation create= new CreateChangeOperation(refactoring);
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
	 * @param name
	 * @param packages an optional list of packages to add to the project when it is created
	 * @throws Exception
	 */
	protected void createProject(String name, String[] packages) throws Exception {
		if(name == null) {
			return;
		}
        // create project and import source
        IJavaProject jproject = ProjectUtils.createPluginProject(name, new String[] {PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID});
        assertNotNull("The java project must have been created", jproject);
        IPackageFragmentRoot root = jproject.getPackageFragmentRoot(jproject.getProject().getFolder(ProjectUtils.SRC_FOLDER));
        assertTrue("the src root must have been created", root.exists());
        if(packages != null) {
	        IPackageFragment fragment = null;
	        for (int i = 0; i < packages.length; i++) {
	        	fragment = root.createPackageFragment(packages[i], true, new NullProgressMonitor());
	    		assertNotNull("the package fragment "+packages[i]+" cannot be null", fragment);
			}
        }
         
        IApiBaseline baseline = getWorkspaceBaseline();
        assertNotNull("the workspace baseline cannot be null", baseline);
        IApiComponent component = baseline.getApiComponent(name);
        assertNotNull("the test project api component must exist in the workspace baseline", component);
	}
	
	/**
	 * Deletes a project with the given name
	 * @param name
	 */
	protected void deleteProject(String name) {
		if(name == null) {
			return;
		}
		getWorkspaceBaseline().dispose();
		try {
	        IProject pro = getProject(name);
	        if (pro.exists()) {
	        	ResourceEventWaiter waiter = new ResourceEventWaiter(new Path(name), IResourceChangeEvent.POST_CHANGE, IResourceDelta.CHANGED, 0);
	            pro.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, new NullProgressMonitor());
	            Object obj = waiter.waitForEvent();
	            assertNotNull("the project delete event did not arrive", obj);
	        }
		}
		catch(Exception e) {
			System.err.println("tearDown failed");
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Returns a the project with the given name. The returned project has not been checked
	 * for existence.
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
