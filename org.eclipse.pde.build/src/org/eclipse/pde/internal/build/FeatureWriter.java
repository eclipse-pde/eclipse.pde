/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.URLEntryModel;

public class FeatureWriter extends XMLWriter implements IPDEBuildConstants {
	protected Feature feature;
	protected FeatureBuildScriptGenerator generator;
	private Map parameters = new HashMap(10);

	public FeatureWriter(OutputStream out, Feature feature, FeatureBuildScriptGenerator generator) throws IOException {
		super(out);
		this.feature = feature;
		this.generator = generator;
	}

	public void printFeature() throws CoreException {
		printFeatureDeclaration();
		printInstallHandler();
		printDescription();
		printCopyright();
		printLicense();
		printURL();
		printIncludes();
		printRequires();
		printPlugins();
		printData();
		endTag("feature"); //$NON-NLS-1$
		super.close();
	}

	public void printFeatureDeclaration() {
		parameters.clear();
		parameters.put("id", feature.getFeatureIdentifier()); //$NON-NLS-1$
		parameters.put("version", feature.getVersionedIdentifier().getVersion().toString()); //$NON-NLS-1$
		parameters.put("label", feature.getLabelNonLocalized()); //$NON-NLS-1$
		parameters.put("provider-name", feature.getProviderNonLocalized()); //$NON-NLS-1$
		parameters.put("image", feature.getImageURLString()); //$NON-NLS-1$
		parameters.put("os", feature.getOS()); //$NON-NLS-1$
		parameters.put("arch", feature.getOSArch()); //$NON-NLS-1$
		parameters.put("ws", feature.getWS()); //$NON-NLS-1$
		parameters.put("nl", feature.getNL()); //$NON-NLS-1$
		parameters.put("colocation-affinity", feature.getAffinityFeature()); //$NON-NLS-1$
		parameters.put("primary", new Boolean(feature.isPrimary())); //$NON-NLS-1$
		parameters.put("application", feature.getApplication()); //$NON-NLS-1$

		startTag("feature", parameters, true); //$NON-NLS-1$
	}

	public void printInstallHandler() {
		if (feature.getInstallHandlerEntry() == null)
			return;
		parameters.clear();
		parameters.put("library", feature.getInstallHandlerModel().getLibrary()); //$NON-NLS-1$
		parameters.put("handler", feature.getInstallHandlerModel().getHandlerName()); //$NON-NLS-1$
		startTag("install-handler", parameters); //$NON-NLS-1$
		endTag("install-handler"); //$NON-NLS-1$
	}

	public void printDescription() {
		if (feature.getDescriptionModel() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getDescriptionModel().getURLString()); //$NON-NLS-1$

		startTag("description", parameters, true); //$NON-NLS-1$
		printTabulation();
		println(feature.getDescriptionModel().getAnnotationNonLocalized());
		endTag("description"); //$NON-NLS-1$
	}

	private void printCopyright() {
		if (feature.getCopyrightModel() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getCopyrightModel().getURLString()); //$NON-NLS-1$
		startTag("copyright", parameters, true); //$NON-NLS-1$
		printTabulation();
		println(feature.getCopyrightModel().getAnnotationNonLocalized());
		endTag("copyright"); //$NON-NLS-1$
	}

	public void printLicense() {
		if (feature.getLicenseModel() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getLicenseModel().getURLString()); //$NON-NLS-1$
		startTag("license", parameters, true); //$NON-NLS-1$
		printTabulation();
		println(feature.getLicenseModel().getAnnotationNonLocalized());
		endTag("license"); //$NON-NLS-1$
	}

	public void printURL() {
		if (feature.getUpdateSiteEntryModel() != null || feature.getDiscoverySiteEntryModels().length != 0) {
			parameters.clear();

			startTag("url", null); //$NON-NLS-1$
			if (feature.getUpdateSiteEntryModel() != null) {
				parameters.clear();
				parameters.put("url", feature.getUpdateSiteEntryModel().getURLString()); //$NON-NLS-1$
				parameters.put("label", feature.getUpdateSiteEntryModel().getAnnotationNonLocalized()); //$NON-NLS-1$
				printTag("update", parameters, true, true, true); //$NON-NLS-1$
			}

			URLEntryModel[] siteEntries = feature.getDiscoverySiteEntryModels();
			for (int i = 0; i < siteEntries.length; i++) {
				parameters.clear();
				parameters.put("url", siteEntries[i].getURLString()); //$NON-NLS-1$
				parameters.put("label", siteEntries[i].getAnnotationNonLocalized()); //$NON-NLS-1$
				printTag("discovery", parameters, true, true, true); //$NON-NLS-1$
			}
			endTag("url"); //$NON-NLS-1$
		}
	}

	public void printIncludes() throws CoreException {
		IIncludedFeatureReference[] features = feature.getRawIncludedFeatureReferences();
		for (int i = 0; i < features.length; i++) {
			parameters.clear();
			try {
				parameters.put("id", features[i].getVersionedIdentifier().getIdentifier()); //$NON-NLS-1$
				IFeature tmpFeature = generator.getSite(false).findFeature(features[i].getVersionedIdentifier().getIdentifier(), null, true);
				parameters.put("version", tmpFeature.getVersionedIdentifier().getVersion().toString()); //$NON-NLS-1$
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingFeature, features[i].getVersionedIdentifier().getIdentifier());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}

			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printRequires() {
		if (feature.getImportModels().length == 0)
			return;
		startTag("requires", null); //$NON-NLS-1$
		printImports();
		endTag("requires"); //$NON-NLS-1$
	}

	private void printImports() {
		IImport[] imports = feature.getRawImports();
		for (int i = 0; i < imports.length; i++) {
			parameters.clear();
			if (imports[i].getKind() == IImport.KIND_PLUGIN) {
				parameters.put("plugin", imports[i].getVersionedIdentifier().getIdentifier()); //$NON-NLS-1$
				parameters.put("version", imports[i].getVersionedIdentifier().getVersion().toString()); //$NON-NLS-1$
			} else {
				//The import refers to a feature
				parameters.put("feature", imports[i].getVersionedIdentifier().getIdentifier()); //$NON-NLS-1$
				parameters.put("version", imports[i].getVersionedIdentifier().getVersion().toString()); //$NON-NLS-1$
			}
			parameters.put("match", getStringForMatchingRule(imports[i].getRule())); //$NON-NLS-1$
			printTag("import", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	/**
	 * Method getStringForMatchingRule.
	 * @param ruleNumber
	 */
	private String getStringForMatchingRule(int ruleNumber) {
		switch (ruleNumber) {
			case 1 :
				return "perfect"; //$NON-NLS-1$
			case 2 :
				return "equivalent"; //$NON-NLS-1$
			case 3 :
				return "compatible"; //$NON-NLS-1$
			case 4 :
				return "greaterOrEqual"; //$NON-NLS-1$
			case 0 :
			default :
				return ""; //$NON-NLS-1$
		}
	}

	public void printPlugins() throws CoreException {
		IPluginEntry[] plugins = feature.getRawPluginEntries();
		for (int i = 0; i < plugins.length; i++) {
			parameters.clear();
			parameters.put("id", plugins[i].getVersionedIdentifier().getIdentifier()); //$NON-NLS-1$

			String versionRequested = plugins[i].getVersionedIdentifier().getVersion().toString();
			BundleDescription effectivePlugin = null;
			try {
				effectivePlugin = generator.getSite(false).getRegistry().getResolvedBundle(plugins[i].getVersionedIdentifier().getIdentifier(), versionRequested);
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingPlugin, plugins[i].getVersionedIdentifier());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			if (effectivePlugin == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, plugins[i].getVersionedIdentifier());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			parameters.put("version", effectivePlugin.getVersion()); //$NON-NLS-1$
			parameters.put("fragment", new Boolean(plugins[i].isFragment())); //$NON-NLS-1$
			parameters.put("os", plugins[i].getOS()); //$NON-NLS-1$
			parameters.put("arch", plugins[i].getOSArch()); //$NON-NLS-1$
			parameters.put("ws", plugins[i].getWS()); //$NON-NLS-1$
			parameters.put("nl", plugins[i].getNL()); //$NON-NLS-1$
			if (plugins[i] instanceof PluginEntry && !((PluginEntry)plugins[i]).isUnpack())
				parameters.put("unpack", Boolean.FALSE.toString()); //$NON-NLS-1$
			parameters.put("download-size", new Long(plugins[i].getDownloadSize() != -1 ? plugins[i].getDownloadSize() : 0)); //$NON-NLS-1$
			parameters.put("install-size", new Long(plugins[i].getInstallSize() != -1 ? plugins[i].getInstallSize() : 0)); //$NON-NLS-1$
			printTag("plugin", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printData() {
		INonPluginEntry[] entries = feature.getNonPluginEntries();
		for (int i = 0; i < entries.length; i++) {
			parameters.put("id", entries[i].getIdentifier()); //$NON-NLS-1$
			parameters.put("os", entries[i].getOS()); //$NON-NLS-1$
			parameters.put("arch", entries[i].getOSArch()); //$NON-NLS-1$
			parameters.put("ws", entries[i].getWS()); //$NON-NLS-1$
			parameters.put("nl", entries[i].getNL()); //$NON-NLS-1$
			parameters.put("download-size", new Long(entries[i].getDownloadSize() != -1 ? entries[i].getDownloadSize() : 0)); //$NON-NLS-1$
			parameters.put("install-size", new Long(entries[i].getInstallSize() != -1 ? entries[i].getInstallSize() : 0)); //$NON-NLS-1$
			printTag("data", parameters, true, true, true); //$NON-NLS-1$
		}
	}
}
