/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

public class PackageScriptGenerator extends AssembleScriptGenerator {
	private String packagingPropertiesLocation;
	private String[] featureList;
	//TODO Need to change this value before releasing
	private String outputFormat = "zip"; //$NON-NLS-1$
	
	
	public PackageScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) throws CoreException {
		super(directory, assemblageInformation, featureId);
	}
	
	protected AssembleConfigScriptGenerator getConfigScriptGenerator() {
		return new PackageConfigScriptGenerator();
	}
	
	protected String getScriptName() {
		return "package" + '.' + (featureId.equals("") ? "" : featureId + '.') + DEFAULT_ASSEMBLE_ALL;
	}
	
	public void setPropertyFile(String propertyFile) {
		packagingPropertiesLocation = propertyFile;
	}

	public void setFeatureList(String features) {
		featureList = Utils.getArrayFromString(features, ","); //$NON-NLS-1$
	}

	public void setOutput(String format) { //TODO To rename
		this.outputFormat = format;
	}

	protected void generateAssembleConfigFileTargetCall(Config aConfig) throws CoreException {
		basicGenerateAssembleConfigFileTargetCall(aConfig, assemblageInformation.getBinaryPlugins(aConfig), assemblageInformation.getBinaryFeatures(aConfig), assemblageInformation.getFeatures(aConfig), null); //TODO Check if there are case where this is interesting
	}
	
	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection binaryPlugins, Collection binaryFeatures, Collection allFeatures, Collection rootFiles) throws CoreException {
		configScriptGenerator.initialize(directory, featureId, aConfig, binaryPlugins, binaryFeatures, allFeatures, rootFiles); 
		((PackageConfigScriptGenerator) configScriptGenerator).setPackagingPropertiesLocation(packagingPropertiesLocation);
		setOutputFormat(outputFormat);
		setForceUpdateJar(forceUpdateJarFormat);
		setBrandExecutable(false);
		configScriptGenerator.generate();

		Map params = new HashMap(1);
		params.put("assembleScriptName", configScriptGenerator.getTargetName() + ".xml");
		script.printAntTask(getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, computeBackwardCompatibleName(aConfig), null, null, params);
	}
	
	private String computeBackwardCompatibleName(Config configInfo) {
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING)) + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
}
