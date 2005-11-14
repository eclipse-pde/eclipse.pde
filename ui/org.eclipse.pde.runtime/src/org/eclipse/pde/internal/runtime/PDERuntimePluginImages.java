/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import java.net.URL;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

public class PDERuntimePluginImages {

	private final static ImageRegistry PLUGIN_REGISTRY =
		PDERuntimePlugin.getDefault().getImageRegistry();

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_LCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$
	private static final String PATH_LCL_DISABLED = ICONS_PATH + "dlcl16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr16/"; //$NON-NLS-1$
	private static final String PATH_EVENTS = ICONS_PATH + "eview16/"; //$NON-NLS-1$


	public static final ImageDescriptor DESC_ERROR_ST_OBJ =
		create(PATH_OBJ, "error_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_STACK_OBJ =
		create(PATH_OBJ, "error_stack.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINT_OBJ =
		create(PATH_OBJ, "ext_point_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ =
		create(PATH_OBJ, "ext_points_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSION_OBJ =
		create(PATH_OBJ, "extension_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ =
		create(PATH_OBJ, "extensions_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ =
		create(PATH_OBJ, "generic_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INFO_ST_OBJ =
		create(PATH_OBJ, "info_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ =
		create(PATH_OBJ, "java_lib_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NATIVE_LIB_OBJ =
		create(PATH_OBJ, "native_lib_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OK_ST_OBJ =
		create(PATH_OBJ, "ok_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_OBJ =
		create(PATH_OBJ, "plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ =
		create(PATH_OBJ, "req_plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ =
		create(PATH_OBJ, "req_plugins_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_RUNTIME_OBJ =
		create(PATH_OBJ, "runtime_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WARNING_ST_OBJ =
		create(PATH_OBJ, "warning_st_obj.gif"); //$NON-NLS-1$

	/*
	 * Local tool bar image descriptors
	 */

	public static final ImageDescriptor DESC_PROPERTIES =
		create(PATH_LCL, "properties.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OPEN_LOG =
		create(PATH_LCL, "open_log.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OPEN_LOG_DISABLED =
		create(PATH_LCL_DISABLED, "open_log.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_PROPERTIES_DISABLED =
		create(PATH_LCL_DISABLED, "properties.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REFRESH =
		create(PATH_LCL, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REFRESH_DISABLED =
		create(PATH_LCL_DISABLED, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLEAR = create(PATH_LCL, "clear.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_CLEAR_DISABLED =
		create(PATH_LCL_DISABLED, "clear.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_READ_LOG =
		create(PATH_LCL, "restore_log.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_READ_LOG_DISABLED =
		create(PATH_LCL_DISABLED, "restore_log.gif"); //$NON-NLS-1$
		
	public static final ImageDescriptor DESC_REMOVE_LOG =
		create(PATH_LCL, "remove.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REMOVE_LOG_DISABLED =
		create(PATH_LCL_DISABLED, "remove.gif"); //$NON-NLS-1$
		
	public static final ImageDescriptor DESC_FILTER =
		create(PATH_LCL, "filter_ps.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FILTER_DISABLED =
		create(PATH_LCL_DISABLED, "filter_ps.gif"); //$NON-NLS-1$

	public static final ImageDescriptor DESC_EXPORT =
		create(PATH_LCL, "export_log.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_DISABLED =
		create(PATH_LCL_DISABLED, "export_log.gif"); //$NON-NLS-1$
		
	public static final ImageDescriptor DESC_IMPORT =
		create(PATH_LCL, "import_log.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_IMPORT_DISABLED =
		create(PATH_LCL_DISABLED, "import_log.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_COLLAPSE_ALL =
		create(PATH_LCL, "collapseall.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_HORIZONTAL_VIEW =
		create(PATH_LCL, "th_horizontal.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_HORIZONTAL_VIEW_DISABLED = 
		create(PATH_LCL_DISABLED, "th_horizontal.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_VERTICAL_VIEW =
		create(PATH_LCL, "th_vertical.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_VERTICAL_VIEW_DISABLED = 
		create(PATH_LCL_DISABLED, "th_vertical.gif"); //$NON-NLS-1$
	
	public static final ImageDescriptor DESC_HIDE_PANE =
		create(PATH_EVENTS, "hide_pane.gif"); //$NON-NLS-1$
	
	/*
	 * Event Details
	 */
	public static final ImageDescriptor DESC_PREV_EVENT =
		create(PATH_EVENTS, "event_prev.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NEXT_EVENT = 
		create(PATH_EVENTS, "event_next.gif"); //$NON-NLS-1$
	/*
	 * Overlays
	 */
	public static final ImageDescriptor DESC_RUN_CO =
		create(PATH_OVR, "run_co.gif"); //$NON-NLS-1$

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeIconURL(prefix, name));
	}

	public static Image get(String key) {
		return PLUGIN_REGISTRY.get(key);
	}
	private static URL makeIconURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return Platform.find(PDERuntimePlugin.getDefault().getBundle(), new Path(path));
	}
	public static Image manage(String key, ImageDescriptor desc) {
		Image image = desc.createImage();
		PLUGIN_REGISTRY.put(key, image);
		return image;
	}
}
