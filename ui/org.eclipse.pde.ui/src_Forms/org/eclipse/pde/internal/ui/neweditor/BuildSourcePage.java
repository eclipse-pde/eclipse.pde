package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
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
	class BuildOutlineContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object [] getChildren(Object parent) {
			if (parent instanceof IBuild)
				return ((IBuild)parent).getBuildEntries();
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length>0;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object [] getElements(Object parent) {
			return new Object[0];
		}
	}
	class BuildLabelProvider extends LabelProvider {
		
	}
	public BuildSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new BuildSourceViewerConfiguration());
	}
	protected ILabelProvider createOutlineLabelProvider() {
		return new BuildLabelProvider();
	}
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BuildOutlineContentProvider();
	}
	protected void outlineSelectionChanged(SelectionChangedEvent e) {
	}
}