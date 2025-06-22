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
 *     Les Jones <lesojones@gmail.com> - Bug 214457
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Locale;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.osgi.framework.Constants;

/**
 *	Contains constants for the core PDE models including the target platform.
 */
public interface ICoreConstants {

	// Preferences to allow products to add additional options in the target environment combos
	// Unknown if they are being used
	String OS_EXTRA = "org.eclipse.pde.os.extra"; //$NON-NLS-1$
	String WS_EXTRA = "org.eclipse.pde.ws.extra"; //$NON-NLS-1$
	String NL_EXTRA = "org.eclipse.pde.nl.extra"; //$NON-NLS-1$
	String ARCH_EXTRA = "org.eclipse.pde.arch.extra"; //$NON-NLS-1$

	/** Constant for the string <code>extension</code> */
	String EXTENSION_NAME = "extension"; //$NON-NLS-1$

	/** Constant for the string <code>plugin.xml</code> */
	String PLUGIN_FILENAME_DESCRIPTOR = "plugin.xml"; //$NON-NLS-1$

	/** Constant for the string <code>feature.xml</code> */
	String FEATURE_FILENAME_DESCRIPTOR = "feature.xml"; //$NON-NLS-1$

	/** Constant for the string <code>fragment.xml</code> */
	String FRAGMENT_FILENAME_DESCRIPTOR = "fragment.xml"; //$NON-NLS-1$

	/** Constant for the string <code>META-INF/MANIFEST.MF</code> */
	String BUNDLE_FILENAME_DESCRIPTOR = "META-INF/MANIFEST.MF"; //$NON-NLS-1$

	/** Constant for the string <code>MANIFEST.MF</code> */
	String MANIFEST_FILENAME = "MANIFEST.MF"; //$NON-NLS-1$

	/** Constant for the string <code>.options</code> */
	String OPTIONS_FILENAME = ".options"; //$NON-NLS-1$

	/** Constant for the string <code>manifest.mf</code> */
	String MANIFEST_FILENAME_LOWER_CASE = MANIFEST_FILENAME.toLowerCase(Locale.ENGLISH);

	/** Constant for the string <code>build.properties</code> */
	String BUILD_FILENAME_DESCRIPTOR = "build.properties"; //$NON-NLS-1$

	/**
	 * Default version number for plugin and feature
	 */
	String DEFAULT_VERSION = "0.0.0"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.0</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET30 = "3.0"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.1</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET31 = "3.1"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.2</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET32 = "3.2"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.3</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET33 = "3.3"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.4</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET34 = "3.4"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.5</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET35 = "3.5"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.6</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET36 = "3.6"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.7</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET37 = "3.7"; //$NON-NLS-1$

	/**
	 * Target version of <code>3.8</code>
	 * <p>
	 * PDE constant for the version of Eclipse a file/project/bundle is targeting.  The version of
	 * a target platform is determined by what version of Equinox (org.eclipse.osgi) is included in the
	 * target.  The version is only relevant if the user is building an Eclipse plug-in. This constant
	 * can be used to process PDE files that have changed structure between releases.
	 * </p><p>
	 * Anytime a new version constant is added, {@link ICoreConstants#TARGET_VERSION_LATEST} must be updated to
	 * the newest version.  {@link TargetPlatformHelper#getTargetVersionString()} must be updated to return the
	 * new version.
	 * </p>
	 */
	String TARGET38 = "3.8"; //$NON-NLS-1$

	/**
	 * The highest version of of Eclipse that PDE recognizes as having a special file structure. The value of
	 * this constant may change between releases.
	 * <p>
	 * Currently the latest version is {@link #TARGET38}.  If a new version constant is added to PDE, this
	 * constant must be updated to the latest version.  Also, {@link TargetPlatformHelper#getTargetVersionString()}
	 * must be updated to return the new version.
	 * </p><p>
	 * If the set of target versions available when creating a project changes, NewLibraryPluginCreationPage,
	 * NewProjectCreationPage, and {@link IBundleProjectDescription} must all be updated.
	 * </p>
	 */
	String TARGET_VERSION_LATEST = TARGET38;

	/**
	 * Preference key that stores a list of user specified source locations.
	 * No longer supported in the UI.
	 * @deprecated Not supported in the UI.
	 */
	@Deprecated
	String P_SOURCE_LOCATIONS = "source_locations"; //$NON-NLS-1$

	String EQUINOX = "Equinox"; //$NON-NLS-1$

	// project preferences
	String SELFHOSTING_BIN_EXCLUDES = "selfhosting.binExcludes"; //$NON-NLS-1$
	String EQUINOX_PROPERTY = "pluginProject.equinox"; //$NON-NLS-1$
	String EXTENSIONS_PROPERTY = "pluginProject.extensions"; //$NON-NLS-1$
	String RESOLVE_WITH_REQUIRE_BUNDLE = "resolve.requirebundle"; //$NON-NLS-1$

	/**
	 * Configures launch shortcuts visible in the manifest editor for a project.
	 * Value is a comma separated list of <code>org.eclipse.pde.ui.launchShortcuts</code>
	 * extension identifiers.
	 *
	 * @since 3.6
	 */
	String MANIFEST_LAUNCH_SHORTCUTS = "manifest.launchShortcuts"; //$NON-NLS-1$

	/**
	 * Configures the export wizard used in the manifest editor for a project.
	 * Value is an <code>org.eclipse.ui.exportWizards</code> extension identifier.
	 *
	 * @since 3.6
	 */
	String MANIFEST_EXPORT_WIZARD = "manifest.exportWizard"; //$NON-NLS-1$

	// for backwards compatibility with Eclipse 3.0 bundle manifest files
	String PROVIDE_PACKAGE = "Provide-Package"; //$NON-NLS-1$
	String REPROVIDE_ATTRIBUTE = "reprovide"; //$NON-NLS-1$
	String OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$
	String REQUIRE_PACKAGES_ATTRIBUTE = "require-packages"; //$NON-NLS-1$
	String SINGLETON_ATTRIBUTE = "singleton"; //$NON-NLS-1$
	String PACKAGE_SPECIFICATION_VERSION = "specification-version"; //$NON-NLS-1$
	String IMPORT_SERVICE = "Import-Service"; //$NON-NLS-1$
	String EXPORT_SERVICE = "Export-Service"; //$NON-NLS-1$

	// Equinox-specific headers
	String EXTENSIBLE_API = "Eclipse-ExtensibleAPI"; //$NON-NLS-1$
	String PATCH_FRAGMENT = "Eclipse-PatchFragment"; //$NON-NLS-1$
	String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$
	/**
	 * The 'Eclipse-AutoStart=true' header was used up to and including 3.1 to mean LAZY.
	 * However, this was a poorly named header, since "auto-start" sounds UN-LAZY, and
	 * was replaced with 'Eclipse-LazyStart=true' in 3.2.
	 */
	String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$
	/**
	 * The 'Eclipse-LazyStart=true' header replaced the 'Eclipse-AutoStart' header
	 * with a better name in 3.2. And since 3.4 (OSGi R4.1), the 'Bundle-ActivationPolicy: lazy'
	 * replaces all of these.
	 */
	String ECLIPSE_LAZYSTART = "Eclipse-LazyStart"; //$NON-NLS-1$
	String ECLIPSE_JREBUNDLE = "Eclipse-JREBundle"; //$NON-NLS-1$
	String ECLIPSE_BUDDY_POLICY = "Eclipse-BuddyPolicy"; //$NON-NLS-1$
	String ECLIPSE_REGISTER_BUDDY = "Eclipse-RegisterBuddy"; //$NON-NLS-1$
	String ECLIPSE_GENERIC_CAPABILITY = "Eclipse-GenericCapability"; //$NON-NLS-1$
	String ECLIPSE_GENERIC_REQUIRED = "Eclipse-GenericRequire"; //$NON-NLS-1$
	String PLATFORM_FILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$
	String ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle"; //$NON-NLS-1$
	String ECLIPSE_SYSTEM_BUNDLE = "Eclipse-SystemBundle"; //$NON-NLS-1$
	String ECLIPSE_BUNDLE_SHAPE = "Eclipse-BundleShape"; //$NON-NLS-1$
	String ECLIPSE_SOURCE_REFERENCES = "Eclipse-SourceReferences"; //$NON-NLS-1$
	String SERVICE_COMPONENT = "Service-Component"; //$NON-NLS-1$
	String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name"; //$NON-NLS-1$
	String ECLIPSE_EXPORT_EXTERNAL_ANNOTATIONS = "Eclipse-ExportExternalAnnotations"; //$NON-NLS-1$

	// Equinox-specific system properties
	String OSGI_SYSTEM_BUNDLE = "osgi.system.bundle"; //$NON-NLS-1$
	String OSGI_OS = "osgi.os"; //$NON-NLS-1$
	String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	String OSGI_NL = "osgi.nl"; //$NON-NLS-1$
	String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	String OSGI_RESOLVE_OPTIONAL = "osgi.resolveOptional"; //$NON-NLS-1$
	String OSGI_RESOLVER_MODE = "osgi.resolverMode"; //$NON-NLS-1$

	// Equinox-specific directives
	String INTERNAL_DIRECTIVE = "x-internal"; //$NON-NLS-1$
	String FRIENDS_DIRECTIVE = "x-friends"; //$NON-NLS-1$

	String SHAPE_JAR = "jar"; //$NON-NLS-1$
	String SHAPE_DIR = "dir"; //$NON-NLS-1$
	String[] SHAPE_VALUES = new String[] {SHAPE_DIR, SHAPE_JAR};

	String[] TRANSLATABLE_HEADERS = new String[] {Constants.BUNDLE_VENDOR, Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_CATEGORY, Constants.BUNDLE_CONTACTADDRESS};


	// Common folder names
	String MANIFEST_FOLDER_NAME = "META-INF/"; //$NON-NLS-1$
	String OSGI_INF_FOLDER_NAME = "OSGI-INF/"; //$NON-NLS-1$
	String FEATURE_FOLDER_NAME = "features"; //$NON-NLS-1$

	// Common paths
	IPath MANIFEST_PATH = IPath.fromOSString(BUNDLE_FILENAME_DESCRIPTOR);
	IPath PLUGIN_PATH = IPath.fromOSString(PLUGIN_FILENAME_DESCRIPTOR);
	IPath FRAGMENT_PATH = IPath.fromOSString(FRAGMENT_FILENAME_DESCRIPTOR);
	IPath FEATURE_PATH = IPath.fromOSString(FEATURE_FILENAME_DESCRIPTOR);
	IPath BUILD_PROPERTIES_PATH = IPath.fromOSString(BUILD_FILENAME_DESCRIPTOR);
	IPath OSGI_INF_PATH = IPath.fromOSString(OSGI_INF_FOLDER_NAME);

	// Extension point identifiers
	String EXTENSION_POINT_SOURCE = PDECore.PLUGIN_ID + ".source"; //$NON-NLS-1$
	String EXTENSION_POINT_BUNDLE_IMPORTERS = PDECore.PLUGIN_ID + ".bundleImporters"; //$NON-NLS-1$

	// file extensions

	/**
	 * File extension for target definitions
	 */
	String TARGET_FILE_EXTENSION = "target"; //$NON-NLS-1$

	/**
	 * Preference key for the active workspace target platform handle memento.  If not set,
	 * old target preferences will be used to create a default.  If no external bundles are
	 * wanted, this value should be set to {@link #NO_TARGET}.
	 */
	String WORKSPACE_TARGET_HANDLE = "workspace_target_handle"; //$NON-NLS-1$

	/**
	 * Preference key for the workspace bundle overriding target bundle for the
	 * same id
	 */
	String WORKSPACE_PLUGINS_OVERRIDE_TARGET = "workspace_plugins_override_target"; //$NON-NLS-1$
	/**
	 * Boolean preference whether API analysis has been disabled
	 */
	String DISABLE_API_ANALYSIS_BUILDER = "Preferences.MainPage.disableAPIAnalysisBuilder";//$NON-NLS-1$
	/**
	 * Boolean preference whether API analysis should run asynchronous to the
	 * build as background job
	 */
	String RUN_API_ANALYSIS_AS_JOB = "Preferences.MainPage.runAPIAnalysisAsJob";//$NON-NLS-1$
	/**
	 * Boolean preference whether add
	 * '-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true' to VM
	 * arguments when creating a new launch configuration
	 */
	String ADD_SWT_NON_DISPOSAL_REPORTING = "Preferences.MainPage.addSwtNonDisposalReporting ";//$NON-NLS-1$

	/**
	 * Explicit preference value for {@link #WORKSPACE_TARGET_HANDLE} when the user chooses no
	 * target for the workspace (no external bundles).
	 */
	String NO_TARGET = "NO_TARGET"; //$NON-NLS-1$

	/**
	 * Preference key for the patterns that determine if a plugin project should be
	 * treated as test code
	 */
	String TEST_PLUGIN_PATTERN = "test_plugin_pattern"; //$NON-NLS-1$

	// default value for
	String TEST_PLUGIN_PATTERN_DEFAULTVALUE = "[.]test[s]?$|[.]tests[.]"; //$NON-NLS-1$

	IPath JUNIT_CONTAINER_PATH = new org.eclipse.core.runtime.Path("org.eclipse.jdt.junit.JUNIT_CONTAINER"); //$NON-NLS-1$
	IPath JUNIT5_CONTAINER_PATH = JUNIT_CONTAINER_PATH.append("5"); //$NON-NLS-1$
	IPath JUNIT4_CONTAINER_PATH = JUNIT_CONTAINER_PATH.append("4"); //$NON-NLS-1$
}
