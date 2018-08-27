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
package org.eclipse.pde.internal.ui.editor.actions;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.BundleSourcePage;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FormatAction extends Action {

	protected ITextEditor fTextEditor;

	public FormatAction() {
		setText(PDEUIMessages.FormatManifestAction_actionText);
	}

	@Override
	public void runWithEvent(Event event) {
		run();
	}

	@Override
	public void run() {
		if (fTextEditor == null || fTextEditor.getEditorInput() == null)
			return;

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new FormatOperation(new Object[] {fTextEditor.getEditorInput()}));
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
		}
	}

	public void setTextEditor(ITextEditor textEditor) {
		// TODO Temporary:  Until plug-in manifest XML source page format
		// functionality is completed
		setEnabled(textEditor instanceof BundleSourcePage);
		fTextEditor = textEditor;
	}

}
