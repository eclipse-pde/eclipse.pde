package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;

/**
 * @author melhem
 *
 */
public interface IReconcilingParticipant {
	
	void reconciled(IDocument document);
}
