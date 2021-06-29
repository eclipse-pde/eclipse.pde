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
package org.eclipse.pde.internal.core.update.configurator;

import org.eclipse.osgi.util.NLS;

final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.update.internal.configurator.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String cfig_unableToLoad_noURL;

	public static String InstalledSiteParser_UnableToCreateURL;
	public static String InstalledSiteParser_UnableToCreateURLForFile;

	public static String InstalledSiteParser_date;

	public static String FeatureParser_IdOrVersionInvalid;

	public static String ConfigurationParser_cannotLoadSharedInstall;

	public static String SiteEntry_computePluginStamp;
	public static String SiteEntry_cannotFindFeatureInDir;
	public static String SiteEntry_duplicateFeature;

	public static String PlatformConfiguration_cannotFindConfigFile;


	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String XMLPrintHandler_unsupportedNodeType;
}