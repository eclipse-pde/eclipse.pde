package org.eclipse.pde.internal.ui.neweditor.text;

import org.eclipse.jface.text.*;

/**
 * @author melhem
 *
 */
public interface IReconcilingParticipant {
	
	void reconciled(IDocument document);
}
