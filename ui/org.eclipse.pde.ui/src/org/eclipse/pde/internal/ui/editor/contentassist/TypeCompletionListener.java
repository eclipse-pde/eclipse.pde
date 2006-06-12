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

import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;

/**
 * TypeCompletionListener
 *
 */
public class TypeCompletionListener implements ICompletionListener {
	/**
	 * 
	 */
	public TypeCompletionListener() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionEnded(org.eclipse.jface.text.contentassist.ContentAssistEvent)
	 */
	public void assistSessionEnded(ContentAssistEvent event) {
		IContentAssistProcessor processor = event.processor;
		if (processor instanceof TypeCompletionProcessor) {
			((TypeCompletionProcessor)processor).assistSessionEnded();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionStarted(org.eclipse.jface.text.contentassist.ContentAssistEvent)
	 */
	public void assistSessionStarted(ContentAssistEvent event) {
		// Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionListener#selectionChanged(org.eclipse.jface.text.contentassist.ICompletionProposal, boolean)
	 */
	public void selectionChanged(ICompletionProposal proposal,
			boolean smartToggle) {
		// Do nothing
	}
}
