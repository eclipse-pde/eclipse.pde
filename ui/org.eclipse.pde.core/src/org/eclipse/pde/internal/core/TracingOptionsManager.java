/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

public class TracingOptionsManager {
	private Properties template;

	public TracingOptionsManager() {
		super();
	}

	private void createTemplate() {
		template = new Properties();
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < models.length; i++) {
			addToTemplate(models[i]);
		}		
	}
	
	private void addToTemplate(IPluginModelBase model) {
		Properties modelOptions = getOptions(model);
		if (modelOptions == null)
			return;
		for (Enumeration keys = modelOptions.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			String value = modelOptions.getProperty(key);
			if (key != null && value != null)
				template.setProperty(key, value);
		}
	}
	
	private Properties getOptions(IPluginModelBase model) {
		try {
			InputStream stream = model.getResourceURL(".options").openStream();
			Properties modelOptions = new Properties();
			modelOptions.load(stream);
			stream.close();
			return modelOptions;
		} catch (IOException e1) {
		}
		return null;
	}
	
	public Hashtable getTemplateTable(String pluginId) {
		if (template == null)
			createTemplate();
		Hashtable defaults = new Hashtable();
		for (Enumeration keys = template.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			if (belongsTo(key, pluginId)) {
				defaults.put(key, template.get(key));
			}
		}
		return defaults;
	}

	private boolean belongsTo(String option, String pluginId) {
		IPath path = new Path(option);
		String firstSegment = path.segment(0);
		return pluginId.equalsIgnoreCase(firstSegment);
	}

	public Properties getTracingOptions(Map storedOptions) {
		// Start with the fresh template from plugins
		Properties defaults = getTracingTemplateCopy();
		if (storedOptions != null) {
			// Load stored values, but only for existing keys
			Iterator iter = storedOptions.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				if (defaults.containsKey(key)) {
					defaults.setProperty(key, (String) storedOptions.get(key));
				}			
			}
		}
		return defaults;
	}

	public Properties getTracingTemplateCopy() {
		if (template == null)
			createTemplate();
		return (Properties) template.clone();
	}

	public static boolean isTraceable(IPluginModelBase model) {
		try {
			InputStream stream = model.getResourceURL(".options").openStream();
			stream.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public void reset() {
		template = null;
	}

	private void save(String fileName, Properties properties) {
		try {
			FileOutputStream stream = new FileOutputStream(fileName);
			properties.store(stream, "Master Tracing Options"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public void save(String filename, Map map, HashSet selected) {
		Properties properties = getTracingOptions(map);
		for (Enumeration keys = properties.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			Path path = new Path(key);
			if (path.segmentCount() < 1 || !selected.contains(path.segment(0).toString())) {
				properties.remove(key);
			}
		}
		save(filename, properties);
	}

	public void save(String filename, Map map) {
		save(filename, getTracingOptions(map));		
	}
}
