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
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

/**
 * Generate build scripts for the listed elements. This is the implementation of the "eclipse.buildScript" Ant task.
 */
public class BuildScriptGeneratorTask extends Task {

	/**
	 * The application associated with this Ant task.
	 */
	protected BuildScriptGenerator generator = new BuildScriptGenerator();

	/**
	 * Set the boolean value indicating whether or not children scripts should
	 * be generated.
	 * 
	 * @param children <code>true</code> if child scripts should be generated
	 * and <code>false</code> otherwise
	 */
	public void setChildren(boolean children) {
		generator.setChildren(children);
	}

	/**
	 * Set the development entries for the compile classpath to be the given	value.
	 *  
	 * @param devEntries the classpath dev entries
	 */
	public void setDevEntries(String devEntries) {
		generator.setDevEntries(devEntries);
	}

	/**
	 * Set the plug-in path to be the given value.
	 * 
	 * @param pluginPath the plug-in path
	 */
	public void setPluginPath(String pluginPath) throws CoreException {
		generator.setPluginPath(Utils.getArrayFromString(pluginPath));
	}

	/**
	 * Set the source elements for the script to be the given value.
	 * 
	 * @param elements the source elements for the script
	 */
	public void setElements(String elements) {
		generator.setElements(Utils.getArrayFromString(elements));
	}

	public void execute() throws BuildException {
		try {
			run();
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	public void run() throws CoreException {
		generator.generate();
	}

	/** 
	 * Set the folder in which the build will occur.
	 * 
	 * @param buildDirectory the location where the build will occur.
	 */
	public void setBuildDirectory(String buildDirectory) throws MalformedURLException {
		generator.setWorkingDirectory(buildDirectory);
	}

	/** 
	 * Set the folder in which the build will occur.
	 * 
	 * @param installLocation the location where the build will occur.
	 * @deprecated see {@link #setBuildDirectory(String)}
	 */
	public void setInstall(String installLocation) {
		generator.setWorkingDirectory(installLocation);
	}
	
	/**
	 * Set the boolean value indicating whether or not the build scripts should be
	 * generated for nested features. The default is set to true.
	 * @param recursiveGeneration <code>true</code> if the scripts for the nested features should be generated
	 * and <code>false</code> otherwise
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		generator.setRecursiveGeneration(recursiveGeneration);
	}

	/** 
	 * Set the configuration for which the script should be generated. The default is set to be configuration independent.
	 * @param configInfo an ampersand separated list of configuration (for example win32, win32, x86 & macoxs, carbon, ppc).
	 * @throws CoreException
	 */
	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(configInfo);
	}

	/**
	 * Set a location that contains plugins and features required by plugins and features for which build scripts are being generated.
	 * @param baseLocation a path to a folder
	 */
	public void setBaseLocation(String baseLocation) {
		BuildTimeSiteFactory.setInstalledBaseSite(baseLocation);
	}

	/**
	 * Set the boolean value indicating whether or not the plug-ins and features for which scripts are being generated target eclipse 3.0 or greater. 
	 * The default is set to true. 
	 * @param osgi <code>true</code> if the scripts are generated for eclipse 3.0 or greated and <code>false</code> otherwise
	 */
	public void setBuildingOSGi(boolean osgi) {
		generator.setBuildingOSGi(osgi);
	}
	
	/**
	 * Set the folder in which the build will occur.
	 * <p>
	 * Note: This API is experimental.
	 * </p>
	 * 
	 * @param installLocation the location where the build will occur
	 */
	public void setWorkingDirectory(String installLocation) throws MalformedURLException {
		generator.setWorkingDirectory(installLocation);
	}
}