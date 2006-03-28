package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ChooseClassXMLResolution extends AbstractXMLMarkerResolution {

	public ChooseClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IPluginModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof PluginAttribute))
			return;
		PluginAttribute attrib = (PluginAttribute)object;
		IDocumentNode element = attrib.getEnclosingElement();
		String type = selectType();
		if (type != null)
			element.setXMLAttribute(attrib.getName(), type);
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return PDEUIMessages.ChooseClassXMLResolution_label;
	}

}
