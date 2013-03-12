/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.imports;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportAsSourceTestCase extends BaseImportTestCase {

	private static int TYPE = PluginImportOperation.IMPORT_WITH_SOURCE;

	public static Test suite() {
		return new TestSuite(ImportAsSourceTestCase.class);
	}

	protected int getType() {
		return TYPE;
	}

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
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
				return true;
		}
		return false;
	}
	
	// TODO Test currently disabled for bug 403098
	public void testImportLinksMultiple() {
		System.out.println("org.eclipse.pde.ui.tests.imports.ImportAsSourceTestCase.testImportLinksMultiple()");
		System.out.println("This test is currently disabled (Bug 403098)");
	}
	
	// TODO Test currently disabled for bug 403098
	public void testImportFlat() {
		System.out.println("org.eclipse.pde.ui.tests.imports.ImportAsSourceTestCase.testImportFlat()");
		System.out.println("This test is currently disabled (Bug 403098)");
	}
}
