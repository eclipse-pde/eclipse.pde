/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

public class SchemaEditorContributor extends PDEFormTextEditorContributor {
	private PreviewAction fPreviewAction;

	class PreviewAction extends Action {
		public PreviewAction() {
		}

		@Override
		public void run() {
			if (getEditor() != null) {
				final SchemaEditor schemaEditor = (SchemaEditor) getEditor();
				BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), () -> schemaEditor.previewReferenceDocument());
			}
		}
	}

	public SchemaEditorContributor() {
		super("&Schema"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}

	@Override
	public void contextMenuAboutToShow(IMenuManager mm, boolean addClipboard) {
		super.contextMenuAboutToShow(mm, addClipboard);
		mm.add(new Separator());
		mm.add(fPreviewAction);
	}

	public Action getPreviewAction() {
		return fPreviewAction;
	}

	@Override
	protected void makeActions() {
		super.makeActions();
		fPreviewAction = new PreviewAction();
		fPreviewAction.setText(PDEUIMessages.SchemaEditorContributor_previewAction);
	}
}
