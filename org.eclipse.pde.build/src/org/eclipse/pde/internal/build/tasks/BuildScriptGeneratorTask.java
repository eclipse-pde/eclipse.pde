package org.eclipse.pde.internal.core.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
/**
 * 
 */
public class BuildScriptGeneratorTask extends Task implements IXMLConstants, IPDECoreConstants {

	/**
	 * Indicates whether scripts for a feature's children should be generated.
	 */
	protected boolean children = true;

	/**
	 * Source elements for script generation.
	 */
	protected String[] elements;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected String[] devEntries = new String[0];

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;
	
	/**
	 * Plugin path. URLs that point where to find the plugins.
	 */
	protected String[] pluginPath;

	/**
	 * Build variables.
	 */
	protected String buildVariableOS;
	protected String buildVariableWS;
	protected String buildVariableNL;
	protected String buildVariableARCH;

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	this.children = children;
}


/**
 * Sets the devEntries.
 */
public void setDevEntries(String devEntries) {
	this.devEntries = Utils.getArrayFromString(devEntries);
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(String pluginPath) {
	this.pluginPath = Utils.getArrayFromString(pluginPath);
}

/**
 * Sets the devEntries.
 */
public void internalSetDevEntries(String[] devEntries) {
	this.devEntries = devEntries;
}

/**
 * Sets the pluginPath.
 */
public void internalSetPlugins(String[] pluginPath) {
	this.pluginPath = pluginPath;
}


/**
 * Sets the elements.
 */
public void setElements(String elements) {
	this.elements = Utils.getArrayFromString(elements);
}

/**
 * Sets the elements.
 */
public void internalSetElements(String[] elements) {
	this.elements = elements;
}

/**
 * Separate elements by kind.
 */
protected void sortElements(List features, List plugins, List fragments) {
	for (int i = 0; i < elements.length; i++) {
		int index = elements[i].indexOf('@');
		String type = elements[i].substring(0, index);
		String element = elements[i].substring(index + 1);
		if (type.equals("plugin")) 
			plugins.add(element);
		if (type.equals("fragment")) 
			fragments.add(element);
		if (type.equals("feature")) 
			features.add(element);
	}
}

public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

public void run() throws CoreException {
	List plugins = new ArrayList(5);
	List fragments = new ArrayList(5);
	List features = new ArrayList(5);
	sortElements(features, plugins, fragments);
	generateModels(new PluginBuildScriptGenerator(), plugins);
	generateModels(new FragmentBuildScriptGenerator(), fragments);
	generateFeatures(features);
}

protected void generateFeatures(List features) throws CoreException {
	if (features.isEmpty())
		return;
	FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(asURL(pluginPath));
	generator.setGenerateChildrenScript(children);
	for (int i = 0; i < features.size(); i++) {
		generator.setFeature((String) features.get(i));
		generator.generate();
	}
}

protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(asURL(pluginPath));
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		String model = (String) iterator.next();
		generator.setModelId(model);
		generator.generate();
	}
}

protected URL[] asURL(String[] target) throws CoreException {
	if (target == null)
		return null;
	try {
		URL[] result = new URL[target.length];
		for (int i = 0; i < target.length; i++)
			result[i] = new URL(target[i]);
		return result;
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_MALFORMED_URL, e.getMessage(), e));
	}
}



/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
}

/**
 * Sets the buildVariableARCH.
 */
public void setARCH(String buildVariableARCH) {
	this.buildVariableARCH = buildVariableARCH;
}

/**
 * Sets the buildVariableNL.
 */
public void setNL(String buildVariableNL) {
	this.buildVariableNL = buildVariableNL;
}

/**
 * Sets the buildVariableOS.
 */
public void setOS(String buildVariableOS) {
	this.buildVariableOS = buildVariableOS;
}

/**
 * Sets the buildVariableWS.
 */
public void setWS(String buildVariableWS) {
	this.buildVariableWS = buildVariableWS;
}

}