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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class DSCompletionProposal implements ICompletionProposal {

	private final IDSObject fObject;
	private final int fStartOffset;
	private final int fLength;

	public DSCompletionProposal(IDSObject object, int startOffset) {
		this(object, startOffset, 0);
	}

	public DSCompletionProposal(IDSObject object, int startOffset, int length) {
		fObject = object;
		fStartOffset = startOffset;
		fLength = length;
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(fStartOffset, fLength, fObject.toString());
		} catch (BadLocationException e) {
			// DEBUG
			// e.printStackTrace();
		}
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayString() {
		return this.getTextbyType(fObject.getType());
	}

	private String getTextbyType(int type) {
		switch (type) {
		case IDSConstants.TYPE_PROPERTIES:
			return IDSConstants.ELEMENT_PROPERTIES;

		case IDSConstants.TYPE_PROPERTY:
			return IDSConstants.ELEMENT_PROPERTY;

		case IDSConstants.TYPE_PROVIDE:
			return IDSConstants.ELEMENT_PROVIDE;

		case IDSConstants.TYPE_REFERENCE:
			return IDSConstants.ELEMENT_REFERENCE;

		case IDSConstants.TYPE_SERVICE:
			return IDSConstants.ELEMENT_SERVICE;

		case IDSConstants.TYPE_IMPLEMENTATION:
			return IDSConstants.ELEMENT_IMPLEMENTATION;

		case IDSConstants.TYPE_COMPONENT:
			return IDSConstants.ELEMENT_COMPONENT;

		default:
			return null;
		}
	}

	@Override
	public Image getImage() {
		if (fObject.getType() == IDSConstants.TYPE_IMPLEMENTATION) {
			return SharedImages.getImage(SharedImages.DESC_IMPLEMENTATION);
		} else if (fObject.getType() == IDSConstants.TYPE_PROPERTIES) {
			return SharedImages.getImage(SharedImages.DESC_PROPERTIES);
		} else if (fObject.getType() == IDSConstants.TYPE_PROPERTY) {
			return SharedImages.getImage(SharedImages.DESC_PROPERTY);
		} else if (fObject.getType() == IDSConstants.TYPE_PROVIDE) {
			return SharedImages.getImage(SharedImages.DESC_PROVIDE);
		} else if (fObject.getType() == IDSConstants.TYPE_REFERENCE) {
			return SharedImages.getImage(SharedImages.DESC_REFERENCE);
		} else if (fObject.getType() == IDSConstants.TYPE_COMPONENT) {
			return SharedImages.getImage(SharedImages.DESC_ROOT);
		} else if (fObject.getType() == IDSConstants.TYPE_SERVICE) {
			return SharedImages.getImage(SharedImages.DESC_SERVICE);
		}
		return null;
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(fStartOffset + fObject.toString().length(), 0);
	}

}
