/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.target;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetDefinition;

/**
 * Tests for target definitions.  The tested targets will be backed by a workspace file.
 *
 * @see LocalTargetDefinitionTests
 * @since 3.5
 */
public class WorkspaceTargetDefinitionTests extends LocalTargetDefinitionTests {

	private static final String PROJECT_NAME = "WorkspaceTargetDefinitionTests";

	@Override
	protected void setUp() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!project.exists()){
			project.create(null);
		}
		assertTrue("Could not create test project",project.exists());
		project.open(null);
		assertTrue("Could not open test project", project.isOpen());
	}

	@Override
	protected void tearDown() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (project.exists()){
			project.delete(true, null);
		}
		assertFalse("Could not delete test project",project.exists());
	}

	@Override
	protected ITargetDefinition getNewTarget() {
		IFile target = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME)
				.getFile(Long.toString(System.currentTimeMillis()) + ".target");
		try {
			return getTargetService().getTarget(target).getTargetDefinition();
		} catch (CoreException e){
			fail(e.getMessage());
		}
		return null;
	}

}
