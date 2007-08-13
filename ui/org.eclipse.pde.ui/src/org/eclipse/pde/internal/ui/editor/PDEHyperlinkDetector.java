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

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

/**
 * PDEHyperlinkDetector
 *
 */
public abstract class PDEHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;	

	/**
	 * @param page
	 */
	public PDEHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			IRegion region, boolean canShowMultipleHyperlinks) {

		if (region == null || canShowMultipleHyperlinks)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset(), true);
		if (!XMLUtil.withinRange(element, region.getOffset()))
			return null;
		
		if (element instanceof IDocumentAttribute)
			return detectAttributeHyperlink((IDocumentAttribute)element);
		if (element instanceof IDocumentNode)
			return detectNodeHyperlink((IDocumentNode)element);
		if (element instanceof IDocumentTextNode)
			return detectTextNodeHyperlink((IDocumentTextNode)element);
		return null;		
	}

	/**
	 * @param attr
	 * @return
	 */
	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttribute attr) {
		return null;
	}
	
	/**
	 * @param node
	 * @return
	 */
	protected IHyperlink[] detectNodeHyperlink(IDocumentNode node) {
		return null;
	}
	
	/**
	 * @param node
	 * @return
	 */
	protected IHyperlink[] detectTextNodeHyperlink(IDocumentTextNode node) {
		return null;
	}
	
}
