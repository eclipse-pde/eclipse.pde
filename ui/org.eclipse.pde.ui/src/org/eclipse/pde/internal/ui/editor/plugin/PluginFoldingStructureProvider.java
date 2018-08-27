/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.ui.editor.AbstractFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class PluginFoldingStructureProvider extends AbstractFoldingStructureProvider {

	private Map<Position, IDocumentElementNode> fPositionToElement = new HashMap<>();

	public PluginFoldingStructureProvider(PDESourcePage editor, IEditingModel model) {
		super(editor, model);
	}

	@Override
	public void addFoldingRegions(Set<Position> currentRegions, IEditingModel model) throws BadLocationException {
		IExtensions extensions = ((IPluginModelBase) model).getExtensions();
		IPluginExtension[] pluginExtensions = extensions.getExtensions();

		addFoldingRegions(currentRegions, pluginExtensions, model.getDocument());
	}

	private void addFoldingRegions(Set<Position> regions, IPluginExtension[] nodes, IDocument document) throws BadLocationException {
		for (IPluginExtension node : nodes) {
			IDocumentElementNode element = (IDocumentElementNode) node;
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

	private void addFoldingRegions(Set<Position> regions, IDocumentElementNode[] nodes, IDocument document) throws BadLocationException {
		for (IDocumentElementNode node : nodes) {
			int startLine = document.getLineOfOffset(node.getOffset());
			int endLine = document.getLineOfOffset(node.getOffset() + node.getLength());
			if (startLine < endLine) {
				int start = document.getLineOffset(startLine);
				int end = document.getLineOffset(endLine) + document.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, node);
			}
			IDocumentElementNode[] children = node.getChildNodes();
			if (children != null) {
				addFoldingRegions(regions, children, document);
			}
		}
	}

}
