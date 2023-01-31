/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
		@Override
		public void run() {
			if (fEditor != null)
				fEditor.doRevert();
		}

		@Override
		public void update() {
			setEnabled(fEditor != null ? fEditor.isDirty() : false);
		}
	}

	@Override
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
}
