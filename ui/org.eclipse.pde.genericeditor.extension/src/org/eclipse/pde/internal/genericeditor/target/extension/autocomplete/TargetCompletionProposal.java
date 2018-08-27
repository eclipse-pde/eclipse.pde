/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class TargetCompletionProposal implements ICompletionProposal, ICompletionProposalExtension6 {

	/** The string to be displayed in the completion proposal pop up. */
	private StyledString fDisplayString;
	/** The replacement string. */
	private String fReplacementString;
	/** The replacement offset. */
	private int fReplacementOffset;
	/** The replacement length. */
	private int fReplacementLength;
	/** The cursor position after this proposal has been applied. */
	private int fCursorPosition;

	/**
	 * Creates a new completion proposal. All fields are initialized based on the
	 * provided information.
	 *
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to
	 *            replacementOffset
	 * @param replacementOffset
	 *            the offset of the text to be replaced
	 * @param replacementLength
	 *            the length of the text to be replaced
	 * @param displayString
	 *            the string to be displayed for the proposal
	 */
	public TargetCompletionProposal(String replacementString, int cursorPosition, int replacementOffset,
			int replacementLength,
			StyledString displayString) {
		Assert.isNotNull(replacementString);
		Assert.isTrue(cursorPosition >= 0);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);
		Assert.isNotNull(displayString);
		fReplacementString = replacementString;
		fCursorPosition = cursorPosition;
		fReplacementOffset = replacementOffset;
		fReplacementLength = replacementLength;
		fDisplayString = displayString;
	}

	@Override
	public StyledString getStyledDisplayString() {
		return fDisplayString;
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
		} catch (BadLocationException x) {
			// ignore
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fDisplayString.toString();
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

}
