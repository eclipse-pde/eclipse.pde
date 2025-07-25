/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	private static final IClasspathContainer EMPTY_CLASSPATH_CONTAINER = new IClasspathContainer() {

		@Override
		public IPath getPath() {
			return PDECore.REQUIRED_PLUGINS_CONTAINER_PATH;
		}

		@Override
		public int getKind() {
			return K_APPLICATION;
		}

		@Override
		public String getDescription() {
			return PDECoreMessages.RequiredPluginsClasspathContainer_description;
		}

		@Override
		public IClasspathEntry[] getClasspathEntries() {
			// nothing yet, will be updated soon
			return new IClasspathEntry[0];
		}
	};

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		IProject project = javaProject.getProject();
		IClasspathContainer savedState = ClasspathComputer.readState(project);
		if (savedState == null) {
			if (PDECore.DEBUG_STATE) {
				System.out.println(String.format("%s has no saved state!", javaProject.getProject().getName())); //$NON-NLS-1$
			}
			JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, new IJavaProject[] { javaProject },
					new IClasspathContainer[] { EMPTY_CLASSPATH_CONTAINER }, null);
		} else {
			if (PDECore.DEBUG_STATE) {
				System.out.println(
						String.format("%s is restored from previous state.", javaProject.getProject().getName())); //$NON-NLS-1$
			}
			JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, new IJavaProject[] { javaProject },
					new IClasspathContainer[] { savedState }, null);
		}
		// The saved state might be stale, request a classpath update here, this
		// will run in a background job and update the classpath if needed.
		ClasspathComputer.requestClasspathUpdate(project, savedState);
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		if (containerPath == null || project == null) {
			return null;
		}

		return containerPath.segment(0) + "/" + project.getPath().segment(0); //$NON-NLS-1$
	}

	@Override
	public String getDescription(IPath containerPath, IJavaProject project) {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description;
	}

	@Override
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
		// Allow custom source attachments for PDE classpath containers (Bug 338182)
		return Status.OK_STATUS;
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		// The only supported update is to modify the source attachment
		return true;
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
		// The only supported update is to modify the source attachment
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project}, new IClasspathContainer[] {containerSuggestion}, null);
	}

}
