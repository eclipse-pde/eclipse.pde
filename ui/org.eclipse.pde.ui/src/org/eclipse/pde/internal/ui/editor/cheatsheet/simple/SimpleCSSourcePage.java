/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSPreviewAction;

/**
 * SimpleCSSourcePage
 *
 */
public class SimpleCSSourcePage extends XMLSourcePage {

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public SimpleCSSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineComparator()
	 */
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineContentProvider()
	 */
	public ITreeContentProvider createOutlineContentProvider() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineLabelProvider()
	 */
	public ILabelProvider createOutlineLabelProvider() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void updateSelection(SelectionChangedEvent event) {
		// NO-OP
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		// Get the editor
		PDEFormEditor editor = (PDEFormEditor)getEditor();
		// Get the form editor contributor
		SimpleCSEditorContributor contributor = 
			(SimpleCSEditorContributor)editor.getContributor();
		// Get the model
		// TODO: MP: SimpleCS:  Preview does not show unsaved changes made to source page, 
		// check if fixed after implementing text edit operations
		ISimpleCSModel model = (ISimpleCSModel)editor.getAggregateModel();
		// Get the preview action
		SimpleCSPreviewAction previewAction = contributor.getPreviewAction();
		// Set the cheat sheet object
		previewAction.setDataModelObject(model.getSimpleCS());
		// Set the editor input
		previewAction.setEditorInput(editor.getEditorInput());
		// Add the preview action to the context menu
		menu.add(previewAction);
		menu.add(new Separator());
		super.editorContextMenuAboutToShow(menu);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlinePage()
	 */
	protected ISortableContentOutlinePage createOutlinePage() {
		// TODO: MP: SimpleCS:  Use until source page defined for source page
		return new SimpleCSFormOutlinePage((PDEFormEditor)getEditor());
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#setPartName(java.lang.String)
	 */
	protected void setPartName(String partName) {
		super.setPartName(PDEUIMessages.EditorSourcePage_name);
	}
	
}
