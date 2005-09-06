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
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestPackageMoveParticipant extends PDEMoveParticipant {

	protected boolean initialize(Object element) {
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment)element;
			IJavaProject javaProject = (IJavaProject)fragment.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.hasBundleManifest(project)) {
				fProject = javaProject.getProject();
				fElement = fragment;
				return true;
			}
		}
		return false;
	}

	public String getName() {
		return PDEUIMessages.ManifestPackageRenameParticipant_packageRename;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = fProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (file.exists()) {
			IProject destProject = getDestinationProject();
			if (destProject != null && !fProject.equals(destProject)) {
				MoveFromChange change = BundleManifestChange.createMovePackageChange(file, 
						fElement.getElementName(), 
						pm);
				if (change != null) {
					result.add(change);
					IFile dest = destProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
					if (dest.exists()) {
						Change second = BundleManifestChange.createMoveToPackageChange(
								dest,  
								change.getMovedElement(), pm);
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
			IResource resource = (IResource)((IAdaptable)dest).getAdapter(IResource.class);
			if (resource != null) 
				return resource.getProject();
		}
		return null;		
	}

	protected boolean isInterestingForExtensions() {
		return false;
	}

}
