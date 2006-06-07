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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.bundle.BundleActivatorHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.JavaHyperlink;

public class BundleHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;
	
	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public BundleHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || canShowMultipleHyperlinks)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset());
		if (!(element instanceof ManifestHeader))
			return null;
		
		ManifestHeader header = (ManifestHeader)element;
		String target = null;
		if (element instanceof BundleActivatorHeader) { // add else if statments for other headers
			target = ((BundleActivatorHeader)element).getClassName();
		}
		
		if (target == null || target.length() == 0)
			return null;
		
		IProject project = header.getModel().getUnderlyingResource().getProject();
		IDocumentRange range = BundleSourcePage.getSpecificRange(header.getModel(), header, target);
		if (range == null)
			return null;
		IRegion linkRegion = new Region(range.getOffset(), range.getLength());
		
		if (element instanceof BundleActivatorHeader)
			return new IHyperlink[] { new JavaHyperlink(linkRegion, project, target)};
		
		return null;
	}
	
}
