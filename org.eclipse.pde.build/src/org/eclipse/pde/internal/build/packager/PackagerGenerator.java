/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.AssemblyInformation;
import org.eclipse.pde.internal.build.builder.*;

public class PackagerGenerator extends AbstractScriptGenerator {
	/**
	 * Indicates whether the assemble script should contain the archive
	 * generation statement.
	 */
	protected boolean generateArchive = true;
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
	protected DevClassPathHelper devEntries;

	/**
	 * Plugin path. URLs that point where to find the plugins.
	 */
	protected String[] pluginPath;

	protected boolean recursiveGeneration = true;
	protected boolean signJars = false;
	protected boolean generateJnlp = false;

	/**
	 * flag indicating if the assemble script should be generated
	 */
	private boolean generateAssembleScript = true;

	/**
	 * flag indicating if the errors detected when the state is resolved must be reported or not.
	 * For example in releng mode we are interested in reporting the errors. It is the default. 
	 */
	private boolean reportResolutionErrors = true;
	
	/** flag indicating if missing properties file should be logged */
	private boolean ignoreMissingPropertiesFile = false;
	
	private String[] featureList;
	private String propertyFile;
	private boolean includePlatformIndependent = true;
	private boolean groupConfigs = false;
	
	/**
	 * 
	 * @throws CoreException
	 */
	public void generate() throws CoreException {
		List features = new ArrayList(5);
		generateFeatures(features);
	}
	
	/**
	 * 
	 * @param models
	 * @throws CoreException
	 */
	protected void generateModels(List models) throws CoreException {
		for (Iterator iterator = models.iterator(); iterator.hasNext();) {
			ModelBuildScriptGenerator generator = new ModelBuildScriptGenerator();
			generator.setReportResolutionErrors(reportResolutionErrors);
			generator.setIgnoreMissingPropertiesFile(ignoreMissingPropertiesFile);
			//Filtering is not required here, since we are only generating the
			// build for a plugin or a fragment
			String model = (String) iterator.next();
			generator.setModelId(model);
			generator.setSignJars(signJars);
			generator.generate();
		}
	}

	private String[] getNameAndVersion(String id) {
		int versionPosition = id.indexOf(":"); //$NON-NLS-1$
		String[] result = new String[2];
		if (versionPosition != -1) {
			result[1] = id.substring(versionPosition + 1);
			result[0] = id.substring(0, versionPosition);
		} else
			result[0] = id;
		return result;
	}
	
	protected void generateFeatures(List features) throws CoreException {
		AssemblyInformation assemblageInformation = null;
		assemblageInformation = new AssemblyInformation();
		
		for (int i = 0; i < featureList.length; i++) {
			String[] featureInfo = getNameAndVersion(featureList[i]);			
			FeatureBuildScriptGenerator generator = new ElementCollector(featureInfo[0], assemblageInformation);
			generator.setGenerateIncludedFeatures(true);
			generator.setAnalyseChildren(true);
			generator.setSourceFeatureGeneration(false);
			generator.setBinaryFeatureGeneration(true);
			generator.setScriptGeneration(false);
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(null);
			generator.setDevEntries(devEntries);
			generator.setCompiledElements(null);
			generator.setBuildingOSGi(isBuildingOSGi());
			generator.includePlatformIndependent(includePlatformIndependent);
//			generator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
//			setFeature(featureList[i]);
			generator.setReportResolutionErrors(reportResolutionErrors);
			generator.setIgnoreMissingPropertiesFile(ignoreMissingPropertiesFile);
			generator.setSignJars(signJars);
			generator.setGenerateJnlp(generateJnlp);		
			generator.generate();
		}
		
		if (generateAssembleScript == true) {
			String[] featureInfo = null;
			if (features.size() == 1)
				featureInfo = getNameAndVersion((String) features.get(0));
			else
				featureInfo = new String[] {""};
			
			generatePackageScripts(assemblageInformation, featureInfo);
		}
	}

	private void generatePackageScripts(AssemblyInformation assemblageInformation, String[] featureInfo) throws CoreException {
		PackageScriptGenerator assembler = null;
		if (groupConfigs)
			assembler = new DeltaPackScriptGenerator(workingDirectory, assemblageInformation, featureInfo[0]);
		else 
			assembler = new PackageScriptGenerator(workingDirectory, assemblageInformation, featureInfo[0]);
		
		assembler.setSignJars(signJars);
		assembler.setGenerateJnlp(generateJnlp);
		assembler.setPropertyFile(propertyFile);
		assembler.generate();
	}

	public void setGenerateArchive(boolean generateArchive) {
		this.generateArchive = generateArchive;
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
	public void setDevEntries(String devEntries) {
		if (devEntries != null)
			this.devEntries = new DevClassPathHelper(devEntries);
	}

	/**
	 * 
	 * @param elements
	 */
	public void setElements(String[] elements) {
		this.elements = elements;
	}

	public void setPluginPath(String[] pluginPath) {
		this.pluginPath = pluginPath;
	}

	/**
	 * Sets the recursiveGeneration.
	 * 
	 * @param recursiveGeneration
	 *            The recursiveGeneration to set
	 */
	public void setRecursiveGeneration(boolean recursiveGeneration) {
		this.recursiveGeneration = recursiveGeneration;
	}

	/**
	 * @param generateAssembleScript
	 *            The generateAssembleScript to set.
	 */
	public void setGenerateAssembleScript(boolean generateAssembleScript) {
		this.generateAssembleScript = generateAssembleScript;
	}
	/**
	 * @param value The reportResolutionErrors to set.
	 */
	public void setReportResolutionErrors(boolean value) {
		this.reportResolutionErrors = value;
	}

	/**
	 * @param value The ignoreMissingPropertiesFile to set.
	 */
	public void setIgnoreMissingPropertiesFile(boolean value) {
		ignoreMissingPropertiesFile = value;
	}

	public void setSignJars(boolean value) {
		signJars = value;
	}

	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	public void includePlatformIndependent(boolean b) {
		includePlatformIndependent  = b;		
	}

	public void groupConfigs(boolean value) {
		groupConfigs = value;
	}

	public void setPropertyFile(String propertyFile) {
		this.propertyFile = propertyFile;
	}
	
	public void setFeatureList(String features) {
		featureList = Utils.getArrayFromString(features, ","); //$NON-NLS-1$
	}
}
