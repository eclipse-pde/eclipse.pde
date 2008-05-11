/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public final class SharedImages {

	private SharedImages() { // do nothing
	}

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$

	public static final String DESC_IMPLEMENTATION = PATH_OBJ + "class_obj.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTY = PATH_OBJ + "property_obj.gif"; //$NON-NLS-1$
	public static final String DESC_PROPERTIES = PATH_OBJ
			+ "properties_obj.gif"; //$NON-NLS-1$	
	public static final String DESC_PROVIDE = PATH_OBJ + "int_obj.gif"; //$NON-NLS-1$
	public static final String DESC_REFERENCE = PATH_OBJ + "reference_obj.gif"; //$NON-NLS-1$
	public static final String DESC_ROOT = PATH_OBJ + "component_obj.gif"; //$NON-NLS-1$
	public static final String DESC_SERVICE = PATH_OBJ + "service_obj.gif"; //$NON-NLS-1$
	public static final String DESC_DS = PATH_OBJ + "ds_obj.gif"; //$NON-NLS-1$

	public static ImageDescriptor getImageDescriptor(String key) {
		return Activator.getDefault().getImageRegistry().getDescriptor(key);
	}

	public static Image getImage(String key) {
		return Activator.getDefault().getImageRegistry().get(key);
	}

}
