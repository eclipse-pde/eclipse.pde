/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.standalone.site;

 
import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.*;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors.
 * Responsible for the redirection of global actions to the active editor.
 * Multi-page contributor replaces the contributors for the individual editors in the multi-page editor.
 */
public class MultiPageContributor extends MultiPageEditorActionBarContributor {
	/** The global actions to be connected with editor actions */
	/*private final static String[] ACTIONS = {
		ITextEditorActionConstants.UNDO,
		ITextEditorActionConstants.REDO,
		ITextEditorActionConstants.CUT,
		ITextEditorActionConstants.COPY,
		ITextEditorActionConstants.PASTE,
		ITextEditorActionConstants.DELETE,
		ITextEditorActionConstants.SELECT_ALL,
		ITextEditorActionConstants.FIND,
		ITextEditorActionConstants.BOOKMARK,
		ITextEditorActionConstants.ADD_TASK,
		ITextEditorActionConstants.PRINT,
		ITextEditorActionConstants.REVERT };
	*/		
	private IEditorPart activeEditorPart;
	private TextEditorActionContributor fSourceContributor;
	
	/**
	 * Creates a multi-page contributor. */
	public MultiPageContributor() {
		super();
		fSourceContributor = new TextEditorActionContributor();
	}
	
	/**
	 * Returns the action registed with the given text editor.
	 * 
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(ITextEditor editor, String actionID) {
		return (editor == null || actionID == null) ? null : editor.getAction(actionID);
	}
	
	/* (non-JavaDoc) Method declared in MultiPageEditorActionBarContributor. */
	public void setActivePage(IEditorPart part) {
		if (activeEditorPart == part)
			return;

		activeEditorPart = part;

		if (part instanceof ITextEditor) {
			fSourceContributor.setActiveEditor(part);
			fSourceContributor.getActionBars().updateActionBars();
		}
		/*if (actionBars != null) {
			ITextEditor editor =
				(part instanceof ITextEditor) ? (ITextEditor) part : null;
			for (int i = 0; i < ACTIONS.length; i++)
				actionBars.setGlobalActionHandler(
					ACTIONS[i],
					getAction(editor, ACTIONS[i]));
			actionBars.updateActionBars();
		}*/
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorActionBarContributor#init(org.eclipse.ui.IActionBars)
	 */
	public void init(IActionBars bars) {
		super.init(bars);
		fSourceContributor.init(bars);
	}
	
}
