/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;

public class ImportAsSourceTestCase extends BaseImportTestCase {
	

	private static int TYPE = PluginImportOperation.IMPORT_WITH_SOURCE;

	public static Test suite() {
		return new TestSuite(ImportAsSourceTestCase.class);
	}
	
	public void testImportSourceJAR() {
		runOperation(new String[] {"org.eclipse.pde.core"}, TYPE);
		verifySourceProject("org.eclipse.pde.core", true);
	}
	
	public void testImportSourceFlat() {
		runOperation(new String[] {"org.eclipse.jdt.debug"}, TYPE);
		verifySourceProject("org.eclipse.jdt.debug", true);
	}
	
	public void testImportSourceNotJavaJARd() {
		runOperation(new String[] {"org.eclipse.jdt.doc.user"}, TYPE);
		verifySourceProject("org.eclipse.jdt.doc.user", false);
	}
	
	public void testImportSourceMultiple() {
		runOperation(new String[] {"org.eclipse.core.filebuffers", "org.eclipse.jdt.doc.user", "org.eclipse.pde.build"},
					TYPE);
		verifySourceProject("org.eclipse.core.filebuffers", true);
		verifySourceProject("org.eclipse.jdt.doc.user", false);
		verifySourceProject("org.eclipse.pde.build", true);		
	}

	private void verifySourceProject(String projectName, boolean isJava) {
		try {
			IProject project =  verifyProject(projectName);
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
}
