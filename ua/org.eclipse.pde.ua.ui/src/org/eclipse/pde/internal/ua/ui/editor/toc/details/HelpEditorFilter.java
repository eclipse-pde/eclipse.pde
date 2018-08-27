/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc.details;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.ua.ui.editor.toc.HelpEditorUtil;

public class HelpEditorFilter extends ViewerFilter {
	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IFile) {
			IPath path = ((IFile) element).getFullPath();

			return HelpEditorUtil.hasValidPageExtension(path);
		}

		if (element instanceof IProject && !((IProject) element).isOpen()) {
			return false;
		}

		if (element instanceof IContainer) {
			try {
				IResource[] resources = ((IContainer) element).members();
				for (IResource resource : resources) {
					if (select(viewer, parent, resource)) {
						return true;
					}
				}
			} catch (CoreException e) {
			}
		}

		return false;
	}
}
