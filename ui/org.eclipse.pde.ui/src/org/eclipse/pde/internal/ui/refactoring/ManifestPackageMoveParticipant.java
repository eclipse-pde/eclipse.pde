/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestPackageMoveParticipant extends PDEMoveParticipant {

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) element;
			IJavaProject javaProject = (IJavaProject) fragment.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (PDEProject.getManifest(project).exists()) {
				fProject = javaProject.getProject();
				fElements = new HashMap<>();
				fElements.put(fragment, getNewName(getArguments().getDestination(), element));
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestPackageRenameParticipant_packageRename;
	}

	@Override
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (file.exists()) {
			IProject destProject = getDestinationProject();
			if (destProject != null && !fProject.equals(destProject)) {
				MoveFromChange change = BundleManifestChange.createMovePackageChange(file, fElements.keySet().toArray(), pm);
				if (change != null) {
					result.add(change);
					IFile dest = PDEProject.getManifest(destProject);
					if (dest.exists()) {
						Change second = BundleManifestChange.createMoveToPackageChange(dest, change, pm);
						if (second != null)
							result.add(second);
					}
				}
			}
		}
	}

	private IProject getDestinationProject() {
		Object dest = getArguments().getDestination();
		if (dest instanceof IAdaptable) {
			IResource resource = ((IAdaptable) dest).getAdapter(IResource.class);
			if (resource != null) {
				return resource.getProject();
			}
		}
		return null;
	}

	@Override
	protected boolean isInterestingForExtensions() {
		return false;
	}

}
