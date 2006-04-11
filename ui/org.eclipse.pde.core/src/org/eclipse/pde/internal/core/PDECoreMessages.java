/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.osgi.util.NLS;

public class PDECoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.core.pderesources";//$NON-NLS-1$

	public static String BaseExportTask_pdeExport;

	public static String BundleErrorReporter_badFilter;

	public static String BundleErrorReporter_unresolvedExporter;

	public static String BundleErrorReporter_unresolvedHost;

	public static String BundleErrorReporter_unsatisfiedConstraint;

	public static String BundleErrorReporter_unsupportedSingletonDirective;

	public static String PluginModelManager_outOfSync;

	// Status text #####################################
	public static String BinaryRepositoryProvider_veto;
	public static String RequiredPluginsClasspathContainer_description;
	public static String ExternalJavaSearchClasspathContainer_description;

	public static String SchemaElementReference_refElementMissing;
	public static String TargetPlatform_exceptionThrown;

	public static String FeatureInfo_description;
	public static String FeatureInfo_license;
	public static String FeatureInfo_copyright;
	public static String PluginObject_readOnlyChange;
	public static String FeatureObject_readOnlyChange;
	public static String SiteObject_readOnlyChange;


	public static String SearchablePluginsManager_saving;
	public static String BuildObject_readOnlyException;
	public static String BundleObject_readOnlyException;
	public static String PluginBase_librariesNotFoundException;
	public static String PluginParent_siblingsNotFoundException;
	public static String PluginBase_importsNotFoundException;
	public static String AbstractExtensions_extensionsNotFoundException;
	public static String SchemaCompositor_all;
	public static String SchemaCompositor_choice;
	public static String SchemaCompositor_group;
	public static String SchemaCompositor_sequence;
	public static String SiteBuildObject_readOnlyException;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDECoreMessages.class);
	}

	// Builders and natures 
	public static String Builders_updating;
	public static String Builders_verifying;

	public static String Builders_DependencyLoopFinder_loopName;

	public static String Builders_Feature_reference;
	public static String Builders_Feature_freference;
	public static String Builders_Feature_multiplicity;
	public static String Builders_Feature_empty;
	public static String Builders_Feature_badURL;
	public static String Builders_Feature_exclusiveAttributes;
	public static String Builders_Feature_patchPlugin;
	public static String Builders_Feature_patchedVersion;
	public static String Builders_Feature_patchedMatch;
	public static String Builders_Feature_missingUnpackFalse;
	public static String Builders_Schema_compiling;
	public static String Builders_Schema_compilingSchemas;
	public static String Builders_Schema_removing;

	public static String Builders_Schema_noMatchingEndTag;
	public static String Builders_Schema_noMatchingStartTag;
	public static String Builders_Schema_forbiddenEndTag;
	public static String Builders_Schema_valueRequired;
	public static String Builders_Schema_valueNotRequired;

	public static String Builders_Manifest_missingRequired;
	public static String Builders_Manifest_dependency;
	public static String Builders_Manifest_ex_point;
	public static String Builders_Manifest_child;
	public static String Builders_Manifest_illegalRoot;
	public static String Builders_Manifest_attribute;
	public static String Builders_Manifest_att_value;
	public static String Builders_Manifest_compositeID;
	public static String Builders_Manifest_simpleID;
	public static String Builders_Manifest_non_ext_attribute;
	public static String Builders_Manifest_dont_translate_att;
	public static String Builders_Manifest_non_ext_element;
	public static String Builders_Manifest_deprecated_attribute;
	public static String Builders_Manifest_deprecated_element;
	public static String Builders_Manifest_deprecated_rootElement;
	public static String Builders_Manifest_deprecated_rootElementSuggestion;
	public static String Builders_Manifest_unused_element;
	public static String Builders_Manifest_unused_attribute;
	public static String Builders_Manifest_class;
	public static String Builders_Manifest_resource;
	public static String Builders_Manifest_deprecated_3_0;
	public static String Builders_Manifest_key_not_found;

	public static String Builders_Convert_missingAttribute;
	public static String Builders_Convert_illegalValue;

	public static String BundleErrorReporter_lineTooLong;
	public static String BundleErrorReporter_noMainSection;
	public static String BundleErrorReporter_duplicateHeader;
	public static String BundleErrorReporter_noColon;
	public static String BundleErrorReporter_noSpaceValue;
	public static String BundleErrorReporter_nameHeaderInMain;
	public static String BundleErrorReporter_noNameHeader;
	public static String BundleErrorReporter_invalidHeaderName;
	public static String BundleErrorReporter_noLineTermination;
	public static String BundleErrorReporter_parseHeader;
	public static String BundleErrorReporter_att_value;
	public static String BundleErrorReporter_dir_value;
	public static String BundleErrorReporter_illegal_value;
	public static String BundleErrorReporter_deprecated_attribute_optional;
	public static String BundleErrorReporter_deprecated_attribute_reprovide;
	public static String BundleErrorReporter_deprecated_attribute_singleton;
	public static String BundleErrorReporter_deprecated_attribute_specification_version;
	public static String BundleErrorReporter_directive_hasNoEffectWith_;
	public static String BundleErrorReporter_singletonAttrRequired;
	public static String BundleErrorReporter_singletonRequired;
	public static String BundleErrorReporter_headerMissing;
	public static String BundleErrorReporter_NoSymbolicName;
	public static String BundleErrorReporter_ClasspathNotEmpty;
	public static String BundleErrorReporter_fragmentActivator;
	public static String BundleErrorReporter_NoExist;
	public static String BundleErrorReporter_InvalidFormatInBundleVersion;
	public static String BundleErrorReporter_NotExistInProject;
	public static String BundleErrorReporter_BundleRangeInvalidInBundleVersion;
	public static String BundleErrorReporter_invalidVersionRangeFormat;
	public static String BundleErrorReporter_NotExistPDE;
	public static String BundleErrorReporter_HostNotExistPDE;
	public static String BundleErrorReporter_HostNeeded;
	public static String BundleErrorReporter_PackageNotExported;
	public static String BundleErrorReporter_UnknownDirective;
	public static String BundleErrorReporter_InvalidSymbolicName;
	public static String BundleErrorReporter_FileNotExist;
	public static String BundleErrorReporter_NativeNoProcessor;
	public static String BundleErrorReporter_NativeNoOSName;
	public static String BundleErrorReporter_NativeInvalidFilter;
	public static String BundleErrorReporter_NativeInvalidOSName;
	public static String BundleErrorReporter_NativeInvalidProcessor;
	public static String BundleErrorReporter_NativeInvalidLanguage;
	public static String BundleErrorReporter_NativeInvalidOSVersion;
	public static String BundleErrorReporter_invalidFilterSyntax;
	public static String FeatureConsistencyTrigger_JobName;

	public static String BundleErrorReporter_startHeader_autoStartDeprecated;

	public static String BundleErrorReporter_startHeader_tooManyElements;

	public static String BundleErrorReporter_startHeader_illegalValue;

	public static String BundleErrorReporter_exportNoJRE;

	public static String BundleErrorReporter_importNoJRE;

	public static String ManifestConsistencyChecker_projectCheck;

	public static String BundleErrorReporter_lazyStart_unsupported;

	public static String ManifestConsistencyChecker_buildPropertiesSubtask;

	public static String BuildErrorReporter_missingEntry;

	public static String BuildErrorReporter_missingFolder;

	public static String BuildErrorReporter_emptyEntry;

	public static String BuildErrorReporter_binIncludesMissing;

	public static String BuildErrorReporter_sourceMissing;

	public static String BuildErrorReporter_classpathEntryMissing;

	public static String BuildErrorReporter_missingFile;

	public static String BuildErrorReporter_entiresMustRefDirs;

	public static String BuildErrorReporter_dirsMustEndSlash;

	public static String BuildErrorReporter_classpathEntryMissing1;

	public static String LoadTargetOperation_mainTaskName;

	public static String LoadTargetOperation_argsTaskName;

	public static String LoadTargetOperation_envTaskName;

	public static String LoadTargetOperation_jreTaskName;

	public static String LoadTargetOperation_implicitPluginsTaskName;

	public static String LoadTargetOperation_loadPluginsTaskName;

	public static String LoadTargetOperation_reloadTaskName;

	public static String LoadTargetOperation_selectPluginsTaskName;

	public static String LoadTargetOperation_enablePluginsTaskName;

	public static String LoadTargetOperation_findPluginsTaskName;
	
	public static String ExportWizard_badDirectory;

	public static String FeatureExportJob_taskName;

}
