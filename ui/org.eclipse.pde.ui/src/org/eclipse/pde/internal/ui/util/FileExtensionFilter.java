/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.util;

import java.util.Locale;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class FileExtensionFilter extends ViewerFilter {

	private String fTargetExtension;

	public FileExtensionFilter(String targetExtension) {
		fTargetExtension = targetExtension;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			return ((IFile) element).getName().toLowerCase(Locale.ENGLISH).endsWith("." + fTargetExtension); //$NON-NLS-1$
		}

		if (element instanceof IProject && !((IProject) element).isOpen())
			return false;

		if (element instanceof IContainer) { // i.e. IProject, IFolder
			try {
				IResource[] resources = ((IContainer) element).members();
				for (IResource resource : resources) {
					if (select(viewer, parent, resource))
						return true;
				}
			} catch (CoreException e) {
			}
		}
		return false;
	}

}
