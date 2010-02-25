/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

/**
 * Dummy class path container.
 */
public class ClasspathContainerInitializer extends org.eclipse.jdt.core.ClasspathContainerInitializer {
	
	public static final IPath PATH = new Path("org.eclipse.pde.ui.tests.classpath.container");

	public ClasspathContainerInitializer() {
	}

	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[]{project}, new IClasspathContainer[]{new ClasspathContainer(PATH)}, null);
	}

	class ClasspathContainer implements IClasspathContainer {
		
		private IPath fPath;
		
		/**
		 * Constructs a new container for the given path.
		 * 
		 * @param path container path
		 */
		public ClasspathContainer(IPath path) {
			fPath = path;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
		 */
		public IClasspathEntry[] getClasspathEntries() {
			return new IClasspathEntry[0]; // empty
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
		 */
		public String getDescription() {
			return "PDE Test Classpath Container";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
		 */
		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
		 */
		public IPath getPath() {
			return fPath;
		}
		
	}
	
}
