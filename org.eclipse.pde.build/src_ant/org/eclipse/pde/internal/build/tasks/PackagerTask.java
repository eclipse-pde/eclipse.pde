/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.packager.PackagerGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeSiteFactory;
import org.eclipse.pde.internal.build.site.ProfileManager;

/** 
 * Internal task.
 * Generate assemble scripts to repackage binary distributions.
 * @since 3.0
 */
public class PackagerTask extends Task {

	protected PackagerGenerator generator;
	private final Properties antProperties = new Properties();

	{
		generator = new PackagerGenerator();
		generator.setReportResolutionErrors(true);
		generator.setIgnoreMissingPropertiesFile(true);
		BuildTimeSiteFactory.setInstalledBaseSite(null);
	}

	/**
	 * Set the directory where the packaging will occur
	 * @param workingLocation the location
	 */
	public void setWorkingDirectory(String workingLocation) {
		generator.setWorkingDirectory(workingLocation);
	}

	/**
	 * Set the features to assemble
	 * @param featureList a comma separated list of features to package 
	 */
	public void setFeatureList(String featureList) throws BuildException {
		generator.setFeatureList(featureList);
	}

	/**
	 * Set the configuration for which the assembling is being done
	 * @param configInfo a configuration
	 * @throws CoreException
	 */
	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(configInfo);
	}

	/** 
	 * Set on a configuration basis, the format of the archive being produced. The default is set to be configuration independent.
	 * @param archivesFormat an ampersand separated list of configuration (for example win32, win32 - zip, x86 & macoxs, carbon, ppc - tar).
	 * @throws CoreException
	 * @since 3.0
	 */
	public void setArchivesFormat(String archivesFormat) throws CoreException {
		generator.setArchivesFormat(archivesFormat);
	}

	/**
	 * Set the location where to find features, plugins and fragments
	 * @param baseLocation a comma separated list of paths
	 */
	public void setBaseLocation(String baseLocation) throws BuildException {
		String[] locations = Utils.getArrayFromString(baseLocation);
		generator.setPluginPath(locations);
	}

	public void execute() throws BuildException {
		try {
			initializeAntProperties(antProperties);
			generator.setImmutableAntProperties(antProperties);
			generator.setProduct(getProject().getProperty("product")); //$NON-NLS-1$
			generator.setGenerateVersionsList(Boolean.valueOf(getProject().getProperty("generateVersionsList")).booleanValue()); //$NON-NLS-1$
			BundleHelper.getDefault().setLog(this);
			generator.generate();
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		} finally {
			BundleHelper.getDefault().setLog(null);
		}
	}

	private void initializeAntProperties(Properties properties) {
		properties.setProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE, "true"); //$NON-NLS-1$

		String value = getProject().getProperty(IBuildPropertiesConstants.RESOLVER_DEV_MODE);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.RESOLVER_DEV_MODE, "true"); //$NON-NLS-1$

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES, "true"); //$NON-NLS-1$

		ProfileManager manager = new ProfileManager(null, true);
		manager.copyEEProfileProperties(getProject().getProperties(), antProperties);
	}

	/**
	 *  Set the property file containing information about packaging
	 * @param propertyFile the path to a property file
	 */
	public void setPackagePropertyFile(String propertyFile) {
		generator.setPropertyFile(propertyFile);
	}

	public void setDeltaPack(boolean value) {
		generator.includePlatformIndependent(!value);
		generator.groupConfigs(value);
	}

	public void setFilteredDependencyCheck(boolean value) {
		generator.setFilterState(value);
	}

	public void setNormalize(boolean value) {
		if (value)
			antProperties.setProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER, "true"); //$NON-NLS-1$
		else
			antProperties.setProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER, "false"); //$NON-NLS-1$
	}
}
