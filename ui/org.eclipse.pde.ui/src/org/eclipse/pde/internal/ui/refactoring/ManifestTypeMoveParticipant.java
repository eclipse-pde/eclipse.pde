/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
import org.eclipse.jdt.core.*;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeMoveParticipant extends PDEMoveParticipant {

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType) element;
			IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElements = new HashMap<>();
				fElements.put(element, getNewName(getArguments().getDestination(), element));
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestTypeRenameParticipant_composite;
	}

	@Override
	protected boolean isInterestingForExtensions() {
		Object dest = getArguments().getDestination();
		if (dest instanceof IJavaElement) {
			IJavaElement destination = (IJavaElement) dest;
			IJavaProject jProject = (IJavaProject) destination.getAncestor(IJavaElement.JAVA_PROJECT);
			return jProject.getProject().equals(fProject);
		}
		return false;
	}

	@Override
	protected void addChange(CompositeChange result, IFile file, IProgressMonitor pm) throws CoreException {
		if (file.exists()) {
			Change change = PluginManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), getTextChange(file), pm);
			if (change != null)
				result.add(change);
		}
	}

	@Override
	protected String getNewName(Object destination, Object element) {
		if (destination instanceof IPackageFragment && element instanceof IJavaElement) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(((IPackageFragment) destination).getElementName());
			if (buffer.length() > 0)
				buffer.append('.');
			return buffer.append(((IJavaElement) element).getElementName()).toString();
		}
		return super.getNewName(destination, element);
	}

	@Override
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getManifest(fProject);
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}

}
