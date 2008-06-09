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

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;

public class DSContentAssistProcessor extends TypePackageCompletionProcessor
		implements IContentAssistProcessor, ICompletionListener {
	protected boolean fAssistSessionStarted;

	private PDESourcePage fSourcePage;

	public DSContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
	}

	public void assistSessionStarted(ContentAssistEvent event) {
	}

	public void selectionChanged(ICompletionProposal proposal,
			boolean smartToggle) {
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int documentOffset) {

		// TODO This is a STUB!

		DSSourcePage sourcePage = (DSSourcePage) fSourcePage;
		IDocumentRange rangeElement = sourcePage.getRangeElement(viewer
				.getSelectedRange().x, true);

		if (rangeElement instanceof IDocumentAttributeNode) {
			rangeElement = ((IDocumentAttributeNode) rangeElement)
					.getEnclosingElement();
		}

		if (rangeElement instanceof IDocumentTextNode) {
			rangeElement = ((IDocumentTextNode) rangeElement)
					.getEnclosingElement();
		}

		if (rangeElement instanceof IDSObject) {
			return new ICompletionProposal[] { new DSCompletionProposal(
					(IDSObject) rangeElement) };
		} else {
			return null;
		}
		
		// TODO End of Stub.
	}

	public void dispose() {

	}
}
