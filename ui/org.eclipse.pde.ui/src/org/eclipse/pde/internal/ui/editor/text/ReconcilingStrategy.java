/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.ArrayList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;

public class ReconcilingStrategy implements IReconcilingStrategy {

	private IDocument fDocument;
	private ArrayList<IReconcilingParticipant> fParticipants = new ArrayList<>();

	public ReconcilingStrategy() {
	}

	@Override
	public void setDocument(IDocument document) {
		fDocument = document;
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		if (fDocument != null)
			notifyParticipants();
	}

	@Override
	public void reconcile(IRegion partition) {
		if (fDocument != null)
			notifyParticipants();
	}

	private synchronized void notifyParticipants() {
		for (int i = 0; i < fParticipants.size(); i++) {
			fParticipants.get(i).reconciled(fDocument);
		}
	}

	public void addParticipant(IReconcilingParticipant participant) {
		fParticipants.add(participant);
	}

	public void removeParticipant(IReconcilingParticipant participant) {
		fParticipants.remove(participant);
	}
}
