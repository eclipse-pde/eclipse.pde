/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 */
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

/**
 * 
 * @throws CoreException
 */
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
		if (type.equals("plugin"))  //$NON-NLS-1$
			plugins.add(element);
		if (type.equals("fragment"))  //$NON-NLS-1$
			fragments.add(element);
		if (type.equals("feature"))  //$NON-NLS-1$
			features.add(element);
	}
}

/**
 * 
 * @param generator
 * @param models
 * @throws CoreException
 */
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

/**
 * 
 * @param features
 * @throws CoreException
 */
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
 * 
 * @param children
 */
public void setChildren(boolean children) {
	this.children = children;
}

/**
 * 
 * @param devEntries
 */
public void setDevEntries(String[] devEntries) {
	this.devEntries = devEntries;
}

/**
 * 
 * @param elements
 */
public void setElements(String[] elements) {
	this.elements = elements;
}

/**
 * 
 * @param pluginPath
 */
public void setPlugins(String[] pluginPath) {
	this.pluginPath = pluginPath;
}

/**
 * 
 * @param installLocation
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
}

/**
 * 
 * @param pluginPath
 */
public void setPluginPath(String[] pluginPath) {
	this.pluginPath = pluginPath;
}
}
