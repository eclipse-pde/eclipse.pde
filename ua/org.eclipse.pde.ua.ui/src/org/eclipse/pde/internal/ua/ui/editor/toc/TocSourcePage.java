/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.editor.IFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;

/**
 * TocSourcePage
 */
public class TocSourcePage extends XMLSourcePage {

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public TocSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return true;
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
		return new TocContentProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineLabelProvider()
	 */
	public ILabelProvider createOutlineLabelProvider() {
		return PDEUserAssistanceUIPlugin.getDefault().getLabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#setPartName(java.lang.String)
	 */
	protected void setPartName(String partName) {
		super.setPartName(TocMessages.TocSourcePage_title);
	}

	protected boolean isSelectionListener() {
		return true;
	}

	public Object getAdapter(Class adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return new TocHyperlinkDetector(this);
		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentElementNode) && !((IDocumentElementNode) object).isErrorNode()) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
		}
	}

	protected IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof IDocumentElementNode)
			return (IDocumentElementNode) selectedObject;

		return null;
	}

	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		TocObject toc = ((TocModel) getInputContext().getModel()).getToc();
		return findNode(toc, offset, searchChildren);
	}

	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange rangeElement = getRangeElement(offset, true);
		updateHighlightRange(rangeElement);
		// TODO: MP: TEO: LOW: Generalize for parent - search children = true and handle attributes
		if (rangeElement instanceof IDocumentAttributeNode) {
			rangeElement = ((IDocumentAttributeNode) rangeElement).getEnclosingElement();
		}
		updateOutlinePageSelection(rangeElement);
	}

	protected void initializeEditor() {
		super.initializeEditor();
		setHelpContextId(IHelpContextIds.TOC_EDITOR);
	}

	protected IFoldingStructureProvider getFoldingStructureProvider(IEditingModel model) {
		return new TocFoldingStructureProvider(this, model);
	}
}
