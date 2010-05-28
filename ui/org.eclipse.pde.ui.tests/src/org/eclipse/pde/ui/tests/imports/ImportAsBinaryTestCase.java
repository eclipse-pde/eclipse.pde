/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportAsBinaryTestCase extends BaseImportTestCase {

	private static int TYPE = PluginImportOperation.IMPORT_BINARY;

	public static Test suite() {
		return new TestSuite(ImportAsBinaryTestCase.class);
	}
	
	protected int getType() {
		return TYPE;
	}
	
	public void testImportAnt() {
		// Note: Ant is exempt from importing as source
		doSingleImport("org.apache.ant", true);
	}
	
	public void testImportJUnit4() {
		// Note: JUnit 4 does not have source but it is a java project
		doSingleImport("org.junit", 4, true);
	}

	protected void verifyProject(String projectName, boolean isJava) {
		try {
			IProject project = verifyProject(projectName);
			assertEquals(PDECore.BINARY_PROJECT_VALUE, project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY));
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
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				return true;
		}
		return false;
	}

}
