/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestPackageDeleteParticipant extends PDEDeleteParticipant {

	@Override
	protected boolean initialize(Object element) {
		try {
			if (element instanceof IPackageFragment fragment) {
				if (!fragment.containsJavaResources()) {
					return false;
				}
				IJavaProject javaProject = (IJavaProject) fragment.getAncestor(IJavaElement.JAVA_PROJECT);
				IProject project = javaProject.getProject();
				if (WorkspaceModelManager.isPluginProject(project)) {
					fProject = javaProject.getProject();
					fElements = new HashMap<>();
					fElements.put(fragment, fragment.getElementName());
					return true;
				}
			}
		} catch (JavaModelException e) {
			// Log error but return false to skip this participant
		}
		return false;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestPackageDeleteParticipant_packageDelete;
	}

	@Override
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (file.exists()) {
			Change change = BundleManifestChange.createMultiplePackageDeleteChange(file, fElements.keySet().toArray(), pm);
			if (change != null) {
				result.add(change);
			}
		}
	}

}
