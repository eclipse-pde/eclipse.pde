package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.dnd.Clipboard;

public class SiteEditorContributor extends PDEEditorContributor {
	private Action buildAction;
	private Action rebuildAllAction;

	public SiteEditorContributor() {
		super("Site"); //$NON-NLS-1$
	}
	public void contextMenuAboutToShow(IMenuManager mng) {
		super.contextMenuAboutToShow(mng);
		mng.add(new Separator());
		mng.add(buildAction);
		mng.add(rebuildAllAction);
	}

	protected void makeActions() {
		super.makeActions();
		buildAction = new Action() {
			public void run() {
				SiteEditor editor = (SiteEditor)getEditor();
				BuildControlSection.handleBuild(editor, false);
			}
		};
		buildAction.setText(PDEPlugin.getResourceString("SiteEditorContributor.build")); //$NON-NLS-1$
		rebuildAllAction = new Action() {
			public void run() {
				SiteEditor editor = (SiteEditor)getEditor();
				BuildControlSection.handleBuild(editor, true);
			}
		};
		rebuildAllAction.setText(PDEPlugin.getResourceString("SiteEditorContributor.rebuildAll")); //$NON-NLS-1$
	}

	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}
