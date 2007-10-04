/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.eclipse.pde.internal.build.site.compatibility.*;

public class FeatureWriter extends XMLWriter implements IPDEBuildConstants {
	protected Feature feature;
	private BuildTimeSite site;
	private Map parameters = new LinkedHashMap(10);

	public FeatureWriter(OutputStream out, Feature feature, BuildTimeSite site) throws IOException {
		super(out);
		this.feature = feature;
		this.site = site;
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
		parameters.put("id", feature.getId()); //$NON-NLS-1$
		parameters.put("version", feature.getVersion()); //$NON-NLS-1$
		parameters.put("label", feature.getLabel()); //$NON-NLS-1$
		parameters.put("provider-name", feature.getProviderName()); //$NON-NLS-1$
		parameters.put("image", feature.getImage()); //$NON-NLS-1$
		parameters.put("os", feature.getOS()); //$NON-NLS-1$
		parameters.put("arch", feature.getArch()); //$NON-NLS-1$
		parameters.put("ws", feature.getWS()); //$NON-NLS-1$
		parameters.put("nl", feature.getNL()); //$NON-NLS-1$
//		parameters.put("colocation-affinity", feature.getAffinityFeature()); //$NON-NLS-1$
//		parameters.put("primary", new Boolean(feature.isPrimary())); //$NON-NLS-1$
//		parameters.put("application", feature.getApplication()); //$NON-NLS-1$

		startTag("feature", parameters, true); //$NON-NLS-1$
	}

	public void printInstallHandler() {
		String url = feature.getInstallHandlerURL();
		String library = feature.getInstallHandlerLibrary();
		String handler = feature.getInstallHandler();
		if (url == null && library == null && handler == null)
			return;
		parameters.clear();
		parameters.put("library", library); //$NON-NLS-1$
		parameters.put("handler", handler); //$NON-NLS-1$
		parameters.put("url", url); //$NON-NLS-1$
		startTag("install-handler", parameters); //$NON-NLS-1$
		endTag("install-handler"); //$NON-NLS-1$
	}

	public void printDescription() {
		if (feature.getDescription() == null && feature.getDescriptionURL() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getDescriptionURL()); //$NON-NLS-1$

		startTag("description", parameters, true); //$NON-NLS-1$
		printTabulation();
		printlnEscaped(feature.getDescription());
		endTag("description"); //$NON-NLS-1$
	}

	private void printCopyright() {
		if (feature.getCopyright() == null && feature.getCopyrightURL() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getCopyrightURL()); //$NON-NLS-1$
		startTag("copyright", parameters, true); //$NON-NLS-1$
		printTabulation();
		printlnEscaped(feature.getCopyright());
		endTag("copyright"); //$NON-NLS-1$
	}

	public void printLicense() {
		if (feature.getLicense() == null && feature.getLicenseURL() == null)
			return;
		parameters.clear();
		parameters.put("url", feature.getLicenseURL()); //$NON-NLS-1$
		startTag("license", parameters, true); //$NON-NLS-1$
		printTabulation();
		printlnEscaped(feature.getLicense());
		endTag("license"); //$NON-NLS-1$
	}

	public void printURL() {
		String updateSiteLabel = feature.getUpdateSiteLabel();
		String updateSiteURL = feature.getUpdateSiteURL();
		URLEntry[] siteEntries = feature.getDiscoverySites();
		if (updateSiteLabel != null || updateSiteURL != null || siteEntries.length != 0) {
			parameters.clear();

			startTag("url", null); //$NON-NLS-1$
			if (updateSiteLabel != null && updateSiteURL != null) {
				parameters.clear();
				parameters.put("url", updateSiteURL); //$NON-NLS-1$
				parameters.put("label", updateSiteLabel); //$NON-NLS-1$
				printTag("update", parameters, true, true, true); //$NON-NLS-1$
			}

			for (int i = 0; i < siteEntries.length; i++) {
				parameters.clear();
				parameters.put("url", siteEntries[i].getURL()); //$NON-NLS-1$
				parameters.put("label", siteEntries[i].getAnnotation()); //$NON-NLS-1$
				printTag("discovery", parameters, true, true, true); //$NON-NLS-1$
			}
			endTag("url"); //$NON-NLS-1$
		}
	}

	public void printIncludes() throws CoreException {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || entries[i].isPlugin())
				continue;

			parameters.clear();
			try {
				parameters.put("id", entries[i].getId()); //$NON-NLS-1$
				BuildTimeFeature tmpFeature = site.findFeature(entries[i].getId(), null, true);
				parameters.put("version", tmpFeature.getVersion()); //$NON-NLS-1$
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingFeature, entries[i].getId());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}

			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printRequires() {
//		if (feature.getImportModels().length == 0)
//			return;
		startTag("requires", null); //$NON-NLS-1$
		printImports();
		endTag("requires"); //$NON-NLS-1$
	}

	private void printImports() {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (!entries[i].isRequires())
				continue;
			parameters.clear();
			if (entries[i].isPlugin()) {
				parameters.put("plugin", entries[i].getId()); //$NON-NLS-1$
				parameters.put("version", entries[i].getVersion()); //$NON-NLS-1$
			} else {
				//The import refers to a feature
				parameters.put("feature", entries[i].getId()); //$NON-NLS-1$
				parameters.put("version", entries[i].getVersion()); //$NON-NLS-1$
			}
			parameters.put("match", entries[i].getMatch()); //$NON-NLS-1$
			printTag("import", parameters, true, true, true); //$NON-NLS-1$
		}
	}

//	/**
//	 * Method getStringForMatchingRule.
//	 * @param ruleNumber
//	 */
//	private String getStringForMatchingRule(int ruleNumber) {
//		switch (ruleNumber) {
//			case 1 :
//				return "perfect"; //$NON-NLS-1$
//			case 2 :
//				return "equivalent"; //$NON-NLS-1$
//			case 3 :
//				return "compatible"; //$NON-NLS-1$
//			case 4 :
//				return "greaterOrEqual"; //$NON-NLS-1$
//			case 0 :
//			default :
//				return ""; //$NON-NLS-1$
//		}
//	}

	public void printPlugins() throws CoreException {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || !entries[i].isPlugin())
				continue;
			parameters.clear();
			parameters.put("id", entries[i].getId()); //$NON-NLS-1$

			String versionRequested = entries[i].getVersion();
			BundleDescription effectivePlugin = null;
			try {
				effectivePlugin = site.getRegistry().getResolvedBundle(entries[i].getId(), versionRequested);
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingPlugin, entries[i].getId() + "_" + entries[i].getVersion()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			if (effectivePlugin == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, entries[i].getId() + "_" + entries[i].getVersion()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			parameters.put("version", effectivePlugin.getVersion()); //$NON-NLS-1$
			if (entries[i].isFragment())
				parameters.put("fragment", new Boolean(entries[i].isFragment())); //$NON-NLS-1$
			parameters.put("os", entries[i].getOS()); //$NON-NLS-1$
			parameters.put("arch", entries[i].getOS()); //$NON-NLS-1$
			parameters.put("ws", entries[i].getWS()); //$NON-NLS-1$
			parameters.put("nl", entries[i].getNL()); //$NON-NLS-1$
			if (!entries[i].isUnpack())
				parameters.put("unpack", Boolean.FALSE.toString()); //$NON-NLS-1$
//			parameters.put("download-size", new Long(entries[i].getDownloadSize() != -1 ? entries[i].getDownloadSize() : 0)); //$NON-NLS-1$
//			parameters.put("install-size", new Long(entries[i].getInstallSize() != -1 ? entries[i].getInstallSize() : 0)); //$NON-NLS-1$
			printTag("plugin", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printData() {
//		INonPluginEntry[] entries = feature.getNonPluginEntries();
//		for (int i = 0; i < entries.length; i++) {
//			parameters.put("id", entries[i].getIdentifier()); //$NON-NLS-1$
//			parameters.put("os", entries[i].getOS()); //$NON-NLS-1$
//			parameters.put("arch", entries[i].getOSArch()); //$NON-NLS-1$
//			parameters.put("ws", entries[i].getWS()); //$NON-NLS-1$
//			parameters.put("nl", entries[i].getNL()); //$NON-NLS-1$
//			parameters.put("download-size", new Long(entries[i].getDownloadSize() != -1 ? entries[i].getDownloadSize() : 0)); //$NON-NLS-1$
//			parameters.put("install-size", new Long(entries[i].getInstallSize() != -1 ? entries[i].getInstallSize() : 0)); //$NON-NLS-1$
//			printTag("data", parameters, true, true, true); //$NON-NLS-1$
//		}
	}
}
