package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ChooseManifestClassResolution extends AbstractManifestMarkerResolution {

	private String fHeader;
	
	public ChooseManifestClassResolution(int type, String headerName) {
		super(type);
		fHeader = headerName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);
		String type = selectType();
		if (type != null)
			header.setValue(type);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.ChooseManifestClassResolution_label, fHeader);
	}

}
