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
package org.eclipse.pde.ui.tests.wizards.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

public abstract class NewJavaProjectTest extends NewProjectTest {

	public void testJavaNature() {
		assertTrue("Project does not have a Java nature.", hasNature(JavaCore.NATURE_ID));
	}
	
	public void testSourceFolder() {
		IJavaProject jProject = JavaCore.create(getProject());
		boolean found = false;
		try {
			IClasspathEntry[] entries = jProject.getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (entry.getPath().equals(getSourceFolderPath())) {
						found = true;
						break;
					}
				}
			}
		} catch (JavaModelException e) {
		}
		assertTrue("Specified source folder not found.", found);
	}
	
	public void testOutputFolder() {
		IJavaProject jProject = JavaCore.create(getProject());
		IPath path = null;
		try {
			path = jProject.getOutputLocation();
		} catch (JavaModelException e) {
		}
		boolean found = path != null && path.equals(getOutputLocationPath());
		assertTrue("Specified output folder not found.", found);	
	}
	
	protected abstract IPath getSourceFolderPath();
	
	protected abstract IPath getOutputLocationPath();
}
