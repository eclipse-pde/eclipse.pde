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
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.Catalog;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.ScriptRunner;

public class ImportAsSourceTestCase extends PDETestCase {

	public static Test suite() {
		return new TestSuite(ImportAsSourceTestCase.class);
	}
	
	public void testImportSource1() {
		ScriptRunner.run(Catalog.IMPORT_SOURCE_1, getWorkbench());
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			verifyProject(root.getProject("org.eclipse.core.filebuffers"));
		} catch (CoreException e) {
			fail("testImportSource1:" + e);
		}
	}

	public void testImportSource2() {
		ScriptRunner.run(Catalog.IMPORT_SOURCE_2, getWorkbench());
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			verifyProject(root.getProject("org.eclipse.osgi"));
		} catch (CoreException e) {
			fail("testImportSource2:" + e);
		}
	}
	
	public void testImportSource3() {
		ScriptRunner.run(Catalog.IMPORT_SOURCE_3, getWorkbench());
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject[] projects = root.getProjects();
			assertTrue(projects.length > 0);
			for (int i = 0; i < projects.length; i++) {
				verifyProject(projects[i]);				
			}
		} catch (CoreException e) {
			fail("testImportSource3:" + e);
		}
	}

	private void verifyProject(IProject project) throws CoreException {
		assertTrue("Project was not created.", project.exists());
		if (project.hasNature(JavaCore.NATURE_ID))
			assertTrue(checkSourceAttached(JavaCore.create(project)));
	}
		
	private boolean checkSourceAttached(IJavaProject jProject) throws CoreException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			IClasspathEntry entry = roots[i].getRawClasspathEntry();
			if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER 
					|| !entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				continue;
			if (roots[i].getSourceAttachmentPath() == null)
				return false;
		}
		return true;
	}
	

}
