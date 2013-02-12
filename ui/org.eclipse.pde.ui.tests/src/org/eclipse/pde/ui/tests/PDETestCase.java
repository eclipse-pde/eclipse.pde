/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests;

import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.*;

/**
 * Provides a default {@link #tearDown()} implementation to delete all
 * projects in the workspace.
 *
 */
public abstract class PDETestCase extends TestCase {

	protected void tearDown() {
		// Close any editors we opened
		IWorkbenchWindow[] workbenchPages = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < workbenchPages.length; i++) {
			IWorkbenchPage page = workbenchPages[i].getActivePage();
			if (page != null){
				page.closeAllEditors(false);
			}
		}
		
		// Delete any projects that were created
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (int i = 0; i < projects.length; i++) {
				projects[i].delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
		}
	}
}
