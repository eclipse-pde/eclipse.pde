/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

public class PDERuntimePluginImages {

	private static ImageRegistry PLUGIN_REGISTRY;

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_DCL = ICONS_PATH + "dlcl16/"; //$NON-NLS-1$
	private static final String PATH_LCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr16/"; //$NON-NLS-1$

	// Plug-in Spy related images
	public static final String IMG_CLASS_OBJ = "class_obj.gif"; //$NON-NLS-1$
	public static final String IMG_INTERFACE_OBJ = "int_obj.gif"; //$NON-NLS-1$
	public static final String IMG_PLUGIN_OBJ = "plugin_obj.gif"; //$NON-NLS-1$
	public static final String IMG_SPY_OBJ = "pdespy_obj.gif"; //$NON-NLS-1$
	public static final String IMG_MENU_OBJ = "menu_obj.gif"; //$NON-NLS-1$
	public static final String IMG_ID_OBJ = "generic_xml_obj.gif"; //$NON-NLS-1$
	public static final String IMG_MENUSPY_OBJ = "menuspy_obj.gif"; //$NON-NLS-1$
	public static final String IMG_CONTEXTID_OBJ = "contextid_obj.gif"; //$NON-NLS-1$
	public static final String IMG_SAVE_IMAGE_AS_OBJ = "save_image_as_obj.gif"; //$NON-NLS-1$
	public static final String IMG_COPY_QNAME = "cpyqual_menu.gif"; //$NON-NLS-1$

	public static final ImageDescriptor CLASS_OBJ = create(PATH_OBJ, IMG_CLASS_OBJ);
	public static final ImageDescriptor INTERFACE_OBJ = create(PATH_OBJ, IMG_INTERFACE_OBJ);
	public static final ImageDescriptor PLUGIN_OBJ = create(PATH_OBJ, IMG_PLUGIN_OBJ);
	public static final ImageDescriptor SPY_OBJ = create(PATH_OBJ, IMG_SPY_OBJ);
	public static final ImageDescriptor MENU_OBJ = create(PATH_OBJ, IMG_MENU_OBJ);
	public static final ImageDescriptor ID_OBJ = create(PATH_OBJ, IMG_ID_OBJ);
	public static final ImageDescriptor MENUSPY_OBJ = create(PATH_OBJ, IMG_MENUSPY_OBJ);
	public static final ImageDescriptor CONTEXTID_OBJ = create(PATH_OBJ, IMG_CONTEXTID_OBJ);
	public static final ImageDescriptor SAVE_IMAGE_AS_OBJ = create(PATH_OBJ, IMG_SAVE_IMAGE_AS_OBJ);
	public static final ImageDescriptor COPY_QNAME = create(PATH_LCL, IMG_COPY_QNAME);

	public static final ImageDescriptor DESC_REFRESH_DISABLED = create(PATH_DCL, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REFRESH = create(PATH_LCL, "refresh.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_COLLAPSE_ALL = create(PATH_LCL, "collapseall.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_ST_OBJ = create(PATH_OBJ, "error_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_STACK_OBJ = create(PATH_OBJ, "error_stack.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINT_OBJ = create(PATH_OBJ, "ext_point_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXT_POINTS_OBJ = create(PATH_OBJ, "ext_points_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSION_OBJ = create(PATH_OBJ, "extension_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXTENSIONS_OBJ = create(PATH_OBJ, "extensions_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_GENERIC_XML_OBJ = create(PATH_OBJ, "generic_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ATTR_XML_OBJ = create(PATH_OBJ, "attr_xml_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_INFO_ST_OBJ = create(PATH_OBJ, "info_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_JAVA_LIB_OBJ = create(PATH_OBJ, "java_lib_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_NATIVE_LIB_OBJ = create(PATH_OBJ, "native_lib_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_OK_ST_OBJ = create(PATH_OBJ, "ok_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGIN_OBJ = create(PATH_OBJ, "plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGIN_OBJ = create(PATH_OBJ, "req_plugin_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_REQ_PLUGINS_OBJ = create(PATH_OBJ, "req_plugins_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_RUNTIME_OBJ = create(PATH_OBJ, "runtime_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_WARNING_ST_OBJ = create(PATH_OBJ, "warning_st_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_LOCATION = create(PATH_OBJ, "location_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_IMP_OBJ = create(PATH_OBJ, "bundle-importer.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXP_OBJ = create(PATH_OBJ, "bundle-exporter.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_SERVICE_OBJ = create(PATH_OBJ, "int_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PROPERTY_OBJ = create(PATH_OBJ, "property_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PLUGINS_OBJ = create(PATH_OBJ, "plugins_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_FRAGMENT_OBJ = create(PATH_OBJ, "frgmt_obj.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_PACKAGE_OBJ = create(PATH_OBJ, "package_obj.gif"); //$NON-NLS-1$

	/*
	 * Overlays
	 */
	public static final ImageDescriptor DESC_RUN_CO = create(PATH_OVR, "run_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_EXPORT_CO = create(PATH_OVR, "export_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_ERROR_CO = create(PATH_OVR, "error_co.gif"); //$NON-NLS-1$
	public static final ImageDescriptor DESC_DEFAULT_CO = create(PATH_OVR, "default_co.gif"); //$NON-NLS-1$

	private static final void initialize() {
		PLUGIN_REGISTRY = PDERuntimePlugin.getDefault().getImageRegistry();
		manage(IMG_CLASS_OBJ, CLASS_OBJ);
		manage(IMG_INTERFACE_OBJ, INTERFACE_OBJ);
		manage(IMG_PLUGIN_OBJ, PLUGIN_OBJ);
		manage(IMG_SPY_OBJ, SPY_OBJ);
		manage(IMG_MENU_OBJ, MENU_OBJ);
		manage(IMG_ID_OBJ, ID_OBJ);
		manage(IMG_MENUSPY_OBJ, MENUSPY_OBJ);
		manage(IMG_CONTEXTID_OBJ, CONTEXTID_OBJ);
		manage(IMG_SAVE_IMAGE_AS_OBJ, SAVE_IMAGE_AS_OBJ);
		manage(IMG_COPY_QNAME, COPY_QNAME);
	}

	private static ImageDescriptor create(String prefix, String name) {
		return ImageDescriptor.createFromURL(makeIconURL(prefix, name));
	}

	public static Image get(String key) {
		if (PLUGIN_REGISTRY == null)
			initialize();
		return PLUGIN_REGISTRY.get(key);
	}

	private static URL makeIconURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return FileLocator.find(PDERuntimePlugin.getDefault().getBundle(), new Path(path), null);
	}

	public static Image manage(String key, ImageDescriptor desc) {
		Image image = desc.createImage();
		PLUGIN_REGISTRY.put(key, image);
		return image;
	}

}
