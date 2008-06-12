/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 233997
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class DSCompletionProposal implements ICompletionProposal {

	IDSObject fObject;

	public DSCompletionProposal(IDSObject object) {
		fObject = object;
	}

	public void apply(IDocument document) {
		// TODO Auto-generated method stub
	}

	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public IContextInformation getContextInformation() {
		// TODO Auto-generated method stub
		return null;
	}

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

		case IDSConstants.TYPE_ROOT:
			return IDSConstants.ELEMENT_COMPONENT;

		default:
			return fObject.getName();
		}
	}

	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	public Point getSelection(IDocument document) {
		// TODO Auto-generated method stub
		return null;
	}

}
