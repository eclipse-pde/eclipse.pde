package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;

public class FileExtensionFilter extends ViewerFilter {

	private String fTargetExtension;

	public FileExtensionFilter(String targetExtension) {
		fTargetExtension = targetExtension;
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			return fTargetExtension.equals(((IFile)element).getFileExtension());
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
