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
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class ManifestSourcePage extends PDESourcePage {
	public static final String MANIFEST_TYPE = "__plugin_manifest";
	protected IColorManager colorManager = new ColorManager();

	public ManifestSourcePage(ManifestEditor editor) {
		super(editor);
		initializeViewerConfiguration();
	}
	protected void initializeViewerConfiguration() {
		setSourceViewerConfiguration(new XMLConfiguration(colorManager));
	}
	public IContentOutlinePage createContentOutlinePage() {
		return new ManifestSourceOutlinePage(
			getEditorInput(),
			getDocumentProvider(),
			this);
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	protected void editorContextMenuAboutToShow(MenuManager menu) {
		getEditor().editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		super.editorContextMenuAboutToShow(menu);
	}
}
