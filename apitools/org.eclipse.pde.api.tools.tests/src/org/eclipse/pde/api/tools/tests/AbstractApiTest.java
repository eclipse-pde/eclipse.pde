/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;

/**
 * Abstract class with commonly used methods for API Tooling tests
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
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
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
}
