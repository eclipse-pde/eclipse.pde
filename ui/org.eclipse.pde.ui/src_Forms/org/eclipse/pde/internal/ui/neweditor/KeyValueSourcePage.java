package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.neweditor.text.*;

public abstract class KeyValueSourcePage extends PDESourcePage {

	class BuildSourceViewerConfiguration extends SourceViewerConfiguration {
		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			ReconcilingStrategy strategy = new ReconcilingStrategy();
			strategy.addParticipant((IReconcilingParticipant) getInputContext()
					.getModel());
			MonoReconciler reconciler = new MonoReconciler(strategy, false);
			reconciler.setDelay(500);
			return reconciler;
		}
	}
	public KeyValueSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new BuildSourceViewerConfiguration());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESourcePage#createViewerSorter()
	 */
	protected ViewerSorter createViewerSorter() {
		return new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IDocumentKey key1 = (IDocumentKey)e1;
				IDocumentKey key2 = (IDocumentKey)e2;
				return key1.getOffset() < key2.getOffset() ? -1 : 1;
			}
		};
	}
	
	protected void outlineSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentKey) {
				setHighlightRange((IDocumentKey)first);				
			} else {
				resetHighlightRange();
			}
		}
	}
	
	public void setHighlightRange(IDocumentKey key) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = key.getOffset();
		int length = key.getLength();
		setHighlightRange(offset, length, true);
		sourceViewer.setSelectedRange(offset, key.getName().length());
	}

}
