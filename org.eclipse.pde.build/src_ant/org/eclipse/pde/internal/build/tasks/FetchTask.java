/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

/**
 * Generate fetch scripts for the given elements. This is the implementation of the "eclipse.fetch" Ant task.
 */
public class FetchTask extends Task {

	/**
	 * The application associated with this Ant task.
	 */
	protected FetchScriptGenerator generator;

	/**
	 * Default constructor this class.
	 */
	public FetchTask() {
		generator = new FetchScriptGenerator();
	}

	/**
	 * Set the boolean value indicating whether or not the fetch scripts should be
	 * generated for the children of the elements.  The default is set to <code>true</code>
	 * 
	 * @param children <code>true</code> if the children scripts should be generated
	 * and <code>false</code> otherwise
	 * @since 3.0
	 */
	public void setChildren(boolean children) {
		generator.setFetchChildren(children);
	}

	/**
	 * Set the location for the CVS password file.
	 * @param cvsPassFileLocation the location of the password file
	 */
	public void setCvsPassFile(String cvsPassFileLocation) {
		generator.setCvsPassFileLocation(cvsPassFileLocation);
	}

	/**
	 * The path to a directory file.
	 * @param directoryLocation the location of a directory file
	 */
	public void setDirectory(String directoryLocation) {
		generator.setDirectoryLocation(directoryLocation);
	}

	/**
	 * @param element
	 */
	public void setElements(String element) {
		generator.setElement(element);
	}

	/**
	 * Overrides the tags provided in directory file by the given value.
	 * @param value the tag to be fetched.
	 * @since 3.0 
	 */
	public void setFetchTag(String value) {
		generator.setFetchTagAsString(value);
	}

	/**
	 * Set the folder in which the scripts will be generated, and in which the plugins and features will be fetched.
	 * @param buildDirectory the location where the scripts will be generated and the files fetched.
	 * @since 3.0
	 */
	public void setBuildDirectory(String buildDirectory) {
		generator.setWorkingDirectory(buildDirectory);
	}

	/**
	 * Set the folder in which the scripts will be generated, and in which the plugins and features will be fetched.
	 * @param installLocation the location where the scripts will be generated and the files fetched.
	 * @deprecated see {@link #setBuildDirectory(String)}
	 */
	public void setInstall(String installLocation) {
		generator.setWorkingDirectory(installLocation);
	}

	public void execute() throws BuildException {
		try {
			BundleHelper.getDefault().setLog(this);

			String fetchCache = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_FETCH_CACHE);
			if (fetchCache != null && !fetchCache.startsWith("${")) //$NON-NLS-1$
				generator.setFetchCache(fetchCache);
			generator.setScriptRunner(new AntScriptRunner(this));
			generator.generate();
			BundleHelper.getDefault().setLog(null);
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		}
	}

	/**
	 * Set the boolean value indicating whether or not the fetch scripts should be
	 * generated for nested features. The default is set to true.
	 * @param recursiveGeneration <code>true</code> if the scripts for the nested features should be generated
	 * and <code>false</code> otherwise.
	 * @since 3.0
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		generator.setRecursiveGeneration(recursiveGeneration);
	}

	/**
	 * Set the plug-in path to be the given value.
	 * 
	 * @param pluginPath a File.pathSeparator separated list of paths
	 */
	public void setPluginPath(String pluginPath) {
		generator.setPluginPath(Utils.getArrayFromString(pluginPath, File.pathSeparator));
	}

	/** 
	 * Set the configuration for which the script should be generated. The default is set to be configuration independent.
	 * @param configInfo an ampersand separated list of configuration (for example win32, win32, x86 & macoxs, carbon, ppc).
	 * @throws CoreException
	 * @since 3.0
	 */
	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(configInfo);
	}

	/**
	 * Set a location that contains plugins and features required by plugins and features for which build scripts are being generated.
	 * @param baseLocation a path to a folder
	 * @since 3.1
	 */
	public void setBaseLocation(String baseLocation) {
		BuildTimeSiteFactory.setInstalledBaseSite(baseLocation);
	}
}
