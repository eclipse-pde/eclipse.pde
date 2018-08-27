/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

	@Override
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

		@Override
		public IClasspathEntry[] getClasspathEntries() {
			return new IClasspathEntry[0]; // empty
		}

		@Override
		public String getDescription() {
			return "PDE Test Classpath Container";
		}

		@Override
		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		@Override
		public IPath getPath() {
			return fPath;
		}

	}

}
