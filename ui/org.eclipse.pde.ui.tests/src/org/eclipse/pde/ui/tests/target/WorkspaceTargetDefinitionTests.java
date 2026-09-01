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

import org.junit.jupiter.api.TestInfo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Tests for target definitions.  The tested targets will be backed by a workspace file.
 *
 * @see LocalTargetDefinitionTests
 * @since 3.5
 */
public class WorkspaceTargetDefinitionTests extends LocalTargetDefinitionTests {

	private static final String PROJECT_NAME = "WorkspaceTargetDefinitionTests";

	@Override
	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!project.exists()){
			project.create(null);
		}
		assertTrue(project.exists(), "Could not create test project"); //$NON-NLS-1$
		project.open(null);
		assertTrue(project.isOpen(), "Could not open test project"); //$NON-NLS-1$
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (project.exists()){
			project.delete(true, null);
		}
		assertFalse(project.exists(), "Could not delete test project"); //$NON-NLS-1$
		super.tearDown();
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
