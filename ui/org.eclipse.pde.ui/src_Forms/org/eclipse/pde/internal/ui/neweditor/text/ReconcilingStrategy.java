package org.eclipse.pde.internal.ui.neweditor.text;

import java.util.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;

/**
 * @author melhem
 *
 */
public class ReconcilingStrategy implements IReconcilingStrategy {
	
	private IDocument fDocument;
	private ArrayList fParticipants = new ArrayList();
	
	public ReconcilingStrategy () {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		fDocument = document;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		if (fDocument == null)
			return;
		notifyParticipants();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		if (fDocument == null)
			return;
		notifyParticipants();
	}
	
	private synchronized void notifyParticipants() {
		for (int i = 0; i < fParticipants.size(); i++) {
			((IReconcilingParticipant)fParticipants.get(i)).reconciled(fDocument);
		}
	}
	
	public void addParticipant(IReconcilingParticipant participant) {
		fParticipants.add(participant);
	}
	
	public void removeParticipant(IReconcilingParticipant participant) {
		fParticipants.remove(participant);
	}
}
