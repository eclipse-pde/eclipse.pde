package org.eclipse.pde.internal.core;
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
import org.eclipse.pde.core.internal.ant.AntScript;


/**
 * 
 */
public abstract class AbstractBuildScriptGenerator extends AbstractScriptGenerator {

	/**
	 * 
	 */
	protected BuildAntScript script;

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;

	/**
	 * Location of the plug-ins and fragments.
	 */
	private URL[] pluginPath;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected List devEntries;

	/**
	 * Plug-in registry for the elements. Should only be accessed by getRegistry().
	 */
	private PluginRegistryModel registry;

	/**
	 * Properties read from the build.properties file.
	 */
	private Properties buildProperties;
	private Map conditionalProperties;
	
protected void readProperties(String root) {
	try {
		buildProperties = new Properties();
		conditionalProperties = new HashMap(10);
		File file = new File(root, PROPERTIES_FILE);
		InputStream is = new FileInputStream(file);
		try {
			buildProperties.load(is);
			filterProperties(buildProperties, conditionalProperties);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		// if the file does not exist then we'll use default values, which is fine
	}
}

/**
 * 
 */
protected void setUpAntBuildScript() {
	String external = getBuildProperty(PROPERTY_ZIP_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setZipExternal(true);

	external = getBuildProperty(PROPERTY_JAR_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setJarExternal(true);

	String executable = getBuildProperty(PROPERTY_ZIP_PROGRAM);
	if (executable != null)
		script.setZipExecutable(executable);
	
	String arg = getBuildProperty(PROPERTY_ZIP_ARGUMENT);
	if (arg != null)
		script.setZipArgument(arg);
}

/**
 * Filters properties, separating the ones that have conditions based
 * on build variables (os, ws, nl and arch). The resulting filtered
 * properties (to) is a Map of containing the real key and a Map
 * of all its variations.
 */
protected void filterProperties(Properties from, Map to) {
	for (Enumeration enum = from.keys(); enum.hasMoreElements();) {
		String key = (String) enum.nextElement();
		if (!key.startsWith(PROPERTY_ASSIGNMENT_PREFIX))
			continue;
		String realKey = extractRealKey(key);
		Map variations = (Map) to.get(realKey);
		if (variations == null)
			variations = new HashMap(10);
		variations.put(key, from.get(key));
		to.put(realKey, variations);
		from.remove(key);
	}
}





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

/**
 * Removes build specific variables from this key.
 * For example ${os/linux,ws/motif}.bin.includes
 * becomes bin.includes
 */
protected String extractRealKey(String target) {
	int index = target.indexOf(PROPERTY_ASSIGNMENT_SUFFIX);
	String result = target.substring(index + PROPERTY_ASSIGNMENT_SUFFIX.length() + 1);
	return result;
}

protected String getBuildProperty(String key) {
	return buildProperties.getProperty(key);
}

protected void setBuildProperty(String key, String value) {
	if (value == null)
		return;
	buildProperties.setProperty(key, value);
}

protected Properties getBuildProperties() {
	return buildProperties;
}

protected Map getConditionalProperties() {
	return conditionalProperties;
}


public void setInstallLocation(String location) {
	this.installLocation = location;
}

public void setDevEntries(String[] entries) {
	this.devEntries = Arrays.asList(entries);
}

public void setDevEntries(List entries) {
	this.devEntries = entries;
}

protected PluginRegistryModel getRegistry() throws CoreException {
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDECORE, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null);
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		IStatus status = factory.getStatus();
		if (contains(status, IStatus.ERROR))
			throw new CoreException(status);
	}
	return registry;
}

protected boolean contains(IStatus status, int severity) {
	if (status.matches(severity))
		return true;
	if (status.isMultiStatus()) {
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++)
			if (contains(children[i], severity))
				return true;
	}
	return false;
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
 * Makes a full path relative to an Ant property. For example:
 * 	property = ${install}
 * 	fullPath = c:\temp\my\path
 * 	pathToTrim = c:\temp
 * The result will be ${install}/my/path
 */
protected String makeRelative(String property, String fullPath, String pathToTrim) {
	IPath prefix = new Path(pathToTrim);
	IPath full = new Path(fullPath);
	if (!prefix.isPrefixOf(full))
		return fullPath;
	IPath result = new Path(property);
	result = result.append(full.removeFirstSegments(prefix.segmentCount()));
	return result.toString();
}

	
/**
 * @deprecated
 */
public void setBuildVariableARCH(String buildVariableARCH) {
}

/**
 * @deprecated
 */
public void setBuildVariableNL(String buildVariableNL) {
}

/**
 * @deprecated
 */
public void setBuildVariableOS(String buildVariableOS) {
}

/**
 * @deprecated
 */
public void setBuildVariableWS(String buildVariableWS) {
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(URL[] pluginPath) {
	this.pluginPath = pluginPath;
}

}