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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;

public class BuildHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;

	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public BuildHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset(), true);
		if (!(element instanceof BuildEntry))
			return null;

		BuildEntry entry = (BuildEntry) element;
		if (!entry.getModel().isEditable() || !(entry.getModel() instanceof IEditingModel))
			return null;

		String name = entry.getName();
		// as of now only scanning bin.includes, src.includes and source.* entries
		if (!name.equals(IBuildEntry.BIN_INCLUDES) && !name.equals(IBuildEntry.SRC_INCLUDES) && !name.startsWith(IBuildEntry.JAR_PREFIX))
			return null;

		if (region.getOffset() <= entry.getOffset() + entry.getName().length())
			return null;

		return matchLinkFor(entry, region.getOffset());
	}

	private IHyperlink[] matchLinkFor(BuildEntry header, int mainOffset) {
		try {
			IDocument doc = ((IEditingModel) header.getModel()).getDocument();
			String value = doc.get(header.getOffset(), header.getLength());
			int offset = mainOffset - header.getOffset();
			if (skipChar(value.charAt(offset)))
				return null;

			// all chars up to the offset
			String pre = value.substring(0, offset);
			char[] preChars = pre.toCharArray();
			int start = pre.lastIndexOf(',');
			if (start == -1)
				// we are looking at 1st entry, skip to ':'
				if ((start = value.indexOf('=')) == 0)
					return null;

			// skip to 1st non whitespace char
			while (++start < preChars.length)
				if (!skipChar(preChars[start]))
					break;

			// all chars past to ofset
			String post = value.substring(offset);
			char[] postChars = post.toCharArray();
			int end = post.indexOf(',');
			if (end == -1)
				// we are looking at last entry, skip to end
				end = post.length();

			// move back to 1st non whitespace char
			while (--end < postChars.length)
				if (!skipChar(postChars[end]))
					break;
			end += 1;

			String match = value.substring(start, preChars.length + end);
			if (match.length() == 0 || match.indexOf('*') != -1)
				return null;

			IResource res = header.getModel().getUnderlyingResource();
			if (res == null)
				return null;
			res = res.getProject().findMember(match);
			return new IHyperlink[] {new ResourceHyperlink(new Region(header.getOffset() + start, match.length()), match, res)};

		} catch (BadLocationException e) {
		}
		return null;
	}

	private boolean skipChar(char c) {
		return Character.isWhitespace(c) || c == '\\' || c == ',';
	}

}
