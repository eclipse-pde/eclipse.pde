/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ContainerRenameParticipant extends PDERenameParticipant {

	public String getName() {
		return PDEUIMessages.ContainerRenameParticipant_renameFolders;
	}

	protected boolean initialize(Object element) {
		if (element instanceof IContainer) {
			IProject project = ((IContainer)element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				IPath path = ((IContainer)element).getProjectRelativePath().removeLastSegments(1);
				String newName = path.append(getArguments().getNewName()).toString();
				fProject = project;
				fElements = new HashMap();
				fElements.put(element, newName);
				return true;
			}
		}
		return false;
	}
	
	public Change createChange(IProgressMonitor pm) throws CoreException,
	OperationCanceledException {
		// for the special case of a project rename, we need to only check the manifest for changes
		if (fElements.size() == 1 && fElements.keySet().iterator().next() instanceof IProject) {
			if (!getArguments().getUpdateReferences())
				return null;
			CompositeChange result = new CompositeChange(PDEUIMessages.ContainerRenameParticipant_renameBundleId);
			addBundleManifestChange(result, pm);
			return result;
		}
		return super.createChange(pm);
	}

}
