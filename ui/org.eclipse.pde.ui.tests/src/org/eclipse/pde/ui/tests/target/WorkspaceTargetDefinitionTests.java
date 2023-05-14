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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.junit.After;
import org.junit.Before;

/**
 * Tests for target definitions.  The tested targets will be backed by a workspace file.
 *
 * @see LocalTargetDefinitionTests
 * @since 3.5
 */
public class WorkspaceTargetDefinitionTests extends LocalTargetDefinitionTests {

	private static final String PROJECT_NAME = "WorkspaceTargetDefinitionTests";

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!project.exists()){
			project.create(null);
		}
		assertTrue("Could not create test project",project.exists());
		project.open(null);
		assertTrue("Could not open test project", project.isOpen());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (project.exists()){
			project.delete(true, null);
		}
		assertFalse("Could not delete test project",project.exists());
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
