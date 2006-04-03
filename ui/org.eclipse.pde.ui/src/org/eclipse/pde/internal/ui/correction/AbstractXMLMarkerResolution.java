/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginObjectNode;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;

public abstract class AbstractXMLMarkerResolution extends AbstractPDEMarkerResolution {

	protected String fLocationPath;
	
	public AbstractXMLMarkerResolution(int resolutionType, IMarker marker) {
		super(resolutionType);
		try {
			fLocationPath = (String)marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH);
		} catch (CoreException e) {
		}
	}
	
	protected IModel loadModel(IDocument doc) {
		AbstractEditingModel model;
		// if externalizing MANIFEST.MF strings - using xml class
		// since we need IPluginModelBase
		if (fResource.getName().equals("MANIFEST.MF")) //$NON-NLS-1$
			model = new BundleModel(doc, true);
		else if (fResource.getName().equals("fragment.xml")) //$NON-NLS-1$
			model = new FragmentModel(doc, true);
		else
			model = new PluginModel(doc, true);
		model.setUnderlyingResource(fResource);
		try {
			model.load();
		} catch (CoreException e) {
		}
		return model;
	}
	
	protected abstract void createChange(IPluginModelBase model);
	
	protected void createChange(IBaseModel model) {
		if (model instanceof IBundleModel) {
			BundlePluginModel pluginModel = new BundlePluginModel();
			pluginModel.setBundleModel((IBundleModel)model);
			createChange(pluginModel);
		} else if (model instanceof IPluginModelBase)
			createChange((IPluginModelBase)model);
	}
	
	protected IModelTextChangeListener createListener(IDocument doc) {
		if (fResource.getName().equals("MANIFEST.MF")) //$NON-NLS-1$
			return new BundleTextChangeListener(doc);
		return new XMLTextChangeListener(doc);
	}
	
	protected Object findNode(IPluginModelBase base) {
		if (fLocationPath == null)
			return null;
		
		// special case for externalizing strings in manifest.mf
		if (fLocationPath.charAt(0) != '(' &&
				base instanceof IBundlePluginModelBase) {
			IBundle bundle = ((IBundlePluginModelBase)base).getBundleModel().getBundle();
			return bundle.getManifestHeader(fLocationPath);
		}
		
		IDocumentNode node = (PluginObjectNode)base.getPluginBase();
		StringTokenizer strtok = new StringTokenizer(
				fLocationPath,
				Character.toString(XMLErrorReporter.F_CHILD_SEP));
		while (node != null && strtok.hasMoreTokens()) {
			String token = strtok.nextToken();
			int childIndex = Integer.parseInt(token.substring(1, token.indexOf(')')));
			token = token.substring(token.indexOf(')') + 1);
			IDocumentNode[] children = node.getChildNodes();
			node = children[childIndex];
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
		return fLocationPath.indexOf(XMLErrorReporter.F_ATT_PREFIX) != -1;
	}
}
