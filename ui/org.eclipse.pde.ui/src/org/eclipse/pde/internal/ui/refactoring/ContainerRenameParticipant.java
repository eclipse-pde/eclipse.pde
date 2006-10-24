package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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

}
