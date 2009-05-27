/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.*;

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
		if (region == null)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset(), false);
		if (!(element instanceof ManifestHeader))
			return null;

		ManifestHeader header = (ManifestHeader) element;
		if (!header.getModel().isEditable())
			return null;

		String headerName = header.getName();
		if (region.getOffset() <= header.getOffset() + headerName.length())
			return null;

		String[] translatable = ICoreConstants.TRANSLATABLE_HEADERS;
		for (int i = 0; i < translatable.length; i++) {
			if (!headerName.equals(translatable[i]))
				continue;
			String value = header.getValue();
			if (value == null || value.length() == 0 || value.charAt(0) != '%')
				break;

			IDocumentRange range = BundleSourcePage.getSpecificRange(header.getModel(), header, value);
			return new IHyperlink[] {new TranslationHyperlink(new Region(range.getOffset(), range.getLength()), value, header.getModel())};
		}

		if (header instanceof BundleActivatorHeader) { // add else if statments for other headers
			String target = ((BundleActivatorHeader) element).getClassName();
			if (target == null || target.length() == 0)
				return null;
			IDocumentRange range = BundleSourcePage.getSpecificRange(header.getModel(), header, target);
			if (range == null)
				return null;
			return new IHyperlink[] {new JavaHyperlink(new Region(range.getOffset(), range.getLength()), target, header.getModel().getUnderlyingResource())};
		} else if (header instanceof BasePackageHeader || header instanceof RequireBundleHeader) {
			return matchLinkFor(header, region.getOffset());
		}
		return null;
	}

	private IHyperlink[] matchLinkFor(ManifestHeader header, int mainOffset) {
		IDocument doc = ((IEditingModel) header.getModel()).getDocument();
		String value;
		try {
			value = doc.get(header.getOffset(), header.getLength());
			int offset = mainOffset - header.getOffset();
			// Our offset falls outside the value range
			if (offset >= value.length()) {
				return null;
			}

			// ensure we are over a letter or period
			char c = value.charAt(offset);
			if (!elementChar(c, true))
				return null;

			// scan backwards to find the start of the word
			int downOffset = offset;
			c = value.charAt(--downOffset);
			while (c != ',' && c != ':' && downOffset > 0) {
				// c == ';' means we are at a directive / attribute name (NOT value)
				if (c == ';' || !elementChar(c, false))
					return null;
				downOffset -= 1;
				c = value.charAt(downOffset);
			}
			// backtrack forwards to remove whitespace etc.
			while (downOffset < offset && !elementChar(c, true))
				c = value.charAt(++downOffset);

			// scan forwards to find the end of the word
			int upOffset = offset;
			c = value.charAt(upOffset);
			int length = value.length();
			while (c != ';' && c != ',' && upOffset < length - 1) {
				if (!elementChar(c, false))
					return null;
				upOffset += 1;
				c = value.charAt(upOffset);
			}
			// backtrack to remove extra chars
			if (c == ';' || c == ',')
				upOffset -= 1;

			String match = value.substring(downOffset, upOffset + 1);
			if (match.length() > 0) {
				IRegion region = new Region(mainOffset - (offset - downOffset), match.length());
				if (header instanceof BasePackageHeader)
					return new IHyperlink[] {new PackageHyperlink(region, match, (BasePackageHeader) header)};
				if (header instanceof RequireBundleHeader)
					return new IHyperlink[] {new BundleHyperlink(region, match)};
			}
		} catch (BadLocationException e) {
		}
		return null;
	}

	private boolean elementChar(char c, boolean noWhitespace) {
		if (noWhitespace && Character.isWhitespace(c))
			return false;
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '.' || Character.isWhitespace(c);
	}

}
