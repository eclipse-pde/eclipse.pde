package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class TracingOptionsManager {
	private Properties template;
	private Properties options;

	public TracingOptionsManager() {
		super();
	}
	private void addToTemplate(IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			addToTemplate(models[i]);
		}
	}
	private void addToTemplate(IPluginModelBase model) {
		Properties modelOptions = getOptions(model);
		if (modelOptions == null)
			return;
		for (Enumeration enum = modelOptions.keys(); enum.hasMoreElements();) {
			String key = enum.nextElement().toString();
			template.setProperty(key, modelOptions.getProperty(key));
		}
	}
	private void createTemplate() {
		template = new Properties();
		addToTemplate(
			PDECore.getDefault().getWorkspaceModelManager().getWorkspacePluginModels());
		addToTemplate(PDECore.getDefault().getExternalModelManager().getModels());
	}
	public void ensureTracingFileExists() {
		String fileName = getTracingFileName();
		File file = new File(fileName);
		if (file.exists() == false) {
			// Force creation
			getTracingOptions();
			save();
		}
	}
	private Properties getOptions(IPluginModelBase model) {
		InputStream stream = openInputStream(model);
		if (stream != null) {
			Properties modelOptions = new Properties();
			try {
				modelOptions.load(stream);
				stream.close();
				return modelOptions;
			} catch (IOException e) {
			}
		}
		return null;
	}
	public Hashtable getTemplateTable(String pluginId) {
		if (template == null)
			createTemplate();
		Hashtable defaults = new Hashtable();
		for (Enumeration enum = template.keys(); enum.hasMoreElements();) {
			String key = enum.nextElement().toString();
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

	public String getTracingFileName() {
		IPath stateLocation = PDECore.getDefault().getStateLocation();
		return stateLocation.append(".options").toOSString();
	}

	public Properties getTracingOptions() {
		// Start with the fresh template from plugins
		Properties defaults = getTracingTemplateCopy();
		options = defaults;
		// Load stored values, but only for existing keys
		loadStoredOptions();
		return options;
	}

	public Properties getTracingTemplateCopy() {
		if (template == null)
			createTemplate();
		return (Properties) template.clone();
	}

	public static boolean isTraceable(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null) {
			IProject project = model.getUnderlyingResource().getProject();
			IPath path = project.getFullPath().append(".options");
			IFile file = project.getWorkspace().getRoot().getFile(path);
			return file.exists();
		} else {
			String location = model.getInstallLocation();
			String fileName = location + File.separator + ".options";
			File file = new File(fileName);
			return file.exists();
		}
	}
	private void loadStoredOptions() {
		String fileName = getTracingFileName();
		File file = new File(fileName);
		if (file.exists() == false)
			return;
		try {
			FileInputStream stream = new FileInputStream(file);
			Properties storedOptions = new Properties();
			storedOptions.load(stream);
			stream.close();
			// Transfer only options that still exist (plugins can come and go)
			for (Enumeration enum = storedOptions.keys(); enum.hasMoreElements();) {
				String key = enum.nextElement().toString();
				if (options.containsKey(key)) {
					options.setProperty(key, storedOptions.getProperty(key));
				}
			}
		} catch (Exception e) {
			PDECore.logException(e);
		}
	}
	private InputStream openInputStream(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null) {
			IProject project = model.getUnderlyingResource().getProject();
			IPath path = project.getFullPath().append(".options");
			IFile file = project.getWorkspace().getRoot().getFile(path);
			if (file.exists()) {
				try {
					return file.getContents();
				} catch (CoreException e) {
				}
			}
		} else {
			String fileName = model.getInstallLocation() + File.separator + ".options";
			File file = new File(fileName);
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
			}
		}
		return null;
	}
	public void reset() {
		template = null;
	}
	public void save() {
		save(getTracingFileName(), options);
	}
	private void save(String fileName, Properties properties) {
		try {
			FileOutputStream stream = new FileOutputStream(fileName);
			properties.store(stream, "Master Tracing Options");
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	public void setTracingOptions(Properties options) {
		this.options = options;
	}

	public void save(Map map) {
		Properties properties = new Properties();
		properties.putAll(map);
		save(getTracingFileName(), properties);
	}
}