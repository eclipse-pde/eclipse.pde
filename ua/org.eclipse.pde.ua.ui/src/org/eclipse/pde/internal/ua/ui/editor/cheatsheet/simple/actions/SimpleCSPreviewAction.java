/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;

/**
 * SimpleCSPreviewAction
 *
 */
public class SimpleCSPreviewAction extends Action {

	private ISimpleCS fDataModelObject;

	private IEditorInput fEditorInput;

	/**
	 * @param input
	 */
	public SimpleCSPreviewAction() {
		fDataModelObject = null;
		fEditorInput = null;
		// Set action name
		setText(SimpleActionMessages.SimpleCSPreviewAction_actionText);
		// Set action image
		setImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_OBJ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		// Ensure we have our input
		if (fDataModelObject == null) {
			return;
		} else if (fEditorInput == null) {
			return;
		}
		// Get the editor input
		// Could be IFileEditorInput (File in workpspace - e.g. Package Explorer View)
		// Could be IStorageEditorInput (File not in workpsace - e.g. CVS Repositories View)
		try {
			// Write the current model into a String as raw XML
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			fDataModelObject.write("", writer); //$NON-NLS-1$
			writer.flush();
			swriter.close();
			// Launch in the cheat sheet view
			// Note:  Having a null URL is valid for simple cheat sheets
			OpenCheatSheetAction openAction = new OpenCheatSheetAction(fEditorInput.getName(), fEditorInput.getName(), swriter.toString(), null);
			openAction.run();
		} catch (IOException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
	}

	/**
	 * @param object
	 */
	public void setDataModelObject(ISimpleCS object) {
		fDataModelObject = object;
	}

	/**
	 * @param editorInput
	 */
	public void setEditorInput(IEditorInput editorInput) {
		fEditorInput = editorInput;
	}

}
