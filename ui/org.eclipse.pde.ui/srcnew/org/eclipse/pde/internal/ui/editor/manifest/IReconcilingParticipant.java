package org.eclipse.pde.internal.ui.editor.manifest;

/**
 * Interface of an object participating in reconciling.
 */
public interface IReconcilingParticipant {
	
	/**
	 * Called after reconciling has been finished.
	 */
	void reconciled();
}
