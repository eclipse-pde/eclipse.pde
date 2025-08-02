/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ClasspathContainerState;
import org.eclipse.pde.internal.core.PDECore;

public class BndResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			Set<IProject> updateProjects = new HashSet<>();
			try {
				delta.accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource instanceof IFile file) {
							IProject project = file.getProject();
							Object sessionProperty = project.isOpen()
									? project.getSessionProperty(PDECore.BND_CLASSPATH_INSTRUCTION_FILE)
									: null;
							if (sessionProperty instanceof IFile instr) {
								if (instr.equals(file)) {
									updateProjects.add(file.getProject());
								}
							}
						}
						return true;
					}
				});
				ClasspathContainerState.requestClasspathUpdate(updateProjects);
			} catch (CoreException e) {
				// can't do anything then...
			}
		}
	}

}
