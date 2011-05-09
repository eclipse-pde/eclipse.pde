/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Compuware Corporation - Sebastien Angers <sebastien.angers@compuware.com> 
 *     		- Enabled additional mirror slicingOptions in Headless PDE Build
 *     		- Enabled 'raw' attribute for mirror step in Headless PDE Build
 *     		- https://bugs.eclipse.org/338878
 *******************************************************************************/
package org.eclipse.pde.internal.build;

public interface IBuildPropertiesConstants {
	public final static String PERMISSIONS = "permissions"; //$NON-NLS-1$
	public final static String LINK = "link"; //$NON-NLS-1$
	public final static String EXECUTABLE = "executable"; //$NON-NLS-1$
	public final static String ROOT_PREFIX = "root."; //$NON-NLS-1$
	public final static String ROOT = "root"; //$NON-NLS-1$
	public final static String ROOT_FOLDER_PREFIX = ROOT_PREFIX + "folder."; //$NON-NLS-1$
	public final static String FOLDER_INFIX = ".folder."; //$NON-NLS-1$
	public final static String PERMISSIONS_INFIX = ".permissions."; //$NON-NLS-1$
	public final static String LINK_SUFFIX = ".link"; //$NON-NLS-1$

	public final static String TRUE = "true"; //$NON-NLS-1$
	public final static String FALSE = "false"; //$NON-NLS-1$

	public static final String PROPERTY_JAR_EXTRA_CLASSPATH = "jars.extra.classpath"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_ORDER = "jars.compile.order"; //$NON-NLS-1$
	public static final String PROPERTY_SOURCE_PREFIX = "source."; //$NON-NLS-1$
	public static final String PROPERTY_OUTPUT_PREFIX = "output."; //$NON-NLS-1$
	public static final String PROPERTY_EXTRAPATH_PREFIX = "extra."; //$NON-NLS-1$	
	public static final String PROPERTY_EXCLUDE_PREFIX = "exclude."; //$NON-NLS-1$
	public static final String PROPERTY_JAR_SUFFIX = ".jar"; //$NON-NLS-1$
	public static final String PROPERTY_MANIFEST_PREFIX = "manifest."; //$NON-NLS-1$

	public static final String PROPERTY_CONVERTED_MANIFEST = "convertedManifest"; //$NON-NLS-1$
	public static final String PROPERTY_SOURCE_REFERENCE = "sourceReference"; //$NON-NLS-1$
	public static final String PROPERTY_QUALIFIER = "qualifier"; //$NON-NLS-1$
	public static final String PROPERTY_VERSION_REPLACEMENT = "versionReplacement"; //$NON-NLS-1$
	public static final String PROPERTY_REQUIRED_BUNDLE_IDS = "requiredBundleIds"; //$NON-NLS-1$
	public static final String PROPERTY_NONE = "none"; //$NON-NLS-1$
	public static final String PROPERTY_CONTEXT = "context"; //$NON-NLS-1$

	public final static String GENERATION_SOURCE_PREFIX = "generate."; //$NON-NLS-1$
	public final static String GENERATION_SOURCE_FEATURE_PREFIX = GENERATION_SOURCE_PREFIX + "feature@"; //$NON-NLS-1$
	public final static String GENERATION_SOURCE_PLUGIN_PREFIX = GENERATION_SOURCE_PREFIX + "plugin@"; //$NON-NLS-1$
	public final static String PROPERTY_SOURCE_FEATURE_NAME = "sourceFeature.name"; //$NON-NLS-1$
	public final static String PRODUCT_PREFIX = "product@"; //$NON-NLS-1$

	public static final String PROPERTY_CUSTOM = "custom"; //$NON-NLS-1$
	public static final String PROPERTY_GENERATE_SOURCE_BUNDLE = "generateSourceBundle"; //$NON-NLS-1$
	public static final String PROPERTY_ZIP_SUFFIX = ".zip"; //$NON-NLS-1$

	public static final String PROPERTY_BIN_EXCLUDES = "bin.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$

	public static final String PROPERTY_SRC_EXCLUDES = "src.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	public static final String PROPERTY_SRC_ROOTS = "src.additionalRoots"; //$NON-NLS-1$

	public static final String PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX = "javacDefaultEncoding."; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_CUSTOM_ENCODINGS_PREFIX = "javacCustomEncodings."; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_WARNINGS_PREFIX = "javacWarnings."; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_ERRORS_PREFIX = "javacErrors."; //$NON-NLS-1$
	public static final String PROPERTY_PROJECT_SETTINGS = "javacProjectSettings"; //$NON-NLS-1$

	public static final String DEFAULT_MATCH_ALL = "*"; //$NON-NLS-1$
	public static final String DEFAULT_FINAL_SHAPE = "*"; //$NON-NLS-1$

	public static final String PROPERTY_OVERWRITE_ROOTFILES = "overwriteRootFiles"; //$NON-NLS-1$

	public static final String PROPERTY_CUSTOM_BUILD_CALLBACKS = "customBuildCallbacks"; //$NON-NLS-1$
	public static final String PROPERTY_CUSTOM_CALLBACKS_BUILDPATH = "customBuildCallbacks.buildpath"; //$NON-NLS-1$
	public static final String PROPERTY_CUSTOM_CALLBACKS_FAILONERROR = "customBuildCallbacks.failonerror"; //$NON-NLS-1$
	public static final String PROPERTY_CUSTOM_CALLBACKS_INHERITALL = "customBuildCallbacks.inheritall"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_SOURCE = "javacSource"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_TARGET = "javacTarget"; //$NON-NLS-1$
	public static final String PROPERTY_BOOT_CLASSPATH = "bootClasspath"; //$NON-NLS-1$
	public static final String PROPERTY_JRE_COMPILATION_PROFILE = "jre.compilation.profile"; //$NON-NLS-1$

	public static final String PROPERTY_SIGNIFICANT_VERSION_DIGITS = "significantVersionDigits"; //$NON-NLS-1$
	public static final String PROPERTY_GENERATED_VERSION_LENGTH = "generatedVersionLength"; //$NON-NLS-1$

	public static final String RESOLVER_DEV_MODE = "resolution.devMode"; //$NON-NLS-1$
	public static final String PROPERTY_INDIVIDUAL_SOURCE = "individualSourceBundles"; //$NON-NLS-1$
	public static final String PROPERTY_ALLOW_BINARY_CYCLES = "allowBinaryCycles"; //$NON-NLS-1$

	public static final String PROPERTY_TRANSFORMED_REPO = "transformedRepoLocation"; //$NON-NLS-1$
	public static final String PROPERTY_P2_METADATA_REPO = "p2.metadata.repo"; //$NON-NLS-1$
	public static final String PROPERTY_P2_ARTIFACT_REPO = "p2.artifact.repo"; //$NON-NLS-1$
	public static final String PROPERTY_P2_CONTEXT_REPOS = "p2.context.repos"; //$NON-NLS-1$
	public static final String PROPERTY_P2_PUBLISH_ARTIFACTS = "p2.publish.artifacts"; //$NON-NLS-1$
	public static final String PROPERTY_P2_CATEGORY_SITE = "p2.category.site"; //$NON-NLS-1$
	public static final String PROPERTY_P2_CATEGORY_DEFINITION = "p2.category.definition"; //$NON-NLS-1$
	public static final String PROPERTY_P2_CATEGORY_VERSION = "p2.category.version"; //$NON-NLS-1$
	public static final String PROPERTY_P2_CATEGORY_PREFIX = "p2.category.prefix"; //$NON-NLS-1$
	public static final String PROPERTY_P2_ROOT_NAME = "p2.root.name"; //$NON-NLS-1$
	public static final String PROPERTY_P2_ROOT_VERSION = "p2.root.version"; //$NON-NLS-1$
	public static final String PROPERTY_P2_PRODUCT_QUALIFIER = "p2.product.qualifier"; //$NON-NLS-1$
	public static final String PROPERTY_P2_FINAL_MODE_OVERRIDE = "p2.final.mode.override"; //$NON-NLS-1$
	public static final String PROPERTY_P2_FLAVOR = "p2.flavor"; //$NON-NLS-1$
	public static final String PROPERTY_P2_APPEND = "p2.append"; //$NON-NLS-1$
	public static final String PROPERTY_P2_COMPRESS = "p2.compress"; //$NON-NLS-1$
	public static final String PROPERTY_P2_METADATA_REPO_NAME = "p2.metadata.repo.name"; //$NON-NLS-1$
	public static final String PROPERTY_P2_ARTIFACT_REPO_NAME = "p2.artifact.repo.name"; //$NON-NLS-1$
	public static final String PROPERTY_P2_GATHERING = "p2.gathering"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_RAW = "p2.mirror.raw"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_FILTER = "p2.mirror.slicing.filter"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_FOLLOW_ONLY_FILTERED_REQS = "p2.mirror.slicing.followOnlyFilteredRequirements"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_FOLLOW_STRICT = "p2.mirror.slicing.followStrict"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_INCLUDE_FEATURES = "p2.mirror.slicing.includeFeatures"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_INCLUDE_NON_GREEDY = "p2.mirror.slicing.includeNonGreedy"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_INCLUDE_OPTIONAL = "p2.mirror.slicing.includeOptional"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_LATEST_VERSION_ONLY = "p2.mirror.slicing.latestVersionOnly"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_SLICING_PLATFORM_FILTER = "p2.mirror.slicing.platformFilter"; //$NON-NLS-1$
	public static final String PROPERTY_SKIP_MIRRORING = "skipMirroring"; //$NON-NLS-1$
	public static final String PROPERTY_SKIP_DIRECTOR = "skipDirector"; //$NON-NLS-1$

	public static final String PROPERTY_SOURCE_FILE_EXTENSIONS = "sourceFileExtensions"; //$NON-NLS-1$
	public static final String PROPERTY_COMPILER_ADAPTER = "compilerAdapter"; //$NON-NLS-1$
	public static final String PROPERTY_ADAPTER_USELOG = "compilerAdapter.useLog"; //$NON-NLS-1$
	public static final String PROPERTY_ADAPTER_USEARGFILE = "compilerAdapter.useArgFile"; //$NON-NLS-1$

	//Internal usage only
	public static final String PROPERTY_P2_BUILD_REPO = "p2.build.repo"; //$NON-NLS-1$
	public static final String PROPERTY_P2_GENERATION_MODE = "p2.generation.mode"; //$NON-NLS-1$
	public static final String PROPERTY_P2_PRODUCT_MOD = "p2.generation.product.mod"; //$NON-NLS-1$
	public static final String PROPERTY_P2_REPO = "p2.repo"; //$NON-NLS-1$
	public static final String PROPERTY_P2_DIRECTOR_IU = "p2.director.iu"; //$NON-NLS-1$
	public static final String PROPERTY_P2_DIRECTOR_VERSION = "p2.director.version"; //$NON-NLS-1$
	public static final String PROPERTY_P2_DIRECTOR_INSTALLPATH = "p2.director.installPath"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_METADATA_DEST = "p2.mirror.metadata.destination"; //$NON-NLS-1$
	public static final String PROPERTY_P2_MIRROR_ARTIFACT_DEST = "p2.mirror.artifact.destination"; //$NON-NLS-1$
	public static final String PROPERTY_P2_PUBLISH_PARTS = "p2.publish.parts"; //$NON-NLS-1$
	public static final String PROPERTY_P2_PUBLISHONERROR = "p2.publishonerror"; //$NON-NLS-1$
	public static final String PROPERTY_P2_GROUP_ID = "p2.group.id"; //$NON-NLS-1$

	public static final String PROPERTY_COMPILE_PROBLEM_MARKER = "compilation.problem.marker"; //$NON-NLS-1$
	public static final String PROPERTY_COMPILE_PROBLEM_MARKER_EXISTS = "compilation.problem.marker.exists"; //$NON-NLS-1$
	public static final String PROPERTY_COMPILATION_ERROR = "compilation.error.occured"; //$NON-NLS-1$
	public static final String PROPERTY_PREREQ_COMPILE_LOG = "compilation.prereq.log"; //$NON-NLS-1$

	public static final String PROPERTY_SUPPRESS_RESOLUTION_ERRORS = "suppressResolutionErrors"; //$NON-NLS-1$
	public static final String PROPERTY_ANT_VERSION = "ant.version"; //$NON-NLS-1$

	public static final String SOURCE_PLUGIN = "sourcePlugin"; //$NON-NLS-1$

	public static final String PROPERTY_PACKAGER_MODE = "packagerMode"; //$NON-NLS-1$
	public static final String PROPERTY_PACKAGER_AS_NORMALIZER = "packagerAsNormalizer"; //$NON-NLS-1$

	public static final String PROPERTY_GENERATE_ECLIPSEPRODUCT = "generateEclipseProduct"; //$NON-NLS-1$
	public static final String PROPERTY_GENERATED_FEATURE_LABEL = "generatedFeatureLabel"; //$NON-NLS-1$
	public static final String PROPERTY_FETCH_CACHE = "fetchCacheLocation"; //$NON-NLS-1$
}
