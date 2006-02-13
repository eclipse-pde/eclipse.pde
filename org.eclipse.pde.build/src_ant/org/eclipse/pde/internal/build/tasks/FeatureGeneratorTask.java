/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.FeatureGenerator;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;

/**
 * Generate a container feature based on a .product file and/or provided feature, plugin lists
 * @since 3.2
 */
public class FeatureGeneratorTask extends Task {
	private FeatureGenerator generator = new FeatureGenerator();

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
	 * @param buildDirectory the location where the build will occur.
	 */
	public void setBuildDirectory(String buildDirectory) {
		generator.setWorkingDirectory(buildDirectory);
	}

	/**
	 * Set the product file to base the feature on
	 * @param productFile
	 */
	public void setProductFile(String productFile) {
		generator.setProductFile(productFile);
	}

	/**
	 * Set a location that contains plugins and features required by plugins and features 
	 * for which the feature is being generated
	 * @param baseLocation
	 */
	public void setBaseLocation(String baseLocation) {
		BuildTimeSiteFactory.setInstalledBaseSite(baseLocation);
	}

	/**
	 * Set a list of plugin ids to be included in the generated feature
	 * @param pluginList a comma separated list of plugin ids
	 */
	public void setPluginList(String pluginList) {
		if (pluginList != null && !pluginList.startsWith("${")) //$NON-NLS-1$
			generator.setPluginList(Utils.getArrayFromString(pluginList));
	}

	/**
	 * Set a list of feature ids to be include in the generated feature
	 * @param featureList a comma separated list of feature ids
	 */
	public void setFeatureList(String featureList) {
		if (featureList != null && !featureList.startsWith("${")) //$NON-NLS-1$
			generator.setFeatureList(Utils.getArrayFromString(featureList));
	}
	
	/**
	 * The id to give to the generated feature
	 * @param featureId
	 */
	public void setFeatureId(String featureId) {
		generator.setFeatureId(featureId);
	}
	
	/**
	 * Set the list of additional paths in which to look for required plugins
	 * 
	 * @param pluginPath a {@link File.pathSeparator} separated list of paths
	 */
	public void setPluginPath(String pluginPath) {
		generator.setPluginPath(Utils.getArrayFromString(pluginPath, File.pathSeparator));
	}
	
	/**
	 * Set to true if you want to verify that the plugins and features are available.  
	 * @param verify
	 */
	public void setVerify(boolean verify) {
		generator.setVerify(verify);
	}
}
