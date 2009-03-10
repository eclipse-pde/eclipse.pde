/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

public final class SharedImages {

	private SharedImages() { // do nothing
	}

	public final static int F_DYNAMIC = 1;

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_OVR = ICONS_PATH + "ovr16/"; //$NON-NLS-1$
	private static final String PATH_WIZBAN = ICONS_PATH + "wizban/"; //$NON-NLS-1$

	public static final String DESC_IMPLEMENTATION = PATH_OBJ + "class_obj.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTY = PATH_OBJ + "property_obj.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTIES = PATH_OBJ
			+ "properties_obj.gif"; //$NON-NLS-1$	
	public static final String DESC_PROVIDE = PATH_OBJ + "int_obj.gif"; //$NON-NLS-1$
	public static final String DESC_REFERENCE = PATH_OBJ + "reference_obj.gif"; //$NON-NLS-1$
	public static final String DESC_REFERENCE_ONE_N = PATH_OBJ
			+ "reference_one_n_obj.gif"; //$NON-NLS-1$
	public static final String DESC_REFERENCE_ZERO_ONE = PATH_OBJ
			+ "reference_zero_one_obj.gif"; //$NON-NLS-1$
	public static final String DESC_REFERENCE_ZERO_N = PATH_OBJ
			+ "reference_zero_n_obj.gif"; //$NON-NLS-1$
	public static final String DESC_ROOT = PATH_OBJ + "component_obj.gif"; //$NON-NLS-1$
	public static final String DESC_SERVICE = PATH_OBJ + "service_obj.gif"; //$NON-NLS-1$
	public static final String DESC_SERVICES = PATH_OBJ + "services_obj.gif"; //$NON-NLS-1$
	public static final String DESC_DS = PATH_OBJ + "ds_obj.gif"; //$NON-NLS-1$
	public static final String DESC_ATTR = PATH_OBJ + "attribute_obj.gif"; //$NON-NLS-1$
	public static final String DESC_DETAILS = PATH_OBJ + "details_obj.gif"; //$NON-NLS-1$

	public static final String OVR_DYNAMIC = PATH_OVR + "synch_co.gif"; //$NON-NLS-1$

	public static final String DESC_DS_WIZ = PATH_WIZBAN + "defcon_wiz.png"; //$NON-NLS-1$

	public static ImageDescriptor getImageDescriptor(String key) {
		return Activator.getDefault().getImageRegistry().getDescriptor(key);
	}

	public static Image getImage(String key) {
		return Activator.getDefault().getImageRegistry().get(key);
	}

	public static Image getImage(String key, int flags) {
		// TODO crufty code
		Image image = Activator.getDefault().getImageRegistry().get(key);
		if ((flags & F_DYNAMIC) != 0) {
			Image o = Activator.getDefault().getImageRegistry().get(
					key + OVR_DYNAMIC);
			if (o != null)
				return o;
			Image i = new DecorationOverlayIcon(image, SharedImages
					.getImageDescriptor(OVR_DYNAMIC),
 IDecoration.TOP_RIGHT)
					.createImage();
			Activator.getDefault().getImageRegistry().put(key + OVR_DYNAMIC, i);
			return i;
		}
		return image;
	}

}
