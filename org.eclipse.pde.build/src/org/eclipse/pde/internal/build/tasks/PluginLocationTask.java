package org.eclipse.pde.internal.core.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.core.*;
/**
 * Sets a property with the location of a given plugin.
 */
public class PluginLocationTask extends Task implements IPDECoreConstants, IXMLConstants {
	
	protected String pluginId;
	protected String fragmentId;
	protected String propertyName;
	protected String[] pluginPath;
	protected static final String REGISTRY_REFERENCE_ID = "pluginLocation.registry";

/**
 * Sets the pluginId.
 */
public void setPlugin(String pluginId) {
	this.pluginId = pluginId;
}

/**
 * Sets the fragmentId.
 */
public void setFragment(String fragmentId) {
	this.fragmentId = fragmentId;
}

/**
 * Sets the propertyName.
 */
public void setProperty(String propertyName) {
	this.propertyName = propertyName;
}

/**
 *
 */
public void execute() throws BuildException {
	try {
		PluginModel descriptor;
		if (pluginId != null) {
			descriptor = getRegistry().getPlugin(pluginId);
			if (descriptor == null)
				throw new BuildException("Could not find plugin: " + pluginId); // FIXME: externalize string
		} else {
			descriptor = getRegistry().getFragment(fragmentId);
			if (descriptor == null)
				throw new BuildException("Could not find fragment: " + fragmentId); // FIXME: externalize string
		}
		String location = new File(new URL(descriptor.getLocation()).getFile()).getAbsolutePath();
		getProject().setProperty(propertyName, location);
	} catch (Exception e) {
		throw new BuildException(e);
	}
}

/**
 * Returns the plugin registry for the current set of plugins.
 */
protected PluginRegistryModel getRegistry() throws CoreException, MalformedURLException {
	PluginRegistryModel registry = (PluginRegistryModel) getProject().getReferences().get(REGISTRY_REFERENCE_ID);
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDECORE, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null);
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		IStatus status = factory.getStatus();
		if (Utils.contains(status, IStatus.ERROR))
			throw new CoreException(status);
		getProject().addReference(REGISTRY_REFERENCE_ID, registry);
	}
	return registry;
}
protected URL[] guessPluginPath() throws MalformedURLException {
	List urls = new ArrayList(5);
	urls.add(new File(getProject().getBaseDir(), "..").toURL());
	String installLocation = getProject().getProperty(PROPERTY_INSTALL);
	if (installLocation != null)
		urls.add(new File(installLocation, "plugins").toURL());
	return (URL[]) urls.toArray(new URL[urls.size()]);
}
/**
 * Gets the pluginPath.
 */
protected URL[] getPluginPath() throws CoreException, MalformedURLException {
	if (pluginPath == null)
		return guessPluginPath();
	return Utils.asURL(pluginPath);
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(String pluginPath) {
	// FIXME: this should be a script property and not set as a parameter of this task
	this.pluginPath = Utils.getArrayFromString(pluginPath);
}

}