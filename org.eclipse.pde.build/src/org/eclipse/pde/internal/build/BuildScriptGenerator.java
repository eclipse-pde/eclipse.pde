/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;

import org.eclipse.core.runtime.CoreException;

public class BuildScriptGenerator {

	/**
	 * Indicates whether scripts for a feature's children should be generated.
	 */
	protected boolean children = true;

	/**
	 * Source elements for script generation.
	 */
	protected String[] elements;

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected String[] devEntries = new String[0];

	/**
	 * Plugin path. URLs that point where to find the plugins.
	 */
	protected String[] pluginPath;

public void run() throws CoreException {
	List plugins = new ArrayList(5);
	List fragments = new ArrayList(5);
	List features = new ArrayList(5);
	sortElements(features, plugins, fragments);
	generateModels(new PluginBuildScriptGenerator(), plugins);
	generateModels(new FragmentBuildScriptGenerator(), fragments);
	generateFeatures(features);
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

protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(Utils.asURL(pluginPath));
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		String model = (String) iterator.next();
		generator.setModelId(model);
		generator.generate();
	}
}

protected void generateFeatures(List features) throws CoreException {
	if (features.isEmpty())
		return;
	FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(Utils.asURL(pluginPath));
	generator.setGenerateChildrenScript(children);
	for (int i = 0; i < features.size(); i++) {
		generator.setFeature((String) features.get(i));
		generator.generate();
	}
}

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	this.children = children;
}

/**
 * Sets the devEntries.
 */
public void setDevEntries(String[] devEntries) {
	this.devEntries = devEntries;
}

/**
 * Sets the elements.
 */
public void setElements(String[] elements) {
	this.elements = elements;
}

/**
 * Sets the pluginPath.
 */
public void setPlugins(String[] pluginPath) {
	this.pluginPath = pluginPath;
}

/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(String[] pluginPath) {
	this.pluginPath = pluginPath;
}
}