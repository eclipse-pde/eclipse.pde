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
import org.eclipse.pde.internal.core.BinaryRepositoryProvider;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.team.core.RepositoryProvider;

public class ImportWithLinksTestCase extends BaseImportTestCase {
	
	private static int TYPE = PluginImportOperation.IMPORT_BINARY_WITH_LINKS;

	public static Test suite() {
		return new TestSuite(ImportWithLinksTestCase.class);
	}
	
	public void testImportLinksJAR() {
		runOperation(new String[] {"org.eclipse.pde.core"}, TYPE);
		verifyLinkedProject("org.eclipse.pde.core", true);
	}
	
	public void testImportLinksFlat() {
		runOperation(new String[] {"org.eclipse.jdt.debug"}, TYPE);
		verifyLinkedProject("org.eclipse.jdt.debug", true);
	}
	
	public void testImportLinksNotJavaFlat() {
		runOperation(new String[] {"org.eclipse.pde"}, TYPE);
		verifyLinkedProject("org.eclipse.pde", false);
	}
	
	public void testImportLinksNotJavaJARd() {
		runOperation(new String[] {"org.eclipse.jdt.doc.user"}, TYPE);
		verifyLinkedProject("org.eclipse.jdt.doc.user", false);
	}
	
	public void testImportLinksMultiple() {
		runOperation(new String[] {"org.eclipse.core.filebuffers", "org.eclipse.jdt.doc.user", "org.eclipse.pde.build"},
					TYPE);
		verifyLinkedProject("org.eclipse.core.filebuffers", true);
		verifyLinkedProject("org.eclipse.jdt.doc.user", false);
		verifyLinkedProject("org.eclipse.pde.build", true);		
	}

	private void verifyLinkedProject(String projectName, boolean isJava) {
		try {
			IProject project =  verifyProject(projectName);
			RepositoryProvider provider = RepositoryProvider.getProvider(project);
			assertTrue(provider instanceof BinaryRepositoryProvider);			
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
