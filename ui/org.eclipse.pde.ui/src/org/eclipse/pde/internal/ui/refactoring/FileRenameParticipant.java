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
			IProject project = ((IFile)element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				IPath path = ((IFile)element).getProjectRelativePath().removeLastSegments(1);
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
