/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.wizards;

import org.eclipse.pde.ui.tests.PDETestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public abstract class NewProjectTestCase extends PDETestCase {

	protected void verifyProjectExistence() {
		assertTrue("Project does not exist", getProject().exists()); //$NON-NLS-1$
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
