/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ui.editor.AbstractFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class TocFoldingStructureProvider extends AbstractFoldingStructureProvider {

	private Map<Position, IDocumentElementNode> fPositionToElement = new HashMap<>();

	public TocFoldingStructureProvider(PDESourcePage editor, IEditingModel model) {
		super(editor, model);
	}

	@Override
	public void addFoldingRegions(Set<Position> currentRegions, IEditingModel model) throws BadLocationException {
		TocObject toc = ((TocModel) model).getToc();
		List<TocObject> childList = toc.getChildren();
		IDocumentElementNode[] children = childList.toArray(new IDocumentElementNode[childList.size()]);

		addFoldingRegions(currentRegions, children, model.getDocument());
	}

	private void addFoldingRegions(Set<Position> regions, IDocumentElementNode[] nodes, IDocument document)
			throws BadLocationException {
		for (IDocumentElementNode element : nodes) {
			int startLine = document.getLineOfOffset(element.getOffset());
			int endLine = document.getLineOfOffset(element.getOffset() + element.getLength());
			if (startLine < endLine) {
				int start = document.getLineOffset(startLine);
				int end = document.getLineOffset(endLine) + document.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}
			IDocumentElementNode[] children = element.getChildNodes();
			if (children != null) {
				addFoldingRegions(regions, children, document);
			}
		}
	}

}
