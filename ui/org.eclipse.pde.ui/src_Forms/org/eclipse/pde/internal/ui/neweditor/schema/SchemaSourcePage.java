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
package org.eclipse.pde.internal.ui.neweditor.schema;
import org.eclipse.jface.action.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.XMLSourcePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SchemaSourcePage extends XMLSourcePage {
	public SchemaSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}
	public IContentOutlinePage createContentOutlinePage() {
		return null;
		/*
		 * return new SchemaSourceOutlinePage( getEditorInput(),
		 * getDocumentProvider(), this);
		 */
	}
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		SchemaEditorContributor contributor = (SchemaEditorContributor) ((PDEFormEditor) getEditor())
				.getContributor();
		menu.add(contributor.getPreviewAction());
	}
}