/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;

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
		String location = model.getInstallLocation();
		if (location == null)
			return false;
		
		File pluginLocation = new File(location);
		InputStream stream = null;
		ZipFile jarFile = null;
		try {
			if (pluginLocation.isDirectory())
				return new File(pluginLocation, ".options").exists(); //$NON-NLS-1$
			
			jarFile = new ZipFile(pluginLocation, ZipFile.OPEN_READ);
			ZipEntry manifestEntry = jarFile.getEntry(".options"); //$NON-NLS-1$
			if (manifestEntry != null) {
				stream = jarFile.getInputStream(manifestEntry);
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null) 
					stream.close();
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e) {
			}
		}
		return stream != null;
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
	
	private Properties getOptions(IPluginModelBase model) {
		String location = model.getInstallLocation();
		if (location == null)
			return null;
		
		File pluginLocation = new File(location);
		InputStream stream = null;
		ZipFile jarFile = null;
		try {
			if (pluginLocation.isDirectory()) {
				File file = new File(pluginLocation, ".options"); //$NON-NLS-1$
				if (file.exists()) 
					stream = new FileInputStream(file);
			} else {
					jarFile = new ZipFile(pluginLocation, ZipFile.OPEN_READ);
					ZipEntry manifestEntry = jarFile.getEntry(".options"); //$NON-NLS-1$
					if (manifestEntry != null) {
						stream = jarFile.getInputStream(manifestEntry);
					}
			}
			if (stream != null) {
				Properties modelOptions = new Properties();
				modelOptions.load(stream);
				return modelOptions;
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null) 
					stream.close();
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

}
