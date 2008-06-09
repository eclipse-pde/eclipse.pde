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
		return fObject.getName();
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
