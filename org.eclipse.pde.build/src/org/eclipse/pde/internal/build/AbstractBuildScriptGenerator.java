package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.build.ant.AntScript;


/**
 * 
 */
public abstract class AbstractBuildScriptGenerator extends AbstractBuildScriptGeneratorTemp {

	/**
	 * 
	 */
	protected BuildAntScript script;

	/**
	 * 
	 */
	protected String buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;

	/**
	 * Location of the plug-ins and fragments.
	 */
	private URL[] pluginPath;

	/**
	 * Plug-in registry for the elements. Should only be accessed by getRegistry().
	 */
	private PluginRegistryModel registry;


/**
 * Checks if the given element is already present in the list.
 * This method is case sensitive.
 */
protected boolean contains(String[] list, String element) {
	for (int i = 0; i < list.length; i++) {
		String string = list[i];
		if (string.equals(element))
			return true;
	}
	return false;
}







public void setInstallLocation(String location) {
	this.installLocation = location;
}



protected PluginRegistryModel getRegistry() throws CoreException {
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null);
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		IStatus status = factory.getStatus();
		if (Utils.contains(status, IStatus.ERROR))
			throw new CoreException(status);
	}
	return registry;
}


protected URL[] getPluginPath() {
	// Get the plugin path if one was spec'd.
	if (pluginPath != null)
		return pluginPath;
	// Otherwise, if the install location was spec'd, compute the default path.
	if (installLocation != null) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("file:");
			sb.append(installLocation);
			sb.append("/");
			sb.append(DEFAULT_PLUGIN_LOCATION);
			sb.append("/");
			return new URL[] { new URL(sb.toString()) };
		} catch (MalformedURLException e) {
		}
	}
	return null;
}

public void setRegistry(PluginRegistryModel registry) {
	this.registry = registry;
}



/**
 * 
 */
protected String getModelLocation(PluginModel descriptor) {
	try {
		return new URL(descriptor.getLocation()).getFile();
	} catch (MalformedURLException e) {
		return "../" + descriptor.getId() + "/";
	}
}



	




/**
 * Sets the pluginPath.
 */
public void setPluginPath(URL[] pluginPath) {
	this.pluginPath = pluginPath;
}


/**
 * Sets the buildScriptName.
 */
public void setBuildScriptName(String buildScriptName) {
	if (buildScriptName == null)
		this.buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;
	else
		this.buildScriptName = buildScriptName;
}


}