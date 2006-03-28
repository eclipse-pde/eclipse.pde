package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveNodeXMLResolution extends AbstractXMLMarkerResolution {

	public RemoveNodeXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IPluginModelBase model) {
		Object node = findNode(model);
		if (!(node instanceof IPluginObject))
			return;
		try {
			IPluginObject pluginObject = (IPluginObject)node;
			IPluginObject parent = pluginObject.getParent();
			if (parent instanceof IPluginParent)
				((IPluginParent)parent).remove(pluginObject);
			else if (parent instanceof PluginBaseNode)
				((PluginBaseNode)parent).remove(pluginObject);
			else if (pluginObject instanceof PluginAttribute) {
				PluginAttribute attr = (PluginAttribute)pluginObject;
				attr.getEnclosingElement().setXMLAttribute(attr.getName(), null);
			}
				
		} catch (CoreException e) {
		}
	}

	public String getLabel() {
		if (isAttrNode())
			return NLS.bind(PDEUIMessages.RemoveNodeXMLResolution_attrLabel, getNameOfNode());
		return NLS.bind(PDEUIMessages.RemoveNodeXMLResolution_label, getNameOfNode());
	}

}
