/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.*;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

public class SchemaEditorContributor extends PDEFormEditorContributor {
	private PreviewAction previewAction;

	class PreviewAction extends Action {
		public PreviewAction() {
		}
		public void run() {
			if (getEditor() != null) {
				final SchemaEditor schemaEditor = (SchemaEditor) getEditor();
				BusyIndicator
					.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
					public void run() {
						schemaEditor.previewReferenceDocument();
					}
				});
			}
		}
	}

	public SchemaEditorContributor() {
		super("&Schema"); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}

	public void contextMenuAboutToShow(IMenuManager mm, boolean addClipboard) {
		super.contextMenuAboutToShow(mm, addClipboard);
		mm.add(new Separator());
		mm.add(previewAction);
	}
	
	public Action getPreviewAction() {
		return previewAction;
	}

	protected void makeActions() {
		super.makeActions();
		previewAction = new PreviewAction();
		previewAction.setText(
			PDEPlugin.getResourceString(
				"SchemaEditorContributor.previewAction")); //$NON-NLS-1$
	}
}
