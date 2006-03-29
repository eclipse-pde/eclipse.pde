package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.nls.GetNonExternalizedStringsAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class ExternalizeStringsResolution extends AbstractPDEMarkerResolution {
	
	public ExternalizeStringsResolution(int type) {
		super(type);
	}
	
	public void run(final IMarker marker) {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
			public void run() {
				GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
				IStructuredSelection selection = new StructuredSelection(marker.getResource().getProject());
				fGetExternAction.selectionChanged(null, selection);
				fGetExternAction.run(null);
			}
		});
	}
	
	protected void createChange(IBaseModel model) {
		// nothin to do - all handled by run
	}

	public String getDescription() {
		return PDEUIMessages.ExternalizeStringsResolution_desc;
	}

	public String getLabel() {
		return PDEUIMessages.ExternalizeStringsResolution_label;
	}
	
	protected IModelTextChangeListener createListener(IDocument doc) {
		// all handled by run
		return null;
	}

	protected IModel loadModel(IDocument doc) {
		// all handled by run
		return null;
	}
	
}
