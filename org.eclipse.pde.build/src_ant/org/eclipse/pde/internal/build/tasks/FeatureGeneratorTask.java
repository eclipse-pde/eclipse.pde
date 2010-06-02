/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.pde.internal.build.site.ProfileManager;

/**
 * Generate a container feature based on a .product file and/or provided feature, plugin lists
 * @since 3.2
 */
public class FeatureGeneratorTask extends Task {
	private static final String ANT_PREFIX = "${"; //$NON-NLS-1$
	private final FeatureGenerator generator = new FeatureGenerator();
	private final Properties antProperties = new Properties();

	public void execute() throws BuildException {
		try {
			BundleHelper.getDefault().setLog(this);
			initializeAntProperties(antProperties);
			generator.setImmutableAntProperties(antProperties);
			run();
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		} finally {
			BundleHelper.getDefault().setLog(null);
		}
	}

	private void initializeAntProperties(Properties properties) {
		String value = getProject().getProperty(IBuildPropertiesConstants.RESOLVER_DEV_MODE);
		if (Boolean.valueOf(value).booleanValue())
			antProperties.put(IBuildPropertiesConstants.RESOLVER_DEV_MODE, "true"); //$NON-NLS-1$

		ProfileManager manager = new ProfileManager(null, true);
		manager.copyEEProfileProperties(getProject().getProperties(), antProperties);
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
	 * Set whether or not to automatically include the launchers in the product
	 * Default is true
	 * @param includeLaunchers
	 */
	public void setIncludeLaunchers(boolean includeLaunchers) {
		generator.setIncludeLaunchers(includeLaunchers);
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
		if (pluginList != null && !pluginList.startsWith(ANT_PREFIX))
			generator.setPluginList(Utils.getArrayFromString(pluginList));
	}

	/**
	 * Set a list of plugin fragment ids to be included in the generated feature
	 * @param fragmentList a comma separated list of plugin ids
	 */
	public void setFragmentList(String fragmentList) {
		if (fragmentList != null && !fragmentList.startsWith(ANT_PREFIX))
			generator.setFragmentList(Utils.getArrayFromString(fragmentList));
	}

	/**
	 * Set a list of feature ids to be include in the generated feature
	 * @param featureList a comma separated list of feature ids
	 */
	public void setFeatureList(String featureList) {
		if (featureList != null && !featureList.startsWith(ANT_PREFIX))
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
	 * @param pluginPath a {@link File#pathSeparator} separated list of paths
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

	/** 
	 * Set the configuration for which the script should be generated. The default is set to be configuration independent.
	 * @param configInfo an ampersand separated list of configuration (for example win32, win32, x86 & macoxs, carbon, ppc).
	 * @throws CoreException
	 */
	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(configInfo);
	}

	/**
	 * Set to the location of a build.properties to be used for the generated feature
	 * @param buildPropertiesFile
	 */
	public void setBuildPropertiesFile(String buildPropertiesFile) {
		generator.setBuildProperties(buildPropertiesFile);
	}

	public void setNestedInclusions(String nested) {
		if (nested != null && !nested.startsWith(ANT_PREFIX))
			generator.setNestedInclusions(nested);
	}

	public void setFilterP2Base(boolean value) {
		generator.setFilterP2Base(value);
	}
}
