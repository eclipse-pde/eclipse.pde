package org.eclipse.pde.internal;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.io.*;
import org.eclipse.core.runtime.*;
import java.util.*;

public class TracingOptionsManager {
	private Properties template;
	private Properties options;

public TracingOptionsManager() {
	super();
}
private void addToTemplate(IPluginModel[] models) {
	for (int i = 0; i < models.length; i++) {
		addToTemplate(models[i]);
	}
}
private void addToTemplate(IPluginModel model) {
	Properties modelOptions = getOptions(model);
	if (modelOptions==null) return;
	for (Enumeration enum=modelOptions.keys(); enum.hasMoreElements();) {
		String key = enum.nextElement().toString();
		template.setProperty(key, modelOptions.getProperty(key));
	}
}
private void createTemplate() {
	template = new Properties();
	addToTemplate(PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels());
	addToTemplate(PDEPlugin.getDefault().getExternalModelManager().getModels());
}
public void ensureTracingFileExists() {
	String fileName = getTracingFileName();
	File file = new File(fileName);
	if (file.exists()==false) {
		// Force creation
		getTracingOptions();
		save();
	}
}
private Properties getOptions(IPluginModel model) {
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
		if (key.startsWith(pluginId)) {
			defaults.put(key, template.get(key));
		}
	}
	return defaults;
}
public String getTracingFileName() {
	IPath stateLocation = PDEPlugin.getDefault().getStateLocation();
	return stateLocation.append(".options").toOSString();
}
public Properties getTracingOptions() {
	// Start with the fresh template from plugins
	if (template==null) createTemplate();
	options = (Properties)template.clone();
	// Load stored values, but only for existing keys
	loadStoredOptions();
	return options;
}
public static boolean isTraceable(IPluginModel model) {
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
		PDEPlugin.logException(e);
	}
}
private InputStream openInputStream(IPluginModel model) {
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
	save(getTracingFileName());
}
private void save(String fileName) {
	try {
		FileOutputStream stream = new FileOutputStream(fileName);
		options.store(stream, "Master Tracing Options");
		stream.flush();
		stream.close();
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
}
public void setTracingOptions(Properties options) {
	this.options = options;
}
}
