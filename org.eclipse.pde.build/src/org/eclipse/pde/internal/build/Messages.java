/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.build.messages";//$NON-NLS-1$

	// warning
	public static String warning_cannotLocateSource;
	public static String warning_missingPassword;
	public static String warning_fallBackVersion;
	public static String warning_problemsParsingMapFileEntry;
	public static String warning_ant171Required;

	// error
	public static String error_pluginCycle;
	public static String error_missingDirectoryEntry;
	public static String error_incorrectDirectoryEntry;
	public static String error_directoryEntryRequiresIdAndRepo;
	public static String error_missingElement;
	public static String error_missingFeatureId;
	public static String error_cannotFetchNorFindFeature;
	public static String error_missingInstallLocation;
	public static String error_creatingFeature;
	public static String error_readingDirectory;
	public static String error_fetchingFeature;
	public static String error_fetchingFailed;
	public static String error_configWrongFormat;
	public static String error_missingCustomBuildFile;
	public static String error_missingSourceFolder;
	public static String error_noCorrespondingFactory;
	public static String error_retrieveFailed;
	public static String error_invalidURLInMapFileEntry;
	public static String error_licenseRootWithoutLicenseRef;

	// exception
	public static String exception_missingElement;
	public static String exception_missingFeature;
	public static String exception_missingFeatureInRange;
	public static String exception_missingFile;
	public static String exception_missingPlugin;
	public static String exception_unresolvedPlugin;
	public static String exception_unableToGenerateSourceFromBinary;
	public static String exception_writeScript;
	public static String exception_pluginParse;
	public static String exception_featureParse;
	public static String exception_readingFile;
	public static String exception_writingFile;
	public static String exception_url;
	public static String exception_stateAddition;
	public static String exception_registryResolution;
	public static String exception_errorConverting;
	public static String exception_cannotAcquireService;
	public static String exception_hostNotFound;
	public static String exception_missing_pdebuild_folder;

	// build.xml
	public static String build_plugin_buildJars;
	public static String build_plugin_jar;
	public static String build_plugin_buildUpdateJar;
	public static String build_plugin_clean;
	public static String build_plugin_zipPlugin;
	public static String build_plugin_refresh;
	public static String build_plugin_unrecognizedJRE;

	public static String build_feature_buildJars;
	public static String build_feature_buildUpdateJar;
	public static String build_feature_clean;
	public static String build_feature_zips;
	public static String build_feature_refresh;

	public static String build_compilerSetting;
	public static String invalid_archivesFormat;
	public static String error_loading_platform_properties;

	// assemble.xml
	public static String assemble_jarUp;
	public static String sign_Jar;

	// unsatisfied constraints
	public static String unsatisfied_import;
	public static String unsatisfied_required;
	public static String unsatisfied_optionalBundle;
	public static String unsatisfied_host;
	public static String unsatisfied_nativeSpec;

	public static String fetching_p2Repo;
	public static String includedFromFeature;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}
