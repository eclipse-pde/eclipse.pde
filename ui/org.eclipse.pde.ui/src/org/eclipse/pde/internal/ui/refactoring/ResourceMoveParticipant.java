/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

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

	protected void addChange(CompositeChange result, String filename, IProgressMonitor pm) throws CoreException {
		IFile file = fProject.getFile(filename);
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
		IFile file = fProject.getFile("build.properties"); //$NON-NLS-1$
		if (file.exists()) {
			Change change = BuildPropertiesChange.createRenameChange(file, fElements.keySet().toArray(), getNewNames(), pm);
			if (change != null)
				result.add(change);
		}
	}

}
