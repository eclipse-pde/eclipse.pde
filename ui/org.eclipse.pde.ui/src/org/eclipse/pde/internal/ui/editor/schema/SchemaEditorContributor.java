package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEEditorContributor;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;

public class SchemaEditorContributor extends PDEEditorContributor {
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
		super("&Schema");
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
				"SchemaEditorContributor.previewAction"));
	}
}