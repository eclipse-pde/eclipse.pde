/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.net.*;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.graphics.Image;

/**
 * Bundle of all images used by the PDE plugin.
 */
public class PDEPluginImages {

	private static final String NAME_PREFIX= PDEPlugin.getPluginId()+".";

	private final static URL BASE_URL = PDEPlugin.getDefault().getDescriptor().getInstallURL();

	private static ImageRegistry PLUGIN_REGISTRY;
	
	public final static String ICONS_PATH = "icons/";

	/**
	 * Set of predefined Image Descriptors.
	 */
	
	private static final String PATH_OBJ= ICONS_PATH+"obj16/";
	private static final String PATH_VIEW = ICONS_PATH+"view16/";
	private static final String PATH_LCL= ICONS_PATH+"elcl16/";
	private static final String PATH_LCL_HOVER= ICONS_PATH+"clcl16/";
	private static final String PATH_LCL_DISABLED= ICONS_PATH+"dlcl16/";
	private static final String PATH_TOOL = ICONS_PATH + "etool16/";
	private static final String PATH_TOOL_HOVER = ICONS_PATH + "ctool16/";
	//private static final String PATH_TOOL_DISABLED = ICONS_PATH + "dtool16/";
	private static final String PATH_OVR = ICONS_PATH + "ovr16/";
	private static final String PATH_WIZBAN = ICONS_PATH + "wizban/";

	/**
	 * Frequently used images
	 */
	public static final String IMG_FORM_WIZ = NAME_PREFIX+"FORM_WIZ";
	public static final String IMG_FORM_BANNER = NAME_PREFIX+"FORM_BANNER";
	public static final String IMG_ATT_CLASS_OBJ = NAME_PREFIX + "IMG_ATT_CLASS_OBJ";
	public static final String IMG_ATT_FILE_OBJ  = NAME_PREFIX + "IMG_ATT_FILE_OBJ";
	public static final String IMG_ATT_IMPL_OBJ  = NAME_PREFIX + "IMG_ATT_IMPL_OBJ";
	public static final String IMG_ATT_REQ_OBJ   = NAME_PREFIX + "IMG_ATT_REQ_OBJ";
	public static final String IMG_GENERIC_XML_OBJ  = NAME_PREFIX + "IMG_GENERIC_XML_OBJ";


	/**
	 * OBJ16
	 */
	public static final ImageDescriptor DESC_ALL_SC_OBJ    = create(PATH_OBJ, "all_sc_obj.gif");
	public static final ImageDescriptor DESC_ATT_CLASS_OBJ = create(PATH_OBJ, "att_class_obj.gif");
	public static final ImageDescriptor DESC_ATT_FILE_OBJ  = create(PATH_OBJ, "att_file_obj.gif");
	public static final ImageDescriptor DESC_ATT_IMPL_OBJ  = create(PATH_OBJ, "att_impl_obj.gif");
	public static final ImageDescriptor DESC_ATT_REQ_OBJ   = create(PATH_OBJ, "att_req_obj.gif");
	public static final ImageDescriptor DESC_ATT_URI_OBJ   = create(PATH_OBJ, "att_URI_obj.gif");
	public static final ImageDescriptor DESC_CHOICE_SC_OBJ = create(PATH_OBJ, "choice_sc_obj.gif");
	public static final ImageDescriptor DESC_FEATURE_JAR_OBJ  = create(PATH_OBJ, "ftr_jar_obj.gif");
	public static final ImageDescriptor DESC_FEATURE_MF_OBJ   = create(PATH_OBJ, "ftr_mf_obj.gif");
	public static final ImageDescriptor DESC_FEATURE_OBJ   = create(PATH_OBJ, "feature_obj.gif");
	public static final ImageDescriptor DESC_NOREF_FEATURE_OBJ = create(PATH_OBJ, "noref_feature_obj.gif");
	public static final ImageDescriptor DESC_ELREF_SC_OBJ  = create(PATH_OBJ, "elref_sc_obj.gif");
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ = create(PATH_OBJ, "extensions_obj.gif");
	public static final ImageDescriptor DESC_EXTENSION_OBJ = create(PATH_OBJ, "extension_obj.gif");
	public static final ImageDescriptor DESC_EXT_PLUGIN_OBJ   = create(PATH_OBJ, "ext_plugin_obj.gif");
	public static final ImageDescriptor DESC_EXT_FRAGMENT_OBJ   = create(PATH_OBJ, "external_frgmt_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ   = create(PATH_OBJ, "ext_points_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINT_OBJ   = create(PATH_OBJ, "ext_point_obj.gif");
	public static final ImageDescriptor DESC_GEL_SC_OBJ    = create(PATH_OBJ, "gel_sc_obj.gif");
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ = create(PATH_OBJ, "generic_xml_obj.gif");
	public static final ImageDescriptor DESC_GROUP_SC_OBJ   = create(PATH_OBJ, "group_sc_obj.gif");
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ   = create(PATH_OBJ, "java_lib_obj.gif");
	public static final ImageDescriptor DESC_OVERVIEW_OBJ  = create(PATH_OBJ, "overview_obj.gif");
	public static final ImageDescriptor DESC_PAGE_OBJ   = create(PATH_OBJ, "page_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_MF_OBJ = create(PATH_OBJ, "plugin_mf_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENT_MF_OBJ = create(PATH_OBJ, "frgmt_mf_obj.gif");
	public static final ImageDescriptor DESC_BUILD_VAR_OBJ = create(PATH_OBJ, "build_var_obj.gif");
	public static final ImageDescriptor DESC_LOOP_OBJ = create(PATH_OBJ, "loop_obj.gif");
	public static final ImageDescriptor DESC_LOOP_NODE_OBJ = create(PATH_OBJ, "loop_node_obj.gif");
	public static final ImageDescriptor DESC_PROCESSING_INST_OBJ = create(PATH_OBJ, "processinginst.gif");
	public static final ImageDescriptor DESC_XML_ELEMENT_OBJ = create(PATH_OBJ, "element.gif");
	
	public static final ImageDescriptor DESC_PLUGIN_OBJ   = create(PATH_OBJ, "plugin_obj.gif");
	public static final ImageDescriptor DESC_BUNDLE_OBJ   = create(PATH_OBJ, "bundle_obj.gif");
	public static final ImageDescriptor DESC_BUNDLE_FRAGMENT_OBJ   = create(PATH_OBJ, "bundlef_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_DIS_OBJ   = create(PATH_OBJ, "plugin_dis_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_CONFIGS_OBJ   = create(PATH_OBJ, "plugin_configs_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_CONFIG_OBJ   = create(PATH_OBJ, "plugin_config_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENT_OBJ   = create(PATH_OBJ, "frgmt_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENT_DIS_OBJ   = create(PATH_OBJ, "frgmt_dis_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ   = create(PATH_OBJ, "req_plugins_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENTS_OBJ   = create(PATH_OBJ, "frgmts_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ   = create(PATH_OBJ, "req_plugin_obj.gif");
	public static final ImageDescriptor DESC_RUNTIME_OBJ   = create(PATH_OBJ, "runtime_obj.gif");
	public static final ImageDescriptor DESC_SCHEMA_OBJ   = create(PATH_OBJ, "schema_obj.gif");
	public static final ImageDescriptor DESC_SCOMP_JAR_OBJ = create(PATH_OBJ, "scomp_jar_obj.gif");
	public static final ImageDescriptor DESC_SEQ_SC_OBJ   = create(PATH_OBJ, "seq_sc_obj.gif");
	public static final ImageDescriptor DESC_DOC_SECTION_OBJ = create(PATH_OBJ, "doc_section_obj.gif");
	public static final ImageDescriptor DESC_ALERT_OBJ = create(PATH_OBJ, "alert_obj.gif");
	public static final ImageDescriptor DESC_TSK_ALERT_OBJ = create(PATH_OBJ, "tsk_alert_obj.gif");
	public static final ImageDescriptor DESC_LINK_OBJ = create(PATH_OBJ, "link_obj.gif");
	public static final ImageDescriptor DESC_LINKS_OBJ = create(PATH_OBJ, "links_obj.gif");
	public static final ImageDescriptor DESC_ERROR_ST_OBJ = create(PATH_OBJ, "error_st_obj.gif");
	public static final ImageDescriptor DESC_WARNING_ST_OBJ = create(PATH_OBJ, "warning_st_obj.gif");
	public static final ImageDescriptor DESC_INFO_ST_OBJ = create(PATH_OBJ, "info_st_obj.gif");
	public static final ImageDescriptor DESC_CATEGORY_OBJ = create(PATH_OBJ, "category_obj.gif");

	public static final ImageDescriptor DESC_JUNIT_MAIN_TAB = create(PATH_OBJ, "test.gif");
	
	/**
	 * OVR16
	 */
	public static final ImageDescriptor DESC_DOC_CO   = create(PATH_OVR, "doc_co.gif");
	public static final ImageDescriptor DESC_WARNING_CO   = create(PATH_OVR, "warning_co.gif");
	public static final ImageDescriptor DESC_ERROR_CO   = create(PATH_OVR, "error_co.gif");
	public static final ImageDescriptor DESC_EXPORT_CO   = create(PATH_OVR, "export_co.gif");
	public static final ImageDescriptor DESC_EXTERNAL_CO   = create(PATH_OVR, "external_co.gif");
	public static final ImageDescriptor DESC_BINARY_CO   = create(PATH_OVR, "binary_co.gif");
	public static final ImageDescriptor DESC_JAVA_CO   = create(PATH_OVR, "java_co.gif");
	public static final ImageDescriptor DESC_JAR_CO   = create(PATH_OVR, "jar_co.gif");
	public static final ImageDescriptor DESC_PROJECT_CO   = create(PATH_OVR, "project_co.gif");

	/**
	 * TOOL16
	 */
	public static final ImageDescriptor DESC_DEFCON_TOOL = create(PATH_TOOL, "defcon_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXPRJ_TOOL = create(PATH_TOOL, "newexprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXP_TOOL = create(PATH_TOOL, "newexp_wiz.gif");
	public static final ImageDescriptor DESC_NEWEX_TOOL = create(PATH_TOOL, "newex_wiz.gif");
	public static final ImageDescriptor DESC_NEWFTRPRJ_TOOL = create(PATH_TOOL, "newftrprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWPPRJ_TOOL = create(PATH_TOOL, "newpprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWFRAGPRJ_TOOL = create(PATH_TOOL, "newfragprj_wiz.gif");



	/**
	 * LCL
	 */
	public static final ImageDescriptor DESC_ADD_ATT = create(PATH_LCL, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT = create(PATH_LCL, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL = create(PATH_LCL, "clone_el.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS = create(PATH_LCL, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ = create(PATH_LCL, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES = create(PATH_LCL, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH = create(PATH_LCL, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT = create(PATH_LCL, "remove_att.gif");
	public static final ImageDescriptor DESC_MAXIMIZE = create(PATH_LCL, "maximize.gif");
	public static final ImageDescriptor DESC_RESTORE = create(PATH_LCL, "restore.gif");
	public static final ImageDescriptor DESC_FULL_HIERARCHY = create(PATH_LCL, "full_hierarchy.gif");
	public static final ImageDescriptor DESC_HORIZONTAL = create(PATH_LCL, "th_horizontal.gif");
	public static final ImageDescriptor DESC_VERTICAL = create(PATH_LCL, "th_vertical.gif");

	public static final ImageDescriptor DESC_ADD_ATT_HOVER = create(PATH_LCL_HOVER, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT_HOVER = create(PATH_LCL_HOVER, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL_HOVER = create(PATH_LCL_HOVER, "clone_el.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS_HOVER = create(PATH_LCL_HOVER, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ_HOVER = create(PATH_LCL_HOVER, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES_HOVER = create(PATH_LCL_HOVER, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH_HOVER = create(PATH_LCL_HOVER, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT_HOVER = create(PATH_LCL_HOVER, "remove_att.gif");
	public static final ImageDescriptor DESC_HORIZONTAL_HOVER = create(PATH_LCL_HOVER, "th_horizontal.gif");
	public static final ImageDescriptor DESC_VERTICAL_HOVER = create(PATH_LCL_HOVER, "th_vertical.gif");	

	public static final ImageDescriptor DESC_ADD_ATT_DISABLED = create(PATH_LCL_DISABLED, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT_DISABLED = create(PATH_LCL_DISABLED, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL_DISABLED = create(PATH_LCL_DISABLED, "clone_el.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS_DISABLED = create(PATH_LCL_DISABLED, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ_DISABLED = create(PATH_LCL_DISABLED, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES_DISABLED = create(PATH_LCL_DISABLED, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH_DISABLED = create(PATH_LCL_DISABLED, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT_DISABLED = create(PATH_LCL_DISABLED, "remove_att.gif");
	public static final ImageDescriptor DESC_HORIZONTAL_DISABLED = create(PATH_LCL_DISABLED, "th_horizontal.gif");
	public static final ImageDescriptor DESC_VERTICAL_DISABLED = create(PATH_LCL_DISABLED, "th_vertical.gif");

	public static final ImageDescriptor DESC_RUN_EXC = create(PATH_OBJ, "run_exc.gif");
	public static final ImageDescriptor DESC_DEBUG_EXC = create(PATH_OBJ, "debug_exc.gif");
	public static final ImageDescriptor DESC_WORKBENCH_LAUNCHER_WIZ = create(PATH_TOOL_HOVER, "eclipse_launcher_wiz.gif");

	/**
	 * WIZ
	 */
 	public static final ImageDescriptor DESC_NEWPPRJ_WIZ = create(PATH_WIZBAN, "newpprj_wiz.gif");
 	public static final ImageDescriptor DESC_NEWBPRJ_WIZ = create(PATH_WIZBAN, "newbprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWFRAGPRJ_WIZ = create(PATH_WIZBAN, "newfprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWBFPRJ_WIZ = create(PATH_WIZBAN, "newbfprj_wiz.gif");
	public static final ImageDescriptor DESC_DEFCON_WIZ  = create(PATH_WIZBAN, "defcon_wiz.gif");
	public static final ImageDescriptor DESC_NEWEX_WIZ   = create(PATH_WIZBAN, "newex_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXP_WIZ  = create(PATH_WIZBAN, "newexp_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXPRJ_WIZ   = create(PATH_WIZBAN, "newexprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWBEXPRJ_WIZ   = create(PATH_WIZBAN, "newbexprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWFTRPRJ_WIZ   = create(PATH_WIZBAN, "newftrprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWSITEPRJ_WIZ   = create(PATH_WIZBAN, "newsiteprj_wiz.gif");
	public static final ImageDescriptor DESC_CONVJPPRJ_WIZ =create(PATH_WIZBAN, "convjpprj_wiz.gif");
	public static final ImageDescriptor DESC_FORM_WIZ     = create(PATH_WIZBAN, "form_wiz.gif");
	public static final ImageDescriptor DESC_DEBUG_WIZ     = create(PATH_WIZBAN, "debug_wiz.gif");
	public static final ImageDescriptor DESC_RUN_WIZ     = create(PATH_WIZBAN, "run_wiz.gif");
	public static final ImageDescriptor DESC_PLUGIN_IMPORT_WIZ  = create(PATH_WIZBAN, "imp_extplug_wiz.gif");
	public static final ImageDescriptor DESC_PLUGIN_EXPORT_WIZ  = create(PATH_WIZBAN, "exp_deployplug_wiz.gif");
	public static final ImageDescriptor DESC_FEATURE_IMPORT_WIZ  = create(PATH_WIZBAN, "imp_extfeat_wiz.gif");
	public static final ImageDescriptor DESC_FEATURE_EXPORT_WIZ  = create(PATH_WIZBAN, "exp_deployfeat_wiz.gif");
	public static final ImageDescriptor DESC_PLUGIN2BUNDLE_WIZ  = create(PATH_WIZBAN, "plugin2bundle_wiz.gif");
	public static final ImageDescriptor DESC_MIGRATE_30_WIZ = create(PATH_WIZBAN, "migrate_30_wiz.gif");
	public static final ImageDescriptor DESC_FORM_BANNER  = create(PATH_WIZBAN, "form_banner.gif");
	
	
	/**
	 * View
	 */
	public static final ImageDescriptor DESC_ARGUMENT_TAB  = create(PATH_VIEW, "variable_tab.gif");
	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeImageURL(prefix, name));
	}
	public static Image get(String key) {
		if (PLUGIN_REGISTRY==null) initialize();
		return PLUGIN_REGISTRY.get(key);
	}

public static ImageDescriptor getImageDescriptorFromPlugin(
	IPluginDescriptor pluginDescriptor, 
	String subdirectoryAndFilename) {
	URL installURL = pluginDescriptor.getInstallURL();
	try {
		URL newURL = new URL(installURL, subdirectoryAndFilename);
		return ImageDescriptor.createFromURL(newURL);
	}
	catch (MalformedURLException e) {
	}
	return null;
}

/* package */
private static final void initialize() {
	PLUGIN_REGISTRY = new ImageRegistry();
	manage(IMG_FORM_WIZ, DESC_FORM_WIZ);
	manage(IMG_FORM_BANNER, DESC_FORM_BANNER);
	manage(IMG_ATT_CLASS_OBJ,DESC_ATT_CLASS_OBJ);
	manage(IMG_ATT_FILE_OBJ, DESC_ATT_FILE_OBJ);
	manage(IMG_ATT_IMPL_OBJ, DESC_ATT_IMPL_OBJ);
	manage(IMG_ATT_REQ_OBJ,  DESC_ATT_REQ_OBJ);
	manage(IMG_GENERIC_XML_OBJ, DESC_GENERIC_XML_OBJ);
}

private static URL makeImageURL(String prefix, String name) {
	String path = prefix + name;
	URL url = null;
	try {
		url = new URL(BASE_URL, path);
	}
	catch (MalformedURLException e) {
		return null;
	}
	return url;
}

public static Image manage(String key, ImageDescriptor desc) {
	Image image = desc.createImage();
	PLUGIN_REGISTRY.put(key, image);
	return image;
}
}
