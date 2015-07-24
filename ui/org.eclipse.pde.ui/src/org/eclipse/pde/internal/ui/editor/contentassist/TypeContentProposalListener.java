/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import org.eclipse.jface.fieldassist.*;

/**
 * TypeContentProposalListener
 *
 */
public class TypeContentProposalListener implements IContentProposalListener, IContentProposalListener2 {

	/**
	 * 
	 */
	public TypeContentProposalListener() {
		// NO-OP
	}

	@Override
	public void proposalAccepted(IContentProposal proposal) {
		// NO-OP
	}

	@Override
	public void proposalPopupClosed(ContentProposalAdapter adapter) {
		IContentProposalProvider provider = adapter.getContentProposalProvider();
		if (provider instanceof TypeContentProposalProvider) {
			// Reset state related information used for filtering existing proposals
			((TypeContentProposalProvider) provider).reset();
		}
	}

	@Override
	public void proposalPopupOpened(ContentProposalAdapter adapter) {
		// NO-OP
	}

}
