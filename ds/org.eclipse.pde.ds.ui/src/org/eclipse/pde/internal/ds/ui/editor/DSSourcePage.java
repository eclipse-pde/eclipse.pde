/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262622
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

public class DSSourcePage extends XMLSourcePage {


	public DSSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	public boolean isQuickOutlineEnabled() {
		return true;
	}

	public ViewerComparator createOutlineComparator() {
		return null;
	}

	public ITreeContentProvider createOutlineContentProvider() {
		return new DSContentProvider();
	}

	public ILabelProvider createOutlineLabelProvider() {
		return new DSLabelProvider();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#isSelectionListener()
	 */
	protected boolean isSelectionListener() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentElementNode)
				&& (((IDocumentElementNode) object).isErrorNode() == false)) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#findRange()
	 */
	protected IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof IDocumentElementNode) {
			return (IDocumentElementNode) selectedObject;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#getRangeElement(int,
	 *      boolean)
	 */
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IDocumentElementNode rootNode = ((DSModel) getInputContext().getModel())
				.getDSComponent();
		return findNode(rootNode, offset, searchChildren);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#synchronizeOutlinePage(int)
	 */
	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange range = getRangeElement(offset, true);
		updateHighlightRange(range);
		range = adaptRange(range);
		updateOutlinePageSelection(range);
	}

	/**
	 * @param range
	 */
	public IDocumentRange adaptRange(IDocumentRange range) {
		// Adapt the range to node that is viewable in the outline view
		if (range instanceof IDocumentAttributeNode) {
			// Attribute
			return adaptRange(((IDocumentAttributeNode) range)
					.getEnclosingElement());
		} else if (range instanceof IDocumentTextNode) {
			// Content
			return adaptRange(((IDocumentTextNode) range).getEnclosingElement());
		} else if (range instanceof IDocumentElementNode) {
			// Element
			return range;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#setPartName(java.lang.String)
	 */
	protected void setPartName(String partName) {
		super.setPartName(Messages.DSSourcePage_partName);
	}


	public Object getAdapter(Class adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return new DSHyperlinkDetector(this);
		return super.getAdapter(adapter);
	}

	public void setActive(boolean active) {
		super.setActive(active);
		// Update the text selection if this page is being activated
		if (active) {
			updateTextSelection();
		}
	}

	protected ChangeAwareSourceViewerConfiguration createSourceViewerConfiguration(
			IColorManager colorManager) {
		return new DSSourceViewerConfiguration(colorManager, this);
	}
}
