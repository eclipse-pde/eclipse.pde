package org.eclipse.pde.internal.runtime;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.swt.widgets.Display;


public class PDERuntimePluginImages {

	private static final String NAME_PREFIX= PDERuntimePlugin.getPluginId()+".";
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	private final static URL BASE_URL= PDERuntimePlugin.getDefault().getDescriptor().getInstallURL();
	private final static ImageRegistry PLUGIN_REGISTRY= PDERuntimePlugin.getDefault().getImageRegistry();

	public final static String ICONS_PATH;
	static {
		if(Display.getCurrent().getIconDepth() > 4)
			ICONS_PATH = "icons/full/";//$NON-NLS-1$
		else
			ICONS_PATH = "icons/basic/";//$NON-NLS-1$
	}
	
	private static final String PATH_OBJ= ICONS_PATH+"obj16/";
	private static final String PATH_VIEW = ICONS_PATH+"view16/";
	private static final String PATH_LCL= ICONS_PATH+"elcl16/";
	private static final String PATH_LCL_HOVER= ICONS_PATH+"clcl16/";
	private static final String PATH_LCL_DISABLED= ICONS_PATH+"dlcl16/";
	private static final String PATH_OVR = ICONS_PATH + "ovr16/";



	public static final ImageDescriptor DESC_ERROR_ST_OBJ  = create(PATH_OBJ, "error_st_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINT_OBJ = create(PATH_OBJ, "ext_point_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ = create(PATH_OBJ, "ext_points_obj.gif");
	public static final ImageDescriptor DESC_EXTENSION_OBJ = create(PATH_OBJ, "extension_obj.gif");
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ = create(PATH_OBJ, "extensions_obj.gif");
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ = create(PATH_OBJ, "generic_xml_obj.gif");
	public static final ImageDescriptor DESC_INFO_ST_OBJ = create(PATH_OBJ, "info_st_obj.gif");
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ = create(PATH_OBJ, "java_lib_obj.gif");
	public static final ImageDescriptor DESC_NATIVE_LIB_OBJ = create(PATH_OBJ, "native_lib_obj.gif");
	public static final ImageDescriptor DESC_OK_ST_OBJ = create(PATH_OBJ, "ok_st_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_OBJ = create(PATH_OBJ, "plugin_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ = create(PATH_OBJ, "req_plugin_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ = create(PATH_OBJ, "req_plugins_obj.gif");
	public static final ImageDescriptor DESC_RUNTIME_OBJ = create(PATH_OBJ, "runtime_obj.gif");
	public static final ImageDescriptor DESC_WARNING_ST_OBJ = create(PATH_OBJ, "warning_st_obj.gif");

	/*
	 * Local tool bar image descriptors
	 */ 

	public static final ImageDescriptor DESC_PROPERTIES = create(PATH_LCL, "properties.gif");
	public static final ImageDescriptor DESC_PROPERTIES_HOVER = create(PATH_LCL_HOVER, "properties.gif");
	public static final ImageDescriptor DESC_PROPERTIES_DISABLED = create(PATH_LCL_DISABLED, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH = create(PATH_LCL, "refresh.gif");
	public static final ImageDescriptor DESC_REFRESH_HOVER = create(PATH_LCL_HOVER, "refresh.gif");
	public static final ImageDescriptor DESC_REFRESH_DISABLED = create(PATH_LCL_DISABLED, "refresh.gif");
	public static final ImageDescriptor DESC_CLEAR = create(PATH_LCL, "clear.gif");
	public static final ImageDescriptor DESC_CLEAR_HOVER = create(PATH_LCL_HOVER, "clear.gif");
	public static final ImageDescriptor DESC_CLEAR_DISABLED = create(PATH_LCL_DISABLED, "clear.gif");
	
	/*
	 * Overlays
	 */
	 public static final ImageDescriptor DESC_RUN_CO = create(PATH_OVR, "run_co.gif");
	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeIconURL(prefix, name));
	}
	private static ImageDescriptor createManaged(String prefix, String name) {
		ImageDescriptor result= ImageDescriptor.createFromURL(makeIconURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
		PLUGIN_REGISTRY.put(name, result);
		return result;  
	}
	public static Image get(String key) {
		return PLUGIN_REGISTRY.get(key);
	}
private static URL makeIconURL(String prefix, String name) {
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
