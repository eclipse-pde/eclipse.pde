package org.eclipse.pde.internal.ui.neweditor.build;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.text.*;
import org.eclipse.swt.graphics.Image;
/**
 * @author melhem
 *  
 */
public class BuildSourcePage extends PDESourcePage {
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
	class BuildOutlineContentProvider extends DefaultContentProvider
			implements
				ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			if (child instanceof IBuildEntry)
				return ((IBuildEntry) child).getModel();
			return null;
		}
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuildModel model = (IBuildModel) parent;
				IBuild build = model.getBuild();
				return build.getBuildEntries();
			}
			return new Object[0];
		}
	}
	class BuildLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IBuildEntry) {
				return ((IBuildEntry) obj).getName();
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof IBuildEntry)
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_BUILD_VAR_OBJ);
			return null;
		}
	}
	public BuildSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setSourceViewerConfiguration(new BuildSourceViewerConfiguration());
	}
	protected ILabelProvider createOutlineLabelProvider() {
		return new BuildLabelProvider();
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
	protected ITreeContentProvider createOutlineContentProvider() {
		return new BuildOutlineContentProvider();
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