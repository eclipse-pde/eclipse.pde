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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SchemaSourcePage extends XMLSourcePage implements IPDEEditorPage {
	public static final String SCHEMA_TYPE = "__extension_point_schema";

	public SchemaSourcePage(SchemaEditor editor) {
		super(editor);
	}
	public IContentOutlinePage createContentOutlinePage() {
		return new SchemaSourceOutlinePage(
			getEditorInput(),
			getDocumentProvider(),
			this);
	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		SchemaEditorContributor contributor =
			(SchemaEditorContributor) getEditor().getContributor();
		menu.add(contributor.getPreviewAction());
	}
}
