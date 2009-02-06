/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

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
			openScript(directory, "assemble." + featureId + ".p2.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException e) {
			return;
		}
		generatePrologue();
		generateMainBegin();
		generateGatherCalls();
		generateBrandingCalls();
		generateMainEnd();

		generateGatherBinPartsTarget();
		generateCustomAssemblyTarget();
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
			script.printProperty(PROPERTY_P2_BUILD_REPO, "file:" + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/buildRepo"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		script.printProperty(PROPERTY_ASSEMBLY_TMP, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
		script.println();
		generateCustomGatherMacro();
	}

	protected void generateMainEnd() {
		script.printTargetEnd();
		script.println();
	}

	protected void generateBrandingCalls() {
		ProductFile product = getProductFile();
		if (product != null) {
			List configs = getConfigInfos();
			for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
				Config config = (Config) iterator.next();

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
	}

	protected void generateEpilogue() {
		script.printProjectEnd();
	}
}
