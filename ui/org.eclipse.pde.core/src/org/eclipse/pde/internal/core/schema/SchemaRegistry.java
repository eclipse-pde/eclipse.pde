/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;


public class SchemaRegistry {
	
	private HashMap fRegistry = new HashMap();
	
	public ISchema getSchema(String extPointID) {
		IPluginExtensionPoint point = PDECore.getDefault().findExtensionPoint(extPointID);
		if (point == null) {
			// if there is an old schema associated with this extension point, release it.
			if (fRegistry.containsKey(extPointID))
				fRegistry.remove(extPointID);
			return null;
		}
		
		URL url = getSchemaURL(point);
		if (url == null)
			return null;
		
		ISchemaDescriptor desc = getExistingDescriptor(extPointID, url);
		if (desc == null) {
			desc = new SchemaDescriptor(extPointID, url);
			fRegistry.put(extPointID, desc);
		}
		
		return (desc == null) ? null : desc.getSchema(true);
	}
	
	public ISchema getIncludedSchema(ISchemaDescriptor parent, String schemaLocation) {
		try {
			URL url = IncludedSchemaDescriptor.computeURL(parent, schemaLocation);
			if (url == null)
				return null;
			
			ISchemaDescriptor desc = getExistingDescriptor(url.toString(), url);
			if (desc == null) {
				desc = new IncludedSchemaDescriptor(url);
				fRegistry.put(url.toString(), desc);
			}
			return desc.getSchema(true);
		} catch (MalformedURLException e) {
		}
		return null;
	}
	
	private ISchemaDescriptor getExistingDescriptor(String key, URL url) {
		ISchemaDescriptor desc = null;
		if (fRegistry.containsKey(key)) {
			desc = (ISchemaDescriptor)fRegistry.get(key);
			if (hasSchemaChanged(desc, url))
				desc = null;
		}
		return desc;
	}
	
	public static URL getSchemaURL(IPluginExtensionPoint point) {
		String schema = point.getSchema();
		if (schema == null || schema.trim().length() == 0)
			return null;
		URL url = point.getModel().getResourceURL(schema);
		
		// try in the external plugin, if we did not find anything in workspace
		if (url == null && point.getModel().getUnderlyingResource() != null) {
			String pluginID = point.getPluginBase().getId();
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(pluginID);
			if (entry != null) {
				IPluginModelBase model = entry.getExternalModel();
				if (model != null) {
					url = model.getResourceURL(schema);
				}
			}
		}
		if (url == null) {
			try {
				SourceLocationManager mgr = PDECore.getDefault().getSourceLocationManager();
				File file = mgr.findSourceFile(point.getPluginBase(), new Path(schema));
				if (file != null && file.exists())
					url = file.toURL();
			} catch (MalformedURLException e) {
			}
		}
		return url;
	}
	
	private boolean hasSchemaChanged(ISchemaDescriptor desc, URL url) {
		if (!desc.getSchemaURL().equals(url))
			return true;
		File file = new File(url.getFile());
		return (desc.getLastModified() != file.lastModified());
	}
	
	public void shutdown() {
		fRegistry.clear();
	}

}
