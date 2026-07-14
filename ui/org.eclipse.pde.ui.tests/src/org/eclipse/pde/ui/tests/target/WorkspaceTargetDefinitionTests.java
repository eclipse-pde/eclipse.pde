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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	/** {@code backingFile} resolves file-backed handles and returns {@code null} otherwise. */
	@Test
	public void testBackingFile() throws Exception {
		// Workspace file handle resolves to the file inside the project.
		IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME)
				.getFile("backing.target");
		ITargetHandle workspaceHandle = getTargetService().getTarget(workspaceFile);
		assertEquals(workspaceFile.getLocation().toFile(), TargetPlatformService.backingFile(workspaceHandle));

		// External file handle resolves to the file the URI points at.
		Path external = Files.createTempFile("pde-backing", ".target");
		try {
			URI uri = external.toUri();
			ITargetHandle externalHandle = getTargetService().getTarget(uri);
			assertEquals(external.toFile(), TargetPlatformService.backingFile(externalHandle));
		} finally {
			Files.deleteIfExists(external);
		}

		// A fresh (local metadata) target is not backed by a file.
		ITargetHandle localHandle = getTargetService().newTarget().getHandle();
		assertNull(TargetPlatformService.backingFile(localHandle));
	}

	/** Editing the active target's workspace file triggers an automatic reload. */
	@Test
	public void testReloadOnWorkspaceFileChange() throws Exception {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME).getFile("autoreload.target");
		ITargetHandle handle = getTargetService().getTarget(file);
		ITargetDefinition definition = handle.getTargetDefinition();
		definition.setName("initial-name");
		getTargetService().saveTargetDefinition(definition);
		try {
			setTargetPlatform(definition);
			assertEquals("initial-name", getTargetService().getWorkspaceTargetDefinition().getName());

			// Edit the backing file. The service watches it and should reload.
			ITargetDefinition edited = handle.getTargetDefinition();
			edited.setName("changed-name");
			getTargetService().saveTargetDefinition(edited);

			assertEquals("Target platform was not reloaded after its file changed", "changed-name",
					waitForReloadedName("changed-name"));
		} finally {
			resetTargetPlatform();
		}
	}

	/** Waits for the debounced reload to set the active target's name, returning what was observed. */
	private String waitForReloadedName(String expected) throws CoreException {
		long deadline = System.currentTimeMillis() + 30000;
		String actual;
		do {
			// minTime must exceed the reload coalescing delay so the debounced
			// load is scheduled before we wait for it to finish.
			TestUtils.waitForJobs(name.getMethodName(), 700, 10000);
			ITargetDefinition current = getTargetService().getWorkspaceTargetDefinition();
			actual = current == null ? null : current.getName();
		} while (!expected.equals(actual) && System.currentTimeMillis() < deadline);
		return actual;
	}

}
