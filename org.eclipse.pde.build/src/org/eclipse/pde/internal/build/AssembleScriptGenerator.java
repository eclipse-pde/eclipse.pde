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
package org.eclipse.pde.internal.build;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.pde.internal.build.builder.BuildDirector;

public class AssembleScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected AssemblyInformation assemblageInformation;
	protected String featureId;
	protected HashMap archivesFormat;
	protected boolean groupConfigs = false;
	protected boolean versionsList = false;
	protected String productLocation = null;

	protected AssembleConfigScriptGenerator configScriptGenerator;

	public AssembleScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) {
		this.directory = directory;
		this.assemblageInformation = assemblageInformation;
		this.featureId = featureId;
		configScriptGenerator = getConfigScriptGenerator();
	}

	protected String getScriptName() {
		return DEFAULT_ASSEMBLE_NAME + '.' + (featureId.equals("") ? "" : featureId + '.') + DEFAULT_ASSEMBLE_ALL; //$NON-NLS-1$//$NON-NLS-2$
	}

	protected String getProductDir() {
		return Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product/"; //$NON-NLS-1$
	}

	protected AssembleConfigScriptGenerator getConfigScriptGenerator() {
		return new AssembleConfigScriptGenerator();
	}

	public void generate() throws CoreException {
		//make sure the script generator is initialized with the site before we try doing anything with it.
		configScriptGenerator.setBuildSiteFactory(siteFactory);

		try {
			openScript(directory, getScriptName());
			printProjectDeclaration();
			printAssembleMacroDef();
			generatePrologue();
			generateMainTarget();
			generateReplaceProductTarget();
			generateMetadataTarget();
			script.printProjectEnd();
		} finally {
			if (script != null)
				script.close();
			script = null;
		}
	}

	protected void printProjectDeclaration() {
		script.printProjectDeclaration("Assemble All Config of " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
	}

	protected void generatePrologue() {
		if (productQualifier != null)
			script.printProperty(PROPERTY_P2_PRODUCT_QUALIFIER, productQualifier);
		script.println();
	}

	protected void printDefaultAssembleCondition() {
		// packaging may need to print something different if running in a backward compatible mode.
		script.printConditionIsSet("defaultAssemble.@{config}", "defaultAssemble", "defaultAssemblyEnabled", "assemble.@{element}@{dot}@{config}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	protected void printAssembleMacroDef() {
		List attributes = new ArrayList(2);
		attributes.add("config"); //$NON-NLS-1$
		attributes.add("element"); //$NON-NLS-1$
		attributes.add("dot"); //$NON-NLS-1$
		attributes.add("scriptPrefix"); //$NON-NLS-1$
		script.printMacroDef("assemble", attributes); //$NON-NLS-1$
		printDefaultAssembleCondition();
		script.printConditionIsSet("customOrDefault.@{config}", "assemble.@{element}@{dot}@{config}", "assemble.@{element}@{dot}@{config}", "${defaultAssemble.@{config}}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		Properties properties = new Properties();
		properties.put("assembleScriptName", "@{scriptPrefix}.@{element}@{dot}@{config}.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.put("element", "@{element}"); //$NON-NLS-1$//$NON-NLS-2$
		properties.put("config", "@{config}"); //$NON-NLS-1$//$NON-NLS-2$
		script.printAntTask(Utils.getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, "${customOrDefault.@{config}}", null, null, properties); //$NON-NLS-1$
		script.printEndMacroDef();
	}

	protected void generateMainTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);

		if (BuildDirector.p2Gathering) {
			generateP2ConfigFileTargetCall();
		} else if (configScriptGenerator.getProductFile() != null && configScriptGenerator.haveP2Bundles()) {
			script.printAntCallTask(TARGET_P2_REPLACE_PRODUCT, true, null);
		}

		if (shouldGroupConfigs()) {
			Collection allPlugins = new LinkedHashSet();
			Collection allFeatures = new LinkedHashSet();
			Collection features = new LinkedHashSet();
			Collection rootFiles = new LinkedHashSet();
			for (Iterator allConfigs = getConfigInfos().iterator(); allConfigs.hasNext();) {
				Collection[] configInfo = getConfigInfos((Config) allConfigs.next());
				allPlugins.addAll(configInfo[0]);
				allFeatures.addAll(configInfo[1]);
				features.addAll(configInfo[2]);
				rootFiles.addAll(configInfo[3]);
			}
			basicGenerateAssembleConfigFileTargetCall(new Config("group", "group", "group"), allPlugins, allFeatures, features, rootFiles); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			for (Iterator allConfigs = getConfigInfos().iterator(); allConfigs.hasNext();) {
				Config current = (Config) allConfigs.next();
				Collection[] configInfo = getConfigInfos(current);
				basicGenerateAssembleConfigFileTargetCall(current, configInfo[0], configInfo[1], configInfo[2], configInfo[3]);
			}
		}
		if (configScriptGenerator.haveP2Bundles() && !BuildDirector.p2Gathering)
			script.printAntCallTask(TARGET_P2_METADATA, true, null);
		script.printTargetEnd();
	}

	protected boolean shouldGroupConfigs() {
		if (!BuildDirector.p2Gathering)
			return groupConfigs;

		// product builds are not grouped.
		if (configScriptGenerator.getProductFile() != null)
			return false;

		return true;
	}

	protected Collection[] getConfigInfos(Config aConfig) {
		return new Collection[] {assemblageInformation.getCompiledPlugins(aConfig), assemblageInformation.getCompiledFeatures(aConfig), assemblageInformation.getFeatures(aConfig), assemblageInformation.getRootFileProviders(aConfig)};
	}

	protected void generateP2ConfigFileTargetCall() {
		P2ConfigScriptGenerator p2ConfigGenerator = new P2ConfigScriptGenerator(assemblageInformation, true);
		p2ConfigGenerator.setProduct(productLocation);
		p2ConfigGenerator.setBuildSiteFactory(siteFactory);
		p2ConfigGenerator.initialize(directory, featureId);
		p2ConfigGenerator.setSignJars(configScriptGenerator.isSigning());
		p2ConfigGenerator.setVersionsList(versionsList);
		p2ConfigGenerator.setContextMetadata(contextMetadata);
		p2ConfigGenerator.setContextArtifacts(contextArtifacts);
		p2ConfigGenerator.setProductQualifier(productQualifier);
		p2ConfigGenerator.generate();

		script.printTab();
		script.print("<assemble "); //$NON-NLS-1$
		script.printAttribute("config", "p2", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.printAttribute("element", p2ConfigGenerator.getTargetElement(), true); //$NON-NLS-1$
		script.printAttribute("dot", ".", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.printAttribute("scriptPrefix", "assemble", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("/>"); //$NON-NLS-1$
	}

	protected void basicGenerateAssembleConfigFileTargetCall(Config aConfig, Collection binaryPlugins, Collection binaryFeatures, Collection allFeatures, Collection rootFiles) throws CoreException {
		// generate the script for a configuration
		configScriptGenerator.initialize(directory, featureId, aConfig, binaryPlugins, binaryFeatures, allFeatures, rootFiles);
		configScriptGenerator.setArchiveFormat((String) archivesFormat.get(aConfig));
		configScriptGenerator.setBuildSiteFactory(siteFactory);
		configScriptGenerator.setGroupConfigs(groupConfigs);
		configScriptGenerator.setProductQualifier(productQualifier);
		configScriptGenerator.generate();

		script.printTab();
		script.print("<assemble "); //$NON-NLS-1$
		String config = configScriptGenerator.getTargetConfig();
		script.printAttribute("config", config, true); //$NON-NLS-1$
		script.printAttribute("element", configScriptGenerator.getTargetElement(), true); //$NON-NLS-1$
		script.printAttribute("dot", config.length() > 0 ? "." : "", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printAttribute("scriptPrefix", "assemble", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("/>"); //$NON-NLS-1$
	}

	protected void generateReplaceProductTarget() {
		ProductFile product = configScriptGenerator.getProductFile();
		if (product != null) {
			File productFile = product.getLocation();
			String newProduct = getProductDir() + productFile.getName();
			File p2Inf = new File(productFile.getParentFile(), "p2.inf"); //$NON-NLS-1$

			script.printTargetDeclaration(TARGET_P2_REPLACE_PRODUCT, null, null, null, null);
			script.printCopyFileTask(productFile.getPath(), newProduct, true);
			if (p2Inf.exists())
				script.printCopyTask(p2Inf.getAbsolutePath(), getProductDir(), null, false, true);
			generateProductReplaceTask(product, newProduct, assemblageInformation);
			script.printTargetEnd();
			script.println();
		}
	}

	protected void generateMetadataTarget() {
		if (BuildDirector.p2Gathering)
			return;
		if (configScriptGenerator.haveP2Bundles()) {
			ProductFile product = configScriptGenerator.getProductFile();
			script.printTargetDeclaration(TARGET_P2_METADATA, null, TARGET_P2_METADATA, PROPERTY_RUN_PACKAGER, null);
			script.printConditionIsSet("mode", "incremental", PROPERTY_RUN_PACKAGER, "final"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			script.printProperty(PROPERTY_P2_APPEND, "true"); //$NON-NLS-1$
			script.printProperty(PROPERTY_P2_METADATA_REPO_NAME, ""); //$NON-NLS-1$
			script.printProperty(PROPERTY_P2_ARTIFACT_REPO_NAME, ""); //$NON-NLS-1$
			if (product != null) {
				File productFile = product.getLocation();
				String modLocation = getProductDir() + productFile.getName();
				script.printAvailableTask(PROPERTY_P2_PRODUCT_MOD, modLocation, modLocation);
				script.printProperty(PROPERTY_P2_PRODUCT_MOD, product.getLocation().getPath());
			}
			script.printTab();
			script.print("<p2.generator "); //$NON-NLS-1$
			script.printAttribute("append", Utils.getPropertyFormat(PROPERTY_P2_APPEND), true); //$NON-NLS-1$
			script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.printAttribute("metadataRepository", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO), true); //$NON-NLS-1$ 
			script.printAttribute("artifactRepository", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO), true); //$NON-NLS-1$
			script.printAttribute("metadataRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO_NAME), true); //$NON-NLS-1$
			script.printAttribute("artifactRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO_NAME), true); //$NON-NLS-1$
			script.printAttribute("publishArtifacts", Utils.getPropertyFormat(PROPERTY_P2_PUBLISH_ARTIFACTS), true); //$NON-NLS-1$ 
			script.printAttribute("mode", "${mode}", true); //$NON-NLS-1$ //$NON-NLS-2$

			if (product != null) {
				script.printAttribute("productFile", Utils.getPropertyFormat(PROPERTY_P2_PRODUCT_MOD), true); //$NON-NLS-1$
				if (versionsList) {
					if (product.useFeatures())
						script.printAttribute("versionAdvice", getWorkingDirectory() + '/' + DEFAULT_FEATURE_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
					else
						script.printAttribute("versionAdvice", getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				}
			} else {
				script.printAttribute("root", Utils.getPropertyFormat(PROPERTY_P2_ROOT_NAME), true); //$NON-NLS-1$ 
				script.printAttribute("rootVersion", Utils.getPropertyFormat(PROPERTY_P2_ROOT_VERSION), true); //$NON-NLS-1$ 
			}

			script.println("/>"); //$NON-NLS-1$
			script.printTargetEnd();
		}
	}

	public void setSignJars(boolean value) {
		configScriptGenerator.setSignJars(value);
	}

	public void setProduct(String value) {
		productLocation = value;
		configScriptGenerator.setProduct(value);
	}

	public void setGenerateJnlp(boolean value) {
		configScriptGenerator.setGenerateJnlp(value);
	}

	public void setArchivesFormat(HashMap outputFormat) {
		archivesFormat = outputFormat;
	}

	public void setGroupConfigs(boolean group) {
		groupConfigs = group;
	}

	public void setVersionsList(boolean versionsList) {
		this.versionsList = versionsList;
	}
}
