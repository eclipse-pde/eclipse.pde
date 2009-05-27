/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import java.util.HashSet;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class JARFileFilter extends ViewerFilter {
	private final static String jarExt = "jar"; //$NON-NLS-1$
	private HashSet fPaths;

	public JARFileFilter() {
		fPaths = new HashSet();
	}

	public JARFileFilter(HashSet names) {
		fPaths = names;
	}

	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile)
			return isFileValid(((IFile) element).getProjectRelativePath());

		if (element instanceof IContainer) { // i.e. IProject, IFolder
			try {
				if (!((IContainer) element).isAccessible())
					return false;
				IResource[] resources = ((IContainer) element).members();
				for (int i = 0; i < resources.length; i++) {
					if (select(viewer, parent, resources[i]))
						return true;
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return false;
	}

	public boolean isFileValid(IPath path) {
		String ext = path.getFileExtension();
		if (isPathValid(path) && ext != null && ext.length() != 0)
			return ext.equals(jarExt);
		return false;
	}

	public boolean isPathValid(IPath path) {
		return !fPaths.contains(path);
	}
}
