package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.dnd.Clipboard;

public class SiteEditorContributor extends PDEEditorContributor {
	private Action buildAction;

	public SiteEditorContributor() {
		super("Site");
	}
	public void contextMenuAboutToShow(IMenuManager mng) {
		super.contextMenuAboutToShow(mng);
		mng.add(new Separator());
		mng.add(buildAction);
	}

	protected void makeActions() {
		super.makeActions();
		buildAction = new Action() {
			public void run() {
				PDEMultiPageEditor editor = getEditor();
				BuildControlSection.handleBuild(editor);
			}
		};
		buildAction.setText("&Rebuild All");
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}
