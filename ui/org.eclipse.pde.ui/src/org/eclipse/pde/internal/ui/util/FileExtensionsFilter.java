/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.util;

import java.util.HashSet;
import java.util.Locale;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class FileExtensionsFilter extends ViewerFilter {

	private HashSet fExtensions;

	public FileExtensionsFilter() {
		fExtensions = new HashSet();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// Select based on type
		if (element instanceof IFile) {
			// Files (IFile)
			return processFile((IFile) element);
		} else if (element instanceof IContainer) {
			// Projects (IProject), Folders (IFolder)
			return processContainer((IContainer) element, viewer, parentElement);
		}
		return false;
	}

	private boolean processContainer(IContainer container, Viewer viewer, Object parentElement) {
		// Skip closed projects
		if ((container instanceof IProject) && (((IProject) container).isOpen() == false)) {
			return false;
		}
		// Process the container's members
		try {
			IResource[] resources = container.members();
			for (int i = 0; i < resources.length; i++) {
				if (select(viewer, parentElement, resources[i])) {
					return true;
				}
			}
		} catch (CoreException e) {
			// Ignore
		}
		return false;
	}

	private boolean processFile(IFile file) {
		// Get the file's name (including extension)
		String fileName = file.getName().toLowerCase(Locale.ENGLISH);
		// Get the index of the last '.'
		int lastDotIndex = fileName.lastIndexOf('.');
		int lastIndex = fileName.length() - 1;
		// Validate index
		if (lastDotIndex < 0) {
			return false;
		} else if (lastDotIndex >= lastIndex) {
			return false;
		}
		// Get the file's extension (remove dot)
		String extension = fileName.substring(lastDotIndex + 1);
		// Check to see if the extension should be filtered
		return fExtensions.contains(extension);
	}

	public void addFileExtension(String extension) {
		fExtensions.add(extension);
	}

}
