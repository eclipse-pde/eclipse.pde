/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		menu.add(new Separator());
		SchemaEditorContributor contributor = (SchemaEditorContributor) ((PDEFormEditor) getEditor()).getContributor();
		menu.add(contributor.getPreviewAction());
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	@Override
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	@Override
	public void updateSelection(SelectionChangedEvent e) {
		// NO-OP
	}

	@Override
	protected ISortableContentOutlinePage createOutlinePage() {
		//TODO remove this method when the above three stubs
		// are implemented
		return new SchemaFormOutlinePage((PDEFormEditor) getEditor());
	}

	@Override
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	@Override
	public void updateSelection(Object object) {
		// NO-OP
	}

	@Override
	protected void setPartName(String partName) {
		super.setPartName(PDEUIMessages.EditorSourcePage_name);
	}
}