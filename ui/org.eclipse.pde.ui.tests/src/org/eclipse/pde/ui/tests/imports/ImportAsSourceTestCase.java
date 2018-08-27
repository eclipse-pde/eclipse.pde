/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.imports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportAsSourceTestCase extends BaseImportTestCase {

	private static int TYPE = PluginImportOperation.IMPORT_WITH_SOURCE;

	@Override
	protected int getType() {
		return TYPE;
	}

	@Override
	protected void verifyProject(String projectName, boolean isJava) {
		try {
			IProject project = verifyProject(projectName);
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertEquals(isJava, project.hasNature(JavaCore.NATURE_ID));
			if (isJava) {
				IJavaProject jProject = JavaCore.create(project);
				assertTrue(checkSourceAttached(jProject));
				assertTrue(checkSourceFolder(jProject));
			}
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}

	private boolean checkSourceFolder(IJavaProject jProject) throws JavaModelException {
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
				return true;
		}
		return false;
	}
}
