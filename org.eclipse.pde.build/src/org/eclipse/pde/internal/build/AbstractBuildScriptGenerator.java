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

/**
 * 
 */
public abstract class AbstractBuildScriptGenerator extends AbstractScriptGenerator {

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;

	/**
	 * Location of the plug-ins and fragments.
	 */
	private URL pluginPath;

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

protected void printExternalZipTask(PrintWriter output, int tab, String zipFile, String basedir) {
	String executable = getBuildProperty(PROPERTY_ZIP_PROGRAM);
	List args = new ArrayList(1);
	String arg = getBuildProperty(PROPERTY_ZIP_ARGUMENT);
	if (arg != null) {
		arg = substituteWord(arg, PROPERTY_ZIP_FILE, zipFile);
		args.add(arg);
	}
	printExecTask(output, tab, executable, basedir, args);
}

/**
 * If the user has specified an external zip program in the build.properties file,
 * use it. Otherwise use the default Ant jar task.
 */
protected void printJarTask(PrintWriter output, int tab, String zipFile, String basedir) {
	String external = getBuildProperty(PROPERTY_JAR_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		printExternalZipTask(output, tab, zipFile, basedir);
	else
		printAntJarTask(output, tab, zipFile, basedir);
}

/**
 * If the user has specified an external zip program in the build.properties file,
 * use it. Otherwise use the default Ant zip task.
 */
protected void printZipTask(PrintWriter output, int tab, String zipFile, String basedir) {
	String external = getBuildProperty(PROPERTY_ZIP_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		printExternalZipTask(output, tab, zipFile, basedir);
	else
		printAntZipTask(output, tab, zipFile, basedir);
}




protected void readProperties(String root) {
	try {
		buildProperties = new Properties();
		File file = new File(root, PROPERTIES_FILE);
		InputStream is = new FileInputStream(file);
		try {
			buildProperties.load(is);
			buildProperties = filterProperties(buildProperties);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		// if the file does not exist then we'll use default values, which is fine
	}
}

/**
 * Filters and merges properties that are relative to the current
 * build, based on the values of the build variables (os, ws, nl and arch).
 */
protected Properties filterProperties(Properties target) {
	for(Enumeration keys = target.keys(); keys.hasMoreElements(); ) {
		String key = (String) keys.nextElement();
		if (!key.startsWith(PROPERTY_ASSIGNMENT_PREFIX))
			continue;
		if (propertyMatchesCurrentBuild(key)) {
			String value = target.getProperty(key);
			if (value != null) {
				String realKey = extractRealKey(key);
				String currentValue = target.getProperty(realKey);
				if (currentValue != null) {
					if (!contains(Utils.getArrayFromString(currentValue), value))
						value = currentValue + "," + value;
					else
						value = currentValue;
				}
				target.put(realKey, value);
			}
		}
		target.remove(key);
	}
	return target;
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
		if (!factory.getStatus().isOK())
			throw new CoreException(factory.getStatus());
	}
	return registry;
}
protected URL[] getPluginPath() {
	// get the plugin path.  If one was spec'd on the command line, use that.
	// Otherwise, if the install location was spec'd, compute the default path.
	// Finally, if nothing was said, allow the system to figure out the plugin
	// path based on the current running state.
	if (pluginPath == null && installLocation != null) {
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
	} else {
		return BootLoader.getPluginPath(pluginPath);
	}
	return null;
}

public void setRegistry(PluginRegistryModel registry) {
	this.registry = registry;
}











protected String substituteWord(String sentence, String oldWord, String newWord) {
	int index = sentence.indexOf(oldWord);
	if (index == -1)
		return sentence;
	StringBuffer sb = new StringBuffer();
	sb.append(sentence.substring(0, index));
	sb.append(newWord);
	sb.append(sentence.substring(index + oldWord.length()));
	return sb.toString();
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
	IPath trim = new Path(pathToTrim);
	IPath result = new Path(property);
	result = result.append(new Path(fullPath).removeFirstSegments(trim.segmentCount()));
	return result.toString();
}

/**
 * Checks if the given property key should be included in the current
 * build by looking into the build variables defined with it.
 * For example ${os/linux,ws/motif}.bin.includes is targeted
 * for a linux-motif build and should not be part of a Windows build.
 */
protected boolean propertyMatchesCurrentBuild(String property) {
	int prefix = property.indexOf(PROPERTY_ASSIGNMENT_PREFIX);
	int suffix = property.indexOf(PROPERTY_ASSIGNMENT_SUFFIX);
	String[] variables = Utils.getArrayFromString(property.substring(prefix + PROPERTY_ASSIGNMENT_PREFIX.length(), suffix));
	for (int i = 0; i < variables.length; i++) {
		String[] var = Utils.getArrayFromString(variables[i], "/");
		String key = var[0];
		String value = var[1];
		if (value.equals("*"))
			continue;
		if (key.equalsIgnoreCase(PROPERTY_OS)) {
			if (!value.equalsIgnoreCase(getBuildProperty(PROPERTY_OS)))
				return false;
			continue;
		}
		if (key.equalsIgnoreCase(PROPERTY_WS)) {
			if (!value.equalsIgnoreCase(getBuildProperty(PROPERTY_WS)))
				return false;
			continue;
		}
		if (key.equalsIgnoreCase(PROPERTY_NL)) {
			if (!value.equalsIgnoreCase(getBuildProperty(PROPERTY_NL)))
				return false;
			continue;
		}
		if (key.equalsIgnoreCase(PROPERTY_ARCH)) {
			if (!value.equalsIgnoreCase(getBuildProperty(PROPERTY_ARCH)))
				return false;
			continue;
		}
	}
	return true;
}
	
/**
 * Sets the buildVariableARCH.
 */
public void setBuildVariableARCH(String buildVariableARCH) {
	setBuildProperty(PROPERTY_ARCH, buildVariableARCH);
}

/**
 * Sets the buildVariableNL.
 */
public void setBuildVariableNL(String buildVariableNL) {
	setBuildProperty(PROPERTY_NL, buildVariableNL);
}

/**
 * Sets the buildVariableOS.
 */
public void setBuildVariableOS(String buildVariableOS) {
	setBuildProperty(PROPERTY_OS, buildVariableOS);
}

/**
 * Sets the buildVariableWS.
 */
public void setBuildVariableWS(String buildVariableWS) {
	setBuildProperty(PROPERTY_WS, buildVariableWS);
}

}