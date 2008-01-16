/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.toc;

import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.ui.editor.AbstractFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class TocFoldingStructureProvider extends AbstractFoldingStructureProvider {

	private Map fPositionToElement = new HashMap();

	public TocFoldingStructureProvider(PDESourcePage editor, IEditingModel model) {
		super(editor, model);
	}

	public void addFoldingRegions(Set currentRegions, IEditingModel model) throws BadLocationException {
		TocObject toc = ((TocModel) model).getToc();
		List childList = toc.getChildren();
		IDocumentElementNode[] children = (IDocumentElementNode[]) childList.toArray(new IDocumentElementNode[childList.size()]);

		addFoldingRegions(currentRegions, children, model.getDocument());
	}

	private void addFoldingRegions(Set regions, IDocumentElementNode[] nodes, IDocument document) throws BadLocationException {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentElementNode element = nodes[i];
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
