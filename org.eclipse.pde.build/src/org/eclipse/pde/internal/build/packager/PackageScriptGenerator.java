/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
	private boolean backwardCompatibleName = false;
	
	public PackageScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) {
		super(directory, assemblageInformation, featureId);
	}
	
	protected void printProjectDeclaration() {
		script.printProjectDeclaration("Package all config of " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
	}
	
	protected AssembleConfigScriptGenerator getConfigScriptGenerator() {
		return new PackageConfigScriptGenerator();
	}
	
	protected String getScriptName() {
		if (backwardCompatibleName)
			return "package" + '.' + DEFAULT_ASSEMBLE_ALL;
		return "package" + '.' + (featureId.equals("") ? "" : featureId + '.') + DEFAULT_ASSEMBLE_ALL;
	}
	
	public void setPropertyFile(String propertyFile) {
		packagingPropertiesLocation = propertyFile;
	}

	protected Collection[] getConfigInfos(Config aConfig) {
		return new Collection[] {assemblageInformation.getBinaryPlugins(aConfig), assemblageInformation.getBinaryFeatures(aConfig), assemblageInformation.getFeatures(aConfig), new HashSet(0) };
	}
	
	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection binaryPlugins, Collection binaryFeatures, Collection allFeatures, Collection rootFiles) throws CoreException {
		configScriptGenerator.initialize(directory, featureId, aConfig, binaryPlugins, binaryFeatures, allFeatures, rootFiles); 
		((PackageConfigScriptGenerator) configScriptGenerator).setPackagingPropertiesLocation(packagingPropertiesLocation);
		configScriptGenerator.setArchiveFormat((String) archivesFormat.get(aConfig));
		configScriptGenerator.setGroupConfigs(groupConfigs);
		setForceUpdateJar(forceUpdateJarFormat);
		configScriptGenerator.setBuildSiteFactory(siteFactory);
		configScriptGenerator.generate();

		Map params = new HashMap(1);
		params.put("assembleScriptName", configScriptGenerator.getTargetName() + ".xml");
		script.printAntTask(Utils.getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, computeBackwardCompatibleName(aConfig), null, null, params);
	}
	
	public void setBackwardCompatibleName(boolean value) {
		backwardCompatibleName = value;
	}
	
	private String computeBackwardCompatibleName(Config configInfo) {
		if (backwardCompatibleName)
			return DEFAULT_ASSEMBLE_NAME + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING)) + (backwardCompatibleName ? ".xml" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING)) + (backwardCompatibleName ? ".xml" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}
}
