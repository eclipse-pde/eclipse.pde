/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Bundle of all images used by the PDE plugin.
 */
public class PDEPluginImages {

	private static final String NAME_PREFIX = PDEPlugin.getPluginId() + "."; //$NON-NLS-1$

	private static ImageRegistry PLUGIN_REGISTRY;

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	/**
	 * Set of predefined Image Descriptors.
	 */

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_VIEW = ICONS_PATH + "view16/"; //$NON-NLS-1$
	private static final String PATH_LCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$
	private static final String PATH_LCL_DISABLED = ICONS_PATH + "dlcl16/"; //$NON-NLS-1$
	private static final String PATH_TOOL = ICONS_PATH + "etool16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr16/"; //$NON-NLS-1$
	private static final String PATH_WIZBAN = ICONS_PATH + "wizban/"; //$NON-NLS-1$

	/**
	 * Frequently used images
	 */
	public static final String IMG_FORM_WIZ = NAME_PREFIX + "FORM_WIZ"; //$NON-NLS-1$
	public static final String IMG_FORM_BANNER = NAME_PREFIX + "FORM_BANNER"; //$NON-NLS-1$
	public static final String IMG_ATTRIBUTE_OBJ = NAME_PREFIX + "IMG_ATTRIBUTE_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_CLASS_OBJ = NAME_PREFIX + "IMG_ATT_CLASS_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_FILE_OBJ = NAME_PREFIX + "IMG_ATT_FILE_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_IMPL_OBJ = NAME_PREFIX + "IMG_ATT_IMPL_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_REQ_OBJ = NAME_PREFIX + "IMG_ATT_REQ_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_ID_OBJ = NAME_PREFIX + "IMG_ATT_ID_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_STRING_OBJ = NAME_PREFIX + "IMG_ATT_STRING_OBJ"; //$NON-NLS-1$
	public static final String IMG_ATT_BOOLEAN_OBJ = NAME_PREFIX + "IMG_ATT_BOOLEAN_OBJ"; //$NON-NLS-1$
	public static final String IMG_GENERIC_XML_OBJ = NAME_PREFIX + "IMG_GENERIC_XML_OBJ"; //$NON-NLS-1$
	public static final String OBJ_DESC_GENERATE_CLASS = NAME_PREFIX + "OBJ_DESC_GENERATE_CLASS"; //$NON-NLS-1$
	public static final String OBJ_DESC_GENERATE_INTERFACE = NAME_PREFIX + "OBJ_DESC_GENERATE_INTERFACE"; //$NON-NLS-1$
	public static final String OBJ_DESC_PACKAGE = NAME_PREFIX + "OBJ_DESC_PACKAGE"; //$NON-NLS-1$
	public static final String OBJ_DESC_BUNDLE = NAME_PREFIX + "OBJ_DESC_BUNDLE"; //$NON-NLS-1$

	/**
	 * OBJ16
	 */
	public static final ImageDescriptor DESC_MAIN_TAB = create(PATH_OBJ, "main_tab.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ALL_SC_OBJ = create(PATH_OBJ, "all_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATTRIBUTE_OBJ = create(PATH_OBJ, "attribute_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_CLASS_OBJ = create(PATH_OBJ, "att_class_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_FILE_OBJ = create(PATH_OBJ, "att_file_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_IMPL_OBJ = create(PATH_OBJ, "att_impl_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_REQ_OBJ = create(PATH_OBJ, "att_req_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_URI_OBJ = create(PATH_OBJ, "att_URI_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_ID_OBJ = create(PATH_OBJ, "att_id_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_STRING_OBJ = create(PATH_OBJ, "att_string_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATT_BOOLEAN_OBJ = create(PATH_OBJ, "att_boolean_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUNDLE_OBJ = create(PATH_OBJ, "bundle_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CHOICE_SC_OBJ = create(PATH_OBJ, "choice_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_JAR_OBJ = create(PATH_OBJ, "ftr_jar_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_MF_OBJ = create(PATH_OBJ, "ftr_mf_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_OBJ = create(PATH_OBJ, "feature_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LICENSE_OBJ = create(PATH_OBJ, "license_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NOREF_FEATURE_OBJ = create(PATH_OBJ, "noref_feature_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ELREF_SC_OBJ = create(PATH_OBJ, "elref_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ = create(PATH_OBJ, "extensions_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSION_OBJ = create(PATH_OBJ, "extension_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_PLUGIN_OBJ = create(PATH_OBJ, "ext_plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_FRAGMENT_OBJ = create(PATH_OBJ, "external_frgmt_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ = create(PATH_OBJ, "ext_points_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINT_OBJ = create(PATH_OBJ, "ext_point_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GEL_SC_OBJ = create(PATH_OBJ, "gel_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ = create(PATH_OBJ, "generic_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GROUP_SC_OBJ = create(PATH_OBJ, "group_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAR_OBJ = create(PATH_OBJ, "jar_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAR_LIB_OBJ = create(PATH_OBJ, "jar_l_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ = create(PATH_OBJ, "java_lib_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OVERVIEW_OBJ = create(PATH_OBJ, "overview_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PAGE_OBJ = create(PATH_OBJ, "page_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_MF_OBJ = create(PATH_OBJ, "plugin_mf_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRAGMENT_MF_OBJ = create(PATH_OBJ, "frgmt_mf_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_VAR_OBJ = create(PATH_OBJ, "build_var_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LOOP_OBJ = create(PATH_OBJ, "loop_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LOOP_NODE_OBJ = create(PATH_OBJ, "loop_node_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROCESSING_INST_OBJ = create(PATH_OBJ, "processinginst.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_XML_ELEMENT_OBJ = create(PATH_OBJ, "element.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_XML_ELEMENT_REF_OBJ = create(PATH_OBJ, "elref_sc_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_SIMPLECS_OBJ = create(PATH_OBJ, "cheatsheet_simple_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COMPCS_OBJ = create(PATH_OBJ, "cheatsheet_composite_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSTASKGROUP_OBJ = create(PATH_OBJ, "cheatsheet_taskgroup_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSITEM_OBJ = create(PATH_OBJ, "cheatsheet_item_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSSUBITEM_OBJ = create(PATH_OBJ, "cheatsheet_subitem_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSINTRO_OBJ = create(PATH_OBJ, "cheatsheet_intro_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSCONCLUSION_OBJ = create(PATH_OBJ, "cheatsheet_conclusion_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CSUNSUPPORTED_OBJ = create(PATH_OBJ, "cheatsheet_unsupported_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CHEATSHEET_OBJ = create(PATH_OBJ, "cheatsheet_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_TOC_OBJ = create(PATH_OBJ, "toc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOC_TOPIC_OBJ = create(PATH_OBJ, "toc_topic_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOC_LEAFTOPIC_OBJ = create(PATH_OBJ, "toc_leaftopic_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOC_LINK_OBJ = create(PATH_OBJ, "toc_link_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOC_ANCHOR_OBJ = create(PATH_OBJ, "toc_anchor_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_CTXHELP_CONTEXT_OBJ = create(PATH_OBJ, "ctxhelp_context_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CTXHELP_DESC_OBJ = create(PATH_OBJ, "ctxhelp_desc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CTXHELP_COMMAND_OBJ = create(PATH_OBJ, "ctxhelp_command_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_COMGROUP_OBJ = create(PATH_OBJ, "keygroups_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENCOM_OBJ = create(PATH_OBJ, "command_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_REPOSITORY_OBJ = create(PATH_OBJ, "metadata_repo_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_PLUGIN_OBJ = create(PATH_OBJ, "plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_DIS_OBJ = create(PATH_OBJ, "plugin_dis_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OPERATING_SYSTEM_OBJ = create(PATH_OBJ, "operating_system_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SETTINGS_OBJ = create(PATH_OBJ, "settings.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRAGMENT_OBJ = create(PATH_OBJ, "frgmt_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRAGMENT_DIS_OBJ = create(PATH_OBJ, "frgmt_dis_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ = create(PATH_OBJ, "req_plugins_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRAGMENTS_OBJ = create(PATH_OBJ, "frgmts_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ = create(PATH_OBJ, "req_plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_RUNTIME_OBJ = create(PATH_OBJ, "runtime_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SCHEMA_OBJ = create(PATH_OBJ, "schema_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SCOMP_JAR_OBJ = create(PATH_OBJ, "scomp_jar_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SEQ_SC_OBJ = create(PATH_OBJ, "seq_sc_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DOC_SECTION_OBJ = create(PATH_OBJ, "doc_section_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ALERT_OBJ = create(PATH_OBJ, "alert_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TSK_ALERT_OBJ = create(PATH_OBJ, "tsk_alert_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LINK_OBJ = create(PATH_OBJ, "link_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LINKS_OBJ = create(PATH_OBJ, "links_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_ST_OBJ = create(PATH_OBJ, "error_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WARNING_ST_OBJ = create(PATH_OBJ, "warning_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INFO_ST_OBJ = create(PATH_OBJ, "info_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CATEGORY_OBJ = create(PATH_OBJ, "category_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PSEARCH_OBJ = create(PATH_OBJ, "psearch_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SITE_OBJ = create(PATH_OBJ, "site_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JUNIT_MAIN_TAB = create(PATH_OBJ, "test.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OUTPUT_FOLDER_OBJ = create(PATH_OBJ, "output_folder_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SOURCE_ATTACHMENT_OBJ = create(PATH_OBJ, "source_attach_attrib.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FTR_XML_OBJ = create(PATH_OBJ, "ftr_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OK_TRANSLATE_OBJ = create(PATH_OBJ, "ok_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NO_TRANSLATE_OBJ = create(PATH_OBJ, "incomplete_tsk.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DISCOVERY = create(PATH_OBJ, "discovery.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_XML_TEXT_NODE = create(PATH_OBJ, "xml_text_node.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLIPBOARD = create(PATH_OBJ, "copyviewtoclipboard_tsk.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_EXEC = create(PATH_OBJ, "build_exec.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TARGET_DEFINITION = create(PATH_OBJ, "target_profile_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TARGET_ENVIRONMENT = create(PATH_OBJ, "environment.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PRODUCT_DEFINITION = create(PATH_OBJ, "product_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PRODUCT_LAUNCHING = create(PATH_OBJ, "start_application.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PRODUCT_BRANDING = create(PATH_OBJ, "eclipse.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGINS_FRAGMENTS = create(PATH_OBJ, "plugins_and_fragments.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SITE_XML_OBJ = create(PATH_OBJ, "site_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_IMAGE_APPLICATION = create(PATH_OBJ, "image_application.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FOCUS_ON = create(PATH_OBJ, "focus.gif"); //$NON-NLS-1$

	/**
	 * OVR16
	 */
	public static final ImageDescriptor DESC_DOC_CO = create(PATH_OVR, "doc_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WARNING_CO = create(PATH_OVR, "warning_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_CO = create(PATH_OVR, "error_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_CO = create(PATH_OVR, "export_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTERNAL_CO = create(PATH_OVR, "external_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BINARY_CO = create(PATH_OVR, "binary_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAVA_CO = create(PATH_OVR, "java_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAR_CO = create(PATH_OVR, "jar_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROJECT_CO = create(PATH_OVR, "project_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OPTIONAL_CO = create(PATH_OVR, "optional_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INTERNAL_CO = create(PATH_OVR, "internal_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRIEND_CO = create(PATH_OVR, "friend_co.gif"); //$NON-NLS-1$

	/**
	 * TOOL16
	 */
	public static final ImageDescriptor DESC_DEFCON_TOOL = create(PATH_TOOL, "defcon_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEXPRJ_TOOL = create(PATH_TOOL, "newexprj_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEXP_TOOL = create(PATH_TOOL, "newexp_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEX_TOOL = create(PATH_TOOL, "newex_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWFTRPRJ_TOOL = create(PATH_TOOL, "newftrprj_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWPPRJ_TOOL = create(PATH_TOOL, "newpprj_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWFRAGPRJ_TOOL = create(PATH_TOOL, "newfprj_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WORKBENCH_LAUNCHER_WIZ = create(PATH_TOOL, "eclipse_launcher_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEXP_WIZ_TOOL = create(PATH_TOOL, "newexp_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEPLOYCS_TOOL = create(PATH_TOOL, "new_cheatsheet_wiz.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VALIDATE_TOOL = create(PATH_TOOL, "validate.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_PLUGIN_TOOL = create(PATH_TOOL, "exp_deployplug.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_PRODUCT_TOOL = create(PATH_TOOL, "exp_product.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_FEATURE_TOOL = create(PATH_TOOL, "exp_deployfeat.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_TARGET_TOOL = create(PATH_TOOL, "export_target.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_TOOL = create(PATH_TOOL, "build_exec.gif"); //$NON-NLS-1$

	/**
	 * LCL
	 */
	public static final ImageDescriptor DESC_ADD_ATT = create(PATH_LCL, "add_att.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ALPHAB_SORT_CO = create(PATH_LCL, "alphab_sort_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ALPHAB_SORT_CO_MINI = create(PATH_LCL, "alphab_sort_co_mini.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLONE_ATT = create(PATH_LCL, "clone_att.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLONE_EL = create(PATH_LCL, "clone_el.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERATE_CLASS = create(PATH_LCL, "generate_class.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERATE_INTERFACE = create(PATH_LCL, "generate_interface.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PACKAGE_OBJ = create(PATH_LCL, "package_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GOTOOBJ = create(PATH_LCL, "goto_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROPERTIES = create(PATH_LCL, "properties.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REFRESH = create(PATH_LCL, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DELETE = create(PATH_LCL, "delete_edit.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_MAXIMIZE = create(PATH_LCL, "maximize.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_RESTORE = create(PATH_LCL, "restore.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FULL_HIERARCHY = create(PATH_LCL, "full_hierarchy.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HORIZONTAL = create(PATH_LCL, "th_horizontal.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VERTICAL = create(PATH_LCL, "th_vertical.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COLLAPSE_ALL = create(PATH_LCL, "collapseall.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COLLAPSE_ALL_MINI = create(PATH_LCL, "collapse_all_mini.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOGGLE_EXPAND_STATE = create(PATH_LCL, "toggle_expand_state.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HELP = create(PATH_LCL, "help.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LINK_WITH_EDITOR = create(PATH_LCL, "synced.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CALLEES = create(PATH_LCL, "ch_callees.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CALLERS = create(PATH_LCL, "ch_callers.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEP_LOOP = create(PATH_LCL, "dep_loop.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FLAT_LAYOUT = create(PATH_LCL, "flatLayout.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HIERARCHICAL_LAYOUT = create(PATH_LCL, "hierarchicalLayout.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HISTORY_LIST = create(PATH_LCL, "history_list.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLEAR = create(PATH_LCL, "clear.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILTER = create(PATH_LCL, "filter_ps.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILTER_RELATED = create(PATH_LCL, "filter_related.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ADD_ATT_DISABLED = create(PATH_LCL_DISABLED, "add_att.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ALPHAB_SORT_CO_DISABLED = create(PATH_LCL_DISABLED, "alphab_sort_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLONE_ATT_DISABLED = create(PATH_LCL_DISABLED, "clone_att.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLONE_EL_DISABLED = create(PATH_LCL_DISABLED, "clone_el.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERATE_CLASS_DISABLED = create(PATH_LCL_DISABLED, "generate_class.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GOTOOBJ_DISABLED = create(PATH_LCL_DISABLED, "goto_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROPERTIES_DISABLED = create(PATH_LCL_DISABLED, "properties.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REFRESH_DISABLED = create(PATH_LCL_DISABLED, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REMOVE_ATT_DISABLED = create(PATH_LCL_DISABLED, "remove_att.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HORIZONTAL_DISABLED = create(PATH_LCL_DISABLED, "th_horizontal.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_VERTICAL_DISABLED = create(PATH_LCL_DISABLED, "th_vertical.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COLLAPSE_ALL_DISABLED = create(PATH_LCL_DISABLED, "collapseall.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TOGGLE_EXPAND_STATE_DISABLED = create(PATH_LCL_DISABLED, "toggle_expand_state.gif"); //$NON-NLS-1$	
	public static final ImageDescriptor DESC_LINK_WITH_EDITOR_DISABLED = create(PATH_LCL_DISABLED, "synced.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CALLEES_DISABLED = create(PATH_LCL_DISABLED, "ch_callees.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CALLERS_DISABLED = create(PATH_LCL_DISABLED, "ch_callers.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEP_LOOP_DISABLED = create(PATH_LCL_DISABLED, "dep_loop.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FLAT_LAYOUT_DISABLED = create(PATH_LCL_DISABLED, "flatLayout.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HIERARCHICAL_LAYOUT_DISABLED = create(PATH_LCL_DISABLED, "hierarchicalLayout.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_HISTORY_LIST_DISABLED = create(PATH_LCL_DISABLED, "history_list.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DCLEAR = create(PATH_LCL_DISABLED, "clear.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILTER_DISABLED = create(PATH_LCL_DISABLED, "filter_ps.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILTER_RELATED_DISABLED = create(PATH_LCL_DISABLED, "filter_related.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_RUN_EXC = create(PATH_OBJ, "run_exc.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEBUG_EXC = create(PATH_OBJ, "debug_exc.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROFILE_EXC = create(PATH_OBJ, "profile_exc.gif"); //$NON-NLS-1$

	/**
	 * WIZ
	 */
	public static final ImageDescriptor DESC_NEWPPRJ_WIZ = create(PATH_WIZBAN, "newpprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWFRAGPRJ_WIZ = create(PATH_WIZBAN, "newfprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEFCON_WIZ = create(PATH_WIZBAN, "defcon_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_TARGET_WIZ = create(PATH_WIZBAN, "target_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEX_WIZ = create(PATH_WIZBAN, "newex_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEXP_WIZ = create(PATH_WIZBAN, "newexp_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWEXPRJ_WIZ = create(PATH_WIZBAN, "newexprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWFTRPRJ_WIZ = create(PATH_WIZBAN, "newftrprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWFTRPTCH_WIZ = create(PATH_WIZBAN, "newefix_wizban.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEWSITEPRJ_WIZ = create(PATH_WIZBAN, "newsiteprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILESYSTEM_WIZARD = create(PATH_WIZBAN, "newfolder_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CONVJPPRJ_WIZ = create(PATH_WIZBAN, "convjpprj_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINT_SCHEMA_WIZ = create(PATH_WIZBAN, "schema_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_IMPORT_WIZ = create(PATH_WIZBAN, "imp_extplug_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_EXPORT_WIZ = create(PATH_WIZBAN, "exp_deployplug_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_IMPORT_WIZ = create(PATH_WIZBAN, "imp_extfeat_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FEATURE_EXPORT_WIZ = create(PATH_WIZBAN, "exp_deployfeat_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_MIGRATE_30_WIZ = create(PATH_WIZBAN, "migrate_30_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PRODUCT_WIZ = create(PATH_WIZBAN, "product_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CHEATSHEET_WIZ = create(PATH_WIZBAN, "new_cheatsheet_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PRODUCT_EXPORT_WIZ = create(PATH_WIZBAN, "exp_product.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAR_TO_PLUGIN_WIZ = create(PATH_WIZBAN, "jarToPlugin_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTSTR_WIZ = create(PATH_WIZBAN, "extstr_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_XHTML_CONVERT_WIZ = create(PATH_WIZBAN, "xhtml_wiz.png"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ORGANIZE_MANIFESTS = create(PATH_WIZBAN, "cleanmanifest_wiz.png"); //$NON-NLS-1$
	/**
	 * View
	 */
	public static final ImageDescriptor DESC_ARGUMENT_TAB = create(PATH_VIEW, "variable_tab.gif"); //$NON-NLS-1$

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeImageURL(prefix, name));
	}

	public static Image get(String key) {
		if (PLUGIN_REGISTRY == null)
			initialize();
		return PLUGIN_REGISTRY.get(key);
	}

	/* package */
	private static final void initialize() {
		PLUGIN_REGISTRY = new ImageRegistry();
		manage(IMG_ATT_CLASS_OBJ, DESC_ATT_CLASS_OBJ);
		manage(IMG_ATT_FILE_OBJ, DESC_ATT_FILE_OBJ);
		manage(IMG_ATT_IMPL_OBJ, DESC_ATT_IMPL_OBJ);
		manage(IMG_ATT_REQ_OBJ, DESC_ATT_REQ_OBJ);
		manage(IMG_ATT_ID_OBJ, DESC_ATT_ID_OBJ);
		manage(IMG_ATT_STRING_OBJ, DESC_ATT_STRING_OBJ);
		manage(IMG_ATT_BOOLEAN_OBJ, DESC_ATT_BOOLEAN_OBJ);
		manage(IMG_ATTRIBUTE_OBJ, DESC_ATTRIBUTE_OBJ);
		manage(IMG_GENERIC_XML_OBJ, DESC_GENERIC_XML_OBJ);
		manage(OBJ_DESC_GENERATE_CLASS, DESC_GENERATE_CLASS);
		manage(OBJ_DESC_GENERATE_INTERFACE, DESC_GENERATE_INTERFACE);
		manage(OBJ_DESC_PACKAGE, DESC_PACKAGE_OBJ);
		manage(OBJ_DESC_BUNDLE, DESC_BUNDLE_OBJ);
	}

	private static URL makeImageURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return FileLocator.find(PDEPlugin.getDefault().getBundle(), new Path(path), null);
	}

	public static Image manage(String key, ImageDescriptor desc) {
		Image image = desc.createImage();
		PLUGIN_REGISTRY.put(key, image);
		return image;
	}
}
