/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected boolean isInterestingForExtensions() {
		return true;
	}

	protected boolean initialize(Object element) {
		if (element instanceof IResource) {
			IProject project = ((IResource) element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = project;
				fElements = new HashMap();
				fElements.put(element, getNewName(getArguments().getDestination(), element));
				return true;
			}
		}
		return false;
	}

	protected void addChange(CompositeChange result, IFile file, IProgressMonitor pm) throws CoreException {
		if (file.exists()) {
			Change change = PluginManifestChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), getTextChange(file), pm);
			if (change != null)
				result.add(change);
		}
	}

	protected String getNewName(Object destination, Object element) {
		if (destination instanceof IContainer && element instanceof IResource) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(((IContainer) destination).getProjectRelativePath().toString());
			if (buffer.length() > 0)
				buffer.append('/');
			return buffer.append(((IResource) element).getName()).toString();
		}
		return super.getNewName(destination, element);
	}

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
