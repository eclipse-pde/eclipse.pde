/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.text.SimpleCSModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions.SimpleCSPreviewAction;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;

public class SimpleCSSourcePage extends XMLSourcePage {

	public SimpleCSSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public boolean isQuickOutlineEnabled() {
		return true;
	}

	@Override
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	@Override
	public ITreeContentProvider createOutlineContentProvider() {
		return new SimpleCSContentProvider();
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return PDEUserAssistanceUIPlugin.getDefault().getLabelProvider();
	}

	@Override
	protected boolean isSelectionListener() {
		return true;
	}

	@Override
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentElementNode) && (((IDocumentElementNode) object).isErrorNode() == false)) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
		}
	}

	@Override
	protected IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof IDocumentElementNode) {
			return (IDocumentElementNode) selectedObject;
		}
		return null;
	}

	@Override
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IDocumentElementNode rootNode = ((SimpleCSModel) getInputContext().getModel()).getSimpleCS();
		return findNode(rootNode, offset, searchChildren);
	}

	@Override
	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange range = getRangeElement(offset, true);
		updateHighlightRange(range);
		range = adaptRange(range);
		updateOutlinePageSelection(range);
	}

	@Override
	public IDocumentRange adaptRange(IDocumentRange range) {
		// Adapt the range to node that is viewable in the outline view
		if (range instanceof IDocumentAttributeNode) {
			// Attribute
			return adaptRange(((IDocumentAttributeNode) range).getEnclosingElement());
		} else if (range instanceof IDocumentTextNode) {
			// Content
			return adaptRange(((IDocumentTextNode) range).getEnclosingElement());
		} else if (range instanceof IDocumentElementNode) {
			// Element
			if (range instanceof ISimpleCS) {
				return range;
			} else if (range instanceof ISimpleCSItem) {
				return range;
			} else if (range instanceof ISimpleCSSubItemObject) {
				return range;
			} else if (range instanceof ISimpleCSIntro) {
				return range;
			} else if (range instanceof ISimpleCSPerformWhen) {
				return range;
			} else {
				return adaptRange(((IDocumentElementNode) range).getParentNode());
			}
		}
		return null;
	}

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		// Get the editor
		PDEFormEditor editor = (PDEFormEditor) getEditor();
		// Get the form editor contributor
		SimpleCSEditorContributor contributor = (SimpleCSEditorContributor) editor.getContributor();
		// Get the model
		// TODO: MP: SimpleCS:  Preview does not show unsaved changes made to source page,
		// check if fixed after implementing text edit operations
		ISimpleCSModel model = (ISimpleCSModel) editor.getAggregateModel();
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

	@Override
	protected void setPartName(String partName) {
		super.setPartName(SimpleMessages.SimpleCSSourcePage_title);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setHelpContextId(IHelpContextIds.SIMPLE_CS_EDITOR);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IHyperlinkDetector.class.equals(adapter)) {
			return adapter.cast(new SimpleCSHyperlinkDetector(this));
		}
		return super.getAdapter(adapter);
	}

}
