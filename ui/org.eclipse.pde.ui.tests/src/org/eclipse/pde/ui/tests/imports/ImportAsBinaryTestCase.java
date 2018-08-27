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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportAsBinaryTestCase extends BaseImportTestCase {

	private static int TYPE = PluginImportOperation.IMPORT_BINARY;

	@Override
	protected int getType() {
		return TYPE;
	}

	public void testImportAnt() {
		// Note: Ant is exempt from importing as source
		doSingleImport("org.apache.ant", true);
	}

	@Override
	public void testImportJUnit4() {
		// Note: JUnit 4 does not have source but it is a java project
		doSingleImport("org.junit", 4, true);
	}

	@Override
	protected void verifyProject(String projectName, boolean isJava) {
		try {
			IProject project = verifyProject(projectName);
			// When self hosting the tests, import tests may fail if you have the imported project in the host
			if (!Platform.inDevelopmentMode()) {
				assertEquals(PDECore.BINARY_PROJECT_VALUE, project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY));
			}
			assertTrue(project.hasNature(PDE.PLUGIN_NATURE));
			assertEquals(isJava, project.hasNature(JavaCore.NATURE_ID));
			if (isJava) {
				IJavaProject jProject = JavaCore.create(project);
				assertTrue(checkSourceAttached(jProject));
				assertTrue(checkLibraryEntry(jProject));
			}
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}

	private boolean checkLibraryEntry(IJavaProject jProject) throws JavaModelException {
		// When self hosting the tests, import tests may fail if you have the imported project in the host
		if (Platform.inDevelopmentMode()) {
			return true;
		}

		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				return true;
		}
		return false;
	}

}
