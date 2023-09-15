/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class PlainEclipseProjectTest extends ProjectTestTemplate {

	private static final String PLAIN_ECLIPSE_PROJECT = "PlainEclipseProject";

	@Override
	File getProjectPath() {
		return new File("target/resources/PlainEclipseProject");
	}

	@Override
	IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PLAIN_ECLIPSE_PROJECT);
	}

	@Override
	void checkImportedProject() throws CoreException {
		IProject project = getProject();
		String[] natureIds = project.getDescription().getNatureIds();
		assertEquals("This project should not have any nature", 0, natureIds.length);
	}
}
