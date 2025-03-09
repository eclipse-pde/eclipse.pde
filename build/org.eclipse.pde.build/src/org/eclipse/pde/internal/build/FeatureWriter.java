/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.equinox.p2.publisher.eclipse.URLEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;

public class FeatureWriter extends XMLWriter implements IPDEBuildConstants {
	protected Feature feature;
	private final BuildTimeSite site;
	private final Map<String, String> parameters = new LinkedHashMap<>(10);

	public FeatureWriter(OutputStream out, Feature feature, BuildTimeSite site) {
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
		if (url == null && library == null && handler == null) {
			return;
		}
		parameters.clear();
		parameters.put("library", library); //$NON-NLS-1$
		parameters.put("handler", handler); //$NON-NLS-1$
		parameters.put("url", url); //$NON-NLS-1$
		startTag("install-handler", parameters); //$NON-NLS-1$
		endTag("install-handler"); //$NON-NLS-1$
	}

	public void printDescription() {
		if (feature.getDescription() == null && feature.getDescriptionURL() == null) {
			return;
		}
		parameters.clear();
		parameters.put("url", feature.getDescriptionURL()); //$NON-NLS-1$

		startTag("description", parameters, true); //$NON-NLS-1$
		printTabulation();
		printlnEscaped(feature.getDescription());
		endTag("description"); //$NON-NLS-1$
	}

	private void printCopyright() {
		if (feature.getCopyright() == null && feature.getCopyrightURL() == null) {
			return;
		}
		parameters.clear();
		parameters.put("url", feature.getCopyrightURL()); //$NON-NLS-1$
		startTag("copyright", parameters, true); //$NON-NLS-1$
		printTabulation();
		printlnEscaped(feature.getCopyright());
		endTag("copyright"); //$NON-NLS-1$
	}

	public void printLicense() {
		if (feature.getLicense() == null && feature.getLicenseURL() == null) {
			return;
		}
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

			for (URLEntry siteEntry : siteEntries) {
				parameters.clear();
				parameters.put("url", siteEntry.getURL()); //$NON-NLS-1$
				parameters.put("label", siteEntry.getAnnotation()); //$NON-NLS-1$
				printTag("discovery", parameters, true, true, true); //$NON-NLS-1$
			}
			endTag("url"); //$NON-NLS-1$
		}
	}

	public void printIncludes() throws CoreException {
		FeatureEntry[] entries = feature.getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || entry.isPlugin()) {
				continue;
			}

			parameters.clear();
			try {
				parameters.put(ID, entry.getId());
				BuildTimeFeature tmpFeature = site.findFeature(entry.getId(), null, true);
				parameters.put(VERSION, tmpFeature.getVersion());
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingFeature, entry.getId());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}

			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}

	private void printRequires() {
		boolean haveRequires = false;

		FeatureEntry[] entries = feature.getEntries();
		for (FeatureEntry entry : entries) {
			if (!entry.isRequires()) {
				continue;
			}

			if (!haveRequires) {
				startTag("requires", null); //$NON-NLS-1$
				haveRequires = true;
			}
			parameters.clear();
			if (entry.isPlugin()) {
				parameters.put(PLUGIN, entry.getId());
				parameters.put(VERSION, entry.getVersion());
			} else {
				//The import refers to a feature
				parameters.put(FEATURE, entry.getId());
				parameters.put(VERSION, entry.getVersion());
			}
			parameters.put("match", entry.getMatch()); //$NON-NLS-1$
			printTag("import", parameters, true, true, true); //$NON-NLS-1$
		}
		if (haveRequires) {
			endTag("requires"); //$NON-NLS-1$
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
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || !entry.isPlugin()) {
				continue;
			}
			parameters.clear();
			parameters.put(ID, entry.getId());

			String versionRequested = entry.getVersion();
			BundleDescription effectivePlugin = null;
			try {
				effectivePlugin = site.getRegistry().getResolvedBundle(entry.getId(), versionRequested);
			} catch (CoreException e) {
				String message = NLS.bind(Messages.exception_missingPlugin, entry.getId() + "_" + entry.getVersion()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			if (effectivePlugin == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, entry.getId() + "_" + entry.getVersion()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			parameters.put(VERSION, effectivePlugin.getVersion().toString());
			if (entry.isFragment()) {
				parameters.put(FRAGMENT, String.valueOf(entry.isFragment()));
			}
			parameters.put("os", entry.getOS()); //$NON-NLS-1$
			parameters.put("arch", entry.getArch()); //$NON-NLS-1$
			parameters.put("ws", entry.getWS()); //$NON-NLS-1$
			parameters.put("nl", entry.getNL()); //$NON-NLS-1$
			if (!entry.isUnpack()) {
				parameters.put("unpack", Boolean.FALSE.toString()); //$NON-NLS-1$
			}
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
		//			printTag("data", parameters, true, true, true); //$NON-NLS-1$
		//		}
	}
}
