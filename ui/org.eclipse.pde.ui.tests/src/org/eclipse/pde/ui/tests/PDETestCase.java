/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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

	@Override
	protected void tearDown() {
		// Close any editors we opened
		IWorkbenchWindow[] workbenchPages = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow workbenchPage : workbenchPages) {
			IWorkbenchPage page = workbenchPage.getActivePage();
			if (page != null){
				page.closeAllEditors(false);
			}
		}

		// Delete any projects that were created
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (IProject project : projects) {
				project.delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
		}
	}
}
