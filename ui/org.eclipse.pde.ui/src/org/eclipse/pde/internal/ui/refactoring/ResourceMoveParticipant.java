/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219852
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.project.PDEProject;

public abstract class ResourceMoveParticipant extends PDEMoveParticipant {

	@Override
	protected boolean isInterestingForExtensions() {
		return true;
	}

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IResource) {
			IProject project = ((IResource) element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = project;
				fElements = new HashMap<>();
				fElements.put(element, getNewName(getArguments().getDestination(), element));
				return true;
			}
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
		if (destination instanceof IContainer && element instanceof IResource) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(((IContainer) destination).getProjectRelativePath().toString());
			if (buffer.length() > 0)
				buffer.append('/');
			return buffer.append(((IResource) element).getName()).toString();
		}
		return super.getNewName(destination, element);
	}

	@Override
	protected void addChange(CompositeChange result, IProgressMonitor pm) throws CoreException {
		IFile file = PDEProject.getBuildProperties(fProject);
		if (file.exists()) {
			Change change = BuildPropertiesChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
		file = PDEProject.getManifest(fProject);
		if (file.exists()) {
			Change change = BundleManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}

}
