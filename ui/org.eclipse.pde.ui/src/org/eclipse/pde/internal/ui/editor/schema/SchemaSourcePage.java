/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
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
	protected ILabelProvider createOutlineLabelProvider() {
		return null;
	}
	protected ITreeContentProvider createOutlineContentProvider() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	protected ViewerSorter createOutlineSorter() {
		return null;
	}
	protected void outlineSelectionChanged(SelectionChangedEvent e) {
	}
	protected ISortableContentOutlinePage createOutlinePage() {
		//TODO remove this method when the above three stubs
		// are implemented
		return new SchemaFormOutlinePage((PDEFormEditor) getEditor());
	}
}
