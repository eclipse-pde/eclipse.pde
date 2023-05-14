/*******************************************************************************
 *  Copyright (c) 2005, 2020 IBM Corporation and others.
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

import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public abstract class AbstractXMLMarkerResolution extends AbstractPDEMarkerResolution {

	public AbstractXMLMarkerResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);

	}

	protected abstract void createChange(IPluginModelBase model);

	@Override
	protected void createChange(IBaseModel model) {
		if (model instanceof IPluginModelBase)
			createChange((IPluginModelBase) model);
	}

	protected Object findNode(IPluginModelBase base) {
		String locationPath = null;
		try {
			locationPath = (String) marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
		if (locationPath == null)
			return null;

		// special case for externalizing strings in manifest.mf
		if (locationPath.charAt(0) != '(' && base instanceof IBundlePluginModelBase) {
			IBundle bundle = ((IBundlePluginModelBase) base).getBundleModel().getBundle();
			return bundle.getManifestHeader(locationPath);
		}

		IDocumentElementNode node = null;
		StringTokenizer strtok = new StringTokenizer(locationPath, Character.toString(XMLErrorReporter.F_CHILD_SEP));
		while (strtok.hasMoreTokens()) {
			String token = strtok.nextToken();
			if (node != null) {
				IDocumentElementNode[] children = node.getChildNodes();
				int childIndex = Integer.parseInt(token.substring(1, token.indexOf(')')));
				if ((childIndex >= 0) || (childIndex < children.length)) {
					node = children[childIndex];
				}
				// when externalizing Strings in plugin.xml, we pass in both Manifest and plug-in file (bug 172080 comment #1)
			} else if (base instanceof IBundlePluginModelBase) {
				ISharedExtensionsModel sharedModel = ((IBundlePluginModelBase) base).getExtensionsModel();
				if (sharedModel instanceof IPluginModelBase)
					node = (IDocumentElementNode) ((IPluginModelBase) sharedModel).getPluginBase();
			} else
				node = (IDocumentElementNode) base.getPluginBase();

			int attr = token.indexOf(XMLErrorReporter.F_ATT_PREFIX);
			if (attr != -1) {
				int valueIndex = token.indexOf(XMLErrorReporter.F_ATT_VALUE_PREFIX);
				if (valueIndex == -1)
					return node.getDocumentAttribute(token.substring(attr + 1));
				return node.getDocumentAttribute(token.substring(attr + 1, valueIndex));
			}
		}
		return node;
	}

	protected String getNameOfNode() {
		String fLocationPath = null;
		try {
			fLocationPath = (String) marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
		int lastChild = fLocationPath.lastIndexOf(')');
		if (lastChild < 0)
			return fLocationPath;
		String item = fLocationPath.substring(lastChild + 1);
		lastChild = item.indexOf(XMLErrorReporter.F_ATT_PREFIX);
		if (lastChild == -1)
			return item;
		int valueIndex = item.indexOf(XMLErrorReporter.F_ATT_VALUE_PREFIX);
		if (valueIndex == -1)
			return item.substring(lastChild + 1);
		return item.substring(valueIndex + 1);
	}

	protected boolean isAttrNode() {
		String fLocationPath = null;
		try {
			fLocationPath = (String) marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
		return fLocationPath.indexOf(XMLErrorReporter.F_ATT_PREFIX) != -1;
	}
}
