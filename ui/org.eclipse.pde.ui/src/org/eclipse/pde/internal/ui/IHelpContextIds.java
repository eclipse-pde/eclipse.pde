/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

/**
 *
 */
public interface IHelpContextIds {
	public static final String PREFIX = "org.eclipse.pde.doc.user."; //$NON-NLS-1$
	
	// Preference pages
	public static final String MAIN_PREFERENCE_PAGE = PREFIX + "main_preference_page"; //$NON-NLS-1$
    public static final String BUILD_OPTIONS_PREFERENCE_PAGE = PREFIX + "build_options_preference_page"; //$NON-NLS-1$
    public static final String EDITOR_PREFERENCE_PAGE = PREFIX + "editor_preference_page"; //$NON-NLS-1$
	public static final String SOURCE_PREFERENCE_PAGE = PREFIX + "source_preference_page"; //$NON-NLS-1$
	public static final String COMPILERS_PREFERENCE_PAGE = PREFIX + "compilers_preference_page"; //$NON-NLS-1$
	public static final String TARGET_ENVIRONMENT_PREFERENCE_PAGE = PREFIX + "target_environment_preference_page"; //$NON-NLS-1$
 	public static final String TARGET_PLATFORM_PREFERENCE_PAGE = PREFIX + "target_platform_preference_page"; //$NON-NLS-1$
 	
 	// Property Pages
 	public static final String SELFHOSTING_PROPERTY_PAGE = PREFIX + "selfhosting_property_page"; //$NON-NLS-1$
	public static final String COMPILERS_PROPERTY_PAGE = PREFIX + "compilers_property_page"; //$NON-NLS-1$

 	// Views
 	public static final String PLUGINS_VIEW = PREFIX + "plugins_view";	 //$NON-NLS-1$

 	public static final String DEPENDENCIES_VIEW = PREFIX + "dependencies_view"; //$NON-NLS-1$
	public static final String HISTORY_ACTION = PREFIX + "history_action"; //$NON-NLS-1$
	public static final String HISTORY_LIST_ACTION = PREFIX + "history_list_action"; //$NON-NLS-1$
	public static final String HISTORY_LIST_DIALOG = PREFIX + "history_list_dialog"; //$NON-NLS-1$
	
 	// Wizard Pages
 	public static final String NEW_PROJECT_STRUCTURE_PAGE = PREFIX + "new_project_structure_page"; //$NON-NLS-1$
 	public static final String NEW_PROJECT_REQUIRED_DATA = PREFIX + "new_project_required_data"; //$NON-NLS-1$
 	public static final String NEW_PROJECT_CODE_GEN_PAGE = PREFIX + "new_project_code_gen_page"; //$NON-NLS-1$

 	public static final String NEW_FRAGMENT_STRUCTURE_PAGE = PREFIX + "new_fragment_structure_page"; //$NON-NLS-1$
 	public static final String NEW_FRAGMENT_REQUIRED_DATA = PREFIX + "new_fragment_required_data"; //$NON-NLS-1$
 	
 	public static final String NEW_FEATURE_MAIN = PREFIX + "new_feature_main"; //$NON-NLS-1$
 	public static final String NEW_FEATURE_DATA = PREFIX + "new_feature_data"; //$NON-NLS-1$
 	public static final String NEW_FEATURE_REFERENCED_PLUGINS = PREFIX + "new_feature_referenced_plugins"; //$NON-NLS-1$
 	
 	public static final String NEW_PATCH_MAIN = PREFIX + "new_patch_main"; //$NON-NLS-1$
 	public static final String NEW_PATCH_REQUIRED_DATA = PREFIX + "new_patch_data"; //$NON-NLS-1$
 	public static final String NEW_PATCH_REFERENCED_PLUGINS = PREFIX + "new_patch_referenced_plugins"; //$NON-NLS-1$
 
 	public static final String NEW_SITE_MAIN = PREFIX + "new_site_main"; //$NON-NLS-1$
 	
 	public static final String MIGRATE_3_0 = PREFIX + "migrate_3_0"; //$NON-NLS-1$
 	public static final String CONVERTED_PROJECTS = PREFIX + "converted_projects"; //$NON-NLS-1$
 	public static final String NEW_SCHEMA = PREFIX + "new_schema"; //$NON-NLS-1$
 	
 	public static final String PLUGIN_IMPORT_FIRST_PAGE = PREFIX + "plugin_import_first_page"; //$NON-NLS-1$
 	public static final String PLUGIN_IMPORT_SECOND_PAGE = PREFIX + "plugin_import_second_page"; //$NON-NLS-1$
 	public static final String PLUGIN_IMPORT_EXPRESS_PAGE = PREFIX + "plugin_import_express_page"; //$NON-NLS-1$
 	
	public static final String FEATURE_IMPORT_FIRST_PAGE = PREFIX + "feature_import_first_page"; //$NON-NLS-1$
	public static final String FEATURE_IMPORT_SECOND_PAGE = PREFIX + "feature_import_second_page"; //$NON-NLS-1$
	
	public static final String PLUGIN_EXPORT_WIZARD = PREFIX + "plugin_export_wizard"; //$NON-NLS-1$
	public static final String FEATURE_EXPORT_WIZARD = PREFIX + "feature_export_wizard";	 //$NON-NLS-1$
 	
 	public static final String TEMPLATE_SELECTION = PREFIX + "template_selection"; //$NON-NLS-1$
 	public static final String TEMPLATE_EDITOR = PREFIX + "template_editor"; //$NON-NLS-1$
 	public static final String TEMPLATE_HELLO_WORLD = PREFIX + "template_hello_world"; //$NON-NLS-1$
 	public static final String TEMPLATE_HELP = PREFIX + "template_help"; //$NON-NLS-1$
 	public static final String TEMPLATE_MULTIPAGE_EDITOR = PREFIX + "template_multipage_editor"; //$NON-NLS-1$
 	public static final String TEMPLATE_NEW_WIZARD = PREFIX + "template_new_wizard"; //$NON-NLS-1$
 	public static final String TEMPLATE_POPUP_MENU = PREFIX + "template_popup_menu"; //$NON-NLS-1$
 	public static final String TEMPLATE_PREFERENCE_PAGE = PREFIX + "template_preference_page"; //$NON-NLS-1$
 	public static final String TEMPLATE_PROPERTY_PAGE = PREFIX + "template_property_page"; //$NON-NLS-1$
 	public static final String TEMPLATE_VIEW = PREFIX + "template_view"; //$NON-NLS-1$
 	public static final String TEMPLATE_BUILDER = PREFIX + "template_builder"; //$NON-NLS-1$
 	public static final String TEMPLATE_RCP_MAIL = PREFIX + "rcp_mail"; //$NON-NLS-1$
 	
 	public static final String MANIFEST_ADD_DEPENDENCIES = PREFIX + "manifest_add_dependencies"; //$NON-NLS-1$
 	public static final String ADD_EXTENSIONS_MAIN = PREFIX + "add_extensions_main"; //$NON-NLS-1$
 	public static final String ADD_EXTENSIONS_SCHEMA_BASED = PREFIX + "add_extensions_schema_based"; //$NON-NLS-1$
 	public static final String JAVA_ATTRIBUTE_WIZARD_PAGE = PREFIX + "java_attribute_wizard_page"; //$NON-NLS-1$
	public static final String UPDATE_CLASSPATH = PREFIX + "update_classpath"; //$NON-NLS-1$
	public static final String FEATURE_ADD_REQUIRED_WIZARD = PREFIX + "feature_add_required_wizard"; //$NON-NLS-1$
	public static final String FEATURE_ADD_PACKAGED_WIZARD = PREFIX + "feature_add_packaged_wizard"; //$NON-NLS-1$
	public static final String FEATURE_PORTABILITY_WIZARD = PREFIX + "feature_portability_wizard"; //$NON-NLS-1$

	public static final String PLUGINS_CONTAINER_PAGE = PREFIX + "plugins_container_page"; //$NON-NLS-1$
	
	// dialogs
	public static final String FRAGMENT_ADD_TARGET = PREFIX + "fragment_add_target"; //$NON-NLS-1$
	public static final String SCHEMA_TYPE_RESTRICTION = PREFIX + "schema_type_restriction"; //$NON-NLS-1$
	public static final String SEARCH_PAGE = PREFIX + "search_page"; //$NON-NLS-1$
	public static final String LAUNCHER_BASIC = PREFIX + "launcher_basic"; //$NON-NLS-1$
	public static final String LAUNCHER_ADVANCED = PREFIX + "launcher_advanced"; //$NON-NLS-1$
	public static final String LAUNCHER_TRACING = PREFIX + "launcher_tracing"; //$NON-NLS-1$
	public static final String LAUNCHER_CONFIGURATION = PREFIX + "launcher_configuration"; //$NON-NLS-1$
	
	// Generic source page
	public static final String MANIFEST_SOURCE_PAGE = PREFIX + "manifest_source_page"; //$NON-NLS-1$
	
 	// Manifest Editor
 	public static final String MANIFEST_WELCOME = PREFIX + "manifest_welcome"; //$NON-NLS-1$
 	public static final String MANIFEST_PLUGIN_OVERVIEW = PREFIX + "manifest_plugin_overview"; //$NON-NLS-1$
 	public static final String MANIFEST_PLUGIN_DEPENDENCIES = PREFIX + "manifest_plugin_dependencies"; //$NON-NLS-1$
 	public static final String MANIFEST_PLUGIN_RUNTIME = PREFIX + "manifest_plugin_runtime"; //$NON-NLS-1$
 	public static final String MANIFEST_PLUGIN_EXTENSIONS = PREFIX + "manifest_plugin_extensions"; //$NON-NLS-1$
 	public static final String MANIFEST_PLUGIN_EXT_POINTS = PREFIX + "manifest_plugin_ext_points"; //$NON-NLS-1$
 	
 	public static final String MANIFEST_FRAGMENT_OVERVIEW = PREFIX + "manifest_fragment_overview"; //$NON-NLS-1$
 	public static final String MANIFEST_FRAGMENT_DEPENDENCIES = PREFIX + "manifest_fragment_dependencies"; //$NON-NLS-1$
 	public static final String MANIFEST_FRAGMENT_RUNTIME = PREFIX + "manifest_fragment_runtime"; //$NON-NLS-1$
 	public static final String MANIFEST_FRAGMENT_EXTENSIONS = PREFIX + "manifest_fragment_extensions"; //$NON-NLS-1$
 	public static final String MANIFEST_FRAGMENT_EXT_POINTS = PREFIX + "manifest_fragment_ext_points"; //$NON-NLS-1$
 
 	// Build Properties Editor
 	public static final String BUILD_PAGE = PREFIX + "build_page"; //$NON-NLS-1$
 	
 	// Schema Editor
 	public static final String SCHEMA_EDITOR_MAIN = PREFIX + "schema_editor_main"; //$NON-NLS-1$
 	public static final String SCHEMA_EDITOR_DOC = PREFIX + "schema_editor_doc"; //$NON-NLS-1$
 	
 	// Feature Editor
 	public static final String MANIFEST_FEATURE_OVERVIEW = PREFIX + "manifest_feature_overview"; //$NON-NLS-1$
 	public static final String MANIFEST_FEATURE_INFO = PREFIX + "manifest_feature_info"; //$NON-NLS-1$
 	public static final String MANIFEST_FEATURE_CONTENT = PREFIX + "manifest_feature_content"; //$NON-NLS-1$
 	public static final String MANIFEST_FEATURE_ADVANCED = PREFIX + "manifest_feature_advanced"; //$NON-NLS-1$
 	
 	public static final String FEATURE_SYNCHRONIZE_VERSIONS = PREFIX + "feature_synchronize_versions"; //$NON-NLS-1$

	// Site Editor
	public static final String MANIFEST_SITE_OVERVIEW = PREFIX + "manifest_site_overview"; //$NON-NLS-1$
	public static final String MANIFEST_SITE_BUILD = PREFIX + "manifest_sute_build"; //$NON-NLS-1$
	public static final String MANIFEST_SITE_FEATURES = PREFIX + "manifest_site_features"; //$NON-NLS-1$
	public static final String MANIFEST_SITE_ARCHIVES = PREFIX + "manifest_site_archives";	 //$NON-NLS-1$
	public static final String NEW_CATEGORY_DEF_DIALOG = PREFIX + "new_category_def_dialog";	 //$NON-NLS-1$
	public static final String NEW_FEATURE_DIALOG = PREFIX + "new_feature_dialog";	 //$NON-NLS-1$
	public static final String NEW_ARCHIVE_DIALOG = PREFIX + "new_archive_dialog";	 //$NON-NLS-1$
	
	// Editor Outline
	public static final String OUTLINE_SORT_ACTION = PREFIX + "outline_sort_action";	 //$NON-NLS-1$
	
}
