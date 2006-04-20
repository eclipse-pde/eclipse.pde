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
package org.eclipse.pde.internal.ui;

import org.eclipse.osgi.util.NLS;

public class PDEUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ui.pderesources";//$NON-NLS-1$

	public static String AddLibraryDialog_nospaces;

	public static String CompilersConfigurationTab_incompatEnv;

	public static String ContentSection_addDialogButtonLabel;

	public static String ImportActionGroup_binaryWithLinkedContent;

	public static String ImportActionGroup_importContributingPlugin;

	public static String LibrarySection_addDialogButton;

	public static String MainPreferencePage_group2;

	public static String MainPreferencePage_updateStale;

	public static String RemoveUnknownExecEnvironments_label;

	public static String RevertUnsupportSingletonResolution_desc;

	public static String AddLibraryDialog_emptyLibraries;

	public static String BaseWizardSelectionPage_noDesc;

	public static String ChooseClassXMLResolution_label;

	public static String ChooseManifestClassResolution_label;

	public static String CreateClassXMLResolution_label;

	public static String CreateManifestClassResolution_label;

	public static String ElementSection_missingRefElement;

	public static String ExternalizeResolution_attrib;

	public static String ExternalizeResolution_header;

	public static String ExternalizeResolution_text;

	public static String ExternalizeStringsOperation_propertiesComment;

	public static String HelpNewWizard_wiz;

	public static String OpenManifestAction_noManifest;

	public static String ProjectStructurePage_sourceName;
	public static String ProjectNamesPage_duplicateNames;
	public static String ProjectStructurePage_fsourceName;
	public static String DefaultCodeGenerationPage_initialName;
	public static String DefaultCodeGenerationPage_initialFName;

	public static String RemoveNodeXMLResolution_attrLabel;

	public static String RemoveNodeXMLResolution_label;

	public static String RuntimeInfoSection_replace;

	public static String RuntimeInfoSection_replacedialog;

	public static String SchemaIncludesSection_missingWarningMessage;

	public static String SchemaIncludesSection_missingWarningTitle;

	public static String ShowDescriptionAction_schemaNotAvail;

	public static String UpdateClasspathResolution_label;

	//
	// PDE resource strings
	// Part 2.    (TRANSLATE Part 2)
	// These are the translable properties.
	//

	public static String UpdateManager_noUndo;
	public static String UpdateManager_noRedo;
	public static String UpdateManager_undo;
	public static String UpdateManager_redo;
	public static String UpdateManager_op_add;
	public static String UpdateManager_op_remove;
	public static String UpdateManager_op_change;

	//

	public static String PluginModelManager_outOfSync;

	// Status text #####################################
	public static String ExternalModelManager_scanningProblems;
	public static String ExtensionElementDetails_setSelectedDesc;
	public static String ExternalModelManager_processingPath;
	public static String Errors_SetupError;
	public static String Errors_SetupError_NoPlatformHome;
	public static String Errors_CreationError;
	public static String Errors_CreationError_NoWizard;

	public static String MissingPDENature_title;
	public static String MissingPDENature_message;
	public static String MissingPDENature_stopWarning;
	public static String MissingPDENature_keepWarning;
	public static String MissingPDENature_openWizard;

	public static String MultiPageEditor_wrongEditor;
	public static String MultiPageEditor_defaultPage_source;
	public static String MultiPageEditor_defaultPage_overview;

	public static String CodeGenerator_missing_title;
	public static String CodeGenerator_missing_type;
	public static String CodeGenerator_missing_types;
	public static String CodeGenerator_desc_class;
	public static String CodeGenerator_desc_constructor;
	public static String CodeGenerator_desc_method;

	// Reusable Parts ################################
	public static String WizardCheckboxTablePart_selectAll;
	public static String WizardCheckboxTablePart_deselectAll;
	public static String WizardCheckboxTablePart_counter;

	// Editors #######################################

	// Source ##################################
	public static String SourcePage_title;
	public static String SourcePage_errorTitle;
	public static String SourcePage_errorMessage;

	// Outline #################################
	public static String ToggleLinkWithEditorAction_label;
	public static String ToggleLinkWithEditorAction_toolTip;
	public static String ToggleLinkWithEditorAction_description;
	public static String PDEMultiPageContentOutline_SortingAction_label;
	public static String PDEMultiPageContentOutline_SortingAction_tooltip;
	public static String PDEMultiPageContentOutline_SortingAction_description;

	// build.properties editor #####################
	public static String BuildEditor_header;
	public static String BuildEditor_BuildPage_title;
	public static String BuildEditor_Custom_plugin;
	public static String BuiltFeaturesWizard_title;
	public static String BuildEditor_Custom_fragment;
	public static String BuildEditor_Custom_feature;

	public static String BuildEditor_AddLibraryDialog_duplicate;
	public static String BuildEditor_AddLibraryDialog_label;

	public static String BuildEditor_RuntimeInfoSection_title;
	public static String BuildEditor_RuntimeInfoSection_desc;
	public static String BuildEditor_RuntimeInfoSection_duplicateLibrary;
	public static String BuildEditor_RuntimeInfoSection_duplicateFolder;
	public static String BuildEditor_RuntimeInfoSection_buildInclude;
	public static String BuildEditor_RuntimeInfoSection_addLibrary;
	public static String BuildEditor_RuntimeInfoSection_popupAdd;
	public static String BuildEditor_RuntimeInfoSection_addFolder;
	public static String BuildEditor_RuntimeInfoSection_popupFolder;

	public static String BuildEditor_SrcSection_title;
	public static String BuildEditor_SrcSection_desc;
	public static String BuildEditor_BinSection_title;
	public static String BuildEditor_BinSection_desc;

	public static String BuildEditor_ClasspathSection_add;
	public static String BuildEditor_SourceFolderSelectionDialog_button;
	public static String BuildEditor_ClasspathSection_remove;
	public static String BuildEditor_ClasspathSection_title;
	public static String BuildEditor_ClasspathSection_desc;
	public static String BuildEditor_ClasspathSection_jarsTitle;
	public static String BuildEditor_ClasspathSection_jarsDesc;

	// Feature Manifest Editor ####################
	public static String FeatureEditor_previewAction_label;
	public static String FeatureEditor_BuildAction_label;
	public static String FeatureEditor_Unresolved_title;
	public static String FeatureEditor_Unresolved_message;
	public static String FeatureEditor_FeaturePage_title;
	public static String FeatureEditor_InfoPage_title;
	public static String FeatureEditor_InfoPage_heading;
	public static String FeatureEditor_ReferencePage_title;
	public static String FeatureEditor_ReferencePage_heading;
	public static String FeatureEditor_IncludesPage_title;
	public static String FeatureEditor_IncludesPage_heading;
	public static String FeatureEditor_ReferencePage_portabilityTitle;
	public static String FeatureEditor_ReferencePage_portabilityDesc;
	public static String FeatureEditor_DependenciesPage_heading;
	public static String FeatureEditor_DependenciesPage_title;
	public static String FeatureEditor_Version_title;
	public static String FeatureEditor_Version_message;
	public static String FeatureEditor_AdvancedPage_title;
	public static String FeatureEditor_AdvancedPage_heading;

	public static String FeatureEditor_PortabilityChoicesDialog_title;
	public static String FeatureEditor_PortabilityChoicesDialog_choices;

	public static String FeatureEditor_SpecSection_title;
	public static String FeatureEditor_SpecSection_desc;
	public static String FeatureEditor_SpecSection_desc_patch;
	public static String FeatureEditor_SpecSection_id;
	public static String FeatureEditor_SpecSection_patchedId;
	public static String FeatureEditor_SpecSection_name;
	public static String FeatureEditor_SpecSection_version;
	public static String FeatureEditor_SpecSection_patchedVersion;
	public static String FeatureEditor_SpecSection_provider;
	public static String FeatureEditor_SpecSection_plugin;
	public static String FeatureEditor_SpecSection_browse;
	public static String FeatureEditor_SpecSection_updateSite;
	public static String FeatureEditor_SpecSection_updateUrlLabel;
	public static String FeatureEditor_SpecSection_updateUrl;
	public static String FeatureEditor_SpecSection_createJar;
	public static String FeatureEditor_SpecSection_synchronize;
	public static String FeatureEditor_SpecSection_badVersionTitle;
	public static String FeatureEditor_SpecSection_badVersionMessage;
	public static String FeatureEditor_SpecSection_badUrlTitle;
	public static String FeatureEditor_SpecSection_badUrlMessage;
	public static String FeatureEditor_PortabilitySection_title;
	public static String FeatureEditor_PortabilitySection_desc;
	public static String FeatureEditor_PortabilitySection_os;
	public static String FeatureEditor_PortabilitySection_ws;
	public static String FeatureEditor_PortabilitySection_nl;
	public static String FeatureEditor_PortabilitySection_arch;
	public static String FeatureEditor_PortabilitySection_edit;

	public static String FeatureEditor_IncludedFeatures_title;
	public static String FeatureEditor_IncludedFeatures_desc;
	public static String FeatureEditor_IncludedFeatures_new;

	public static String SiteEditor_IncludedFeaturesDetailsSection_title;
	public static String SiteEditor_IncludedFeaturesDetailsSection_desc;
	public static String SiteEditor_IncludedFeaturesDetailsSection_featureLabel;
	public static String SiteEditor_IncludedFeaturesDetailsSection_optional;
	public static String SiteEditor_IncludedFeaturesDetailsSection_searchLocation;
	public static String SiteEditor_IncludedFeaturesDetailsSection_root;
	public static String SiteEditor_IncludedFeaturesDetailsSection_self;
	public static String SiteEditor_IncludedFeaturesDetailsSection_both;

	public static String FeatureEditor_IncludedFeaturePortabilitySection_title;
	public static String FeatureEditor_IncludedFeaturePortabilitySection_desc;

	public static String FeatureEditor_HandlerSection_title;
	public static String FeatureEditor_HandlerSection_desc;
	public static String FeatureEditor_HandlerSection_library;
	public static String FeatureEditor_HandlerSection_handler;

	public static String FeatureEditor_InstallSection_title;
	public static String FeatureEditor_InstallSection_desc;
	public static String FeatureEditor_InstallSection_colocation_desc;
	public static String FeatureEditor_InstallSection_colocation;
	public static String FeatureEditor_InstallSection_exclusive;

	public static String FeatureEditor_InfoSection_heading;
	public static String FeatureEditor_InfoSection_desc;

	public static String FeatureEditor_InfoSection_info;
	public static String FeatureEditor_InfoSection_url;
	public static String FeatureEditor_InfoSection_text;
	public static String FeatureEditor_info_description;
	public static String FeatureEditor_info_license;
	public static String FeatureEditor_info_copyright;
	public static String FeatureEditor_info_discoveryUrls;

	public static String FeatureEditor_PluginSection_pluginTitle;
	public static String FeatureEditor_PluginSection_pluginDesc;
	public static String FeatureEditor_PluginSection_new;

	public static String FeatureEditor_PluginPortabilitySection_title;
	public static String FeatureEditor_PluginPortabilitySection_desc;

	public static String SiteEditor_PluginDetailsSection_title;
	public static String SiteEditor_PluginDetailsSection_desc;
	public static String SiteEditor_PluginDetailsSection_pluginLabel;
	public static String SiteEditor_PluginDetailsSection_downloadSize;
	public static String SiteEditor_PluginDetailsSection_installSize;
	public static String SiteEditor_PluginDetailsSection_unpack;

	public static String FeatureEditor_DataSection_title;
	public static String FeatureEditor_DataSection_desc;
	public static String FeatureEditor_DataSection_new;

	public static String SiteEditor_DataDetailsSection_title;
	public static String SiteEditor_DataDetailsSection_desc;
	public static String SiteEditor_DataDetailsSection_downloadSize;
	public static String SiteEditor_DataDetailsSection_installSize;

	public static String FeatureEditor_DataDetailsSection_title;
	public static String FeatureEditor_DataDetailsSection_desc;

	public static String FeatureEditor_modelsInUse_title;
	public static String FeatureEditor_modelsInUse_message;
	public static String FeatureExportJob_problems;
	public static String FeatureExportJob_name;
	public static String FeatureExportJob_error;

	public static String BuildSiteJob_refresh;
	
	public static String FeatureEditor_RequiresSection_title;
	public static String FeatureEditor_RequiresSection_desc;
	public static String FeatureEditor_RequiresSection_sync;
	public static String FeatureEditor_RequiresSection_compute;
	public static String FeatureEditor_RequiresSection_plugin;
	public static String FeatureEditor_RequiresSection_feature;

	public static String FeatureEditor_URLSection_desc;
	public static String FeatureEditor_URLSection_new;
	public static String FeatureEditor_URLSection_newDiscoverySite;
	public static String FeatureEditor_URLSection_newURL;
	public static String FeatureEditor_URLDetailsSection_desc;
	public static String FeatureEditor_URLDetailsSection_updateUrlLabel;
	public static String FeatureEditor_URLDetailsSection_updateUrl;
	public static String FeatureEditor_URLDetailsSection_badUrlTitle;
	public static String FeatureEditor_URLDetailsSection_badUrlMessage;


	public static String FeatureEditor_InfoPage_ContentSection_title;
	public static String FeatureEditor_InfoPage_ContentSection_text;

	public static String FeatureEditor_InfoPage_PackagingSection_title;
	public static String FeatureEditor_InfoPage_PackagingSection_text;

	public static String FeatureEditor_InfoPage_PublishingSection_title;
	public static String FeatureEditor_InfoPage_PublishingSection_text;
	public static String FeatureExportWizardPage_targetEnvironmentText;

	public static String FeatureOutlinePage_discoverUrls;

	// Plug-in Manifest Editor ########################
	public static String ManifestEditor_DependenciesForm_title;

	public static String ManifestEditor_DetailChildrenSection_title;
	public static String ManifestEditor_DetailChildrenSection_bodyText;

	public static String ManifestEditor_DetailExtensionPointSection_title;
	public static String ManifestEditor_DetailExtensionPointSection_new;
	public static String ManifestEditor_DetailExtensionPointSection_newExtensionPoint;
	public static String ManifestEditor_DetailExtensionPointSection_openSchema;

	public static String ManifestEditor_DetailExtension_title;
	public static String ManifestEditor_DetailExtension_new;
	public static String ManifestEditor_DetailExtension_edit;
	public static String ManifestEditor_DetailExtension_newExtension;
	public static String ManifestEditor_DetailExtension_collapseAll;
	public static String ManifestEditor_DetailExtension_up;
	public static String ManifestEditor_DetailExtension_down;

	public static String ManifestEditor_BodyTextSection_title;
	public static String ManifestEditor_BodyTextSection_titleFull;

	public static String ManifestEditor_ExportSection_title;
	public static String ManifestEditor_ExportSection_desc;
	public static String ManifestEditor_ExportSection_fullExport;
	public static String ManifestEditor_ExportSection_selectedExport;
	public static String ManifestEditor_ExportSection_add;
	public static String ManifestEditor_ExportSection_remove;
	public static String PackageSelectionDialog_label;
	public static String PackageSelectionDialog_title;
	public static String PackageSelectionDialog_nopackages_message;

	public static String ManifestEditor_LibraryTypeSection_title;
	public static String ManifestEditor_LibraryTypeSection_desc;
	public static String ManifestEditor_LibraryTypeSection_code;
	public static String ManifestEditor_LibraryTypeSection_resources;

	public static String ManifestEditor_ExtensionElementPR_finish;

	public static String ManifestEditor_ExtensionPointForm_title;

	public static String ManifestEditor_ExtensionPointSection_title;
	public static String ManifestEditor_ExtensionPointSection_desc;
	public static String ManifestEditor_ExtensionPointSection_fdesc;
	public static String ManifestEditor_ExtensionPointSection_more;

	public static String ManifestEditor_ExtensionSection_title;
	public static String ManifestEditor_ExtensionSection_desc;
	public static String ManifestEditor_ExtensionSection_fdesc;
	public static String ManifestEditor_ExtensionSection_more;

	public static String ManifestEditor_ExtensionForm_title;

	public static String ManifestEditor_ExtensionsPropertySheet_newAttribute;
	public static String ManifestEditor_ExtensionsPropertySheet_addAttAction_label;
	public static String ManifestEditor_ExtensionsPropertySheet_addAttAction_tooltip;
	public static String ManifestEditor_ExtensionsPropertySheet_removeAttAction_label;
	public static String ManifestEditor_ExtensionsPropertySheet_removeAttAction_tooltip;
	public static String ManifestEditor_ExtensionsPropertySheet_cloneAction_text;
	public static String ManifestEditor_ExtensionsPropertySheet_cloneAction_tooltip;
	public static String ManifestEditor_ExtensionsPropertySheet_newAttributeEntry;

	public static String ManifestEditor_TemplatePage_title;
	public static String ManifestEditor_templatePage_heading;
	public static String ManifestEditor_OverviewPage_title;
	public static String ManifestEditor_DependenciesPage_title;
	public static String ManifestEditor_RuntimePage_title;
	public static String ManifestEditor_ExtensionsPage_title;
	public static String ManifestEditor_ExtensionPointsPage_title;

	public static String ManifestEditor_ExtensionPointDetails_validate_errorStatus;
	public static String ManifestEditor_ExtensionPointDetails_schemaLocation_title;
	public static String ManifestEditor_ExtensionPointDetails_schemaLocation_desc;

	public static String ManifestEditor_ContentSection_title;
	public static String ManifestEditor_ContentSection_ftitle;

	public static String ManifestEditor_DeployingSection_title;

	public static String ManifestEditor_JarsSection_title;
	public static String ManifestEditor_JarsSection_desc;
	public static String ManifestSourcePage_dependencies;
	public static String ManifestEditor_JarsSection_new;
	public static String ManifestEditor_JarsSection_dialogTitle;
	public static String ManifestEditor_JarsSection_dialogMessage;
	public static String ManifestEditor_JarsSection_rtitle;
	public static String ManifestEditor_JarsSection_missingSource_duplicateFolder;

	public static String ManifestEditor_LibrarySection_title;
	public static String ManifestEditor_LibrarySection_fdesc;
	public static String ManifestEditor_LibrarySection_desc;
	public static String ManifestEditor_LibrarySection_new;
	public static String ManifestEditor_LibrarySection_up;
	public static String ManifestEditor_LibrarySection_down;
	public static String ManifestEditor_LibrarySection_newLibrary;
	public static String ManifestEditor_LibrarySection_newLibraryEntry;
	public static String NewManifestEditor_LibrarySection_add;
	public static String NewManifestEditor_LibrarySection_new;
	public static String NewManifestEditor_LibrarySection_remove;
	public static String ManifestEditor_RuntimeLibraryDialog_label;
	public static String ManifestEditor_RuntimeLibraryDialog_default;
	public static String ManifestEditor_RuntimeLibraryDialog_validationError;

	public static String ManifestEditor_noPlatformHome;
	public static String ManifestSourcePage_libraries;
	public static String MailTemplate_perspectiveName;
	public static String ManifestSourcePage_extensions;
	public static String MainMethodSearchEngine_search;
	public static String ManifestEditor_ManifestPropertySheet_gotoAction_label;
	public static String ManifestEditor_ManifestPropertySheet_gotoAction_tooltip;

	public static String ManifestEditor_ImportListSection_title;
	public static String ManifestEditor_ImportListSection_desc;
	public static String ManifestEditor_ImportListSection_fdesc;
	public static String ManifestEditor_ImportListSection_new;
	public static String ManifestEditor_ImportListSection_loopWarning;
	public static String ManifestEditor_ImportListSection_updateBuildPath;
	public static String ManifestEditor_ImportListSection_updatingBuildPath;

	public static String ManifestEditor_ImportStatusSection_title;
	public static String ManifestEditor_ImportStatusSection_desc;
	public static String ManifestEditor_ImportStatusSection_comboLabel;
	public static String ManifestEditor_ImportStatusSection_comboLoops;
	public static String ManifestEditor_ImportStatusSection_comboRefs;
	public static String ManifestEditor_ImportStatusSection_comboFrefs;

	public static String ManifestEditor_MatchSection_title;
	public static String MainTypeSelectionDialog_qualifier;
	public static String ManifestEditor_MatchSection_desc;
	public static String ManifestEditor_MatchSection_optional;
	public static String ManifestEditor_MatchSection_reexport;
	public static String ManifestEditor_MatchSection_version;
	public static String ManifestEditor_MatchSection_rule;
	public static String ManifestEditor_MatchSection_none;
	public static String ManifestEditor_MatchSection_perfect;
	public static String ManifestEditor_MatchSection_equivalent;
	public static String ManifestEditor_MatchSection_compatible;
	public static String ManifestEditor_MatchSection_greater;

	public static String ManifestEditor_PluginSpecSection_title;
	public static String ManifestEditor_PluginSpecSection_desc;
	public static String ManifestEditor_PluginSpecSection_fdesc;

	public static String ManifestEditor_PluginSpecSection_versionMatch;

	public static String ManifestEditor_PointUsageSection_title;
	public static String ManifestEditor_PointUsageSection_desc;
	public static String ManifestEditor_PointUsageSection_fdesc;

	public static String ManifestEditor_RequiresSection_title;
	public static String ManifestEditor_RequiresSection_desc;
	public static String ManifestEditor_RequiresSection_fdesc;
	public static String ManifestEditor_RequiresSection_more;

	public static String ResourceAttributeCellEditor_title;
	public static String ResourceAttributeCellEditor_message;

	public static String ManifestEditor_RuntimeForm_title;
	public static String MainTypeSelectionDialog_matching;

	public static String ManifestEditor_RuntimeSection_title;
	public static String ManifestEditor_RuntimeSection_fdesc;
	public static String ManifestEditor_RuntimeSection_desc;
	public static String ManifestEditor_RuntimeSection_more;
	public static String ManifestSourcePage_extensionPoints;
	public static String MainTypeSelectionDialog_chooseType;

	public static String ManifestEditor_TestingSection_title;

	public static String ManifestEditor_PropertyPage_tagName;

	// Schema Editor ##################################
	public static String SchemaEditorContributor_previewAction;

	public static String SchemaEditor_DescriptionSection_title;
	public static String SchemaEditor_DescriptionSection_desc;

	public static String SchemaEditor_DocSection_desc;
	public static String SchemaEditor_topic_overview;
	public static String SchemaEditor_topic_since;
	public static String SchemaEditor_topic_examples;
	public static String SchemaEditor_topic_implementation;
	public static String SchemaEditor_topic_api;
	public static String SchemaEditor_topic_copyright;

	public static String SchemaEditor_SpecSection_title;
	public static String SchemaEditor_SpecSection_desc;
	public static String SchemaEditor_SpecSection_plugin;
	public static String SchemaEditor_SpecSection_point;
	public static String SchemaEditor_SpecSection_name;

	public static String SchemaEditor_ElementSection_title;
	public static String SchemaEditor_ElementSection_desc;
	public static String SchemaEditor_ElementSection_newElement;
	public static String SchemaEditor_ElementSection_newAttribute;

	public static String RestrictionDialog_wtitle;
	public static String RestrictionDialog_type;
	public static String RestrictionDialog_choices;
	public static String RestrictionDialog_newChoice;
	public static String RestrictionDialog_add;
	public static String RestrictionDialog_remove;
	public static String ReviewPage_noSampleFound;

	public static String SchemaEditor_NewAttribute_label;
	public static String SchemaEditor_NewAttribute_tooltip;
	public static String SchemaEditor_NewAttribute_initialName;

	public static String SchemaEditor_NewElement_label;
	public static String SchemaEditor_NewElement_tooltip;
	public static String SchemaEditor_NewElement_initialName;

	public static String SchemaEditor_NewCompositor_tooltip;

	public static String SchemaEditor_FormPage_title;
	public static String SchemaEditor_DocPage_title;

	public static String AbstractPluginModelBase_error;
	// Launchers #######################################
	public static String MainTab_name;
	public static String WorkspaceDataBlock_workspace;
	public static String WorkspaceDataBlock_location;
	public static String WorkspaceDataBlock_clear;
	public static String WorkspaceDataBlock_askClear;
	public static String BasicLauncherTab_javaExec;
	public static String ProgramBlock_runProduct;
	public static String BasicLauncherTab_jre;
	public static String BasicLauncherTab_installedJREs;
	public static String BasicLauncherTab_jrePreferencePage;
	public static String ProgramBlock_programToRun;
	public static String BasicLauncherTab_bootstrap;
	public static String BasicLauncherTab_javaExecDefault;
	public static String ProgramBlock_runApplication;
	public static String BasicLauncherTab_noJRE;
	public static String WorkspaceDataBlock_noWorkspace;
	public static String JUnitProgramBlock_headless;

	public static String AdvancedLauncherTab_name;
	public static String AdvancedLauncherTab_workspacePlugins;
	public static String AdvancedLauncherTab_validatePlugins;
	public static String AdvancedLauncherTab_useDefault;
	public static String AdvancedLauncherTab_useFeatures;
	public static String AdvancedLauncherTab_useList;
	public static String AdvancedLauncherTab_selectAll;
	public static String AdvancedLauncherTab_deselectAll;
	public static String AdvancedFeatureExportPage_noSite;
	public static String AdvancedPluginExportPage_signJar;
	public static String AdvancedPluginExportPage_noAlias;
	public static String AdvancedLauncherTab_subset;
	public static String AdvancedLauncherTab_addNew;
	public static String AdvancedLauncherTab_defaults;
	public static String AdvancedLauncherTab_workingSet;
	public static String AdvancedLauncherTab_noProblems;
	public static String AdvancedFeatureExportPage_jnlp;
	public static String AdvancedPluginExportPage_alias;
	public static String AdvancedLauncherTab_includeFragments;
	public static String AdvancedLauncherTab_includeOptional;
	public static String AdvancedFeatureExportPage_siteURL;
	public static String AdvancedPluginExportPage_keystore;
	public static String AdvancedPluginExportPage_password;
	public static String AdvancedFeatureExportPage_noVersion;
	public static String AdvancedPluginExportPage_signButton;
	public static String AdvancedPluginExportPage_noKeystore;
	public static String AdvancedPluginExportPage_noPassword;
	public static String AdvancedLauncherTab_pluginValidation;
	public static String AdvancedFeatureExportPage_createJNLP;
	public static String AdvancedFeatureExportPage_jreVersion;
	public static String AdvancedLauncherTab_error_featureSetup;

	public static String TracingLauncherTab_name;
	public static String TracingLauncherTab_tracing;
	public static String TracingLauncherTab_plugins;
	public static String TracingLauncherTab_options;
	public static String TracingLauncherTab_selectAll;
	public static String TracinglauncherTab_deselectAll;

	public static String ConfigurationTab_name;
	public static String ConfigurationTab_clearArea;
	public static String ConfigurationTab_configAreaGroup;
	public static String ConfigurationTab_useDefaultLoc;
	public static String ConfigurationTab_configLog;
	public static String ConfigurationSection_title;
	public static String ConfigurationSection_desc;
	public static String ConfigurationSection_file;
	public static String ConfigurationTab_configBrowse;
	public static String ConfigurationSection_existing;
	public static String ConfigurationTab_configLocMessage;
	public static String ConfigurationTab_configFileGroup;
	public static String ConfigurationTab_defaultConfigIni;
	public static String ConfigurationTab_existingConfigIni;
	public static String ConfigurationTab_templateLoc;
	public static String ConfigurationSection_default;
	public static String ConfigurationSection_browse;
	public static String ConfigurationSection_message;
	public static String ConfigurationTab_templateBrowse;
	public static String ConfigurationTab_fileSelection;
	public static String ConfigurationSection_selection;
	public static String ConfigurationTab_fileDialogMessage;


	public static String WorkbenchLauncherConfigurationDelegate_noJRE;
	public static String WorkbenchLauncherConfigurationDelegate_jrePathNotFound;
	public static String WorkbenchLauncherConfigurationDelegate_badFeatureSetup;
	public static String WorkbenchLauncherConfigurationDelegate_starting;
	public static String WorkbenchLauncherConfigurationDelegate_noStartup;
	public static String WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace;
	public static String JUnitLaunchConfiguration_error_invalidproject;
	public static String JUnitLaunchConfiguration_error_notests;
	public static String JUnitLaunchConfiguration_error_notaplugin;
	public static String JUnitLaunchConfiguration_error_missingPlugin;

	public static String Launcher_error_title;
	public static String LauncherSection_desc;
	public static String LauncherSection_ico;
	public static String LauncherSection_file;
	public static String LauncherSection_icon;
	public static String LauncherSection_tiny;
	public static String Launcher_error_code13;
	public static String Launcher_error_code15;
	public static String Launcher_error_displayInLogView;
	public static String Launcher_error_displayInSystemEditor;
	public static String LauncherSection_browse;
	public static String LauncherSection_title;
	public static String LauncherSection_label;
	public static String LauncherSection_bmpImages;
	public static String LauncherSection_bmpImagesText;
	public static String LauncherSection_Low16;
	public static String LauncherSection_High16;
	public static String LauncherSection_Low24;
	public static String LauncherSection_32Low;
	public static String LauncherSection_32High;
	public static String LauncherSection_48Low;
	public static String LauncherSection_48High;
	public static String LauncherSection_linuxLabel;
	public static String LauncherSection_large;
	public static String LauncherSection_medium;
	public static String LauncherSection_small;
	public static String LauncherSection_macLabel;
	public static String OpenLogDialog_title;
	public static String OpenLogDialog_message;
	public static String OpenLogDialog_cannotDisplay;

	// Preferences ####################################
	public static String Preferences_MainPage_Description;
	public static String Preferences_MainPage_noPDENature;

	public static String Preferences_MainPage_showObjects;
	public static String Preferences_MainPage_useIds;
	public static String Preferences_MainPage_useFullNames;

	public static String Preferences_TargetPlatformPage_Description;
	public static String Preferences_TargetPlatformPage_useThis;
	public static String Preferences_TargetPlatformPage_PlatformHome;
	public static String Preferences_TargetPlatformPage_PlatformHome_Button;
	public static String Preferences_TargetPlatformPage_title;
	public static String Preferences_TargetPlatformPage_question;
	public static String ExternalPluginsBlock_reload;
	public static String ExternalPluginsBlock_selectAll;
	public static String ExternalPluginsBlock_deselectAll;
	public static String ExternalPluginsBlock_addRequired;
	public static String ExternalPluginsBlock_workingSet;

	public static String Preferences_TargetEnvironmentPage_os;
	public static String Preferences_TargetEnvironmentPage_ws;
	public static String Preferences_TargetEnvironmentPage_nl;
	public static String Preferences_TargetEnvironmentPage_arch;

	public static String SourceBlock_add;
	public static String SourceBlock_remove;
	public static String SourceBlock_target;
	public static String SourceBlock_additional;
	public static String SourceBlock_desc;

	//
	public static String PluginPathUpdater_updating;

	// Wizards #######################################
	public static String NewFragmentProjectWizard_title;
	public static String NewProjectWizard_MainPage_ftitle;
	public static String NewProductFileWizard_windowTitle;
	public static String NewProjectWizard_MainPage_fdesc;

	public static String NewProjectWizard_title;
	public static String NewProjectWizard_MainPage_title;
	public static String NewProjectWizard_MainPage_desc;

	public static String ProjectStructurePage_title;
	public static String ProjectNamesPage_emptyName;
	public static String ProjectStructurePage_ftitle;
	public static String ProjectStructurePage_desc;
	public static String ProjectStructurePage_settings;
	public static String ProjectStructurePage_simple;
	public static String ProjectStructurePage_java;
	public static String ProjectStructurePage_library;
	public static String ProjectStructurePage_source;
	public static String ProjectStructurePage_output;
	public static String ProjectStructurePage_noOutput;

	public static String ProjectNamesPage_projectName;
	public static String ProjectNamesPage_multiProjectName;
	public static String ProjectNamesPage_title;
	public static String ProjectNamesPage_desc;
	public static String ProjectNamesPage_noSampleFound;

	public static String ContentPage_title;
	public static String ContentPage_ftitle;
	public static String ContentPage_desc;
	public static String ContentPage_fdesc;
	public static String ContentPage_pGroup;
	public static String ContentPage_fGroup;
	public static String ContentPage_parentPluginGroup;
	public static String ContentPage_pClassGroup;
	public static String ContentPage_pid;
	public static String ContentPage_pversion;
	public static String ContentPage_pname;
	public static String ContentPage_pprovider;
	public static String ContentPage_fid;
	public static String ContentPage_fversion;
	public static String ContentPage_fname;
	public static String ContentPage_fprovider;
	public static String ContentPage_generate;
	public static String ContentPage_classname;
	public static String ContentPage_uicontribution;
	public static String FragmentContentPage_pid;
	public static String FragmentContentPage_pversion;
	public static String ContentPage_browse;
	public static String ContentPage_matchRule;
	public static String ContentPage_noid;
	public static String ContentPage_invalidId;
	public static String ContentPage_noversion;
	public static String ContentPage_badversion;
	public static String ContentPage_noname;
	public static String ContentPage_nopid;
	public static String ContentPage_pluginNotFound;
	public static String ContentPage_nopversion;
	public static String ContentPage_badpversion;
	public static String ContentPage_fragment;
	public static String ContentPage_plugin;
	public static String ContentPage_illegalCharactersInID;
	public static String WizardListSelectionPage_title;
	public static String WizardListSelectionPage_desc;
	public static String WizardListSelectionPage_label;
	public static String WizardListSelectionPage_templates;
	public static String OptionTemplateSection_mustBeSet;

	public static String NewLibraryPluginProjectWizard_title;
	public static String NewLibraryPluginCreationPage_title;
	public static String NewLibraryPluginCreationPage_desc;
	public static String NewLibraryPluginCreationPage_pformat;
	public static String NewLibraryPluginCreationPage_pTarget;
	public static String NewLibraryPluginCreationPage_bundle;
	public static String NewLibraryPluginCreationPage_jarred;
	public static String NewLibraryPluginCreationPage_pGroup;
	public static String NewLibraryPluginCreationPage_pid;
	public static String NewLibraryPluginCreationPage_pversion;
	public static String NewLibraryPluginCreationPage_pname;
	public static String NewLibraryPluginCreationPage_pprovider;
	public static String NewLibraryPluginCreationPage_plugin;
	public static String NewLibraryPluginCreationPage_noid;
	public static String NewLibraryPluginCreationPage_invalidId;
	public static String NewLibraryPluginCreationPage_noversion;
	public static String NewLibraryPluginCreationPage_noname;
	public static String LibraryPluginJarsPage_title;
	public static String LibraryPluginJarsPage_desc;
	public static String LibraryPluginJarsPage_label;
	public static String LibraryPluginJarsPage_add;
	public static String LibraryPluginJarsPage_addExternal;
	public static String LibraryPluginJarsPage_remove;
	public static String LibraryPluginJarsPage_SelectionDialog_title;
	public static String LibraryPluginJarsPage_SelectionDialog_message;

	public static String NewProjectCreationOperation_creating;
	public static String NewProjectCreationOperation_project;
	public static String NewProjectCreationOperation_setClasspath;
	public static String NewProjectCreationOperation_manifestFile;
	public static String NewProjectCreationOperation_buildPropertiesFile;
	public static String NewProjectCreationOperation_copyingJar;
	public static String NewProjectCreationOperation_errorImportingJar;

	public static String AbstractTemplateSection_generating;

	public static String BuildAction_Validate;
	public static String BuildAction_Generate;
	public static String BuildSiteJob_message;
	public static String BuildAction_Update;
	public static String BuildPage_custom;
	public static String BuildPage_name;
	public static String BuildAction_ErrorDialog_Title;
	public static String BuildAction_ErrorDialog_Message;

	public static String NewFeatureWizard_wtitle;
	public static String NewFeatureWizard_MainPage_desc;
	public static String NewFeatureWizard_SpecPage_title;
	public static String NewFeatureWizard_SpecPage_desc;
	public static String NewFeatureWizard_SpecPage_id;
	public static String NewFeatureWizard_SpecPage_name;
	public static String NewFeatureWizard_SpecPage_version;
	public static String NewFeatureWizard_SpecPage_provider;
	public static String NewFeatureWizard_SpecPage_description;
	public static String NewFeatureWizard_SpecPage_versionFormat;
	public static String NewFeatureWizard_sampleCopyrightURL;
	public static String NewFeatureWizard_sampleCopyrightDesc;
	public static String NewFeatureWizard_sampleLicenseURL;
	public static String NewFeatureWizard_sampleLicenseDesc;
	public static String NewFeatureWizard_sampleDescriptionURL;
	public static String NewFeatureWizard_sampleDescriptionDesc;
	public static String NewSiteProjectCreationPage_webTitle;
	public static String NewFeatureWizard_SpecPage_missing;
	public static String NewFeatureWizard_SpecPage_pmissing;
	public static String NewFeatureWizard_SpecPage_invalidId;

	public static String NewFeatureWizard_PlugPage_title;
	public static String NewFeatureWizard_PlugPage_desc;

	public static String NewFeatureWizard_creatingProject;
	public static String NewFeatureWizard_creatingFolders;
	public static String NewFeatureWizard_creatingManifest;
	public static String NewFeatureWizard_overwriteFeature;
	public static String NewFeatureWizard_SpecPage_library;
	public static String NewFeatureWizard_SpecPage_customProject;
	public static String NewFeatureWizard_SpecPage_patch_customProject;
	public static String NewFeatureWizard_SpecPage_patchProperties;

	public static String FeatureDetailsSection_title;
	public static String FeatureDetailsSection_desc;
	public static String FeatureDetailsSection_url;
	public static String FeatureDetailsSection_patch;
	public static String FeatureDetailsSection_requiredURL;
	public static String FeatureDetailsSection_requiredURL_title;

	public static String SiteEditor_PortabilitySection_title;
	public static String SiteEditor_PortabilitySection_desc;
	public static String SiteEditor_PortabilitySection_os;
	public static String SiteEditor_PortabilitySection_ws;
	public static String SiteEditor_PortabilitySection_nl;
	public static String SiteEditor_PortabilitySection_arch;
	public static String SiteEditor_PortabilitySection_edit;
	public static String SiteEditor_PortabilityChoicesDialog_title;

	public static String FeaturePatch_wtitle;
	public static String FeaturePatch_MainPage_desc;
	public static String PatchSpec_title;
	public static String NewFeaturePatch_SpecPage_id;
	public static String NewFeaturePatch_SpecPage_name;
	public static String NewFeaturePatch_SpecPage_provider;
	public static String NewFeaturePatch_SpecPage_notFound;
	public static String FeatureSelectionDialog_title;
	public static String FeatureSelectionDialog_message;
	public static String PatchPlugins_title;
	public static String PatchPlugins_desc;

	public static String VersionSyncWizard_wtitle;
	public static String VersionSyncWizard_title;
	public static String VersionSyncWizard_desc;
	public static String VersionSyncWizard_group;
	public static String VersionSyncWizard_useComponent;
	public static String VersionSyncWizard_usePlugins;
	public static String VersionSyncWizard_usePluginsAtBuild;
	public static String VersionSyncWizard_synchronizing;

	public static String JavaAttributeWizard_wtitle;

	public static String ExtensionsPage_collapseAll;
	public static String ExtensionPointDetails_title;
	public static String ExtensionPointDetails_desc;
	public static String ExtensionPointDetails_id;
	public static String ExtensionPointDetails_name;
	public static String ExtensionPointsPage_title;
	public static String ExtensionPointsPage_tabName;
	public static String ExtensionPointDetails_schema;
	public static String ExtensionPointDetails_schemaLinks;
	public static String ExtensionPointsSection_message1;
	public static String ExtensionPointDetails_browse;
	public static String ExtensionPointsSection_title;
	public static String ExtensionPointDetails_noSchemaLinks;
	public static String ExtensionElementDetails_desc;
	public static String ExtensionDetails_noPoint_title;
	public static String ExtensionDetails_extensionPointLinks;
	public static String ExtensionElementDetails_setDesc;
	public static String ExtensionEditorSelectionPage_title;
	public static String ExtensionEditorSelectionPage_message;
	public static String ExtensionEditorSelectionPage_desc;
	public static String ShowDescriptionAction_noPoint_desc;
	public static String ExtensionElementDetails_title;

	public static String BaseExtensionPoint_pluginId;
	public static String BaseExtensionPoint_id;
	public static String BaseExtensionPoint_name;
	public static String BaseExtensionPoint_missingId;
	public static String BaseExtensionPoint_noPlugin_missingId;
	public static String BaseExtensionPoint_malformedId;
	public static String BaseExtensionPoint_schema;
	public static String BaseExtensionPoint_schemaLocation;
	public static String BaseExtensionPoint_edit;
	public static String BaseExtensionPoint_shared;
	public static String BaseExtensionPoint_sections_overview;
	public static String BaseExtensionPoint_sections_since;
	public static String BaseExtensionPoint_sections_usage;
	public static String BaseExtensionPoint_sections_api;
	public static String BaseExtensionPoint_sections_supplied;
	public static String BaseExportWizard_confirmReplace_desc;
	public static String BaseExportWizardPage_packageJARs;
	public static String BaseExportWizard_wtitle;
	public static String BaseExportWizardPage_fPackageJARs;
	public static String BaseFeatureSpecPage_patchGroup_title;
	public static String BaseFeatureSpecPage_featurePropertiesGroup_title;
	public static String BaseFeatureSpecPage_browse;
	public static String BaseExtensionPoint_sections_copyright;
	public static String BaseExportWizard_confirmReplace_title;
	public static String BaseExtensionPoint_generating;
	public static String GenericExtensionWizard_wtitle;
	public static String GeneralInfoSection_version;
	public static String GeneralInfoSection_provider;
	public static String GeneralInfoSection_pluginId;
	public static String GeneralInfoSection_pluginVersion;
	public static String GeneralInfoSection_hostMinVersionRange;
	public static String GeneralInfoSection_hostMaxVersionRange;

	public static String NewWizard_wtitle;
	public static String NewExtensionWizard_wtitle;
	public static String NewExtensionWizard_title;
	public static String NewElementAction_generic;
	public static String NewExtensionWizard_desc;
	public static String NewExtensionWizard_statusMessage;
	public static String NewExtensionWizard_PointSelectionPage_title;
	public static String NewExtensionWizard_PointSelectionPage_desc;
	public static String NewExtensionRegistryReader_missingProperty;
	public static String NewExtensionTemplateWizard_generating;
	public static String NewExtensionWizard_PointSelectionPage_filterCheck;
	public static String NewExtensionWizard_PointSelectionPage_descButton;
	public static String NewExtensionWizard_PointSelectionPage_dependencyTitle;
	public static String NewExtensionWizard_PointSelectionPage_dependencyMessage;
	public static String NewExtensionWizard_PointSelectionPage_availExtPoints_label;
	public static String NewExtensionWizard_PointSelectionPage_contributedTemplates_title;
	public static String NewExtensionWizard_PointSelectionPage_contributedTemplates_label;
	public static String NewExtensionWizard_PointSelectionPage_showDetails;
	public static String NewExtensionWizard_PointSelectionPage_templateDescription;
	public static String NewExtensionWizard_PointSelectionPage_pluginDescription;
	public static String NewExtensionWizard_PointSelectionPage_extPointDescription;

	public static String ExtensionEditorWizard_wtitle;

	public static String NewExtensionPointWizard_wtitle;
	public static String NewExtensionPointWizard_title;
	public static String NewExtensionPointWizard_desc;

	public static String NewSchemaFileWizard_wtitle;
	public static String NewSchemaFileWizard_title;
	public static String NewSchemaFileWizard_desc;

	public static String ConvertedProjectWizard_title;
	public static String ConvertedProjectWizard_desc;
	public static String ConvertedProjectWizard_projectList;
	public static String ConvertedProjectWizard_converting;
	public static String ConvertedProjectWizard_updating;
	public static String ConvertProjectsAction_find;
	public static String ConvertProjectsAction_none;


	// Supplied templates

	public static String PluginCodeGeneratorWizard_title;
	public static String PluginContentPage_invalidAppID;

	public static String EditorNewWizard_wtitle;
	public static String EditorTemplate_title;
	public static String EditorTemplate_desc;
	public static String EditorTemplate_packageName;
	public static String EditorTemplate_editorClass;
	public static String EditorTemplate_editorName;
	public static String EditorTemplate_fileExtension;
	public static String EditorTemplate_defaultEditorName;

	public static String HelloWorldNewWizard_wtitle;
	public static String HelloWorldTemplate_title;
	public static String HelloWorldTemplate_desc;
	public static String HelloRCPNewWizard_title;
	public static String HelloRCPTemplate_title;
	public static String HelloRCPTemplate_desc;
	public static String HelloRCPTemplate_appId;
	public static String HelloRCPTemplate_appClass;
	public static String HelloNonUIRCPNewWizard_title;
	public static String HelloNonUIRCPTemplate_title;
	public static String HelloNonUIRCPTemplate_desc;
	public static String HelloNonUIRCPTemplate_appClass;
	public static String HelloNonUIRCPTemplate_messageText;
	public static String HelloNonUIRCPTemplate_defaultMessage;
	public static String HelloWorldTemplate_packageName;
	public static String HelloWorldTemplate_className;
	public static String HelloRCPTemplate_windowTitle;
	public static String HelloWorldTemplate_messageText;
	public static String HelloWorldTemplate_defaultMessage;
	public static String HelloWorldTemplate_sampleActionSet;
	public static String HelloWorldTemplate_sampleMenu;
	public static String HelloWorldTemplate_sampleAction;

	public static String IntroNewWizard_wtitle;
	public static String IntroTemplate_title;
	public static String IntroTemplate_desc;
	public static String IntroTemplate_productID;
	public static String IntroTemplate_productName;
	public static String IntroTemplate_application;
    public static String IntroTemplate_generate;
    public static String IntroTemplate_generateDynamicContent;
    public static String IntroTemplate_generateStaticContent;

	public static String ViewRCPNewWizard_title;
	public static String ViewRCPTemplate_title;
	public static String ViewRCPTemplate_desc;
	public static String ViewRCPTemplate_appId;
	public static String ViewRCPTemplate_appClass;
	public static String ViewRCPTemplate_packageName;
	public static String ViewRCPTemplate_className;
	public static String ViewRCPTemplate_windowTitle;

	public static String MultiPageEditorNewWizard_wtitle;
	public static String MultiPageEditorTemplate_title;
	public static String MultiPageEditorTemplate_desc;
	public static String MultiPageEditorTemplate_packageName;
	public static String MultiPageEditorTemplate_className;
	public static String MultiPageEditorTemplate_contributor;
	public static String MultiPageEditorTemplate_editorName;
	public static String MultiPageEditorTemplate_defaultEditorName;
	public static String MultiPageEditorTemplate_extensions;

	public static String NewWizardTemplate_title;
	public static String NewWizardTemplate_desc;
	public static String NewWizardTemplate_packageName;
	public static String NewWizardTemplate_categoryId;
	public static String NewWizardTemplate_categoryName;
	public static String NewWizardTemplate_className;
	public static String NewWizardTemplate_pageClassName;
	public static String NewWizardTemplate_wizardName;
	public static String NewWizardTemplate_defaultName;
	public static String NewWizardTemplate_extension;
	public static String NewWizardTemplate_fileName;
	public static String NewProductFileWizard_title;

	public static String PopupMenuNewWizard_wtitle;
	public static String PopupMenuTemplate_title;
	public static String PointSelectionPage_tab1;
	public static String PointSelectionPage_tab2;
	public static String PopupMenuTemplate_desc;
	public static String PopupMenuTemplate_targetClass;
	public static String PopupMenuTemplate_nameFilter;
	public static String PopupMenuTemplate_newAction;
	public static String PopupMenuTemplate_submenuName;
	public static String PopupMenuTemplate_defaultSubmenuName;
	public static String PopupMenuTemplate_actionLabel;
	public static String PopupMenuTemplate_defaultActionName;
	public static String PopupMenuTemplate_packageName;
	public static String PopupMenuTemplate_actionClass;
	public static String PointSelectionPage_categories;
	public static String PopupMenuTemplate_enabledFor;
	public static String PopupMenuTemplate_singleSelection;
	public static String PopupMenuTemplate_multipleSelection;

	public static String PreferencePageTemplate_title;
	public static String PreferencePageTemplate_desc;
	public static String PreferencePageTemplate_packageName;
	public static String PreferencePageTemplate_className;
	public static String PreferencePageTemplate_pageName;
	public static String PreferencePageTemplate_defaultPageName;
	public static String Product_PluginSection_includeFragments;
	public static String ProductDefinitonWizardPage_productGroup;
	public static String ProductDefinitonWizardPage_productExists;
	public static String ProductDefinitonWizardPage_application;
	public static String ProductDefinitonWizardPage_noProductID;
	public static String ProductFileWizadPage_existingProduct;
	public static String ProductFileWizadPage_existingLaunchConfig;
	public static String ProductDefinitonWizardPage_productDefinition;
	public static String ProductDefinitonWizardPage_applicationGroup;
	public static String ProductDefinitonWizardPage_notInWorkspace;
	public static String ProductDefinitionOperation_readOnly;
	public static String ProductDefinitionOperation_malformed;

	public static String PropertyPageNewWizard_wtitle;
	public static String PropertyPageTemplate_title;
	public static String PropertyPageTemplate_desc;
	public static String PropertyPageTemplate_packageName;
	public static String PropertyPageTemplate_pageClass;
	public static String PropertyPageTemplate_pageName;
	public static String PropertyPageTemplate_defaultPageName;
	public static String PropertyPageTemplate_targetClass;
	public static String PropertyPageTemplate_nameFilter;

	public static String TemplateSelectionPage_title;
	public static String TemplateSelectionPage_desc;
	public static String TemplateSelectionPage_table;
	public static String TemplateSelectionPage_column_name;
	public static String TemplateSelectionPage_column_point;

	public static String ViewNewWizard_wtitle;
	public static String ViewTemplate_title0;
	public static String ViewTemplate_desc0;
	public static String ViewTemplate_title1;
	public static String ViewTemplate_desc1;
	public static String ViewTemplate_packageName;
	public static String ViewTemplate_className;
	public static String ViewTemplate_name;
	public static String ViewTemplate_defaultName;
	public static String ViewTemplate_categoryId;
	public static String ViewTemplate_categoryName;
	public static String ViewTemplate_defaultCategoryName;
	public static String ViewTemplate_select;
	public static String ViewTemplate_table;
	public static String ViewTemplate_tree;
	public static String ViewTemplate_doubleClick;
	public static String ViewTemplate_popup;
	public static String ViewTemplate_toolbar;
	public static String ViewTemplate_pulldown;
	public static String ViewTemplate_sorting;
	public static String ViewTemplate_filtering;
	public static String ViewTemplate_addToPerspective;

	public static String HelpTemplate_title;
	public static String HelpTemplate_desc;
	public static String HelpTemplate_tocLabel;
	public static String HelpTemplate_isPrimary;
	public static String HelpTemplate_generateTest;
	public static String HelpTemplate_gettingStarted;
	public static String HelpTemplate_concepts;
	public static String HelpTemplate_tasks;
	public static String HelpTemplate_reference;
	public static String HelpTemplate_samples;

	public static String BuilderNewWizard_wtitle;
	public static String BuilderTemplate_title;
	public static String BuilderTemplate_desc;
	public static String BuilderTemplate_builderClass;
	public static String BuilderTemplate_builderId;
	public static String BuilderTemplate_builderName;
	public static String BuilderTemplate_natureClass;
	public static String BuilderTemplate_natureId;
	public static String BuilderTemplate_natureName;
	public static String BuilderTemplate_packageLabel;
	public static String BuilderTemplate_actionLabel;
	public static String BuilderTemplate_defaultBuilderName;
	public static String BuilderTemplate_defaultNatureName;
	public static String BuilderTemplate_markerName;
	public static String BuilderTemplate_generateAction;
	
	public static String DecoratorTemplate_title;
	public static String DecoratorTemplate_desc;
	public static String DecoratorTemplate_packageName;
	public static String DecoratorTemplate_placement;
	public static String DecoratorTemplate_resourceLabel;
	public static String DecoratorTemplate_readOnlyLabel;
	public static String DecoratorTemplate_decorateProject;
	public static String DecoratorTemplate_decorateReadOnly;
	public static String DecoratorTemplate_placementChoices;
	public static String DecoratorTemplate_decoratorClass;
	public static String DecoratorTemplate_decoratorClassName;
	
	public static String ImportWizardTemplate_title;
	public static String ImportWizardTemplate_desc;
	public static String ImportWizardTemplate_packageName;
	public static String ImportWizardTemplate_wizardClass;
	public static String ImportWizardTemplate_wizardClassName;
	public static String ImportWizardTemplate_importWizardCategory;
	public static String ImportWizardTemplate_importWizardCategoryName;
	public static String ImportWizardTemplate_pageClass;
	public static String ImportWizardTemplate_pageClassName;
	public static String ImportWizardTemplate_wizardName;
	public static String ImportWizardTemplate_wizardDefaultName;
	public static String ImportWizardTemplate_filterChoices;
	public static String ImportWizardTemplate_filters;
	public static String ImportWizardTemplate_wizardDescription;

	
	//

	public static String PluginSelectionDialog_title;
	public static String PluginSelectionDialog_message;
	public static String PluginImportOperation_linking;
	public static String PluginContentPage_appQuestion;
	public static String PluginSelectionDialog_workspacePlugins;
	public static String PluginValidationOperation_invalidSingular;
	public static String PluginValidationOperation_invalidPlural;
	public static String PluginSelectionDialog_externalPlugins;

	public static String ImportWizard_title;
	public static String ImportWizard_noToAll;
	public static String ImportWizard_messages_title;
	public static String ImportWizard_messages_exists;
	public static String ImportWizard_FirstPage_title;
	public static String ImportWizard_FirstPage_warning;
	public static String ImportWizard_FirstPage_desc;
	public static String ImportWizard_FirstPage_importGroup;
	public static String ImportWizard_FirstPage_importPrereqs;
	public static String ImportWizard_FirstPage_scanAll;
	public static String ImportWizard_FirstPage_importAs;
	public static String ImportWizard_FirstPage_binary;
	public static String ImportWizard_FirstPage_binaryLinks;
	public static String ImportWizard_FirstPage_source;
	public static String ImportWizard_FirstPage_codeLocations;
	public static String ImportWizard_FirstPage_source_label;
	public static String ImportWizard_FirstPage_variables;
	public static String ImportWizard_FirstPage_env;
	public static String ImportWizard_FirstPage_importFrom;
	public static String ImportWizard_FirstPage_target;
	public static String ImportWizard_FirstPage_goToTarget;
	public static String ImportWizard_FirstPage_otherFolder;
	public static String ImportWizard_FirstPage_browse;
	public static String ImportWizard_SecondPage_addFragments;

	public static String ImportWizard_messages_folder_title;
	public static String ImportWizard_messages_folder_message;

	public static String ImportWizard_errors_locationMissing;
	public static String ImportWizard_errors_buildFolderInvalid;
	public static String ImportWizard_errors_buildFolderMissing;

	public static String ImportWizard_expressPage_title;
	public static String ImportWizard_expressPage_desc;
	public static String ImportWizard_expressPage_nonBinary;
	public static String ImportWizard_expressPage_total;

	public static String ImportWizard_DetailedPage_title;
	public static String ImportWizard_DetailedPage_desc;
	public static String ImportWizard_DetailedPage_availableList;
	public static String ImportWizard_DetailedPage_importList;
	public static String ImportWizard_DetailedPage_add;
	public static String ImportWizard_DetailedPage_addAll;
	public static String ImportWizard_DetailedPage_remove;
	public static String ImportWizard_DetailedPage_removeAll;
	public static String ImportWizard_DetailedPage_swap;
	public static String ImportWizard_DetailedPage_existing;
	public static String ImportWizard_DetailedPage_existingUnshared;
	public static String ImportWizard_DetailedPage_addRequired;
	public static String ImportWizard_DetailedPage_count;
	public static String ImportWizard_DetailedPage_locate;
	public static String ImportWizard_DetailedPage_search;

	public static String ImportWizard_operation_creating;
	public static String ImportWizard_operation_multiProblem;
	public static String ImportWizard_operation_problem;
	public static String ImportWizard_operation_creating2;
	public static String ImportWizard_operation_extracting;
	public static String ImportWizard_operation_copyingSource;

	public static String FeatureImportWizard_FirstPage_title;
	public static String FeatureImportWizard_FirstPage_desc;
	public static String FeatureImportWizard_FirstPage_runtimeLocation;
	public static String FeatureImportWizard_FirstPage_otherLocation;
	public static String FeatureImportWizard_FirstPage_otherFolder;
	public static String FeatureImportWizard_FirstPage_binaryImport;
	public static String FeatureImportWizard_FirstPage_browse;
	public static String FeatureImportWizard_messages_folder_title;
	public static String FeatureImportWizard_messages_folder_message;
	public static String FeatureImportWizard_errors_locationMissing;
	public static String FeatureImportWizard_errors_buildFolderInvalid;
	public static String FeatureImportWizard_errors_buildFolderMissing;

	public static String FeatureImportWizard_messages_loadingRuntime;
	public static String FeatureImportWizard_messages_updating;
	public static String FeatureImportWizard_title;
	public static String FeatureMatchSection_patch;
	public static String FeatureSection_removeAll;
	public static String FeatureImportWizard_noToAll;
	public static String FeatureImportWizard_messages_noFeatures;
	public static String FeatureImportWizard_messages_title;
	public static String FeatureImportWizard_messages_exists;

	public static String FeatureImportWizard_operation_creating;
	public static String FeatureImportWizard_operation_multiProblem;
	public static String FeatureImportWizard_operation_problem;
	public static String FeatureImportWizard_operation_creating2;

	public static String UpdateBuildpathWizard_wtitle;
	public static String UpdateBuildpathWizard_title;
	public static String UpdateBuildpathWizard_desc;
	public static String UpdateBuildpathWizard_availablePlugins;

	// Actions ########################################
	public static String EditorActions_save;
	public static String EditorActions_cut;
	public static String EditorActions_copy;
	public static String EditorActions_paste;
	public static String EditorActions_revert;
	public static String Actions_properties_label;
	public static String Actions_open_label;
	public static String Actions_refresh_label;
	public static String Actions_delete_label;
	public static String Actions_synchronizeVersions_label;
	public static String Actions_edit_label;

	public static String Actions_delete_flabel;

	public static String Menus_new_label;
	public static String Menus_edit_label;
	public static String Menus_goTo_label;

	public static String Actions_Feature_OpenProjectWizardAction;
	public static String Actions_Site_OpenProjectWizardAction;

	public static String UpdateClasspathJob_error_title;
	public static String UpdateClasspathJob_error_message;
	public static String UpdateClasspathJob_task;
	public static String UpdateClasspathJob_title;

	public static String RuntimeWorkbenchShortcut_launchFailed;
	public static String RuntimeWorkbenchShortcut_title;
	public static String RuntimeWorkbenchShortcut_select_debug;
	public static String RuntimeWorkbenchShortcut_select_run;
	public static String RuntimeWorkbenchShortcut_name;
	public static String RuntimeInfoSection_addEntry;
	public static String RuntimeInfoSection_rename;
	public static String BaseExtensionPointMainPage_noContainer;
	public static String BaseExtensionPointMainPage_pluginId_tooltip;
	public static String BaseExtensionPointMainPage_schemaLocation_tooltip;
	public static String BaseExtensionPointMainPage_pluginBrowse;
	public static String BaseExtensionPointMainPage_findBrowse;
	public static String BaseProductCreationOperation_taskName;
	public static String BaseExtensionPointMainPage_since;
	public static String BaseExtensionPointMainPage_schemaLocation_title;
	public static String BaseExtensionPointMainPage_schemaLocation_desc;
	public static String SourcePreferencePage_new_title;
	public static String SourcePreferencePage_column_name;
	public static String SourcePreferencePage_column_path;
	public static String SourcePreferencePage_dialogMessage;
	public static String EditorPreferencePage_colorSettings;
	public static String EditorPreferencePage_text;
	public static String EditorPreferencePage_proc;
	public static String EditorPreferencePage_string;
	public static String EditorPreferencePage_tag;
	public static String EditorPreferencePage_comment;
	public static String PluginContentPage_yes;

	//Search Page###############################
	public static String SearchPage_searchString;
	public static String SearchPage_caseSensitive;
	public static String SearchPage_searchFor;
	public static String SearchResult_matches;
	public static String SearchPage_limitTo;
	public static String SearchPage_externalScope;
	public static String SearchPage_plugin;
	public static String SearchPage_fragment;
	public static String SelectionPage_title;
	public static String SearchPage_extPt;
	public static String SearchPage_declarations;
	public static String SearchPage_references;
	public static String SearchPage_allOccurrences;
	public static String SearchPage_all;
	public static String SearchPage_enabledOnly;
	public static String SearchPage_none;
	public static String Search_singleMatch;
	public static String SearchResult_match;
	public static String SelectionPage_desc;
	public static String Search_multipleMatches;
	public static String SearchAction_references;
	public static String SearchAction_Declaration;
	public static String ShowDescriptionAction_label;
	public static String ShowDescriptionAction_title;
	public static String ShowSampleAction_msgTitle;
	public static String DependencyExtent_singular;
	public static String DependencyExtent_plural;
	public static String DetailsBlock_horizontal;
	public static String DependencyExtent_found;
	public static String DependencyExtent_searching;
	public static String DependencyExtentAction_label;
	public static String DependencyExtent_references;
	public static String DependencyExtentQuery_label;
	public static String UnusedDependencies_title;
	public static String UnusedDependencies_action;
	public static String UnusedDependencies_notFound;
	public static String UnusedDependenciesJob_viewResults;
	public static String UnusedDependenciesAction_jobName;
	public static String UnusedDependencies_found;
	public static String UnusedDependencies_remove;
	public static String UnusedDependencies_analyze;
	public static String UnusedDependencies_unused;
	public static String RemoveExportPkgs_label;
	public static String RemoveExportPkgs_description;
	public static String RemoveExportPkgs_jobName;

	public static String DependenciesView_open;
	public static String DependenciesView_ShowCalleesAction_label;
	public static String DependenciesView_ShowCalleesAction_description;
	public static String DependenciesView_ShowCalleesAction_tooltip;
	public static String DependenciesView_ShowCallersAction_label;
	public static String DependenciesView_ShowCallersAction_description;
	public static String DependenciesView_ShowCallersAction_tooltip;
	public static String DependenciesView_ShowListAction_label;
	public static String DependenciesView_ShowListAction_description;
	public static String DependenciesView_ShowListAction_tooltip;
	public static String DependenciesView_ShowTreeAction_label;
	public static String DependenciesView_ShowTreeAction_description;
	public static String DependenciesView_ShowTreeAction_tooltip;
	public static String DependenciesView_ShowLoopsAction_label;
	public static String DependenciesView_ShowLoopsAction_description;
	public static String DependenciesView_ShowLoopsAction_tooltip;
	public static String DependenciesView_callees_tree_title;
	public static String DependenciesView_callees_list_title;
	public static String DependenciesView_callers_tree_title;
	public static String DependenciesView_callers_list_title;
	public static String DependenciesView_cycles_title;
	public static String DependenciesViewTreePage_CollapseAllAction_label;
	public static String DependenciesViewTreePage_CollapseAllAction_description;
	public static String DependenciesViewTreePage_CollapseAllAction_tooltip;
	public static String DependenciesPage_title;
	public static String DependenciesPage_tabName;
	public static String DetailsBlock_vertical;
	public static String DependenciesViewPage_focusOn;
	public static String DependenciesViewPage_focusOnSelection;
	public static String DependencyAnalysisSection_title;
	public static String DependencyAnalysisSection_loops;
	public static String DependencyAnalysisSection_noCycles;
	public static String DependencyAnalysisSection_references;
	public static String DependencyAnalysisSection_dialogtitle;
	public static String DependencyExtentOperation_searching;
	public static String DependencyExtentOperation_inspecting;
	public static String DependencyExtentSearchResult_dependency;
	public static String DependencyExtentSearchResult_dependencies;
	public static String DependencyAnalysisSection_fragment_editable;
	public static String DependencyAnalysisSection_noReferencesFound;
	public static String DependencyAnalysisSection_fragment_notEditable;
	public static String DependencyAnalysisSection_plugin_editable;
	public static String DependencyAnalysisSection_plugin_notEditable;

	public static String HistoryAction_description;
	public static String HistoryAction_tooltip;
	public static String HistoryDropDownAction_tooltip;
	public static String HistoryListAction_label;
	public static String HistoryListDialog_label;
	public static String HistoryListDialog_title;
	public static String HistoryListDialog_remove_button;

	public static String PluginsView_open;
	public static String PluginsView_openDependencies;
	public static String PluginsView_asBinaryProject;
	public static String PluginsView_asSourceProject;
	public static String PluginsView_showDisabled;
	public static String PluginsView_showEnabled;
	public static String PluginsView_showWorkspace;
	public static String PluginsView_textEditor;
	public static String PluginWorkingSet_title;
	public static String PluginsView_systemEditor;
	public static String PluginsView_manifestEditor;
	public static String PluginContentPage_rcpGroup;
	public static String PluginContentPage_appClass;
	public static String PluginWorkingSet_emptyName;
	public static String PluginWorkingSet_nameInUse;
	public static String PluginsView_schemaEditor;
	public static String PluginsView_copy;
	public static String PluginsView_dependentPlugins;
	public static String PluginsView_pluginsInJavaSearch;
	public static String PluginsView_addToJavaSearch;
	public static String PluginsView_removeFromJavaSearch;
	public static String PluginsView_showInNavigator;
	public static String PluginWorkingSet_setContent;
	public static String PluginWorkingSet_selectAll_label;
	public static String PluginWorkingSet_selectAll_toolTip;
	public static String PluginWorkingSet_deselectAll_label;
	public static String PluginWorkingSet_deselectAll_toolTip;
	public static String PluginsView_showInPackageExplorer;
	public static String PluginWorkingSet_noPluginsChecked;
	public static String PluginStatusDialog_pluginValidation;
	public static String PluginsView_openWith;
	public static String PluginsView_import;
	public static String PluginsView_select;
	public static String PluginsView_CollapseAllAction_label;
	public static String PluginsView_CollapseAllAction_description;
	public static String PluginsView_CollapseAllAction_tooltip;
	public static String PluginsView_SelectAllAction_label;

	public static String PluginSection_open;
	public static String PluginsView_unableToOpen;
	public static String PluginWorkingSet_setName;
	public static String PluginStatusDialog_label;
	public static String PluginContentPage_appID;
	public static String PluginContentPage_noApp;
	public static String PluginSection_removeAll;
	public static String PluginsView_disabled;
	public static String PluginContentPage_no;
	public static String PluginSection_remove;
	public static String RequiredPluginsContainerPage_title;
	public static String RequiredPluginsContainerPage_desc;
	public static String RequiredPluginsContainerPage_label;

	public static String NewSiteWizard_wtitle;
	public static String NewSiteWizard_MainPage_title;
	public static String NewSiteWizard_creatingProject;
	public static String NewSiteWizard_creatingFolders;
	public static String NewSiteWizard_creatingManifest;
	public static String NewSiteWizard_MainPage_desc;
	public static String NewFeatureWizard_overwriteSite;

	//
	public static String SiteHTML_checkLabel;
	public static String SiteHTML_webLabel;
	public static String SiteHTML_webError;
	public static String SiteHTML_loadError;

	//
	public static String FeatureBuildOperation_running;

	public static String CompilersConfigurationBlock_error;
	public static String CompilersConfigurationBlock_warning;
	public static String CompilersConfigurationBlock_ignore;
	public static String CompilersConfigurationBlock_plugins;
	public static String CompilersConfigurationBlock_schemas;
	public static String CompilersConfigurationBlock_features;
	public static String CompilersConfigurationBlock_sites;
	public static String CompilersConfigurationBlock_label;
	public static String CompilersConfigurationBlock_altlabel;

	public static String CompilersPreferencePage_desc;
	public static String CompilersPreferencePage_title;

	public static String CompilersPropertyPage_useworkspacesettings_label;
	public static String CompilersPropertyPage_useworkspacesettings_change;
	public static String CompilersPropertyPage_useprojectsettings_label;

	public static String compilers_p_unresolved_import;
	public static String compilers_p_unresolved_ex_points;
	public static String compilers_p_unknown_element;
	public static String compilers_p_unknown_attribute;
	public static String compilers_p_unknown_class;
	public static String compilers_p_unknown_resource;
	public static String compilers_p_no_required_att;
	public static String compilers_p_not_externalized_att;
	public static String compilers_p_deprecated;
	public static String compilers_s_create_docs;
	public static String compilers_s_doc_folder;
	public static String compilers_s_open_tags;
	public static String compilers_s_forbidden_end_tags;
	public static String compilers_s_optional_end_tags;

	public static String compilers_f_unresolved_plugins;
	public static String compilers_f_unresolved_features;

	public static String CompilersConfigurationBlock_rebuild_title;
	public static String CompilersConfigurationBlock_rebuild_message;
	public static String CompilersConfigurationBlock_rebuild_many_title;
	public static String CompilersConfigurationBlock_rebuild_many_message;
	public static String CompilersConfigurationBlock_building;

	public static String SchemaMarkerResolutionGenerator_label;


	public static String ExportWizard_Plugin_pageTitle;
	public static String ExportWizard_Feature_pageTitle;
	public static String ExportWizard_Plugin_pageBlock;
	public static String ExportWizard_Feature_pageBlock;
	public static String ExportWizard_Plugin_description;
	public static String ExportWizard_archive;
	public static String ExportWizard_includeSource;
	public static String ExportWizard_multi_platform;
	public static String ExportWizard_destination;
	public static String ExportWizard_options;
	public static String ExportWizard_directory;
	public static String ExportWizard_workingSet;
	public static String ExportWizard_browse;
	public static String ExportWizard_antCheck;
	public static String ExportWizard_dialog_title;
	public static String ExportWizard_dialog_message;
	public static String ExportWizard_error_message;
	public static String ExportWizard_status_noselection;
	public static String ExportWizard_status_nodirectory;
	public static String ExportWizard_status_nofile;
	public static String ExportWizard_status_noantfile;
	public static String ExtensionsPage_title;
	public static String ExtensionsPage_tabName;
	public static String ExtensionDetails_title;
	public static String ExtensionDetails_desc;
	public static String ExtensionDetails_id;
	public static String ExtensionDetails_name;
	public static String ExtensionDetails_point;
	public static String ExportWizard_zipFileExists;
	public static String ExportWizard_targetEnv_button;
	public static String ExportSection_successfulSync;

	public static String ExportJob_error_message;
	public static String ExportJob_exporting;
	public static String ExportJob_jobTitle;
	public static String ExportSection_sync;

	public static String SiteEditorContributor_build;
	public static String SiteEditorContributor_rebuildAll;
	public static String GrammarPropertySource_minOccursFormat;
	public static String GrammarPropertySource_maxOccursFormat;
	public static String GrammarPropertySource_minOccursValue;
	public static String GrammarPropertySource_maxOccursValue;
	public static String ReferencePropertySource_minOccurs_value;
	public static String ReferencePropertySource_maxOccurs_value;
	public static String FeatureImportWizard_DetailedPage_problemsLoading;
	public static String NewFeatureDialog_alreadyDefined;
	public static String NewArchiveDialog_alreadyExists;
	public static String UpdateClasspathAction_missingPlugin_title;
	public static String UpdateClasspathAction_find;
	public static String UpdateClasspathAction_none;
	public static String RenameDialog_label;
	public static String RenameDialog_validationError;
	public static String ReferenceAttributeRow_browse;
	public static String EditableTablePart_renameAction;
	public static String EditableTablePart_renameTitle;

	// Site Editor #########################################
	public static String CategorySection_title;
	public static String CategorySection_new;
	public static String CategorySection_desc;
	public static String CategorySection_add;
	public static String CategorySection_remove;
	public static String CategorySection_environment;
	public static String CategorySection_buildAll;
	public static String CategorySection_build;
	public static String CategorySection_newCategoryName;
	public static String CategorySection_newCategoryLabel;

	public static String CategoryDetails_title;
	public static String CategoryDetails_sectionDescription;
	public static String CategoryDetails_name;
	public static String CategoryDetails_label;
	public static String CategoryDetails_desc;
	public static String CategoryDetails_alreadyExists;
	public static String CategoryDetails_alreadyExists_title;

	public static String FeaturesPage_title;
	public static String FeaturesPage_header;

	public static String SiteEditor_add;
	public static String SiteEditor_edit;
	public static String SiteEditor_remove;
	public static String SiteEditor_NewArchiveDialog_path;
	public static String SiteEditor_NewArchiveDialog_url;
	public static String SiteEditor_NewArchiveDialog_title;
	public static String SiteEditor_NewArchiveDialog_error;
	public static String SiteEditor_ArchiveSection_header;
	public static String SiteEditor_ArchiveSection_instruction;
	public static String SiteEditor_ArchiveSection_col1;
	public static String SiteEditor_ArchiveSection_col2;
	public static String SiteEditor_DescriptionSection_header;
	public static String SiteEditor_DescriptionSection_desc;
	public static String SiteEditor_DescriptionSection_descLabel;
	public static String SiteEditor_DescriptionSection_urlLabel;
	public static String SiteEditor_MirrorsSection_header;
	public static String SiteEditor_MirrorsSection_desc;
	public static String SiteEditor_MirrorsSection_urlLabel;

	public static String SynchronizePropertiesAction_label;
	public static String SynchronizePropertiesWizard_wtitle;
	public static String SynchronizePropertiesWizardPage_title;
	public static String SynchronizePropertiesWizardPage_desc;
	public static String SynchronizePropertiesWizardPage_group;
	public static String SynchronizationOperation_externalPlugin;
	public static String SynchronizePropertiesWizardPage_oneFeature;
	public static String SynchronizationOperation_noDefiningPlugin;
	public static String SynchronizePropertiesWizardPage_allFeatures;
	public static String SynchronizePropertiesWizardPage_synchronizing;

	//Migration Wizard#############################
	public static String MigrationWizard_title;
	public static String MigrationAction_find;
	public static String MigrationAction_none;
	public static String MigrationWizard_progress;
	public static String MigrationWizardPage_desc;
	public static String MigrationWizardPage_label;
	public static String MigrationWizard_update;

	public static String PDEFormPage_help;
	public static String GeneralInfoSection_id;
	public static String GeneralInfoSection_name;
	public static String GeneralInfoSection_class;
	public static String GeneralInfoSection_browse;
	public static String GeneralInfoSection_platformFilter;
	public static String GeneralInfoSection_selectionTitle;
	public static String RequiresSection_title;
	public static String RequiresSection_fDesc;
	public static String RequiresSection_desc;
	public static String RequiresSection_down;
	public static String RequiresSection_add;
	public static String RequiresSection_up;
	public static String RequiresSection_open;
	public static String RequiresSection_compute;
	public static String RequiresSection_update;
	public static String RequiresSection_delete;
	public static String LoopDialog_title;
	public static String MatchSection_title;
	public static String MailTemplate_title;
	public static String MailTemplate_appId;
	public static String MailNewWizard_title;
	public static String MatchSection_desc;
	public static String MailTemplate_desc;
	public static String ClasspathSection_jarsMessage;
	public static String ClasspathSection_rename;
	public static String OverviewPage_exportingTitle;
	public static String OverviewPage_content;
	public static String OverviewPage_osgi;
	public static String OverviewPage_testing;
	public static String OverviewPage_OSGiTesting;
	public static String OverviewPage_tabName;
	public static String OverviewPage_title;
	public static String OverviewPage_deploying;
	public static String OverviewPage_fOsgi;
	public static String OverviewPage_error;
	public static String OverviewPage_fContent;
	public static String OverviewPage_fTesting;
	public static String OverviewPage_fDeploying;
	public static String ClassAttributeRow_dialogTitle;
	public static String AttributePropertySource_assertBoolean;
	public static String AttributePropertySource_translatable;
	public static String AttributePropertySource_deprecated;
	public static String AttributePropertySource_invalidRestriction;
	public static String ArchivePage_title;
	public static String ArchivePage_name;

	public static String SampleWizard_title;
	public static String SampleEditor_desc;
	public static String SampleWizard_overwrite;
	public static String SampleEditor_content;
	public static String SampleOperation_creating;
	public static String SampleStandbyContent_content;
	public static String SampleStandbyContent_desc;
	public static String ReviewPage_title;
	public static String ReviewPage_desc;
	public static String ReviewPage_descContent;
	public static String ReviewPage_content;
	public static String ShowSampleAction_title;
	public static String ShowSampleAction_msgDesc;
	public static String SelfHostingPropertyPage_label;
	public static String SelfHostingPropertyPage_viewerLabel;
	public static String MigratePluginWizard_cleanProjects;
	public static String RuntimePage_tabName;
	public static String ApplicationSelectionDialog_debug;
	public static String ApplicationSelectionDialog_run;
	public static String ApplicationSelectionDialog_dtitle;
	public static String ApplicationSelectionDialog_rtitle;

	public static String PluginValidationOperation_missingCore;
	public static String PluginValidationOperation_missingApp;
	public static String PluginValidationOperation_missingApp2;
	public static String PluginValidationOperation_missingProduct;
	public static String PluginValidationOperation_missingProduct2;
	public static String PluginValidationOperation_disableFragment;
	public static String PluginValidationOperation_disablePlugin;
	public static String PluginValidationOperation_missingRequired;
	public static String PluginValidationOperation_disabledRequired;
	public static String PluginValidationOperation_missingImport;
	public static String PluginValidationOperation_missingParent;
	public static String PluginValidationOperation_disabledParent;
	public static String ElementPropertySource_translatable;
	public static String ElementPropertySource_deprecated;
	public static String TargetPlatformPreferencePage_pluginsTab;
	public static String TargetPlatformPreferencePage_sourceCode;
	public static String TargetPlatformPreferencePage_environmentTab;
	public static String EnvironmentBlock_jreGroup;
	public static String EnvironmentBlock_jreNote;
	public static String EnvironmentBlock_targetEnv;
	public static String MailTemplate_productName;
	public static String MailTemplate_productID;
	public static String MailTemplate_appClass;
	public static String MailTemplate_packageName;

	//Product Editor#####################
	public static String Product_overview_configuration;
	public static String Product_PluginSection_working;
	public static String ProductInfoSection_titleLabel;
	public static String Product_PluginSection_required;
	public static String ProductInfoSection_prodIdLabel;
	public static String ProductExportAction_errorTitle;
	public static String ProductExportWizardPage_title;
	public static String ProductExportWizardPage_config;
	public static String ProductExportWizardPage_browse;
	public static String Product_PluginSection_removeAll;
	public static String Product_PluginSection_newPlugin;
	public static String Product_ConfigurationPage_title;
	public static String ProductFileWizadPage_groupTitle;
	public static String Product_PluginSection_newFragment;
	public static String Product_overview_testing;
	public static String ProductInfoSection_title;
	public static String Product_PluginSection_add;
	public static String Product_PluginSection_title;
	public static String Product_ExportSection_title;
	public static String Product_FeatureSection_desc;
	public static String ProductInfoSection_appLabel;
	public static String ProductExportAction_jobName;
	public static String ProductExportJob_jobName;
	public static String ProductExportWizardPage_desc;
	public static String ProductExportWizardPage_root;
	public static String ProductExportWizardPage_sync;
	public static String ProductExportWizard_error;
	public static String ProductExportWizard_corrupt;
	public static String ProductDefinitionWizard_title;
	public static String ProductDefinitionWizard_error;
	public static String ProductDefinitonWizardPage_title;
	public static String ProductDefinitonWizardPage_desc;
	public static String ProductDefinitonWizardPage_descNoName;
	public static String ProductDefinitonWizardPage_plugin;
	public static String ProductDefinitonWizardPage_browse;
	public static String ProductDefinitonWizardPage_productId;
	public static String ProductDefinitonWizardPage_productName;
	public static String ProductDefinitonWizardPage_noPluginId;
	public static String ProductDefinitonWizardPage_noPlugin;
	public static String ProductDefinitonWizardPage_invalidId;
	public static String Product_ExportSection_includeSource;
	public static String Product_FeatureSection_title;
	public static String Product_FeatureSection_newFeature;
	public static String ProductExportAction_noDestination;
	public static String ProductExportWizardPage_productGroup;
	public static String ProductExportWizardPage_productNotExists;
	public static String ProductExportWizardPage_wrongExtension;
	public static String ProductExportWizardPage_fileSelection;
	public static String ProductExportWizardPage_productSelection;
	public static String ProductExportWizardPage_syncText;
	public static String ProductExportWizardPage_syncButton;
	public static String ProductExportWizardPage_noProduct;
	public static String Product_OverviewPage_testing;
	public static String Product_PluginSection_desc;
	public static String Product_ExportSection_desc;
	public static String Product_FeatureSection_add;
	public static String ProductFileWizadPage_title;
	public static String ProductFileWizadPage_basic;
	public static String ProductFileWizadPage_error;
	public static String Product_overview_exporting;
	public static String ProductInfoSection_desc;
	public static String ProductInfoSection_id;
	public static String ProductInfoSection_new;
	public static String ProductInfoSection_app;
	public static String SplashSection_title;
	public static String SplashSection_desc;
	public static String SplashSection_label;
	public static String SplashSection_plugin;
	public static String SplashSection_browse;
	public static String SplashSection_selection;
	public static String SplashSection_message;
	public static String BrandingPage_title;
	public static String WindowImagesSection_title;
	public static String WindowImagesSection_desc;
	public static String WindowImagesSection_browse;
	public static String WindowImagesSection_open;
	public static String WindowImagesSection_warning;
	public static String WindowImagesSection_emptyPath;
	public static String WindowImagesSection_dialogTitle;
	public static String WindowImagesSection_dialogMessage;
	public static String AboutSection_title;
	public static String AboutSection_desc;
	public static String AboutSection_image;
	public static String AboutSection_browse;
	public static String AboutSection_text;
	public static String AboutSection_open;
	public static String AboutSection_warning;
	public static String AboutSection_imgTitle;
	public static String AboutSection_imgMessage;
	public static String LauncherSection_solarisLabel;
	public static String LauncherSection_launcherName;
	public static String LauncherSection_dialogTitle;
	public static String LauncherSection_dialogMessage;
	public static String ProductDefinitonWizardPage_applicationDefinition;
	public static String SWTApplicationLaunchShortcut_noMainInEditor;
	public static String SWTApplicationLaunchShortcut_noMainInSelection;
	public static String SWTApplicationLaunchShortcut_debug;
	public static String SWTApplicationLaunchShortcut_run;
	public static String SWTApplicationLaunchShortcut_launch;
	public static String SWTApplicationLaunchShortcut_failed;
	public static String SWTApplicationLaunchShortcut_chooseRun;
	public static String SWTApplicationLaunchShortcut_chooseDebug;
	public static String SWTApplicationLaunchShortcut_error;
	public static String SWTApplicationLaunchShortcut_exception;
	public static String ArgumentsSection_title;
	public static String ArgumentsSection_desc;
	public static String ArgumentsSection_program;
	public static String ArgumentsSection_vm;

	public static String Product_FeatureSection_remove;
	public static String Product_FeatureSection_open;

	public static String ImportPackageSection_desc;
	public static String ImportPackageSection_descFragment;
	public static String ExportPackageSection_desc;
	public static String ExportPackageSection_descFragment;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDEUIMessages.class);
	}

	public static String ExportPackageVisibilitySection_title;

	public static String ExportPackageVisibilitySection_default;

	public static String ExportPackageVisibilitySection_hideAll;

	public static String ExportPackageVisibilitySection_hideOnly;

	public static String ExportPackageSection_0;

	public static String ExportPackageSection_add;

	public static String ExportPackageSection_remove;

	public static String ExportPackageSection_properties;

	public static String ExportPackageSection_title;

	public static String ExportPackageSection_propertyAction;

	public static String DependenciesPage_properties;

	public static String ImportPackageSection_add;

	public static String ImportPackageSection_remove;

	public static String ImportPackageSection_properties;

	public static String ImportPackageSection_required;

	public static String ImportPackageSection_exported;

	public static String ImportPackageSection_selection;

	public static String ImportPackageSection_propertyAction;

	public static String DependencyPropertiesDialog_properties;

	public static String DependencyPropertiesDialog_optional;

	public static String DependencyPropertiesDialog_reexport;

	public static String DependencyPropertiesDialog_version;

	public static String DependencyPropertiesDialog_invalidRange;

	public static String DependencyPropertiesDialog_invalidFormat;

	public static String RequiresSection_properties;

	public static String ClasspathSection_fragment;

	public static String ClasspathSection_plugin;

	public static String EnvironmentBlock_jreTitle;

	public static String DependencyExtentSearchResultPage_referencesInPlugin;

	public static String CrossPlatformExportPage_available;

	public static String CrossPlatformExportPage_title;

	public static String CrossPlatformExportPage_desc;

	public static String BaseImportWizardSecondPage_autobuild;

	public static String CreateHelpIndexAction_creating;

	public static String ExportPackageVisibilitySection_unconditional;

	public static String ExportPackageSection_props;

	public static String ExtensionPointsSection_showDescription;

	public static String HelloRCPTemplate_perspective;

	public static String OverviewPage_buildTitle;

	public static String OverviewPage_buildQuestion;

	public static String BaseExportWizardPage_compilerOptions;

	public static String PluginWorkingSet_message;

	public static String ProductDefinitonWizardPage_noProductName;

	public static String DependencyPropertiesDialog_groupText;

	public static String DependencyPropertiesDialog_comboInclusive;

	public static String DependencyPropertiesDialog_comboExclusive;

	public static String DependencyPropertiesDialog_minimumVersion;

	public static String DependencyPropertiesDialog_maximumVersion;

	public static String DependencyPropertiesDialog_versionRangeError;

	public static String NewProjectCreationPage_pDependsOnRuntime;

	public static String NewProjectCreationPage_pPureOSGi;

	public static String HelloOSGiNewWizard_title;

	public static String HelloOSGiTemplate_startMessage;

	public static String HelloOSGiTemplate_stopMessage;

	public static String HelloOSGiTemplate_pageTitle;

	public static String HelloOSGiTemplate_pageDescription;

	public static String IntroSection_sectionText;

	public static String IntroSection_sectionDescription;

	public static String IntroSection_introLabel;

	public static String IntroSection_introInput;

	public static String IntroSection_new;

	public static String IntroSection_undefinedProductId;

	public static String IntroSection_undefinedProductIdMessage;

	public static String ProductIntroWizard_title;

	public static String ProductIntroWizardPage_title;

	public static String ProductIntroWizardPage_description;

	public static String ProductIntroWizardPage_groupText;

	public static String ProductIntroWizardPage_formText;

	public static String ProductIntroWizardPage_targetLabel;

	public static String ProductIntroWizardPage_browse;

	public static String ProductIntroWizardPage_introLabel;

	public static String ProductIntroWizardPage_targetNotSet;

	public static String ProductIntroWizardPage_introNotSet;

	public static String ProductIntroWizardPage_invalidIntroId;

	public static String ProductIntroWizardPage_introIdExists;
	
	public static String ManifestTypeRenameParticipant_composite;

	public static String LauncherPage_title;

	public static String WindowImagesSection_16;

	public static String WindowImagesSection_32;

	public static String WindowImagesSection_48;

	public static String WindowImagesSection_64;

	public static String WindowImagesSection_128;

	public static String ManifestPackageRenameParticipant_packageRename;

	public static String NewProjectCreationPage_standard;

	public static String PluginDevelopmentPage_presentation;

	public static String PluginDevelopmentPage_extensions;

	public static String PluginDevelopmentPage_equinox;

	public static String MainTab_jreSection;

	public static String PluginsTab_target;

	public static String BaseBlock_workspace;

	public static String BaseBlock_filesystem;

	public static String BaseBlock_variables;
	
	public static String BaseBlock_workspaceS;

	public static String BaseBlock_filesystemS;

	public static String BaseBlock_variablesS;

	public static String BaseBlock_relative;

	public static String OverviewPage_extensionContent;

	public static String OverviewPage_extensionPageMessageTitle;

	public static String OverviewPage_extensionPageMessageBody;

	public static String OverviewPage_fExtensionContent;

	public static String BaseBlock_dirSelection;

	public static String BaseBlock_dirChoose;

	public static String BaseBlock_errorMessage;

	public static String ConfigurationTemplateBlock_name;

	public static String WorkspaceDataBlock_name;

	public static String ConfigurationAreaBlock_0;

	public static String ConfigurationAreaBlock_name;

	public static String AbstractPluginBlock_counter;

	public static String EquinoxPluginBlock_pluginsColumn;

	public static String EquinoxPluginBlock_levelColumn;

	public static String EquinoxPluginBlock_autoColumn;

	public static String EquinoxPluginsTab_defaultStart;

	public static String EquinoxPluginsTab_defaultAuto;

	public static String EquinoxSettingsTab_name;

	public static String EquinoxLaunchConfiguration_oldTarget;

	public static String ModelChangeLabelProvider_instance;

	public static String ModelChangeLabelProvider_instances;

	public static String GetNonExternalizedStringsOperation_taskMessage;

	public static String ExternalizeStringsWizard_title;

	public static String GetNonExternalizedStringsAction_allExternalizedTitle;

	public static String GetNonExternalizedStringsAction_allExternalizedMessage;

	public static String ExternalizeStringsWizardPage_pageTitle;

	public static String ExternalizeStringsWizardPage_pageDescription;

	public static String ExternalizeStringsWizardPage_badLocalizationError;

	public static String ExternalizeStringsWizardPage_resourcelabel;

	public static String ExternalizeStringsWizardPage_selectAllButton;

	public static String ExternalizeStringsWizardPage_deselectAllButton;

	public static String ExternalizeStringsWizardPage_projectLabel;

	public static String ExternalizeStringsWizardPage_noUnderlyingResource;

	public static String ExternalizeStringsWizardPage_localizationLabel;

	public static String ExternalizeStringsWizardPage_propertiesLabel;

	public static String ExternalizeStringsWizardPage_sourceLabel;

	public static String ExternalizeStringsWizardPage_keyEmptyError;

	public static String ExternalizeStringsWizardPage_keyCommentError;

	public static String ExternalizeStringsWizardPage_keyError;

	public static String ExternalizeStringsWizardPage_keyDuplicateError;

	public static String ExternalizeStringsWizardPage_keySuggested;

	public static String NewProjectCreationPage_target;

	public static String NewProjectCreationPage_ftarget;

	public static String NewProjectCreationPage_ptarget;

	public static String RequiredExecutionEnvironmentSection_title;

	public static String PluginGeneralInfoSection_lazyStart;

	public static String ClassSearchParticipant_taskMessage;

	public static String CreateJREBundleHeaderResolution_desc;

	public static String CreateJREBundleHeaderResolution_label;

	public static String RenameAutostartResolution_desc;

	public static String RenameAutostartResolution_label;

	public static String AddSingleon_dir_label;

	public static String AddSingleon_att_label;
	
	public static String AddSingleon_dir_desc;

	public static String AddSingleon_att_desc;

	public static String RemoveBuildOrderEntries_desc;

	public static String RemoveBuildOrderEntries_label;

	public static String PointSelectionPage_extPointDesc;

	public static String PointSelectionPage_noDescAvailable;

	public static String RemoveRequireBundleResolution_description;

	public static String RemoveRequireBundleResolution_label;

	public static String OptionalRequireBundleResolution_description;

	public static String OptionalRequireBundleResolution_label;

	public static String OrganizeManifestJob_ok;

	public static String OrganizeManifestJob_taskName;

	public static String RemoveImportPkgResolution_description;

	public static String RemoveImportPkgResolution_label;

	public static String OptionalImportPkgResolution_description;

	public static String OptionalImportPkgResolution_label;

	public static String OrganizeManifestAction_name;

	public static String OrganizeRequireBundleResolution_Description;

	public static String OrganizeRequireBundleResolution_Label;

	public static String OrganizeImportPackageResolution_Description;

	public static String OrganizeImportPackageResolution_Label;

	public static String OrganizeExportPackageResolution_Description;

	public static String OrganizeExportPackageResolution_Label;

	public static String PluginImportOperation_executionEnvironment;

	public static String PluginValidationOperation_platformFilter;

	public static String PluginValidationOperation_ee;

	public static String PluginValidationOperation_singleton;

	public static String PluginValidationOperation_bundle_uses;

	public static String PluginValidationOperation_version;

	public static String PluginValidationOperation_import_uses;

	public static String PluginValidationOperation_hostVersion;

	public static String AbstractPluginBlock_auto_validate;

	public static String LauncherUtils_title;

	public static String EditorPreferencePage_link;

	public static String ManifestSyntaxColorTab_keys;

	public static String ManifestSyntaxColorTab_assignment;

	public static String ManifestSyntaxColorTab_values;

	public static String SyntaxColorTab_elements;

	public static String SyntaxColorTab_color;

	public static String SyntaxColorTab_bold;

	public static String SyntaxColorTab_italic;

	public static String SyntaxColorTab_preview;

	public static String EditorPreferencePage_xml;

	public static String EditorPreferencePage_manifest;

	public static String ManifestSyntaxColorTab_reservedOSGi;

	public static String ManifestSyntaxColorTab_attributes;

	public static String AbstractSchemaDetails_dtdLabel;

	public static String AbstractSchemaDetails_minOccurLabel;

	public static String AbstractSchemaDetails_maxOccurLabel;

	public static String AbstractSchemaDetails_unboundedButton;

	public static String ElementSection_compositorMenu;

	public static String ElementSection_referenceMenu;

	public static String NewRestrictionDialog_title;

	public static String NewRestrictionDialog_message;

	public static String SchemaIncludesSection_addButton;

	public static String SchemaIncludesSection_removeButton;

	public static String SchemaIncludesSection_title;

	public static String SchemaIncludesSection_description;

	public static String SchemaIncludesSection_dialogMessage;

	public static String SchemaElementDetails_labelProperty;

	public static String SchemaElementDetails_icon;

	public static String SchemaDetails_translatable;

	public static String SchemaElementDetails_title;
	
	public static String SchemaElementDetails_description;

	public static String SchemaCompositorDetails_description;
	
	public static String SchemaElementReferenceDetails_description;
	
	public static String SchemaCompositorDetails_type;

	public static String SchemaCompositorDetails_title;

	public static String SchemaElementReferenceDetails_reference;

	public static String SchemaElementReferenceDetails_title;

	public static String DocSection_text;

	public static String SchemaDetails_deprecated;

	public static String SchemaAttributeDetails_use;

	public static String SchemaAttributeDetails_defaultValue;

	public static String SchemaAttributeDetails_type;

	public static String SchemaAttributeDetails_restrictions;

	public static String SchemaAttributeDetails_addRestButton;

	public static String SchemaAttributeDetails_removeRestButton;

	public static String SchemaAttributeDetails_extends;

	public static String SchemaAttributeDetails_browseButton;

	public static String SchemaAttributeDetails_implements;

	public static String SchemaAttributeDetails_title;

	public static String SchemaAttributeDetails_description;
	
	public static String SchemaAttributeDetails_defaultDefaultValue;

	public static String SchemaDetails_name;

	public static String SchemaRootElementDetails_replacement;

	public static String SecondaryBundlesSection_title;

	public static String SecondaryBundlesSection_desc;

	public static String SecondaryBundlesSection_check;

	public static String SecondaryBundlesSection_resolve;

	public static String ArgumentsSection_allPlatforms;

	public static String TargetPlatformPreferencePage_agrumentsTab;

	public static String JavaArgumentsTab_progamArgsGroup;

	public static String JavaArgumentsTab_programVariables;

	public static String JavaArgumentsTab_vmArgsGroup;

	public static String JavaArgumentsTab_vmVariables;

	public static String NewTargetProfileWizard_title;

	public static String TargetProfileWizardPage_description;

	public static String JavaArgumentsTab_description;

	public static String TargetProfileWizardPage_title;

	public static String TargetProfileWizardPage_error;

	public static String ProductInfoSection_productname;

	public static String ManifestStructureCreator_name;

	public static String ManifestContentMergeViewer_title;

	public static String EnvironmentPage_title;

	public static String JRESection_title;

	public static String JRESection_description;

	public static String JRESection_defaultJRE;

	public static String JRESection_JREName;

	public static String JRESection_ExecutionEnv;

	public static String EnvironmentSection_title;

	public static String EnvironmentSection_description;

	public static String EnvironmentSection_operationSystem;

	public static String EnvironmentSection_windowingSystem;

	public static String EnvironmentSection_architecture;

	public static String EnvironmentSection_locale;

	public static String ArgumentsSection_programTabLabel;

	public static String ArgumentsSection_vmTabLabel;

	public static String ArgumentsSection_editorTitle;

	public static String ArgumentsSection_description;

	public static String ArgumentsSection_argumentsTextLabel;

	public static String ArgumentsSection_variableButtonTitle;

	public static String TargetDefinitionSection_title;

	public static String TargetDefinitionSection_name;

	public static String AbstractFormValidator_noMessageSet;

	public static String TargetDefinitionSection_targetLocation;

	public static String TargetDefinitionSection_sameAsHost;

	public static String TargetDefinitionSection_location;

	public static String TargetDefinitionSection_fileSystem;

	public static String TargetDefinitionSection_variables;

	public static String ContentSection_plugins;

	public static String ContentSection_features;

	public static String ContentSection_add;

	public static String ContentSection_remove;

	public static String ContentSection_removeAll;

	public static String ContentSection_workingSet;

	public static String ContentSection_required;

	public static String ContentSection_allTarget;

	public static String ContentSection_targetContent;

	public static String ContentSection_targetContentDesc;

	public static String ContentSection_open;

	public static String TargetOutlinePage_plugins;

	public static String TargetOutlinePage_features;

	public static String EditorUtilities_noImageData;

	public static String EditorUtilities_pathNotValidImage;

	public static String EditorUtilities_invalidFilePath;

	public static String EditorUtilities_icoError;

	public static String EditorUtilities_incorrectSize;

	public static String EditorUtilities_imageTooLarge;

	public static String EditorUtilities_imageTooLargeInfo;
	
	public static String EditorUtilities_missingIcoNote;

	public static String RequiredExecutionEnvironmentSection_add;

	public static String RequiredExecutionEnvironmentSection_remove;

	public static String RequiredExecutionEnvironmentSection_up;

	public static String RequiredExecutionEnvironmentSection_down;

	public static String RequiredExecutionEnvironmentSection_fragmentDesc;

	public static String RequiredExecutionEnvironmentSection_pluginDesc;

	public static String RequiredExecutionEnvironmentSection_dialog_title;

	public static String RequiredExecutionEnvironmentSection_dialogMessage;

	public static String BuildExecutionEnvironmentSection_configure;

	public static String ExecutionEnvironmentSection_updateClasspath;

	public static String EditorUtilities_incorrectImageDepth;

	public static String ExtensionPointsSection_openSchema;

	public static String ExtensionPointsSection_schemaNotFound;

	public static String TargetPlatformPreferencePage_TargetGroupTitle;

	public static String TargetPlatformPreferencePage_CurrentProfileLabel;

	public static String TargetPlatformPreferencePage_BrowseButton;

	public static String TargetPlatformPreferencePage_ApplyButton;

	public static String TargetPlatformPreferencePage_FileSelectionTitle;

	public static String TargetPlatformPreferencePage_FileSelectionMessage;

	public static String ImplicitDependenicesSection_Add;

	public static String ImplicitDependenicesSection_Remove;

	public static String ImplicitDependenicesSection_RemoveAll;

	public static String ImplicitDependenicesSection_Title;

	public static String TargetImplicitPluginsTab_desc;

	public static String TargetImplicitPluginsTab_removeAll3;

	public static String TargetPluginsTab_readingPlatform;

	public static String TargetPlatformPreferencePage_implicitTab;

	public static String ProductExportWizard_syncTitle;

	public static String ProductExportWizardPage_exportOptionsGroup;

	public static String XHTMLConversionWizard_title;

	public static String GetUnconvertedAction_noAction;

	public static String GetUnconvertedAction_message;

	public static String XHTMLConversionOperation_taskName;

	public static String XHTMLConversionWizardPage_title;

	public static String XHTMLConversionWizardPage_desc;

	public static String XHTMLConversionWizardPage_invalidText;

	public static String XHTMLConversionWizardPage_selectAll;

	public static String XHTMLConversionWizardPage_deselectAll;

	public static String XHTMLConversionWizardPage_viewerLabel;

	public static String XHTMLConversionOperation_createXHTML;

	public static String TargetProfileWizardPage_groupTitle;

	public static String TargetProfileWizardPage_blankTarget;

	public static String TargetProfileWizardPage_currentPlatform;

	public static String TargetProfileWizardPage_existingTarget;

	public static String OpenTargetProfileAction_title;

	public static String OpenTargetProfileAction_missingProfile;

	public static String OpenTargetProfileAction_invalidProfile;

	public static String TargetProfileWindow_title;

	public static String TargetProfileWindow_definition;

	public static String TargetProfileWindow_plugins;

	public static String TargetProfileWindow_features;

	public static String TargetProfileWindow_environment;

	public static String TargetProfileWindow_launching;

	public static String TargetProfileWindow_implicit;

	public static String TargetProfileWindow_jre;

	public static String TargetProfileWindow_program;

	public static String TargetProfileWindow_vm;

	public static String TargetProfileWizardPage_viewProfile;

	public static String TargetPluginsTab_groupPlugins;

	public static String CompilersConfigurationTab_buildPropertiesErrors;

	public static String XHTMLConversionOperation_updatingToc;

	public static String XHTMLConversionOperation_failed;

	public static String XHTMLConversionOperation_1prob;

	public static String XHTMLConversionOperation_multiProb;

	public static String XHTMLConversionOperation_title;

	public static String PluginsView_description;

	public static String TargetProfileWindow_additionalLocations;

	public static String LocationDialog_fileSystem;

	public static String LocationDialog_variables;

	public static String LocationDialog_locationExists;

	public static String LocationDialog_emptyPath;

	public static String LocationsSection_add;

	public static String LocationsSection_edit;

	public static String LocationsSection_remove;

	public static String LocationsSection_title;

	public static String LocationsSection_description;

	public static String LocationDialog_title;

	public static String LocationDialog_path;

	public static String TargetPlatformPreferencePage_reset;

	public static String TargetPlatformPreferencePage_chooseInstall;

	public static String PointSelectionPage_newDepFound;

	public static String PointSelectionPage_newDepMessage;

	public static String OrganizeManifestsWizard_title;

	public static String OrganizeManifestsOperation_export;

	public static String OrganizeManifestsOperation_filterInternal;

	public static String OrganizeManifestsOperation_removeUnresolved;

	public static String OrganizeManifestsOperation_markOptionalUnresolved;

	public static String OrganizeManifestsOperation_unusedDeps;

	public static String OrganizeManifestsOperation_lazyStart;

	public static String OrganizeManifestsOperation_nlIconPath;

	public static String OrganizeManifestsOperation_unusedKeys;

	public static String OrganizeManifestsWizardPage_title;

	public static String OrganizeManifestsWizardPage_description;

	public static String OrganizeManifestsWizardPage_errorMsg;

	public static String OrganizeManifestsWizardPage_exportedGroup;

	public static String OrganizeManifestsWizardPage_addMissing;

	public static String OrganizeManifestsWizardPage_markInternal;

	public static String OrganizeManifestsWizardPage_packageFilter;

	public static String OrganizeManifestsWizardPage_removeUnresolved;

	public static String OrganizeManifestsWizardPage_dependenciesGroup;

	public static String OrganizeManifestsWizardPage_unresolvedDependencies;

	public static String OrganizeManifestsWizardPage_remove;

	public static String OrganizeManifestsWizardPage_markOptional;

	public static String OrganizeManifestsWizardPage_removeUnused;

	public static String OrganizeManifestsWizardPage_generalGroup;

	public static String OrganizeManifestsWizardPage_lazyStart;

	public static String OrganizeManifestsWizardPage_internationalizationGroup;

	public static String OrganizeManifestsWizardPage_prefixNL;

	public static String OrganizeManifestsWizardPage_removeUnusedKeys;

	public static String TargetProfileWindow_targetDescription;
	
	public static String TargetPlatformPreferencePage_notFoundTitle;

	public static String TargetPlatformPreferencePage_notFoundDescription;

	public static String TargetPlatformPreferencePage_invalidTitle;

	public static String TargetPlatformPreferencePage_invalidDescription;
	
	public static String TargetPluginsTab_features;

	public static String TargetPluginsTab_plugins;

	public static String ManifestEditorContributor_externStringsActionName;

	public static String PatchSpecPage_feature;

	public static String FeatureSpecPage_feature;

	public static String SplashSection_progressBar;

	public static String SplashSection_progressX;

	public static String SplashSection_progressWidth;

	public static String SplashSection_progressY;

	public static String SplashSection_progressHeight;

	public static String SplashSection_progressMessage;

	public static String SplashSection_messageX;

	public static String SplashSection_messageWidth;

	public static String SplashSection_messageColor;

	public static String SplashSection_messageY;

	public static String SplashSection_messageHeight;

	public static String FeatureImportWizardPage_reload;
	
	public static String TargetErrorDialog_title;

	public static String TargetErrorDialog_description;
	
	public static String TargetDefinitionSection_description;

	public static String FeatureImportWizardPage_reloadLocation;

	public static String FeatureImportWizardPage_importHasInvalid;

	public static String UniversalWelcomeTemplate_key_directoryName;

	public static String UniversalWelcomeTemplate_key_targetPage;

	public static String UniversalWelcomeTemplate_page_Overview;

	public static String UniversalWelcomeTemplate_page_Tutorials;

	public static String UniversalWelcomeTemplate_page_FirstSteps;

	public static String UniversalWelcomeTemplate_page_Samples;

	public static String UniversalWelcomeTemplate_page_Whatsnew;

	public static String UniversalWelcomeTemplate_page_Migrate;

	public static String UniversalWelcomeTemplate_page_WebResources;

	public static String UniversalWelcomeTemplate_linkUrl;
	
	public static String ProductInfoSection_plugins;

	public static String ProductInfoSection_features;

	public static String ImportPackageSection_goToPackage;

	public static String ExportPackageSection_findReferences;

	public static String RemoveBuildEntryResolution_removeEntry;

	public static String RemoveBuildEntryResolution_removeToken;

	public static String AddBuildEntryResolution_add;

	public static String AppendSeperatorBuildEntryResolution_label;

	public static String AddSourceBuildEntryResolution_label;

	public static String RemoveSeperatorBuildEntryResolution_label;

	public static String ExternalizeStringsResolution_desc;

	public static String ExternalizeStringsResolution_label;
	
	public static String DependencyManagementSection_jobName;

	public static String OrganizeManifestsOperation_additionalDeps;

	public static String OrganizeManifestsWizardPage_addDependencies;

	public static String AddNewDependenciesAction_title;

	public static String AddNewDependenciesAction_notFound;

	public static String AddNewDependenciesOperation_mainTask;

	public static String AddNewDependenciesOperation_searchProject;

	public static String AddNewDependenciesOperation_searchForDependency;

	public static String OpenManifestsAction_cannotFind;

	public static String OpenManifestsAction_cannotOpen;

	public static String OpenManifestsAction_title;

	public static String NewProjectCreationPage_invalidProjectName;

	public static String NewProjectCreationPage_invalidLocationPath;
	
	public static String RemoveInternalDirective_label;
	
	public static String RemoveInternalDirective_desc;

	public static String ImportPackageSection_dialogButtonLabel;
}