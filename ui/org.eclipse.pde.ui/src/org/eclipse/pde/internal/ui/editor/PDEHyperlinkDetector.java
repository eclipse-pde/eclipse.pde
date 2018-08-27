/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

public abstract class PDEHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;

	public PDEHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {

		if (region == null)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset(), true);
		if (!XMLUtil.withinRange(element, region.getOffset()))
			return null;

		if (element instanceof IDocumentAttributeNode)
			return detectAttributeHyperlink((IDocumentAttributeNode) element);
		if (element instanceof IDocumentElementNode)
			return detectNodeHyperlink((IDocumentElementNode) element);
		if (element instanceof IDocumentTextNode)
			return detectTextNodeHyperlink((IDocumentTextNode) element);
		return null;
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttributeNode attr) {
		return null;
	}

	protected IHyperlink[] detectNodeHyperlink(IDocumentElementNode node) {
		return null;
	}

	protected IHyperlink[] detectTextNodeHyperlink(IDocumentTextNode node) {
		return null;
	}

}
