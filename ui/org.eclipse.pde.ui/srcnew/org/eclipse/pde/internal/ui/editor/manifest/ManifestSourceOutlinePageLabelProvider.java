/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.pde.internal.ui.editor.manifest;


import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.plugin.PluginDocumentNode;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Node;


/**
 * Label provider for the plugin.xml model.
 */
public class ManifestSourceOutlinePageLabelProvider extends LabelProvider {
	
	public String getText(Object obj) {
		String result= null;
		
		if (obj instanceof PluginDocumentNode) {
			IPluginObject pluginObject= ((PluginDocumentNode)obj).getPluginObjectNode();
			if (pluginObject != null) {
				result= getLabel(pluginObject);
			}
			if (result == null) {
				Node domNode= ((PluginDocumentNode)obj).getDOMNode();
				if (domNode != null) {
					result= domNode.getNodeName();
					if (result.length() > 0) {
						result= result.substring(0, 1).toUpperCase() + result.substring(1).toLowerCase();
					}
				}
			}
		}
		
		if (result == null) {
			result= super.getText(obj);
		}
		
		if (result == null) {
			result= "##unknown##";
		}
		
		return result;
	}

	private String getLabel(IPluginObject pluginObject) {
		boolean fullNames = MainPreferencePage.isFullNameModeEnabled();
		if (pluginObject instanceof IPluginBase) {
			IPluginBase pluginBase = (IPluginBase) pluginObject;
			String pluginBaseName = pluginBase.getName();
			if (!fullNames)
				return pluginBaseName;
			return pluginBase.getResourceString(pluginBaseName);
		}
		if (pluginObject instanceof IPluginImport) {
			String pluginId = ((IPluginImport) pluginObject).getId();
			if (!fullNames)
				return pluginId;
			IPlugin plugin = PDECore.getDefault().findPlugin(pluginId);
			if (plugin != null)
				return plugin.getResourceString(plugin.getName());
			return pluginId;
		}
		if (pluginObject instanceof IPluginLibrary) {
			return ((IPluginLibrary) pluginObject).getName();
		}
		
		if (pluginObject instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) pluginObject;
			if (!fullNames)
				return extension.getPoint();
			ISchema schema =
				PDECore.getDefault().getSchemaRegistry().getSchema(extension.getPoint());
		
			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			// try extension point declaration
			IPluginExtensionPoint pointInfo =
				PDECore.getDefault().getExternalModelManager().findExtensionPoint(
					extension.getPoint());
			if (pointInfo != null) {
				return pointInfo.getResourceString(pointInfo.getName());
			}
		}
		if (pluginObject instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint point = (IPluginExtensionPoint) pluginObject;
			if (!fullNames)
				return point.getId();
			return point.getTranslatedName();
		}
		return null;
	}

	public Image getImage(Object obj) {
		if (obj instanceof PluginDocumentNode) {
			obj= ((PluginDocumentNode)obj).getPluginObjectNode();
		}

		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image image = provider.getImage(obj);
		if (image != null)
			return image;
		return super.getImage(obj);
	}

}
