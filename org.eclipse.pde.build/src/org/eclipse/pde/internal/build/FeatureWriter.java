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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;

public class FeatureWriter extends XMLWriter implements IPDEBuildConstants {
	protected Feature feature;
	private final BuildTimeSite site;
	private final Map parameters = new LinkedHashMap(10);

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
		endTag(FEATURE);
		super.close();
	}

	public void printFeatureDeclaration() {
		parameters.clear();
		parameters.put(ID, feature.getId());
		parameters.put(VERSION, feature.getVersion());
		parameters.put("label", feature.getLabel()); //$NON-NLS-1$
		if (feature.getLicenseFeature() != null) {
			parameters.put("license-feature", feature.getLicenseFeature()); //$NON-NLS-1$
			parameters.put("license-feature-version", feature.getLicenseFeatureVersion()); //$NON-NLS-1$
		}
		parameters.put("provider-name", feature.getProviderName()); //$NON-NLS-1$
		parameters.put("image", feature.getImage()); //$NON-NLS-1$
		parameters.put("os", feature.getOS()); //$NON-NLS-1$
		parameters.put("arch", feature.getArch()); //$NON-NLS-1$
		parameters.put("ws", feature.getWS()); //$NON-NLS-1$
		parameters.put("nl", feature.getNL()); //$NON-NLS-1$
		//		parameters.put("colocation-affinity", feature.getAffinityFeature()); //$NON-NLS-1$
		//		parameters.put("primary", new Boolean(feature.isPrimary())); //$NON-NLS-1$
		//		parameters.put("application", feature.getApplication()); //$NON-NLS-1$

		startTag(FEATURE, parameters, true);
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
				parameters.put(ID, entries[i].getId());
				BuildTimeFeature tmpFeature = site.findFeature(entries[i].getId(), null, true);
				parameters.put(VERSION, tmpFeature.getVersion());
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingFeature, entries[i].getId());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}

			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printRequires() {
		boolean haveRequires = false;

		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (!entries[i].isRequires())
				continue;

			if (!haveRequires) {
				startTag("requires", null); //$NON-NLS-1$
				haveRequires = true;
			}
			parameters.clear();
			if (entries[i].isPlugin()) {
				parameters.put(PLUGIN, entries[i].getId());
				parameters.put(VERSION, entries[i].getVersion());
			} else {
				//The import refers to a feature
				parameters.put(FEATURE, entries[i].getId());
				parameters.put(VERSION, entries[i].getVersion());
			}
			parameters.put("match", entries[i].getMatch()); //$NON-NLS-1$
			printTag("import", parameters, true, true, true); //$NON-NLS-1$
		}
		if (haveRequires)
			endTag("requires"); //$NON-NLS-1$
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
			parameters.put(ID, entries[i].getId());

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
			parameters.put(VERSION, effectivePlugin.getVersion());
			if (entries[i].isFragment())
				parameters.put(FRAGMENT, new Boolean(entries[i].isFragment()));
			parameters.put("os", entries[i].getOS()); //$NON-NLS-1$
			parameters.put("arch", entries[i].getArch()); //$NON-NLS-1$
			parameters.put("ws", entries[i].getWS()); //$NON-NLS-1$
			parameters.put("nl", entries[i].getNL()); //$NON-NLS-1$
			if (!entries[i].isUnpack())
				parameters.put("unpack", Boolean.FALSE.toString()); //$NON-NLS-1$
			//			parameters.put("download-size", new Long(entries[i].getDownloadSize() != -1 ? entries[i].getDownloadSize() : 0)); //$NON-NLS-1$
			//			parameters.put("install-size", new Long(entries[i].getInstallSize() != -1 ? entries[i].getInstallSize() : 0)); //$NON-NLS-1$
			printTag(PLUGIN, parameters, true, true, true);
		}
	}

	private void printData() {
		//		INonPluginEntry[] entries = feature.getNonPluginEntries();
		//		for (int i = 0; i < entries.length; i++) {
		//			parameters.put(ID, entries[i].getIdentifier());
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
