/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public abstract class PDETestCase extends TestCase {
	
	private static boolean FIRST_TEST = true;
	
	private static IWorkbench fWorkbench;

	protected final void setUp() throws Exception {
		if (FIRST_TEST) {
			fWorkbench = PlatformUI.getWorkbench();
			
			// set to false
			FIRST_TEST = false;
		}
	}
	
	protected IWorkbench getWorkbench() {
		return fWorkbench;
	}
	
	protected final void tearDown() {
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
