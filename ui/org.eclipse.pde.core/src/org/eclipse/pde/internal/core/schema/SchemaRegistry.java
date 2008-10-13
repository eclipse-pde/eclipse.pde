/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class SchemaRegistry {

	private HashMap fRegistry = new HashMap();

	public ISchema getSchema(String extPointID) {
		IPluginExtensionPoint point = PDECore.getDefault().getExtensionsRegistry().findExtensionPoint(extPointID);
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
			desc = (ISchemaDescriptor) fRegistry.get(key);
			if (hasSchemaChanged(desc, url))
				desc = null;
		}
		return desc;
	}

	public static URL getSchemaURL(IPluginExtensionPoint point, IPluginModelBase base) {
		URL url = getSchemaURL(point);
		if (url != null) {
			return url;
		}
		String schema = point.getSchema();
		if ((schema == null) || (schema.trim().length() == 0)) {
			return null;
		}
		return getSchemaURL(getId(point, base), schema);
	}

	public static URL getSchemaURL(IPluginExtensionPoint point) {
		String schema = point.getSchema();
		if (schema == null || schema.trim().length() == 0)
			return null;

		IPluginModelBase model = point.getPluginModel();
		URL url = getSchemaURL(model.getPluginBase().getId(), schema);
		if (url == null)
			url = getSchemaFromSourceExtension(point.getPluginBase(), new Path(schema));
		return url;
	}

	public static URL getSchemaFromSourceExtension(IPluginBase plugin, IPath path) {
		SourceLocationManager mgr = PDECore.getDefault().getSourceLocationManager();
		return mgr.findSourceFile(plugin, path);
	}

	public static URL getSchemaURL(String pluginID, String schema) {
		if (pluginID == null)
			return null;

		URL url = null;
		ModelEntry entry = PluginRegistry.findEntry(pluginID);
		if (entry != null) {
			IPluginModelBase[] models = entry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				url = getSchemaURL(models[i], schema);
				if (url != null)
					break;
			}
			if (url == null) {
				models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					url = getSchemaURL(models[i], schema);
					if (url != null)
						break;
				}
			}
		}
		return url;
	}

	private static URL getSchemaURL(IPluginModelBase model, String schema) {
		try {
			if (model == null)
				return null;

			String location = model.getInstallLocation();
			if (location == null)
				return null;

			File file = new File(location);
			if (file.isDirectory()) {
				File schemaFile = new File(file, schema);
				if (schemaFile.exists())
					return schemaFile.toURL();
			} else if (CoreUtility.jarContainsResource(file, schema, false)) {
				return new URL("jar:file:" + file.getAbsolutePath() + "!/" + schema); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (MalformedURLException e) {
		}
		return null;
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

	private static String getId(IPluginExtensionPoint point, IPluginModelBase base) {
		String id = null;
		if (point instanceof PluginExtensionPointNode) {
			if (base instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) base).getFragment();
				if (fragment != null) {
					id = fragment.getPluginId();
				}
			}
			if (id == null) {
				id = base.getPluginBase().getId();
			}
		}
		return id;
	}

}
