/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ExternalJavaSearchInitializer extends ClasspathContainerInitializer {

	ExternalJavaSearchClasspathContainer fContainer;

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		try {
			JavaCore.setClasspathContainer(PDECore.JAVA_SEARCH_CONTAINER_PATH, new IJavaProject[] {javaProject}, new IClasspathContainer[] {new ExternalJavaSearchClasspathContainer()}, null);
		} catch (OperationCanceledException e) {
			throw e;
		}

	}

}
