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
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;

public class P2ConfigScriptGenerator extends AssembleConfigScriptGenerator {
	private AssemblyInformation assemblyInformation = null;
	private boolean assembling = false;

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
			p.addAll(assembling ? assemblyInformation.getCompiledPlugins(config) : assemblyInformation.getBinaryPlugins(config));
			f.addAll(assembling ? assemblyInformation.getCompiledFeatures(config) : assemblyInformation.getBinaryFeatures(config));
			if (assembling)
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
			script.printProperty(PROPERTY_LAUNCHER_NAME, product.getLauncherName());
			script.printProperty(PROPERTY_LAUNCHER_PROVIDER, FEATURE_EQUINOX_EXECUTABLE);
		}
		script.printProperty(PROPERTY_P2_BUILD_REPO, "file:" + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/buildRepo"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_ASSEMBLY_TMP, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
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
				script.printAttribute("productVersion", product.getVersion(), true); //$NON-NLS-1$
				script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
				script.printAttribute("tempDirectory", Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
		}
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + "/p2.branding", null, null); //$NON-NLS-1$
		script.println();
	}

	protected void generateMetadataTarget() {
		script.printTargetDeclaration(TARGET_P2_METADATA, null, null, assembling ? PROPERTY_RUN_PACKAGER : null, null);

		generateBrandingCalls();

		ProductFile product = getProductFile();
		if (product != null) {

			String productPath = product.getLocation();
			String productDir = getWorkingDirectory() + '/' + DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product"; //$NON-NLS-1$
			File productFile = new File(productPath);
			String newProduct = new File(productDir, productFile.getName()).getAbsolutePath();
			script.printCopyFileTask(productPath, newProduct, true);

			File parent = new File(productPath).getParentFile();
			File p2Inf = new File(parent, "p2.inf"); //$NON-NLS-1$
			if (p2Inf.exists())
				script.printCopyTask(p2Inf.getAbsolutePath(), productDir, null, false, true);
			generateProductReplaceTask(product, newProduct);
			productPath = newProduct;

			script.printTab();
			script.print("<p2.publish.product "); //$NON-NLS-1$
			script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$ 
			script.printAttribute("productFile", newProduct, true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}
		script.printTargetEnd();
	}

	//TODO this is duplicated from AssembleScriptGenerator
	protected void generateProductReplaceTask(ProductFile product, String productDirectory) {
		if (product == null)
			return;

		BuildTimeSite site = null;
		try {
			site = getSite(false);
		} catch (CoreException e1) {
			return;
		}

		List productEntries = product.getProductEntries();
		String mappings = Utils.getEntryVersionMappings((FeatureEntry[]) productEntries.toArray(new FeatureEntry[productEntries.size()]), site);
		script.println("<eclipse.idReplacer productFilePath=\"" + AntScript.getEscaped(productDirectory) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("                    selfVersion=\"" + product.getVersion() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		if (product.useFeatures())
			script.println("                    featureIds=\"" + mappings + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			script.println("                    pluginIds=\"" + mappings + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ 

		return;
	}

	protected void generateEpilogue() {
		script.printProjectEnd();
	}

	protected void generateGatherBinPartsTarget() {
		if (assembling) {
			super.generateGatherBinPartsTarget();
			return;
		}

		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, null, null, null, null);

		ArrayList p2Features = new ArrayList();
		ArrayList p2Bundles = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			p2Bundles.add(new FileSet(pluginLocation.removeLastSegments(1).toOSString(), null, pluginLocation.lastSegment(), null, null, null, null));

		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getURL().getPath());
			featureLocation = featureLocation.removeLastSegments(1);
			p2Features.add(new FileSet(featureLocation.removeLastSegments(1).toOSString(), null, featureLocation.lastSegment(), null, null, null, null));
		}

		String repo = Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO);
		script.printP2PublishFeaturesAndBundles(repo, repo, (FileSet[]) p2Bundles.toArray(new FileSet[p2Bundles.size()]), (FileSet[]) p2Features.toArray(new FileSet[p2Features.size()]), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_SITE));

		script.printTargetEnd();
		script.println();
	}
}
