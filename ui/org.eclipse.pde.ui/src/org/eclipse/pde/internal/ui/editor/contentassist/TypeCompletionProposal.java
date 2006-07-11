/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class TypeCompletionProposal implements ICompletionProposal {

	protected String fReplacementString;
	protected Image fImage;
	protected String fDisplayString;
	protected int fBeginInsertPoint;
	protected int fLength;
	
	public TypeCompletionProposal(String replacementString, Image image, String displayString) {
		this(replacementString, image, displayString, 0, 0);
	}
	
	public TypeCompletionProposal(String replacementString, Image image, String displayString, int startOffset, int length) {
		Assert.isNotNull(replacementString);
		
		fReplacementString = replacementString;
		fImage = image;
		fDisplayString = displayString;
		fBeginInsertPoint = startOffset;
		fLength = length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		if (fLength == -1) {
			String current = document.get();
			fLength = current.length();
		}
		try {
			document.replace(fBeginInsertPoint, fLength, fReplacementString);
		} catch (BadLocationException e) {
			// DEBUG
			// e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		// No additional proposal information
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		// No context information
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fDisplayString;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (fReplacementString.equals("\"\"")) //$NON-NLS-1$
			return new Point(fBeginInsertPoint + 1, 0);
		return new Point(fBeginInsertPoint + fReplacementString.length(), 0);
	}
	
	/**
	 * @return
	 */
	public String getReplacementString() {
		return fReplacementString;
	}

}
