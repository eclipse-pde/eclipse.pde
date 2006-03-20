package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.core.text.plugin.PluginObjectNode;
import org.eclipse.pde.internal.core.text.plugin.PluginParentNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveNodeXMLResolution extends AbstractXMLMarkerResolution {

	public RemoveNodeXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(PluginModelBase model) {
		PluginObjectNode node = findNode(model, fLocationPath);
		try {
			IDocumentNode parent = node.getParentNode();
			if (parent instanceof PluginParentNode)
				((PluginParentNode)parent).remove(node);
			if (parent instanceof PluginBaseNode)
				((PluginBaseNode)parent).remove(node);
		} catch (CoreException e) {
		}
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		int lastChild = fLocationPath.lastIndexOf(')');
		if (lastChild < 0)
			return fLocationPath;
		String item = fLocationPath.substring(lastChild + 1);
		int attrInd = item.indexOf(XMLErrorReporter.F_ATT_PREFIX);
		if (attrInd > -1)
			item = item.substring(attrInd + 1);
		return NLS.bind(
				attrInd == -1 ? PDEUIMessages.RemoveNodeXMLResolution_label :
							"Remove the {0} attribute.",
				item);
	}

}
