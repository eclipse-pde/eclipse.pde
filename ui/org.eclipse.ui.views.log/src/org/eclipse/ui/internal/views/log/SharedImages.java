/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 202583
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public final class SharedImages {

	private SharedImages() { // do nothing
	}

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_LCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$
	private static final String PATH_LCL_DISABLED = ICONS_PATH + "dlcl16/"; //$NON-NLS-1$
	private static final String PATH_EVENTS = ICONS_PATH + "eview16/"; //$NON-NLS-1$

	/* Event Details */
	public static final String DESC_PREV_EVENT = PATH_EVENTS + "event_prev.gif"; //$NON-NLS-1$
	public static final String DESC_NEXT_EVENT = PATH_EVENTS + "event_next.gif"; //$NON-NLS-1$	

	public static final String DESC_CLEAR = PATH_LCL + "clear.gif"; //$NON-NLS-1$
	public static final String DESC_CLEAR_DISABLED = PATH_LCL_DISABLED + "clear.gif"; //$NON-NLS-1$
	public static final String DESC_REMOVE_LOG = PATH_LCL + "remove.gif"; //$NON-NLS-1$
	public static final String DESC_REMOVE_LOG_DISABLED = PATH_LCL_DISABLED + "remove.gif"; //$NON-NLS-1$
	public static final String DESC_EXPORT = PATH_LCL + "export_log.gif"; //$NON-NLS-1$
	public static final String DESC_EXPORT_DISABLED = PATH_LCL_DISABLED + "export_log.gif"; //$NON-NLS-1$
	public static final String DESC_FILTER = PATH_LCL + "filter_ps.gif"; //$NON-NLS-1$
	public static final String DESC_FILTER_DISABLED = PATH_LCL_DISABLED + "filter_ps.gif"; //$NON-NLS-1$
	public static final String DESC_IMPORT = PATH_LCL + "import_log.gif"; //$NON-NLS-1$
	public static final String DESC_IMPORT_DISABLED = PATH_LCL_DISABLED + "import_log.gif"; //$NON-NLS-1$
	public static final String DESC_OPEN_LOG = PATH_LCL + "open_log.gif"; //$NON-NLS-1$
	public static final String DESC_OPEN_LOG_DISABLED = PATH_LCL_DISABLED + "open_log.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTIES = PATH_LCL + "properties.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTIES_DISABLED = PATH_LCL_DISABLED + "properties.gif"; //$NON-NLS-1$
	public static final String DESC_READ_LOG = PATH_LCL + "restore_log.gif"; //$NON-NLS-1$
	public static final String DESC_READ_LOG_DISABLED = PATH_LCL_DISABLED + "restore_log.gif"; //$NON-NLS-1$

	public static final String DESC_ERROR_ST_OBJ = PATH_OBJ + "error_st_obj.gif"; //$NON-NLS-1$
	public static final String DESC_ERROR_STACK_OBJ = PATH_OBJ + "error_stack.gif"; //$NON-NLS-1$
	public static final String DESC_INFO_ST_OBJ = PATH_OBJ + "info_st_obj.gif"; //$NON-NLS-1$
	public static final String DESC_OK_ST_OBJ = PATH_OBJ + "ok_st_obj.gif"; //$NON-NLS-1$
	public static final String DESC_WARNING_ST_OBJ = PATH_OBJ + "warning_st_obj.gif"; //$NON-NLS-1$
	public static final String DESC_HIERARCHICAL_LAYOUT_OBJ = PATH_OBJ + "hierarchical.gif"; //$NON-NLS-1$

	public static ImageDescriptor getImageDescriptor(String key) {
		return Activator.getDefault().getImageRegistry().getDescriptor(key);
	}

	public static Image getImage(String key) {
		return Activator.getDefault().getImageRegistry().get(key);
	}

}
