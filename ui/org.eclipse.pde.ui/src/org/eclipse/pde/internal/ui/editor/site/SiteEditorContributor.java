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
package org.eclipse.pde.internal.ui.editor.site;

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
