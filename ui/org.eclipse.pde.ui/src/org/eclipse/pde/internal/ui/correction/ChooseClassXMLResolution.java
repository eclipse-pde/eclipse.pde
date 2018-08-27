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
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;

public class ChooseClassXMLResolution extends AbstractXMLMarkerResolution {

	public ChooseClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	@Override
	protected void createChange(IPluginModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof PluginAttribute))
			return;
		PluginAttribute attrib = (PluginAttribute) object;
		IDocumentElementNode element = attrib.getEnclosingElement();
		String type = PDEJavaHelperUI.selectType(fResource, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES);
		if (type != null)
			element.setXMLAttribute(attrib.getName(), type);
	}

	@Override
	public String getDescription() {
		return getLabel();
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.ChooseClassXMLResolution_label;
	}

}
