/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

public class P2ConfigScriptGenerator extends AssembleConfigScriptGenerator {
	private AssemblyInformation assemblyInformation = null;
	private boolean assembling = false;
	private boolean versionsList = false;

	public P2ConfigScriptGenerator(AssemblyInformation assemblyInformation, boolean assembling) {
		this.assemblyInformation = assemblyInformation;
		this.assembling = assembling;
	}

	public void initialize(String directoryName, String feature) {
		this.directory = directoryName;
		this.featureId = feature;
	}

	public void generate() {
		initializeCollections();

		try {
			String prefix = assembling ? "assemble." : "package."; //$NON-NLS-1$ //$NON-NLS-2$
			openScript(directory, prefix + featureId + ".p2.xml"); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		generatePrologue();
		generateMainBegin();
		generateGatherCalls();
		generateMetadataCalls();
		generateMainEnd();

		generateGatherBinPartsTarget();
		generateCustomAssemblyTarget();
		generateSigningTarget();
		generateMetadataTarget();
		generateEpilogue();
		closeScript();
	}

	protected void initializeCollections() {
		Collection p = new LinkedHashSet();
		Collection f = new LinkedHashSet();
		Collection r = new LinkedHashSet();
		for (Iterator iterator = getConfigInfos().iterator(); iterator.hasNext();) {
			Config config = (Config) iterator.next();
			p.addAll(assemblyInformation.getPlugins(config));
			f.addAll(assemblyInformation.getFeatures(config));
			r.addAll(assemblyInformation.getRootFileProviders(config));
		}

		this.plugins = (BundleDescription[]) p.toArray(new BundleDescription[p.size()]);
		this.features = (BuildTimeFeature[]) f.toArray(new BuildTimeFeature[f.size()]);
		this.rootFileProviders = r;
	}

	protected void generatePrologue() {
		script.printProjectDeclaration("Publish p2 metadata", TARGET_MAIN, null); //$NON-NLS-1$
		ProductFile product = getProductFile();
		if (product != null) {
			String launcherName = product.getLauncherName() != null ? product.getLauncherName() : "eclipse"; //$NON-NLS-1$
			script.printProperty(PROPERTY_LAUNCHER_NAME, launcherName);
			script.printProperty(PROPERTY_LAUNCHER_PROVIDER, FEATURE_EQUINOX_EXECUTABLE);
		}
		script.printProperty(PROPERTY_P2_BUILD_REPO, "file:" + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/buildRepo"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_ASSEMBLY_TMP, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
		script.printProperty(PROPERTY_SIGN, (signJars ? Boolean.TRUE : Boolean.FALSE).toString());

		if (productQualifier != null)
			script.printProperty(PROPERTY_P2_PRODUCT_QUALIFIER, productQualifier);

		script.println();
		generateCustomGatherMacro();
	}

	protected void generateMainEnd() {
		script.printTargetEnd();
		script.println();
	}

	protected void generateMetadataCalls() {
		script.printAntCallTask(TARGET_P2_METADATA, true, null);
	}

	protected void generateGatherCalls() {
		super.generateGatherCalls();

		if (signJars)
			script.printAntCallTask(TARGET_P2_SIGN_REPO, true, null);
		script.println();
	}

	protected void generateBrandingCalls() {
		ProductFile product = getProductFile();
		if (product != null) {
			List configs = getConfigInfos();
			for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
				Config config = (Config) iterator.next();
				if (Config.genericConfig().equals(config))
					continue;
				script.printTab();
				script.print("<eclipse.brand.p2.artifacts "); //$NON-NLS-1$
				script.printAttribute("launcherName", Utils.getPropertyFormat(PROPERTY_LAUNCHER_NAME), true); //$NON-NLS-1$
				script.printAttribute("config", config.toString("."), true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("iconsList", computeIconsList(config.getOs()), true); //$NON-NLS-1$
				script.printAttribute("launcherProvider", Utils.getPropertyFormat(PROPERTY_LAUNCHER_PROVIDER), true); //$NON-NLS-1$
				script.printAttribute("productId", product.getId(), true); //$NON-NLS-1$
				script.printAttribute("productVersion", getReplacedProductVersion(), true); //$NON-NLS-1$
				script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
				script.printAttribute("tempDirectory", Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
		}
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + "/p2.branding", null, null); //$NON-NLS-1$
		script.println();
	}

	protected void generateSigningTarget() {
		script.printTargetDeclaration(TARGET_P2_SIGN_REPO, null, null, null, null);
		if (signJars && plugins.length + features.length > 0) {
			script.printTab();
			script.print("<p2.process.artifacts"); //$NON-NLS-1$
			script.printAttribute("repositoryPath", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			script.printTab();
			script.print("\t<sign"); //$NON-NLS-1$
			script.printAttribute("keystore", Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE), true); //$NON-NLS-1$
			script.printAttribute("storepass", Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS), true); //$NON-NLS-1$
			script.printAttribute("keypass", Utils.getPropertyFormat(PROPERTY_SIGN_KEYPASS), true); //$NON-NLS-1$
			script.printAttribute("alias", Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS), true); //$NON-NLS-1$
			script.printAttribute("unsign", Utils.getPropertyFormat(PROPERTY_UNSIGN), true); //$NON-NLS-1$
			script.print(" />\n"); //$NON-NLS-1$
			for (int i = 0; i < plugins.length; i++) {
				script.printTab();
				script.print("\t<plugin"); //$NON-NLS-1$
				script.printAttribute("id", plugins[i].getSymbolicName(), true); //$NON-NLS-1$
				script.printAttribute("version", plugins[i].getVersion().toString(), true); //$NON-NLS-1$
				script.print(" /> \n"); //$NON-NLS-1$
			}
			for (int i = 0; i < features.length; i++) {
				script.printTab();
				script.print("\t<feature"); //$NON-NLS-1$
				script.printAttribute("id", features[i].getId(), true); //$NON-NLS-1$
				script.printAttribute("version", features[i].getVersion(), true); //$NON-NLS-1$
				script.print(" />\n"); //$NON-NLS-1$
			}
			script.println("</p2.process.artifacts>"); //$NON-NLS-1$
		}
		script.printTargetEnd();
		script.println();
	}

	protected void generateMetadataTarget() {
		script.printTargetDeclaration(TARGET_P2_METADATA, null, null, null, null);
		script.printProperty(PROPERTY_P2_FLAVOR, "tooling"); //$NON-NLS-1$
		generateBrandingCalls();

		ProductFile product = getProductFile();
		if (product != null) {

			String productPath = product.getLocation();
			String productDir = getWorkingDirectory() + '/' + DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product"; //$NON-NLS-1$
			File productFile = new File(productPath);
			String newProduct = new File(productDir, productFile.getName()).getAbsolutePath();
			script.printCopyFileTask(productPath, newProduct, true);

			if (!generateProductP2Inf(productFile, productDir)) {
				//if we didn't generate the file, copy over the provided one
				File parent = new File(productPath).getParentFile();
				File p2Inf = new File(parent, "p2.inf"); //$NON-NLS-1$
				if (p2Inf.exists())
					script.printCopyTask(p2Inf.getAbsolutePath(), productDir, null, false, true);
			}

			script.printTab();
			script.print("<replace "); //$NON-NLS-1$
			script.printAttribute("file", new File(productDir, "p2.inf").getAbsolutePath(), true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("token", "@FLAVOR@", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("value", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$

			generateProductReplaceTask(product, newProduct);
			productPath = newProduct;

			script.printTab();
			script.print("<p2.publish.product"); //$NON-NLS-1$
			script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$ 
			script.printAttribute("productFile", newProduct, true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			for (Iterator iterator = getConfigInfos().iterator(); iterator.hasNext();) {
				Config config = (Config) iterator.next();
				if (Config.genericConfig().equals(config))
					continue;

				script.printTab();
				script.print("\t<config"); //$NON-NLS-1$
				script.printAttribute("os", config.getOs(), true); //$NON-NLS-1$
				script.printAttribute("ws", config.getWs(), true); //$NON-NLS-1$
				script.printAttribute("arch", config.getArch(), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			if (versionsList) {
				script.printTab();
				script.print("\t<advice"); //$NON-NLS-1$
				script.printAttribute("kind", "featureVersions", true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("file", getWorkingDirectory() + '/' + DEFAULT_FEATURE_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
				script.printTab();
				script.print("\t<advice"); //$NON-NLS-1$
				script.printAttribute("kind", "pluginVersions", true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("file", getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			script.println("</p2.publish.product>"); //$NON-NLS-1$
		}
		script.printTargetEnd();
	}

	private boolean generateProductP2Inf(File productFile, String root) {
		ProductGenerator generator = new ProductGenerator();
		generator.setProduct(productFile.getAbsolutePath());
		generator.setBuildSiteFactory(siteFactory);
		generator.setRoot(root);
		generator.setWorkingDirectory(getWorkingDirectory());
		generator.setAssemblyInfo(assemblyInformation);
		try {
			return generator.generateP2Info();
		} catch (CoreException e) {
			//problem with the .product file
			return false;
		}
	}

	protected void generateEpilogue() {
		script.printProjectEnd();
	}

	protected void generateCustomGatherMacro() {
		List attributes = new ArrayList(5);
		attributes.add("dir"); //$NON-NLS-1$
		attributes.add("propertyName"); //$NON-NLS-1$
		attributes.add("propertyValue"); //$NON-NLS-1$
		attributes.add("subFolder"); //$NON-NLS-1$
		attributes.add(PROPERTY_PROJECT_NAME);
		script.printMacroDef(PROPERTY_CUSTOM_GATHER, attributes);

		Map params = new HashMap();
		params.put("@{propertyName}", "@{propertyValue}"); //$NON-NLS-1$//$NON-NLS-2$
		script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, "@{dir}", TARGET_PUBLISH_BIN_PARTS, null, null, params); //$NON-NLS-1$

		params.put(PROPERTY_PROJECT_LOCATION, "${basedir}/@{dir}"); //$NON-NLS-1$
		params.put(PROPERTY_PROJECT_NAME, "@{projectName}"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET_FOLDER, "@{propertyValue}@{subFolder}"); //$NON-NLS-1$
		printCustomAssemblyAntCall(TARGET_GATHER_BIN_PARTS, params);

		script.printEndMacroDef();
		script.println();
	}

	protected void generateGatherBinPartsTarget() {
		ArrayList binaryFeatures = new ArrayList();
		ArrayList binaryBundles = new ArrayList();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, null, null, null, null);
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			Path pluginLocation = new Path(plugin.getLocation());
			if (Utils.isBinary(plugin))
				binaryBundles.add(new FileSet(pluginLocation.removeLastSegments(1).toOSString(), null, pluginLocation.lastSegment(), null, null, null, null));
			else
				printCustomGatherCall(ModelBuildScriptGenerator.getNormalizedName(plugin), Utils.makeRelative(pluginLocation, new Path(workingDirectory)).toOSString(), PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS), null);
		}

		Set featureSet = BuildDirector.p2Gathering ? new HashSet() : null;
		for (int i = 0; i < features.length; i++) {
			BuildTimeFeature feature = features[i];
			IPath featureLocation = new Path(feature.getURL().getPath()).removeLastSegments(1);
			if (feature.isBinary()) {
				binaryFeatures.add(new FileSet(featureLocation.removeLastSegments(1).toOSString(), null, featureLocation.lastSegment(), null, null, null, null));
			} else {
				String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
				printCustomGatherCall(featureFullName, Utils.makeRelative(featureLocation, new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
				featureSet.add(feature);
			}
		}

		//This will generate gather.bin.parts call to features that provides files for the root
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			BuildTimeFeature feature = (BuildTimeFeature) iter.next();
			if (featureSet.contains(feature))
				continue;
			IPath featureLocation = new Path(feature.getURL().getPath()).removeLastSegments(1);
			String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
			printCustomGatherCall(featureFullName, Utils.makeRelative(featureLocation, new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
		}

		String repo = Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO);
		script.printP2PublishFeaturesAndBundles(repo, repo, (FileSet[]) binaryBundles.toArray(new FileSet[binaryBundles.size()]), (FileSet[]) binaryFeatures.toArray(new FileSet[binaryFeatures.size()]), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_SITE), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_PREFIX), contextMetadata);

		script.printTargetEnd();
		script.println();
	}

	public void setVersionsList(boolean versionsList) {
		this.versionsList = versionsList;
	}
}
