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
package org.eclipse.pde.internal.runtime;

import java.net.*;

import org.eclipse.jface.resource.*;
import org.eclipse.swt.graphics.Image;

public class PDERuntimePluginImages {

	private final static URL BASE_URL =
		PDERuntimePlugin.getDefault().getDescriptor().getInstallURL();
	private final static ImageRegistry PLUGIN_REGISTRY =
		PDERuntimePlugin.getDefault().getImageRegistry();

	public final static String ICONS_PATH = "icons/";

	private static final String PATH_FORMS = ICONS_PATH + "forms/";
	private static final String PATH_OBJ = ICONS_PATH + "obj16/";
	private static final String PATH_LCL = ICONS_PATH + "elcl16/";
	private static final String PATH_LCL_HOVER = ICONS_PATH + "clcl16/";
	private static final String PATH_LCL_DISABLED = ICONS_PATH + "dlcl16/";
	private static final String PATH_OVR = ICONS_PATH + "ovr16/";
	private static final String PATH_EVENTS = ICONS_PATH + "view16/";
	private static final String PATH_EVENTS_DISABLED = ICONS_PATH + "dview16/";

	public static final ImageDescriptor DESC_FORM_BANNER_SHORT  = create(PATH_FORMS, "form_banner.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_ERROR_ST_OBJ =
		create(PATH_OBJ, "error_st_obj.gif");
	public static final ImageDescriptor DESC_ERROR_STACK_OBJ =
		create(PATH_OBJ, "error_stack.gif");
	public static final ImageDescriptor DESC_EXT_POINT_OBJ =
		create(PATH_OBJ, "ext_point_obj.gif");
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ =
		create(PATH_OBJ, "ext_points_obj.gif");
	public static final ImageDescriptor DESC_EXTENSION_OBJ =
		create(PATH_OBJ, "extension_obj.gif");
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ =
		create(PATH_OBJ, "extensions_obj.gif");
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ =
		create(PATH_OBJ, "generic_xml_obj.gif");
	public static final ImageDescriptor DESC_INFO_ST_OBJ =
		create(PATH_OBJ, "info_st_obj.gif");
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ =
		create(PATH_OBJ, "java_lib_obj.gif");
	public static final ImageDescriptor DESC_NATIVE_LIB_OBJ =
		create(PATH_OBJ, "native_lib_obj.gif");
	public static final ImageDescriptor DESC_OK_ST_OBJ =
		create(PATH_OBJ, "ok_st_obj.gif");
	public static final ImageDescriptor DESC_PLUGIN_OBJ =
		create(PATH_OBJ, "plugin_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ =
		create(PATH_OBJ, "req_plugin_obj.gif");
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ =
		create(PATH_OBJ, "req_plugins_obj.gif");
	public static final ImageDescriptor DESC_RUNTIME_OBJ =
		create(PATH_OBJ, "runtime_obj.gif");
	public static final ImageDescriptor DESC_WARNING_ST_OBJ =
		create(PATH_OBJ, "warning_st_obj.gif");

	/*
	 * Local tool bar image descriptors
	 */

	public static final ImageDescriptor DESC_PROPERTIES =
		create(PATH_LCL, "properties.gif");
	public static final ImageDescriptor DESC_PROPERTIES_HOVER =
		create(PATH_LCL_HOVER, "properties.gif");
	public static final ImageDescriptor DESC_PROPERTIES_DISABLED =
		create(PATH_LCL_DISABLED, "properties.gif");
	public static final ImageDescriptor DESC_REFRESH =
		create(PATH_LCL, "refresh.gif");
	public static final ImageDescriptor DESC_REFRESH_HOVER =
		create(PATH_LCL_HOVER, "refresh.gif");
	public static final ImageDescriptor DESC_REFRESH_DISABLED =
		create(PATH_LCL_DISABLED, "refresh.gif");
	public static final ImageDescriptor DESC_CLEAR = create(PATH_LCL, "clear.gif");
	public static final ImageDescriptor DESC_CLEAR_HOVER =
		create(PATH_LCL_HOVER, "clear.gif");
	public static final ImageDescriptor DESC_CLEAR_DISABLED =
		create(PATH_LCL_DISABLED, "clear.gif");

	public static final ImageDescriptor DESC_READ_LOG =
		create(PATH_LCL, "restore_log.gif");
	public static final ImageDescriptor DESC_READ_LOG_HOVER =
		create(PATH_LCL_HOVER, "restore_log.gif");
	public static final ImageDescriptor DESC_READ_LOG_DISABLED =
		create(PATH_LCL_DISABLED, "restore_log.gif");
		
	public static final ImageDescriptor DESC_REMOVE_LOG =
		create(PATH_LCL, "remove.gif");
	public static final ImageDescriptor DESC_REMOVE_LOG_HOVER =
		create(PATH_LCL_HOVER, "remove.gif");
	public static final ImageDescriptor DESC_REMOVE_LOG_DISABLED =
		create(PATH_LCL_DISABLED, "remove.gif");
		
	public static final ImageDescriptor DESC_FILTER =
		create(PATH_LCL, "filter_ps.gif");
	public static final ImageDescriptor DESC_FILTER_HOVER =
		create(PATH_LCL_HOVER, "filter_ps.gif");
	public static final ImageDescriptor DESC_FILTER_DISABLED =
		create(PATH_LCL_DISABLED, "filter_ps.gif");

	public static final ImageDescriptor DESC_EXPORT =
		create(PATH_LCL, "export_log.gif");
	public static final ImageDescriptor DESC_EXPORT_HOVER =
		create(PATH_LCL_HOVER, "export_log.gif");
	public static final ImageDescriptor DESC_EXPORT_DISABLED =
		create(PATH_LCL_DISABLED, "export_log.gif");
		
	public static final ImageDescriptor DESC_IMPORT =
		create(PATH_LCL, "import_log.gif");
	public static final ImageDescriptor DESC_IMPORT_HOVER =
		create(PATH_LCL_HOVER, "import_log.gif");
	public static final ImageDescriptor DESC_IMPORT_DISABLED =
		create(PATH_LCL_DISABLED, "import_log.gif");
	
	/*
	 * Event Properties
	 */
	public static final ImageDescriptor DESC_PREV_EVENT =
		create(PATH_EVENTS, "event_prev.gif");
	public static final ImageDescriptor DESC_NEXT_EVENT = 
		create(PATH_EVENTS, "event_next.gif");
	public static final ImageDescriptor DESC_PREV_EVENT_DISABLED = 
		create(PATH_EVENTS_DISABLED, "event_prev.gif");
	public static final ImageDescriptor DESC_NEXT_EVENT_DISABLED = 
		create(PATH_EVENTS_DISABLED, "event_next.gif");
	/*
	 * Overlays
	 */
	public static final ImageDescriptor DESC_RUN_CO =
		create(PATH_OVR, "run_co.gif");
	public static final ImageDescriptor DESC_WARNING_CO =
		create(PATH_OVR, "warning_co.gif");

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeIconURL(prefix, name));
	}

	public static Image get(String key) {
		return PLUGIN_REGISTRY.get(key);
	}
	private static URL makeIconURL(String prefix, String name) {
		String path = prefix + name;
		URL url = null;
		try {
			url = new URL(BASE_URL, path);
		} catch (MalformedURLException e) {
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
