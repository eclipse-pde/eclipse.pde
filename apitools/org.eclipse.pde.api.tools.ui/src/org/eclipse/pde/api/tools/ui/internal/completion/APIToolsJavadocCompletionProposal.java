/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.completion;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * This class provides a completion proposal for javadoc type extensions, more
 * specifically it is used to contribute the following tags:
 *
 * @since 1.0.0
 */
public class APIToolsJavadocCompletionProposal implements IJavaCompletionProposal, ICompletionProposalExtension2, ICompletionProposalExtension3 {

	private String fReplaceText = null;
	private String fDisplayText = null;
	private Image fImage = null;
	private CompletionContext fContext = null;

	/**
	 * Constructor
	 *
	 * @param replacetext
	 * @param displaytext
	 * @param image
	 */
	public APIToolsJavadocCompletionProposal(CompletionContext context, String replacetext, String displaytext, Image image) {
		fContext = context;
		fReplaceText = replacetext;
		fImage = image;
		fDisplayText = displaytext;
	}

	@Override
	public int getRelevance() {
		return 0;
	}

	@Override
	public void apply(IDocument document) {
	}

	@Override
	public String getAdditionalProposalInfo() {
		int index = fReplaceText.indexOf(fDisplayText);
		if (index > -1) {
			return fReplaceText.substring(index + fDisplayText.length());
		}
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fDisplayText;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			IDocument doc = viewer.getDocument();
			int start = fContext.getTokenStart();
			doc.replace(start, offset - start, fReplaceText);
		} catch (BadLocationException e) {
			ApiUIPlugin.log(e);
		}
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	@Override
	public void unselected(ITextViewer viewer) {
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			int start = fContext.getTokenStart();
			int length = offset - start;
			String prefix = document.get(start, length);
			if (length <= fDisplayText.length()) {
				if (prefix.equals(fDisplayText.substring(0, length))) {
					return true;
				}
			}
		} catch (BadLocationException e) {
		}
		return false;
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		return null;
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return fContext.getTokenStart();
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		if (validate(document, completionOffset, null)) {
			return fReplaceText;
		}
		return null;
	}
}
