package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;

public class ExternalizeXMLResolution extends AbstractXMLMarkerResolution {

	public ExternalizeXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(PluginModelBase model) {
		
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		int lastChild = fLocationPath.lastIndexOf('>');
		if (lastChild < 0)
			return fLocationPath;
		String item = fLocationPath.substring(lastChild + 1);
		return NLS.bind(
				"Externalize the {0} attribute.",
				item.substring(item.indexOf(')') + 1));
	}

}
