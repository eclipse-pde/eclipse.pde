package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;

public class CreateManifestClassResolution extends AbstractManifestMarkerResolution {

	private String fHeader;
	
	public CreateManifestClassResolution(int type, String headerName) {
		super(type);
		fHeader = headerName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);
		
		String name = header.getValue();
		name = trimNonAlphaChars(name).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();
		
		IPluginModelBase modelBase = PDECore.getDefault().getModelManager().findModel(project);
		if (modelBase == null)
			return;
		
		JavaAttributeValue value = new JavaAttributeValue(project, modelBase, null, name);
		name = createClass(name, modelBase, value);
		if (!name.equals(header.getValue())) 
			header.setValue(name);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.CreateManifestClassResolution_label, fHeader);
	}

}
