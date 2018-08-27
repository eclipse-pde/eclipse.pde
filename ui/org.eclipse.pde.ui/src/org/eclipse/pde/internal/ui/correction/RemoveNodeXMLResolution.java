/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveNodeXMLResolution extends AbstractXMLMarkerResolution {

	public RemoveNodeXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	@Override
	protected void createChange(IPluginModelBase model) {
		Object node = findNode(model);
		if (!(node instanceof IPluginObject))
			return;
		try {
			IPluginObject pluginObject = (IPluginObject) node;
			IPluginObject parent = pluginObject.getParent();
			if (parent instanceof IPluginParent)
				((IPluginParent) parent).remove(pluginObject);
			else if (parent instanceof PluginBaseNode)
				((PluginBaseNode) parent).remove(pluginObject);
			else if (pluginObject instanceof PluginAttribute) {
				PluginAttribute attr = (PluginAttribute) pluginObject;
				attr.getEnclosingElement().setXMLAttribute(attr.getName(), null);
			}

		} catch (CoreException e) {
		}
	}

	@Override
	public String getLabel() {
		if (isAttrNode())
			return NLS.bind(PDEUIMessages.RemoveNodeXMLResolution_attrLabel, getNameOfNode());
		return NLS.bind(PDEUIMessages.RemoveNodeXMLResolution_label, getNameOfNode());
	}

}
