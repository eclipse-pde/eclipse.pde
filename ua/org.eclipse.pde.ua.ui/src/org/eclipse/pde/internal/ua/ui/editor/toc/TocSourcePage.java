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

public class TocSourcePage extends XMLSourcePage {

	public TocSourcePage(PDEFormEditor editor, String id, String title) {
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
		return new TocContentProvider();
	}

	@Override
	public ILabelProvider createOutlineLabelProvider() {
		return PDEUserAssistanceUIPlugin.getDefault().getLabelProvider();
	}

	@Override
	protected void setPartName(String partName) {
		super.setPartName(TocMessages.TocSourcePage_title);
	}

	@Override
	protected boolean isSelectionListener() {
		return true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return adapter.cast(new TocHyperlinkDetector(this));
		return super.getAdapter(adapter);
	}

	@Override
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentElementNode) && !((IDocumentElementNode) object).isErrorNode()) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
		}
	}

	@Override
	protected IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof IDocumentElementNode)
			return (IDocumentElementNode) selectedObject;

		return null;
	}

	@Override
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		TocObject toc = ((TocModel) getInputContext().getModel()).getToc();
		return findNode(toc, offset, searchChildren);
	}

	@Override
	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange rangeElement = getRangeElement(offset, true);
		updateHighlightRange(rangeElement);
		// TODO: MP: TEO: LOW: Generalize for parent - search children = true and handle attributes
		if (rangeElement instanceof IDocumentAttributeNode) {
			rangeElement = ((IDocumentAttributeNode) rangeElement).getEnclosingElement();
		}
		updateOutlinePageSelection(rangeElement);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setHelpContextId(IHelpContextIds.TOC_EDITOR);
	}

	@Override
	protected IFoldingStructureProvider getFoldingStructureProvider(IEditingModel model) {
		return new TocFoldingStructureProvider(this, model);
	}
}
