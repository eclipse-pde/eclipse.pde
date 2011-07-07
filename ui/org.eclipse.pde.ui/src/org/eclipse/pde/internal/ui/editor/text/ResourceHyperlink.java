/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class ResourceHyperlink extends AbstractHyperlink {

	private IResource fResource;

	public ResourceHyperlink(IRegion region, String element, IResource res) {
		super(region, element);
		fResource = res;
	}

	public void open() {
		if (fResource == null)
			return;

		IResource resource = processAbsolutePathes();
		if (resource == null) {
			if (fElement.indexOf("$nl$/") == 0) //$NON-NLS-1$
				fElement = fElement.substring(5);
			resource = fResource.getProject().findMember(fElement);
		}
		try {
			if (resource instanceof IFile) {
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile) resource, true);
			} else if (resource != null) {
				IPackagesViewPart part = (IPackagesViewPart) PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
				part.selectAndReveal(resource);
			} else {
				Display.getDefault().beep();
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	private IResource processAbsolutePathes() {
		// Check to see if we got an absolute path
		if (fElement.startsWith("/") == false) { //$NON-NLS-1$
			// Not an absolute path
			return null;
		}
		// Absolute path
		// Format:  /<project-name>/<path-to-file>
		// Remove the '/'
		String path = fElement.substring(1);
		// Parse the project name from the path
		int index = path.indexOf('/');
		String projectName = path.substring(0, index);
		// Ensure we have a valid index
		if ((index + 1) >= path.length()) {
			return null;
		}
		// Parse the file name from the path (skip the '/')
		String fileName = path.substring(index + 1);
		// Get the workspace
		IWorkspace workspace = fResource.getWorkspace();
		// Find the project
		IProject project = workspace.getRoot().getProject(projectName);
		// Ensure the project was found
		if (project == null) {
			return null;
		}
		// Find the file
		return project.findMember(fileName);
	}

}
