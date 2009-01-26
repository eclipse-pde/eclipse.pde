/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Editor contributor for the target definition editor.  Adds support for certain
 * global actions.
 * 
 * @see TargetEditor
 */
public class TargetEditorContributor extends EditorActionBarContributor {

	private TargetEditor fEditor;
	private IAction fRevertAction;

	class RevertAction extends Action implements IUpdate {
		public void run() {
			if (fEditor != null)
				fEditor.doRevert();
		}

		public void update() {
			setEnabled(fEditor != null ? fEditor.isDirty() : false);
		}
	}

	public void setActiveEditor(IEditorPart targetEditor) {
		if (targetEditor instanceof TargetEditor) {
			fEditor = (TargetEditor) targetEditor;
		} else {
			fEditor = null;
		}
		IActionBars bars = getActionBars();
		if (bars == null)
			return;
		bars.setGlobalActionHandler(ActionFactory.REVERT.getId(), getRevertAction());
		bars.updateActionBars();
	}

	private IAction getRevertAction() {
		if (fRevertAction == null) {
			fRevertAction = new RevertAction();
		}
		return fRevertAction;
	}

//	public void setActivePage(IEditorPart activeEditor) {
//		// TODO Revert action is not working correctly, activeEditor is always null
//		fEditor = (TargetEditor) activeEditor;
//		getActionBars().setGlobalActionHandler(ActionFactory.REVERT.getId(), new RevertAction());
//		getActionBars().updateActionBars();
//	}
}
