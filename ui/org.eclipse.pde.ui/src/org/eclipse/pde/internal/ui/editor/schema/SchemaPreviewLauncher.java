/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.search.PreviewReferenceAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorLauncher;

/**
 * SchemaPreviewLauncher
 *
 */
public class SchemaPreviewLauncher implements IEditorLauncher {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorLauncher#open(org.eclipse.core.runtime.IPath)
	 */
	public void open(IPath filePath) {
		// Create the preview action
		PreviewReferenceAction action = new PreviewReferenceAction();
		// Get the file in the workspace which the user right-clicked on and
		// selected "Open With"
		IFile file = 
			ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filePath);
		// Ensure the file is defined
		if (file == null) {
			// This should never happen
			// Probably workspace out of sync with the file system
			Display.getDefault().beep();
		} else {
			// Perform the preview schema action
			// Action parameter not used (unnecessary)
			IAction emptyAction = null;
			// Set data
			action.selectionChanged(emptyAction, new StructuredSelection(file));
			// Run action
			action.run(emptyAction);
		}
	}

}
