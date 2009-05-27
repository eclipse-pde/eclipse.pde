/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class FileRenameParticipant extends PDERenameParticipant {

	public String getName() {
		return PDEUIMessages.FileRenameParticipant_renameFiles;
	}

	protected boolean initialize(Object element) {
		if (element instanceof IFile) {
			IProject project = ((IFile) element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				IPath path = ((IFile) element).getProjectRelativePath().removeLastSegments(1);
				String newName = path.append(getArguments().getNewName()).toString();
				fProject = project;
				fElements = new HashMap();
				fElements.put(element, newName);
				return true;
			}
		}
		return false;
	}

}
