package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;

public class FileNameFilter extends ViewerFilter {

	private String fTargetName;

	public FileNameFilter(String targetName) {
		fTargetName = targetName;
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			return fTargetName.equals(((IFile)element).getName());
		}

		if (element instanceof IContainer){ // i.e. IProject, IFolder
			try {
				IResource[] resources = ((IContainer)element).members();
				for (int i = 0; i < resources.length; i++){
					if (select(viewer, parent, resources[i]))
						return true;
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return false;
	}

}
