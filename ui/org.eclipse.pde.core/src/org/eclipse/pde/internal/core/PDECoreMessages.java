/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - on going enhancements and maintenance
 *     Simon Muschel <smuschel@gmx.de> - bugs 215743, 260549
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.osgi.util.NLS;

public class PDECoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.core.pderesources";//$NON-NLS-1$

	public static String BaseExportTask_pdeExport;

	public static String BuildErrorReporter_cannotFindBundle;

	public static String BuildErrorReporter_cannotFindJar;

	public static String BundleErrorReporter_badFilter;
	public static String BundleErrorReporter_bundleActivationPolicy_unsupported;
	public static String BundleErrorReporter_localization_folder_not_exist;
	public static String BundleErrorReporter_localization_properties_file_not_exist;
	public static String BundleErrorReporter_providePackageHeaderDeprecated;
	public static String BundleErrorReporter_reqExecEnv_conflict;
	public static String BundleErrorReporter_reqExecEnv_unknown;
	public static String BundleErrorReporter_unresolvedExporter;
	public static String BundleErrorReporter_unresolvedHost;
	public static String BundleErrorReporter_unsatisfiedConstraint;
	public static String BundleErrorReporter_unsupportedSingletonDirective;
	public static String BundleErrorReporter_MissingVersion;

	public static String BundleTextChangeListener_editNames_insert;

	public static String BundleTextChangeListener_editNames_modify;

	public static String BundleTextChangeListener_editNames_newLine;

	public static String BundleTextChangeListener_editNames_remove;

	public static String ExecutionEnvironmentProfileManager_0;

	public static String ExtensionPointSchemaBuilder_0;

	public static String ExtensionsErrorReporter_maxOccurrence;
	public static String ExtensionsErrorReporter_minOccurrence;
	public static String ExtensionsErrorReporter_unknownIdentifier;

	// Status text #####################################
	public static String BinaryRepositoryProvider_veto;

	public static String P2Utils_UnableToAcquireP2Service;

	public static String ProductExportOperation_0;

	public static String PropertiesTextChangeListener_editNames_delete;

	public static String PropertiesTextChangeListener_editNames_insert;

	public static String PropertiesTextChangeListener_editNames_remove;
	public static String RequiredPluginsClasspathContainer_description;
	public static String ExternalJavaSearchClasspathContainer_description;

	public static String SchemaElementReference_refElementMissing;

	public static String TargetPlatform_exceptionThrown;

	public static String TargetPlatformProvisionTask_ErrorDefinitionNotFoundAtSpecifiedLocation;
	public static String TargetPlatformProvisionTask_ErrorDefinitionNotSet;
	public static String TargetPlatformProvisionTask_ErrorDestinationNotSet;

	public static String FeatureBasedExportOperation_ProblemDuringExport;

	public static String FeatureConsistencyChecker_0;

	public static String FeatureInfo_description;
	public static String FeatureInfo_license;
	public static String FeatureInfo_copyright;
	public static String PluginObject_readOnlyChange;
	public static String FeatureObject_readOnlyChange;
	public static String SiteBuildOperation_0;

	public static String SiteObject_readOnlyChange;

	public static String BuildObject_readOnlyException;
	public static String PluginBase_librariesNotFoundException;
	public static String PluginParent_siblingsNotFoundException;
	public static String PluginBase_importsNotFoundException;
	public static String AbstractExtensions_extensionsNotFoundException;
	public static String SchemaCompositor_all;
	public static String SchemaCompositor_choice;
	public static String SchemaCompositor_group;
	public static String SchemaCompositor_sequence;

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
	public static String Builders_Feature_mismatchUnpackBundleShape;
	public static String Builders_Feature_mismatchPluginVersion;
	public static String Builders_Schema_compiling;
	public static String Builders_Schema_compilingSchemas;
	public static String Builders_Schema_removing;

	public static String Builders_Schema_noMatchingEndTag;
	public static String Builders_Schema_noMatchingStartTag;
	public static String Builders_Schema_forbiddenEndTag;
	public static String Builders_Schema_valueRequired;
	public static String Builders_Schema_valueNotRequired;
	public static String Builders_Schema_duplicateElement;
	public static String Builders_Schema_includeNotValid;
	public static String Builders_Schema_referencedElementNotFound;

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
	public static String Builders_Manifest_non_ext_element;
	public static String Builders_Manifest_deprecated_attribute;
	public static String Builders_Manifest_deprecated_element;
	public static String Builders_Manifest_internal_rootElement;
	public static String Builders_Manifest_deprecated_rootElement;
	public static String Builders_Manifest_deprecated_rootElementSuggestion;
	public static String Builders_Manifest_unused_element;
	public static String Builders_Manifest_unused_attribute;
	public static String Builders_Manifest_class;
	public static String Builders_Manifest_resource;
	public static String Builders_Manifest_deprecated_3_0;
	public static String Builders_Manifest_key_not_found;
	public static String Builders_Manifest_useless_file;
	public static String Builders_Manifest_discouragedClass;

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
	public static String BundleErrorReporter_illegalManifestVersion;
	public static String BundleErrorReporter_ClasspathNotEmpty;
	public static String BundleErrorReporter_fragmentActivator;
	public static String BundleErrorReporter_NoExist;
	public static String BundleErrorReporter_InvalidFormatInBundleVersion;
	public static String BundleErrorReporter_NotExistInProject;
	public static String BundleErrorReporter_BundleRangeInvalidInBundleVersion;
	public static String BundleErrorReporter_R4SyntaxInR3Bundle;

	public static String BundleErrorReporter_invalidVersionRangeFormat;
	public static String BundleErrorReporter_NotExistPDE;
	public static String BundleErrorReporter_HostNotExistPDE;
	public static String BundleErrorReporter_HostNeeded;
	public static String BundleErrorReporter_PackageNotExported;
	public static String BundleErrorReporter_InvalidSymbolicName;
	public static String BundleErrorReporter_invalidFilterSyntax;
	public static String BundleErrorReporter_importexport_servicesDeprecated;
	public static String BundleErrorReporter_eclipse_genericCapabilityDeprecated;
	public static String BundleErrorReporter_eclipse_genericRequireDeprecated;
	public static String BundleErrorReporter_unecessaryDependencyDueToFragmentHost;
	public static String BundleErrorReporter_missingPackagesInProject;
	public static String BundleErrorReporter_noExecutionEnvironmentSet;

	public static String BundleErrorReporter_startHeader_autoStartDeprecated;

	public static String BundleErrorReporter_exportNoJRE;

	public static String BundleErrorReporter_importNoJRE;

	public static String ManifestConsistencyChecker_0;

	public static String ManifestConsistencyChecker_buildDoesNotExist;

	public static String ManifestConsistencyChecker_builderTaskName;

	public static String ManifestConsistencyChecker_manifestDoesNotExist;

	public static String ManifestConsistencyChecker_manifestMisspelled;

	public static String BundleErrorReporter_lazyStart_unsupported;

	public static String BundleManifestSourceLocationManager_problemProcessBundleManifestHeaderAttributeMissing;

	public static String BundleValidationOperation_multiple_singletons;

	public static String ManifestConsistencyChecker_buildPropertiesSubtask;

	public static String BuildErrorReporter_missingEntry;
	public static String BuildErrorReporter_missingFolder;
	public static String BuildErrorReporter_emptyEntry;
	public static String BuildErrorReporter_binIncludesMissing;
	public static String BuildErrorReporter_buildEntryInvalidWhenNoProjectSettings;

	public static String BuildErrorReporter_buildEntryMissingProjectSpecificSettings;

	public static String BuildErrorReporter_buildEntryMissingValidPath;

	public static String BuildErrorReporter_buildEntryMissingValidRelativePath;

	public static String BuildErrorReporter_BuildEntryNotRequiredMatchesDefault;

	public static String BuildErrorReporter_sourceMissing;

	public static String BuildErrorReporter_srcIncludesSourceFolder;

	public static String BuildErrorReporter_srcIncludesSourceFolder1;
	public static String BuildErrorReporter_classpathEntryMissing;
	public static String BuildErrorReporter_missingFile;
	public static String BuildErrorReporter_entiresMustRefDirs;
	public static String BuildErrorReporter_dirsMustEndSlash;
	public static String BuildErrorReporter_classpathEntryMissing1;

	public static String BuildErrorReporter_CompilercomplianceLevel;

	public static String BuildErrorReporter_DisallowIdentifiers;

	public static String BuildErrorReporter_GeneratedClassFilesCompatibility;

	public static String BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken;

	public static String BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry;

	public static String BuildErrorReporter_SourceCompatibility;

	public static String ExportWizard_badDirectory;

	public static String FeatureExportJob_taskName;

	public static String FeatureExportOperation_0;

	public static String FeatureExportOperation_CompilationErrors;

	public static String FeatureExportOperation_runningAssemblyScript;

	public static String FeatureExportOperation_publishingMetadata;

	public static String FeatureExportOperation_runningBuildScript;

	public static String FeatureExportOperation_runningPackagerScript;

	public static String FeatureExportOperation_workspaceBuildErrorsFoundDuringExport;

	public static String XMLErrorReporter_ExternalEntityResolution;

	public static String ExtensionsErrorReporter_InvalidSchema;

	public static String PluginConverter_BundleLocationIsNull;
	public static String PluginConverter_EclipseConverterErrorCreatingBundleManifest;
	public static String PluginConverter_EclipseConverterErrorParsingPluginManifest;
	public static String PluginConverter_EclipseConverterFileNotFound;

	public static String PluginModelManager_0;
	public static String PluginModelManager_1;

	public static String PluginParser_EclipseConverterMissingAttribute;

	public static String XMLTextChangeListener_editNames_addAttribute;
	public static String XMLTextChangeListener_editNames_addContent;
	public static String XMLTextChangeListener_editNames_insertNode;
	public static String XMLTextChangeListener_editNames_modifyAttribute;
	public static String XMLTextChangeListener_editNames_modifyNode;
	public static String XMLTextChangeListener_editNames_removeAttribute;
	public static String XMLTextChangeListener_editNames_removeNode;

	public static String SearchablePluginsManager_createProjectTaskName;

	public static String SourceEntryErrorReporter_0;
	public static String SourceEntryErrorReporter_1;
	public static String SourceEntryErrorReporter_10;
	public static String SourceEntryErrorReporter_2;
	public static String SourceEntryErrorReporter_3;
	public static String SourceEntryErrorReporter_4;
	public static String SourceEntryErrorReporter_5;
	public static String SourceEntryErrorReporter_6;
	public static String SourceEntryErrorReporter_7;
	public static String SourceEntryErrorReporter_8;
	public static String SourceEntryErrorReporter_9;

	public static String SourceEntryErrorReporter_DifferentTargetLibrary;
	public static String SourceEntryErrorReporter_DupeOutputFolder;
	public static String SourceEntryErrorReporter_DupeSourceFolder;
	public static String SourceEntryErrorReporter_ExtraOutputFolder;
	public static String SourceEntryErrorReporter_InvalidOutputFolder;
	public static String SourceEntryErrorReporter_InvalidSourceFolder;
	public static String SourceEntryErrorReporter_MissingLibrary;

	public static String SourceEntryErrorReporter_MissingOutputEntry;
	public static String SourceEntryErrorReporter_MissingOutputLibForClassFolder;

	public static String SourceLocationManager_problemProcessingBundleManifestSourceHeader;

	public static String UpdateSiteBuilder_0;

	public static String VMHelper_noJreForExecEnv;

	// Target Export ########################################
	public static String ExportTargetDefinition_task;
	public static String ExportTargetDeleteOldData;
	public static String ExportTargetExportFeatures;
	public static String ExportTargetExportPlugins;

	public static String ExportTargetJob_ConfiguringDestination;
	public static String ExportTargetJob_ExportingTargetContents;

}
