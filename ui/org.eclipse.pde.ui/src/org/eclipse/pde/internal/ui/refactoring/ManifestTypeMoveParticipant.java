/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeMoveParticipant extends PDEMoveParticipant {

	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType) element;
			IJavaProject javaProject = (IJavaProject) type
					.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElements = new ArrayList();
				fElements.add(element);
				return true;
			}
		}
		return false;
	}

	public String getName() {
		return PDEUIMessages.ManifestTypeRenameParticipant_composite;
	}
	
	protected boolean isInterestingForExtensions() {
		Object dest = getArguments().getDestination();
		if (dest instanceof IJavaElement) {
			IJavaElement destination = (IJavaElement)dest;
			IJavaProject jProject = (IJavaProject)destination.getAncestor(IJavaElement.JAVA_PROJECT);
			return jProject.getProject().equals(fProject);
		}
		return false;		
	}

	protected void addChange(CompositeChange result, String filename, IProgressMonitor pm)
			throws CoreException {
		IFile file = fProject.getFile(filename);
		if (file.exists()) {
			Change change = PluginManifestChange.createRenameChange(file, 
					getAffectedElements(), 
					getNewNames(), 
					pm);
			if (change != null)
				result.add(change);				
		}
	}
	
	protected IJavaElement[] getAffectedElements() {
		return (IJavaElement[])fElements.toArray(new IJavaElement[fElements.size()]);
	}
	
	private String[] getNewNames() {
		Object destination = getArguments().getDestination();
		StringBuffer buffer = new StringBuffer();
		if (destination instanceof IPackageFragment) {
			buffer.append(((IPackageFragment)destination).getElementName());
			if (buffer.length() > 0)
				buffer.append("."); //$NON-NLS-1$
		}
		String[] result = new String[fElements.size()];
		for (int i = 0; i < fElements.size(); i++) {
			result[i] = buffer.toString() + ((IJavaElement)fElements.get(i)).getElementName();
		}
		return result;
	}

	protected void addBundleManifestChange(CompositeChange result, IProgressMonitor pm)
			throws CoreException {
		IFile file = fProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(
										file, 
										(IJavaElement[])fElements.toArray(new IJavaElement[fElements.size()]),
										getNewNames(), 
										pm);
			if (change != null)
				result.add(change);
		}
	}

}
