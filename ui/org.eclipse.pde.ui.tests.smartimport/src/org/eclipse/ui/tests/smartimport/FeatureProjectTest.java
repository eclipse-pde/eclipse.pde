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

public class FeatureProjectTest extends ProjectTestTemplate {

	private static final String PROJECT_NAME = "FeatureProject";

	@Override
	File getProjectPath() {
		return new File("target/resources/FeatureProject");
	}

	@Override
	void checkImportedProject() throws CoreException {
		IProject project = getProject();
		String[] natureIds = project.getDescription().getNatureIds();
		assertEquals("Project should have exactly 1 nature", 1, natureIds.length);
		assertEquals("Project should have feature nature", "org.eclipse.pde.FeatureNature", natureIds[0]);
	}

	@Override
	IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
	}

}
