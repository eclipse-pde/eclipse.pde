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


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public abstract class NewProjectTest extends PDETestCase {
	
	protected void verifyProjectExistence() {
		assertTrue("Project does not exist", getProject().exists());
	}
	
	protected IProject getProject() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getProject(getProjectName());	
	}
	
	protected boolean hasNature(String nature) {
		boolean hasNature = false;
		try {
			hasNature = getProject().hasNature(nature);
		} catch (CoreException e) {
		}
		return hasNature;
	}
	
	protected abstract String getProjectName();


}
