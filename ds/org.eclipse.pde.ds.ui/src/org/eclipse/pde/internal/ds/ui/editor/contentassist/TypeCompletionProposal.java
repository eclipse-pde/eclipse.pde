/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class TypeCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension5 {

	protected String fReplacementString;
	protected Image fImage;
	protected String fDisplayString;
	protected int fBeginInsertPoint;
	protected int fLength;
	protected String fAdditionalInfo;
	private IInformationControlCreator fCreator;

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

	@Override
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

	@Override
	public String getAdditionalProposalInfo() {
		// No additional proposal information
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		// No context information
		return null;
	}

	@Override
	public String getDisplayString() {
		return fDisplayString;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public Point getSelection(IDocument document) {
		if (fReplacementString.equals("\"\"")) //$NON-NLS-1$
			return new Point(fBeginInsertPoint + 1, 0);
		return new Point(fBeginInsertPoint + fReplacementString.length(), 0);
	}

	public String getReplacementString() {
		return fReplacementString;
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return fAdditionalInfo;
	}

	public void setAdditionalProposalInfo(String info) {
		fAdditionalInfo = info;
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		if (!BrowserInformationControl.isAvailable(null))
			return null;

		if (fCreator == null) {
			fCreator = new AbstractReusableInformationControlCreator() {

				@Override
				public IInformationControl doCreateInformationControl(Shell parent) {
					return new BrowserInformationControl(parent,
							JFaceResources.DIALOG_FONT, false);
				}
			};
		}
		return fCreator;
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return fBeginInsertPoint;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fReplacementString;
	}

}