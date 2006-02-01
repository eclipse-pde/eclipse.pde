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
package org.eclipse.pde.internal.ui.compare;

import java.util.HashMap;

import org.eclipse.compare.CompareUI;
import org.eclipse.pde.internal.ui.PDEPluginImages;

/**
 * This class is the plug-in runtime class for the 
 * <code>"org.eclipse.compare.xml"</code> plug-in.
 * </p>
 */
public final class XMLStructureMapping {
	
	public static final String ECLIPSE_PLUGIN = "Eclipse Plugin"; //$NON-NLS-1$
	public static final String ECLIPSE_SCHEMA = "Eclipse Schema"; //$NON-NLS-1$
	public static final String IMAGE_TYPE_PREFIX = "xml_"; //$NON-NLS-1$

	private static HashMap fMappings;

	static {
		fMappings = new HashMap();
		HashMap idmapHM = new HashMap();
		idmapHM.put(getMapString("plugin"), "id"); //$NON-NLS-1$ //$NON-NLS-2$
		idmapHM.put(getMapString("plugin>requires>import"), "plugin"); //$NON-NLS-1$ //$NON-NLS-2$
		idmapHM.put(getMapString("plugin>runtime>library"), "name"); //$NON-NLS-1$ //$NON-NLS-2$
		idmapHM.put(getMapString("plugin>runtime>library>export"), "name"); //$NON-NLS-1$ //$NON-NLS-2$
		idmapHM.put(getMapString("plugin>extension-point"), "id"); //$NON-NLS-1$ //$NON-NLS-2$
		idmapHM.put(getMapString("plugin>extension"), "point"); //$NON-NLS-1$ //$NON-NLS-2$
		fMappings.put(ECLIPSE_PLUGIN, idmapHM);
		
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_ROOT), PDEPluginImages.DESC_PLUGIN_OBJ);
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_EXTENSION), PDEPluginImages.DESC_EXTENSION_OBJ);
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_EXTENSIONPOINT), PDEPluginImages.DESC_EXT_POINT_OBJ);
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_ELEMENT), PDEPluginImages.DESC_XML_ELEMENT_OBJ); 
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_ATTRIBUTE), PDEPluginImages.DESC_ATT_URI_OBJ);
		CompareUI.registerImageDescriptor(getImageKey(XMLStructureCreator.TYPE_TEXT), PDEPluginImages.DESC_XML_TEXT_NODE);
	}

	protected static String getImageKey(String xmlType) {
		return IMAGE_TYPE_PREFIX + xmlType;
	}
	
	private static String getMapString(String signature) {
		return XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + signature + XMLStructureCreator.SIGN_SEPARATOR;
	}
	
	public static HashMap getMappings() {
		return fMappings;
	}
	
}
