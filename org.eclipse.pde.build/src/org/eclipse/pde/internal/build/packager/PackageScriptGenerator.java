/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
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
			return "package" + '.' + DEFAULT_ASSEMBLE_ALL; //$NON-NLS-1$
		return "package" + '.' + (featureId.equals("") ? "" : featureId + '.') + DEFAULT_ASSEMBLE_ALL; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void setPropertyFile(String propertyFile) {
		packagingPropertiesLocation = propertyFile;
	}

	protected Collection[] getConfigInfos(Config aConfig) {
		return new Collection[] {assemblageInformation.getBinaryPlugins(aConfig), assemblageInformation.getBinaryFeatures(aConfig), assemblageInformation.getFeatures(aConfig), new HashSet(0)};
	}

	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection binaryPlugins, Collection binaryFeatures, Collection allFeatures, Collection rootFiles) throws CoreException {
		configScriptGenerator.initialize(directory, featureId, aConfig, binaryPlugins, binaryFeatures, allFeatures, rootFiles);
		((PackageConfigScriptGenerator) configScriptGenerator).setPackagingPropertiesLocation(packagingPropertiesLocation);
		configScriptGenerator.setArchiveFormat((String) archivesFormat.get(aConfig));
		configScriptGenerator.setGroupConfigs(groupConfigs);
		setForceUpdateJar(forceUpdateJarFormat);
		configScriptGenerator.setBuildSiteFactory(siteFactory);
		configScriptGenerator.generate();

		script.print("<assemble "); //$NON-NLS-1$
		String config = configScriptGenerator.getTargetConfig();
		script.printAttribute("config", config, true); //$NON-NLS-1$
		script.printAttribute("element", configScriptGenerator.getTargetElement(), true); //$NON-NLS-1$
		script.printAttribute("dot", config.length() > 0 ? "." : "", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printAttribute("scriptPrefix", "package", true); //$NON-NLS-1$//$NON-NLS-2$
		script.println("/>"); //$NON-NLS-1$
	}

	public void setBackwardCompatibleName(boolean value) {
		backwardCompatibleName = value;
	}

//	private String computeBackwardCompatibleName(Config configInfo) {
//		if (backwardCompatibleName)
//			return DEFAULT_ASSEMBLE_NAME + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING)) + (backwardCompatibleName ? ".xml" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
//		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING)) + (backwardCompatibleName ? ".xml" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
//	}

	protected void printDefaultAssembleCondition() {
		if (backwardCompatibleName)
			script.printConditionIsSet("defaultAssemble.@{config}", "defaultAssemble", "defaultAssemblyEnabled", "assemble@{dot}@{config}.xml"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else
			script.printConditionIsSet("defaultAssemble.@{config}", "defaultAssemble", "defaultAssemblyEnabled", "assemble.@{element}@{dot}@{config}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	protected void generateMetadataTarget() {
		if (configScriptGenerator.haveP2Bundles()) {
			script.printTargetDeclaration(TARGET_P2_METADATA, null, TARGET_P2_METADATA, null, null);
			script.print("<p2.generator "); //$NON-NLS-1$
			script.printAttribute("append", "true", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("flavor", "${p2.flavor}", true); //$NON-NLS-1$//$NON-NLS-2$
			script.printAttribute("metadataRepository", "${p2.metadata.repo}", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("artifactRepository", "${p2.artifact.repo}", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("publishArtifacts", "${p2.publish.artifacts}", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("mode", "final", true); //$NON-NLS-1$ //$NON-NLS-2$

			ProductFile product = configScriptGenerator.getProductFile();
			if (product != null) {
				script.printAttribute("productFile", product.getLocation(), true); //$NON-NLS-1$
				if (versionsList) {
					if (product.useFeatures())
						script.printAttribute("versionAdvice", getWorkingDirectory() + '/' + DEFAULT_FEATURE_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
					else
						script.printAttribute("versionAdvice", getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				}
			} else {
				script.printAttribute("root", "name", true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("rootVersion", "1.0.0", true); //$NON-NLS-1$ //$NON-NLS-2$
			}

			script.println("/>"); //$NON-NLS-1$
			script.printTargetEnd();
		}
	}
}
