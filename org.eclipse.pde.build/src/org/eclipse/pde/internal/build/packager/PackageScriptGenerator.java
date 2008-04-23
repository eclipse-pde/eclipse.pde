/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.AntScript;

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
			script.printProperty(PROPERTY_P2_APPEND, "true"); //$NON-NLS-1$
			ProductFile product = configScriptGenerator.getProductFile();
			String versionAdvice = null;
			if (versionsList && product != null) {
				if (product.useFeatures())
					versionAdvice = getWorkingDirectory() + '/' + DEFAULT_FEATURE_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX;
				else
					versionAdvice = getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX;
			}
			generateP2FinalCall(script, product != null ? product.getLocation() : null, versionAdvice);
			script.printTargetEnd();
		}
	}

	private static void generateP2FinalCall(AntScript script, String productFileLocation, String versionAdvice) {
		script.print("<p2.generator "); //$NON-NLS-1$
		script.printAttribute("append", Utils.getPropertyFormat(PROPERTY_P2_APPEND), true); //$NON-NLS-1$ 
		script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
		script.printAttribute("metadataRepository", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO), true); //$NON-NLS-1$ 
		script.printAttribute("artifactRepository", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO), true); //$NON-NLS-1$ 
		script.printAttribute("publishArtifacts", Utils.getPropertyFormat(PROPERTY_P2_PUBLISH_ARTIFACTS), true); //$NON-NLS-1$ 
		script.printAttribute("mode", "final", true); //$NON-NLS-1$ //$NON-NLS-2$

		if (productFileLocation != null) {
			script.printAttribute("productFile", productFileLocation, true); //$NON-NLS-1$
			if (versionAdvice != null)
				script.printAttribute("versionAdvice", versionAdvice, true); //$NON-NLS-1$
		} else {
			script.printAttribute("root", Utils.getPropertyFormat(PROPERTY_P2_ROOT_NAME), true); //$NON-NLS-1$
			script.printAttribute("rootVersion", Utils.getPropertyFormat(PROPERTY_P2_ROOT_VERSION), true); //$NON-NLS-1$
		}

		script.println("/>"); //$NON-NLS-1$
	}

	/**
	 * Generate an ant script that can be run to generate final p2 metadata for a product.
	 * Returns null if p2 bundles aren't available.
	 * 
	 * If no product file is given, the generated p2 call generates final metadata for a 
	 * ${p2.root.name}_${p2.root.version} IU.
	 * 
	 * versionAdvice is a properties file with bsn=3.2.1.xyz entries
	 * 
	 * @param workingDir			- the directory in which to generate the script
	 * @param productFileLocation   - the location of a .product file (can be null)
	 * @param versionAdvice			- version advice (can be null)
	 * @return The location of the generated script, or null
	 * @throws CoreException
	 */
	public static String generateP2ProductScript(String workingDir, String productFileLocation, Properties versionAdvice) throws CoreException {
		if (!loadP2Class())
			return null;

		File working = new File(workingDir);
		working.mkdirs();

		File adviceFile = null;
		if (versionAdvice != null) {
			adviceFile = new File(working, "versionAdvice.properties"); //$NON-NLS-1$
			try {
				OutputStream os = new BufferedOutputStream(new FileOutputStream(adviceFile));
				try {
					versionAdvice.store(os, null);
				} finally {
					os.close();
				}
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, adviceFile.toString());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			}
		}

		AntScript p2Script = null;
		try {
			p2Script = newAntScript(workingDir, "p2product.xml"); //$NON-NLS-1$
			p2Script.printProjectDeclaration("P2 Product IU Generation", "main", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			p2Script.println();
			p2Script.printProperty(PROPERTY_P2_APPEND, "true"); //$NON-NLS-1$
			p2Script.printTargetDeclaration("main", null, TARGET_P2_METADATA, null, "Generate the final Product IU"); //$NON-NLS-1$//$NON-NLS-2$
			generateP2FinalCall(p2Script, productFileLocation, adviceFile != null ? adviceFile.getAbsolutePath() : null);
			p2Script.printTargetEnd();
			p2Script.printProjectEnd();
		} finally {
			if (p2Script != null)
				p2Script.close();
		}
		return workingDir + "/p2product.xml"; //$NON-NLS-1$
	}
}
