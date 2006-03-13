package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.nls.GetNonExternalizedStringsAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class ExternalizeStringsResolution extends ManifestHeaderErrorResolution {

	public ExternalizeStringsResolution(int type) {
		super(type);
	}

	protected void createChange(final BundleModel model) {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
			public void run() {
				GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
				IStructuredSelection selection = new StructuredSelection(model.getUnderlyingResource().getProject());
				fGetExternAction.selectionChanged(null, selection);
				fGetExternAction.run(null);
			}
		});
	}

	public String getDescription() {
		return PDEUIMessages.ExternalizeStringsResolution_desc;
	}

	public String getLabel() {
		return PDEUIMessages.ExternalizeStringsResolution_label;
	}
}
