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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class ManifestSourcePage extends XMLSourcePage {
	public static final String MANIFEST_TYPE = "__plugin_manifest";

	public ManifestSourcePage(ManifestEditor editor) {
		super(editor);
	}

	public IContentOutlinePage createContentOutlinePage() {
		return new ManifestSourceOutlinePage(
			getEditorInput(),
			getDocumentProvider(),
			this);
	}
	protected void editorContextMenuAboutToShow(MenuManager menu) {
		getEditor().editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		super.editorContextMenuAboutToShow(menu);
	}
}
