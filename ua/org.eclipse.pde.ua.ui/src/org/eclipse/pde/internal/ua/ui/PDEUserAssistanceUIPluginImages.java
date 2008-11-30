/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Bundle of all images used by the PDE User Assistance UI plugin.
 */
public class PDEUserAssistanceUIPluginImages {

	private static ImageRegistry PLUGIN_REGISTRY;

	public final static String ICONS_PATH = "icons/"; //$NON-NLS-1$

	/**
	 * Set of predefined Image Descriptors.
	 */

	private static final String PATH_OBJ = ICONS_PATH + "obj16/"; //$NON-NLS-1$
	private static final String PATH_WIZBAN = ICONS_PATH + "wizban/"; //$NON-NLS-1$

	/**
	 * OBJ16
	 */
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

	/**
	 * WIZ
	 */
	public static final ImageDescriptor DESC_CHEATSHEET_WIZ = create(PATH_WIZBAN, "new_cheatsheet_wiz.png"); //$NON-NLS-1$

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
	}

	private static URL makeImageURL(String prefix, String name) {
		String path = "$nl$/" + prefix + name; //$NON-NLS-1$
		return FileLocator.find(PDEUserAssistanceUIPlugin.getDefault().getBundle(), new Path(path), null);
	}

	public static Image manage(String key, ImageDescriptor desc) {
		Image image = desc.createImage();
		PLUGIN_REGISTRY.put(key, image);
		return image;
	}
}
