/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.builder.*;

/**
 * 
 */
public class BuildScriptGenerator extends AbstractScriptGenerator {

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
	 * Plugin path. URLs that point where to find the plugins.
	 */
	protected String[] pluginPath;

	protected boolean recursiveGeneration = true;

	/**
	 * 
	 * @throws CoreException
	 */
	public void generate() throws CoreException {
		// TO CHECK Does this usecase really exist? Do we really pass list of map file entry?
		List plugins = new ArrayList(5);
		List fragments = new ArrayList(5);
		List features = new ArrayList(5);
		sortElements(features, plugins, fragments);

		// It is not required to filter in the two first generateModels, since it is only for the building of a single plugin
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
			if (type.equals("plugin")) //$NON-NLS-1$
				plugins.add(element);
			else if (type.equals("fragment")) //$NON-NLS-1$
				fragments.add(element);
			else if (type.equals("feature")) //$NON-NLS-1$
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
		for (Iterator iterator = models.iterator(); iterator.hasNext();) {
			//Filtering is not required here, since we are only generating the build for a plugin or a fragment
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
		for (Iterator i = features.iterator(); i.hasNext();) {
			AssemblyInformation assemblageInformation = null;
			//if (! noAssembleGeneration) //FIXME Put a parameter for assembly Generation
			assemblageInformation = new AssemblyInformation();

			String featureId = (String) i.next();
			FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator(featureId, assemblageInformation);
			generator.setGenerateIncludedFeatures(this.recursiveGeneration);
			generator.setAnalyseChildren(this.children);
			generator.setSourceFeatureGeneration(false);
			generator.setBinaryFeatureGeneration(true);
			generator.setScriptGeneration(true);
			generator.getSite(true); // Force the site to be refreshed
			generator.setPluginPath(Utils.asURL(pluginPath));
			generator.setBuildSiteFactory(null);
			generator.setDevEntries(devEntries);
			generator.setSourceToGather(new SourceFeatureInformation());
			generator.setCompiledElements(generator.getCompiledElements());
			generator.generate();
			AssembleScriptGenerator assembler = new AssembleScriptGenerator(workingDirectory, assemblageInformation, featureId, null);
			assembler.generate();
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
	//TODO Shoudn't we get path instead of url?
	public void setPluginPath(String[] pluginPath) {
		this.pluginPath = pluginPath;
	}

	/**
	 * Sets the recursiveGeneration.
	 * @param recursiveGeneration The recursiveGeneration to set
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		this.recursiveGeneration = recursiveGeneration;
	}

}