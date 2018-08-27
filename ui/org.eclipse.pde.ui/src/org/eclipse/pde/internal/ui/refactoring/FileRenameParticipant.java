/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class FileRenameParticipant extends PDERenameParticipant {

	@Override
	public String getName() {
		return PDEUIMessages.FileRenameParticipant_renameFiles;
	}

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IFile) {
			IProject project = ((IFile) element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				IPath path = ((IFile) element).getProjectRelativePath().removeLastSegments(1);
				String newName = path.append(getArguments().getNewName()).toString();
				fProject = project;
				fElements = new HashMap<>();
				fElements.put(element, newName);
				return true;
			}
		}
		return false;
	}

}
