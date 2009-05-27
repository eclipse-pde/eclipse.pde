/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
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
		SchemaEditorContributor contributor = (SchemaEditorContributor) ((PDEFormEditor) getEditor()).getContributor();
		menu.add(contributor.getPreviewAction());
	}

	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void updateSelection(SelectionChangedEvent e) {
		// NO-OP
	}

	protected ISortableContentOutlinePage createOutlinePage() {
		//TODO remove this method when the above three stubs
		// are implemented
		return new SchemaFormOutlinePage((PDEFormEditor) getEditor());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		// NO-OP
	}

	protected void setPartName(String partName) {
		super.setPartName(PDEUIMessages.EditorSourcePage_name);
	}
}