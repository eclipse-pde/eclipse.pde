package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.ui.neweditor.text.*;

/**
 * @author melhem
 *
 */
public class BuildSourcePage extends PDESourcePage {
	
	class BuildSourceViewerConfiguration extends SourceViewerConfiguration {
		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			ReconcilingStrategy strategy = new ReconcilingStrategy();
			strategy.addParticipant((IReconcilingParticipant)getInputContext().getModel());
			MonoReconciler reconciler = new MonoReconciler(strategy, false);
			reconciler.setDelay(500);
			return reconciler;
		}
	}
	public BuildSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new BuildSourceViewerConfiguration());
	}
}
