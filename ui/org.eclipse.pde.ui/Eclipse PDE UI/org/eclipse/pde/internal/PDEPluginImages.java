package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import org.eclipse.core.runtime.*;
import java.io.File;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import java.net.URL;

/**
 * Bundle of all images used by the PDE plugin.
 */
public class PDEPluginImages {

	private static final String NAME_PREFIX= PDEPlugin.getPluginId()+".";
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	private final static URL BASE_URL = PDEPlugin.getDefault().getDescriptor().getInstallURL();
	private final static String PATH_BASIC = "icons/basic/";
	private final static String PATH_FULL  = "icons/full/";

	private static ImageRegistry PLUGIN_REGISTRY;

	/**
	 * Set of predefined Image Descriptors.
	 */
	
	private static final String BASIC_OBJ= PATH_BASIC+"obj16/";
	private static final String BASIC_VIEW = PATH_BASIC+"view16/";
	private static final String BASIC_LCL= PATH_BASIC+"elcl16/";
	private static final String BASIC_LCL_HOVER= PATH_BASIC+"clcl16/";
	private static final String BASIC_LCL_DISABLED= PATH_BASIC+"dlcl16/";
	private static final String BASIC_TOOL = PATH_BASIC + "etool16/";
	private static final String BASIC_TOOL_HOVER = PATH_BASIC + "ctool16/";
	private static final String BASIC_TOOL_DISABLED = PATH_BASIC + "dtool16/";
	private static final String BASIC_OVR = PATH_BASIC + "ovr16/";
	private static final String BASIC_WIZBAN = PATH_BASIC + "wizban/";

	/**
	 * Frequently used images
	 */
	public static final String IMG_ATT_REQ_OBJ = NAME_PREFIX+"ATT_REQ_OBJ";
	/**
	 * Frequently used images
	 */
	public static final String IMG_FORM_WIZ = NAME_PREFIX+"FORM_WIZ";
	public static final String IMG_FORM_BANNER = NAME_PREFIX+"FORM_BANNER";
	public static final String IMG_ATT_CLASS_OBJ = NAME_PREFIX+"ATT_CLASS_OBJ";
	public static final String IMG_ATT_IMPL_OBJ = NAME_PREFIX+"ATT_IMPL_OBJ";
	public static final String IMG_ATT_FILE_OBJ = NAME_PREFIX+"ATT_FILE_OBJ";

	/**
	 * OBJ16
	 */
	public static final ImageDescriptor DESC_ALL_SC_OBJ    = create(BASIC_OBJ, "all_sc_obj.gif");
	public static final ImageDescriptor DESC_ATT_CLASS_OBJ = create(BASIC_OBJ, "att_class_obj.gif");
	public static final ImageDescriptor DESC_ATT_FILE_OBJ  = create(BASIC_OBJ, "att_file_obj.gif");
	public static final ImageDescriptor DESC_ATT_IMPL_OBJ  = create(BASIC_OBJ, "att_impl_obj.gif");
	public static final ImageDescriptor DESC_ATT_REQ_OBJ   = create(BASIC_OBJ, "att_req_obj.gif");
	public static final ImageDescriptor DESC_ATT_URI_OBJ   = create(BASIC_OBJ, "att_URI_obj.gif");
	public static final ImageDescriptor DESC_CHOICE_SC_OBJ = create(BASIC_OBJ, "choice_sc_obj.gif");
	public static final ImageDescriptor DESC_COMP_JAR_OBJ  = create(BASIC_OBJ, "comp_jar_obj.gif");
	public static final ImageDescriptor DESC_COMP_MF_OBJ   = create(BASIC_OBJ, "comp_mf_obj.gif");
	public static final ImageDescriptor DESC_ELREF_SC_OBJ  = create(BASIC_OBJ, "elref_sc_obj.gif");
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ = create(BASIC_OBJ, "extensions_obj.gif");
	public static final ImageDescriptor DESC_EXTENSION_OBJ = create(BASIC_OBJ, "extension_obj.gif");
	public static final ImageDescriptor DESC_EXT_PLUGIN_OBJ   = create(BASIC_OBJ, "ext_plugin_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ   = create(BASIC_OBJ, "ext_points_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINT_OBJ   = create(BASIC_OBJ, "ext_point_obj.gif");
	public static final ImageDescriptor DESC_GEL_SC_OBJ    = create(BASIC_OBJ, "gel_sc_obj.gif");
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ = create(BASIC_OBJ, "generic_xml_obj.gif");
	public static final ImageDescriptor DESC_GROUP_SC_OBJ   = create(BASIC_OBJ, "group_sc_obj.gif");
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ   = create(BASIC_OBJ, "java_lib_obj.gif");
	public static final ImageDescriptor DESC_NATIVE_LIB_OBJ = create(BASIC_OBJ, "native_lib_obj.gif");
	public static final ImageDescriptor DESC_OVERVIEW_OBJ  = create(BASIC_OBJ, "overview_obj.gif");
	public static final ImageDescriptor DESC_PAGE_OBJ   = create(BASIC_OBJ, "page_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_MF_OBJ = create(BASIC_OBJ, "plugin_mf_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENT_MF_OBJ = create(BASIC_OBJ, "frgmt_mf_obj.gif");
	public static final ImageDescriptor DESC_BUILD_VAR_OBJ = create(BASIC_OBJ, "build_var_obj.gif");
	
	public static final ImageDescriptor DESC_PLUGIN_OBJ   = create(BASIC_OBJ, "plugin_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENT_OBJ   = create(BASIC_OBJ, "frgmt_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ   = create(BASIC_OBJ, "req_plugins_obj.gif");
	public static final ImageDescriptor DESC_FRAGMENTS_OBJ   = create(BASIC_OBJ, "frgmts_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ   = create(BASIC_OBJ, "req_plugin_obj.gif");
	public static final ImageDescriptor DESC_RUNTIME_OBJ   = create(BASIC_OBJ, "runtime_obj.gif");
	public static final ImageDescriptor DESC_SCHEMA_OBJ   = create(BASIC_OBJ, "schema_obj.gif");
	public static final ImageDescriptor DESC_SCOMP_JAR_OBJ = create(BASIC_OBJ, "scomp_jar_obj.gif");
	public static final ImageDescriptor DESC_SEQ_SC_OBJ   = create(BASIC_OBJ, "seq_sc_obj.gif");
	public static final ImageDescriptor DESC_DOC_SECTION_OBJ = create(BASIC_OBJ, "doc_section_obj.gif");
	public static final ImageDescriptor DESC_ALERT_OBJ = create(BASIC_OBJ, "alert_obj.gif");
	public static final ImageDescriptor DESC_TSK_ALERT_OBJ = create(BASIC_OBJ, "tsk_alert_obj.gif");
	public static final ImageDescriptor DESC_LINK_OBJ = create(BASIC_OBJ, "link_obj.gif");
	public static final ImageDescriptor DESC_LINKS_OBJ = create(BASIC_OBJ, "links_obj.gif");
	public static final ImageDescriptor DESC_ERROR_ST_OBJ = create(BASIC_OBJ, "error_st_obj.gif");
	
	
	/**
	 * OVR16
	 */
	public static final ImageDescriptor DESC_DOC_CO   = create(BASIC_OVR, "doc_co.gif");
	public static final ImageDescriptor DESC_ERROR_CO   = create(BASIC_OVR, "error_co.gif");

	/**
	 * TOOL16
	 */
	public static final ImageDescriptor DESC_DEFCON_TOOL = create(BASIC_TOOL, "defcon_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXPRJ_TOOL = create(BASIC_TOOL, "newexprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXP_TOOL = create(BASIC_TOOL, "newexp_wiz.gif");
	public static final ImageDescriptor DESC_NEWEX_TOOL = create(BASIC_TOOL, "newex_wiz.gif");
	public static final ImageDescriptor DESC_NEWPCOMP_TOOL = create(BASIC_TOOL, "newpcomp_wiz.gif");
	public static final ImageDescriptor DESC_NEWPPRJ_TOOL = create(BASIC_TOOL, "newpprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWFRAGPRJ_TOOL = create(BASIC_TOOL, "newfragprj_wiz.gif");
	public static final ImageDescriptor DESC_GRAPH_BACK_TOOL = create(BASIC_TOOL, "backward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_FORWARD_TOOL = create(BASIC_TOOL, "forward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_HOME_TOOL = create(BASIC_TOOL, "home_nav.gif");

	public static final ImageDescriptor DESC_GRAPH_BACK_TOOL_HOVER = create(BASIC_TOOL_HOVER, "backward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_FORWARD_TOOL_HOVER = create(BASIC_TOOL_HOVER, "forward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_HOME_TOOL_HOVER = create(BASIC_TOOL_HOVER, "home_nav.gif");

	public static final ImageDescriptor DESC_GRAPH_BACK_TOOL_DISABLED = create(BASIC_TOOL_DISABLED, "backward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_FORWARD_TOOL_DISABLED = create(BASIC_TOOL_DISABLED, "forward_nav.gif");
	public static final ImageDescriptor DESC_GRAPH_HOME_TOOL_DISABLED = create(BASIC_TOOL_DISABLED, "home_nav.gif");

	/**
	 * LCL
	 */
	public static final ImageDescriptor DESC_ADD_ATT = create(BASIC_LCL, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT = create(BASIC_LCL, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL = create(BASIC_LCL, "clone_el.gif");
	public static final ImageDescriptor DESC_FINDOBJ = create(BASIC_LCL, "find_obj.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS = create(BASIC_LCL, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ = create(BASIC_LCL, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES = create(BASIC_LCL, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH = create(BASIC_LCL, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT = create(BASIC_LCL, "remove_att.gif");

	public static final ImageDescriptor DESC_ADD_ATT_HOVER = create(BASIC_LCL_HOVER, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT_HOVER = create(BASIC_LCL_HOVER, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL_HOVER = create(BASIC_LCL_HOVER, "clone_el.gif");
	public static final ImageDescriptor DESC_FINDOBJ_HOVER = create(BASIC_LCL_HOVER, "find_obj.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS_HOVER = create(BASIC_LCL_HOVER, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ_HOVER = create(BASIC_LCL_HOVER, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES_HOVER = create(BASIC_LCL_HOVER, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH_HOVER = create(BASIC_LCL_HOVER, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT_HOVER = create(BASIC_LCL_HOVER, "remove_att.gif");

	public static final ImageDescriptor DESC_ADD_ATT_DISABLED = create(BASIC_LCL_DISABLED, "add_att.gif");
	public static final ImageDescriptor DESC_CLONE_ATT_DISABLED = create(BASIC_LCL_DISABLED, "clone_att.gif");
	public static final ImageDescriptor DESC_CLONE_EL_DISABLED = create(BASIC_LCL_DISABLED, "clone_el.gif");
	public static final ImageDescriptor DESC_FINDOBJ_DISABLED = create(BASIC_LCL_DISABLED, "find_obj.gif");
	public static final ImageDescriptor DESC_GENERATE_CLASS_DISABLED = create(BASIC_LCL_DISABLED, "generate_class.gif");
	public static final ImageDescriptor DESC_GOTOOBJ_DISABLED = create(BASIC_LCL_DISABLED, "goto_obj.gif");
	public static final ImageDescriptor DESC_PROPERTIES_DISABLED = create(BASIC_LCL_DISABLED, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH_DISABLED = create(BASIC_LCL_DISABLED, "refresh.gif");
	public static final ImageDescriptor DESC_REMOVE_ATT_DISABLED = create(BASIC_LCL_DISABLED, "remove_att.gif");

	/**
	 * WIZ
	 */
 	public static final ImageDescriptor DESC_NEWPPRJ_WIZ = create(BASIC_WIZBAN, "newpprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWFRAGPRJ_WIZ = create(BASIC_WIZBAN, "newfprj_wiz.gif");
	public static final ImageDescriptor DESC_DEFCON_WIZ  = create(BASIC_WIZBAN, "defcon_wiz.gif");
	public static final ImageDescriptor DESC_NEWEX_WIZ   = create(BASIC_WIZBAN, "newex_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXP_WIZ  = create(BASIC_WIZBAN, "newexp_wiz.gif");
	public static final ImageDescriptor DESC_NEWEXPRJ_WIZ   = create(BASIC_WIZBAN, "newexprj_wiz.gif");
	public static final ImageDescriptor DESC_NEWPCOMP_WIZ   = create(BASIC_WIZBAN, "newpcomp_wiz.gif");
	public static final ImageDescriptor DESC_FORM_WIZ     = create(BASIC_WIZBAN, "form_wiz.gif");
	public static final ImageDescriptor DESC_FORM_BANNER  = create(BASIC_WIZBAN, "form_banner.gif");
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

public static Image getImageFromPlugin(
	IPluginDescriptor pluginDescriptor,
	String subdirectoryAndFilename) {
	URL installURL = pluginDescriptor.getInstallURL();
	Image image = null;
	try {
		URL newURL = new URL(installURL, subdirectoryAndFilename);
		String key = newURL.toString();
		if (PLUGIN_REGISTRY==null) initialize();
		image = PLUGIN_REGISTRY.get(key);
		if (image==null) {
			ImageDescriptor desc = ImageDescriptor.createFromURL(newURL);
			image = desc.createImage();
			PLUGIN_REGISTRY.put(key, image);
		}
	}
	catch (MalformedURLException e) {
	}
	return image;
}
/* package */
private static final void initialize() {
	PLUGIN_REGISTRY = new ImageRegistry();
	manage(IMG_ATT_REQ_OBJ, DESC_ATT_REQ_OBJ);
	manage(IMG_ATT_CLASS_OBJ, DESC_ATT_CLASS_OBJ);
	manage(IMG_ATT_IMPL_OBJ, DESC_ATT_IMPL_OBJ);
	manage(IMG_ATT_FILE_OBJ, DESC_ATT_FILE_OBJ);
	manage(IMG_FORM_WIZ, DESC_FORM_WIZ);
	manage(IMG_FORM_BANNER, DESC_FORM_BANNER);

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
