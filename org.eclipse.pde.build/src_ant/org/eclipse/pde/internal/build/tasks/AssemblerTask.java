/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.net.MalformedURLException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.packager.PackagerBuildScriptGenerator;

/** 
 * Internal task.
 * Generate assemble scripts to repackage binary distributions.
 * @since 3.0
 */
public class AssemblerTask extends Task {

	protected PackagerBuildScriptGenerator generator;

	{
		generator = new PackagerBuildScriptGenerator();
		generator.setGenerateIncludedFeatures(true);
		generator.setAnalyseChildren(true);
		generator.setSourceFeatureGeneration(false);
		generator.setBinaryFeatureGeneration(true);
		generator.setScriptGeneration(false);
	}

	/**
	 * Set the directory where the packaging will occur
	 * @param workingLocation: the location
	 * @throws MalformedURLException
	 */
	public void setWorkingDirectory(String workingLocation) throws MalformedURLException {
		generator.setWorkingDirectory(workingLocation);
	}

	/**
	 * Set the features to assemble
	 * @param featureList: a comma separated list of features to package 
	 */
	public void setFeatureList(String featureList) throws BuildException {
		generator.setFeatureList(featureList);
	}

	/**
	 * Set the configuration for which the assembling is being done
	 * @param configInfo: a configuration
	 * @throws CoreException
	 */
	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(configInfo);
	}

	/**
	 * Set the location where to find features, plugins and fragments
	 * @param baseLocation: a comma separated list of paths
	 */
	public void setBaseLocation(String baseLocation) throws BuildException {
		String[] locations = Utils.getArrayFromString(baseLocation);
		generator.setPluginPath(locations);
	}

	public void execute() throws BuildException {
		try {
			generator.run();
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	/**
	 *  Set the property file containing information about packaging
	 * @param propertyFile: the path to a property file
	 */
	public void setPackagePropertyFile(String propertyFile) {
		generator.setPropertyFile(propertyFile);
	}
}