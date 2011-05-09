/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.*;

/**
 * Generate build scripts for the listed elements. This is the implementation of the "eclipse.buildScript" Ant task.
 */
public class BuildScriptGeneratorTask extends Task {
	private final Properties antProperties = new Properties();
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
	 * @since 3.0
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

	public void setFlattenDependencies(boolean flatten) {
		generator.setFlattenDependencies(flatten);
	}

	public void setGenerateSourceReferences(boolean generateSourceRef) {
		generator.setGenerateSourceReferences(generateSourceRef);
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
	 * Set the source elements for the script to be the given value.
	 * 
	 * @param elements the source elements for the script
	 */
	public void setElements(String elements) {
		generator.setElements(Utils.getArrayFromString(elements));
	}

	public void setSignificantVersionDigits(String significantDigits) {
		antProperties.put(IBuildPropertiesConstants.PROPERTY_SIGNIFICANT_VERSION_DIGITS, significantDigits);
	}

	public void setGeneratedVersionLength(String generatedLength) {
		antProperties.put(IBuildPropertiesConstants.PROPERTY_GENERATED_VERSION_LENGTH, generatedLength);
	}

	public void execute() throws BuildException {
		try {
			run();
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		}
	}

	public void run() throws CoreException {
		initializeAntProperties(antProperties);
		setEEProfileProperties(antProperties);

		if (!Boolean.valueOf(antProperties.getProperty(IBuildPropertiesConstants.PROPERTY_SUPPRESS_RESOLUTION_ERRORS)).booleanValue())
			generator.setReportResolutionErrors(true);
		generator.setImmutableAntProperties(antProperties);
		BundleHelper.getDefault().setLog(this);
		generator.generate();
		BundleHelper.getDefault().setLog(null);
	}

	private void initializeAntProperties(Properties properties) {
		String value = getProject().getProperty(IBuildPropertiesConstants.RESOLVER_DEV_MODE);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.RESOLVER_DEV_MODE, "true"); //$NON-NLS-1$

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_INDIVIDUAL_SOURCE);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.PROPERTY_INDIVIDUAL_SOURCE, "true"); //$NON-NLS-1$

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES, "true"); //$NON-NLS-1$

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_P2_GATHERING);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.PROPERTY_P2_GATHERING, "true"); //$NON-NLS-1$ 

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_SUPPRESS_RESOLUTION_ERRORS);
		if (Boolean.valueOf(value).booleanValue())
			properties.put(IBuildPropertiesConstants.PROPERTY_SUPPRESS_RESOLUTION_ERRORS, "true"); //$NON-NLS-1$ 

		value = getProject().getProperty(IBuildPropertiesConstants.PROPERTY_ANT_VERSION);
		if (value != null)
			properties.put(IBuildPropertiesConstants.PROPERTY_ANT_VERSION, value);
	}

	private void setEEProfileProperties(Properties antProperties) {
		ProfileManager manager = new ProfileManager(generator.getEESources(), true);
		manager.copyEEProfileProperties(getProject().getProperties(), antProperties);
	}

	/** 
	 * Set the folder in which the build will occur.
	 * @param buildDirectory the location where the build will occur.
	 * @since 3.0
	 */
	public void setBuildDirectory(String buildDirectory) {
		generator.setWorkingDirectory(buildDirectory);
	}

	/** 
	 * Set the folder in which the build will occur.
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
	 * @since 3.0
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		generator.setRecursiveGeneration(recursiveGeneration);
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
	 * Set on a configuration basis, the format of the archive being produced. The default is set to be configuration independent.
	 * @param archivesFormat an ampersand separated list of configuration (for example win32, win32 - zip, x86 & macoxs, carbon, ppc - tar).
	 * @throws CoreException
	 * @since 3.0
	 */
	public void setArchivesFormat(String archivesFormat) throws CoreException {
		generator.setArchivesFormat(archivesFormat);
	}

	/**
	 * Set a location that contains plugins and features required by plugins and features for which build scripts are being generated.
	 * @param baseLocation a path to a folder
	 * @since 3.0
	 */
	public void setBaseLocation(String baseLocation) {
		BuildTimeSiteFactory.setInstalledBaseSite(baseLocation);
	}

	/**
	 * Set the boolean value indicating whether or not the plug-ins and features for which scripts are being generated target eclipse 3.0 or greater. 
	 * The default is set to true. 
	 * This property is experimental and is likely to be renamed in the future.
	 * @param osgi <code>true</code> if the scripts are generated for eclipse 3.0 or greated and <code>false</code> otherwise
	 * @since 3.0.
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
	public void setWorkingDirectory(String installLocation) {
		generator.setWorkingDirectory(installLocation);
	}

	public void setCustomEESources(String eeSources) {
		if (eeSources != null && !eeSources.startsWith("${")) { //$NON-NLS-1$
			generator.setEESources(Utils.getArrayFromString(eeSources, File.pathSeparator));
		}
	}

	/**
	 * Set the location of the product being built. This should be a / separated path
	 * where the first segment is the id of the plugin containing the .product file.
	 * @param value the location of the .product file being built.
	 */
	public void setProduct(String value) {
		generator.setProduct(value);
	}

	/**
	 * Set the boolean value indicating whether or not to sign any constructed JAs.
	 * @param value <code>true</code> if built jars should be signed.
	 */
	public void setSignJars(boolean value) {
		generator.setSignJars(value);
	}

	/**
	 * Set the boolean value indicating whether or not to generate JNLP files for 
	 * built features.
	 * @param value <code>true</code> if JNLP manifests should be generated.
	 */
	public void setGenerateJnlp(boolean value) {
		generator.setGenerateJnlp(value);
	}

	public void setOutputUpdateJars(boolean value) {
		AbstractScriptGenerator.setForceUpdateJar(value);
	}

	public void setForceContextQualifier(String value) {
		QualifierReplacer.setGlobalQualifier(value);
	}

	/**
	 * Set the boolean value indicating whether or not to generate a version suffix
	 * for features based on their content.
	 * @param value <code>true</code> if version suffixes should be generated.
	 */
	public void setGenerateFeatureVersionSuffix(boolean value) {
		generator.setGenerateFeatureVersionSuffix(value);
	}

	/**
	 * Set whether or not to generate plugin & feature versions lists
	 * @param value
	 */
	public void setGenerateVersionsLists(boolean value) {
		generator.setGenerateVersionsList(value);
	}

	/**
	 * Set the boolean indicating if resulting archive should contain a group of all the configurations being built.
	 * @param value <code>false</code> if the configurations being built should be grouped in one archive.
	 */
	public void setGroupConfiguration(boolean value) {
		generator.setGroupConfigs(value);
	}

	public void setFilteredDependencyCheck(boolean value) {
		generator.setFilterState(value);
	}

	/**
	 * Set the filename to use for the platform properties that will be passed to the state.
	 */
	public void setPlatformProperties(String filename) {
		generator.setPlatformProperties(filename);
	}

	public void setFilterP2Base(boolean value) {
		generator.setFilterP2Base(value);
	}

	public void setParallelCompilation(boolean parallel) {
		generator.setParallel(parallel);
	}

	public void setParallelThreadCount(String count) {
		try {
			generator.setThreadCount(Integer.parseInt(count));
		} catch (NumberFormatException e) {
			//ignore
		}
	}

	public void setParallelThreadsPerProcessor(String threads) {
		try {
			generator.setThreadsPerProcessor(Integer.parseInt(threads));
		} catch (NumberFormatException e) {
			//ignore
		}
	}

	public void setContextRepository(String contexts) {
		String[] locations = Utils.getArrayFromString(contexts, ","); //$NON-NLS-1$
		ArrayList uris = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			String context = locations[i];
			if (context == null || context.startsWith("${") || context.length() == 0) //$NON-NLS-1$
				continue;

			if (!context.startsWith("file:")) //$NON-NLS-1$
				context = "file:" + context; //$NON-NLS-1$
			try {
				uris.add(URIUtil.fromString(context));
			} catch (URISyntaxException e) {
				log("Invalid repository location:" + context); //$NON-NLS-1$
			}
		}
		if (uris.size() > 0)
			generator.setContextMetadataRepositories((URI[]) uris.toArray(new URI[uris.size()]));
	}

	public void setSourceBundleFeatureId(String value) {
		if (value != null && !value.startsWith("${")) //$NON-NLS-1$
			generator.setSourceBundleFeatureId(value);
	}

	public void setSourceBundleTemplateFeature(String value) {
		if (value != null && !value.startsWith("${")) //$NON-NLS-1$
			generator.setSourceBundleTemplateFeature(value);
	}

	public void setSourceBundleFeatureVersion(String value) {
		if (value != null && !value.startsWith("${")) //$NON-NLS-1$
			generator.setSourceBundleFeatureVersion(value);
	}

	public void setSourceBundleMode(String value) {
		if (value != null && !value.startsWith("${")) //$NON-NLS-1$
			generator.setSourceBundleMode(value);
	}
}
