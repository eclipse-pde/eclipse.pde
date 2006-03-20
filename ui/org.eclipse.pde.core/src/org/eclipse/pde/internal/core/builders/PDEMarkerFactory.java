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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public class PDEMarkerFactory implements IMarkerFactory {
	
	public static final String MARKER_ID = "org.eclipse.pde.validation-marker"; //$NON-NLS-1$

	public static final int NO_RESOLUTION = -1;
	
	// manifest source fixes
	public static final int M_DEPRECATED_AUTOSTART = 0x1001;
	public static final int M_JAVA_PACKAGE__PORTED = 0x1002;
	public static final int M_SINGLETON_DIR_NOT_SET = 0x1003;
	public static final int M_SINGLETON_ATT_NOT_SET = 0x1004;
	public static final int M_PROJECT_BUILD_ORDER_ENTRIES = 0x1005;
	public static final int M_EXPORT_PKG_NOT_EXIST = 0x1006; 
	public static final int M_IMPORT_PKG_NOT_AVAILABLE = 0x1007;
	public static final int M_REQ_BUNDLE_NOT_AVAILABLE = 0x1008;
	public static final int M_EXT_STRINGS = 0x1009;
	
	// build properties fixes
	public static final int B_APPEND_SLASH_FOLDER_ENTRY = 0x2001;
	public static final int B_REMOVE_SLASH_FILE_ENTRY = 0x2002;	
	public static final int B_ADDDITION = 0x2003;
	public static final int B_SOURCE_ADDITION = 0x2004;
	public static final int B_REMOVAL = 0x2005;
	
	// plugin.xml fixes
	public static final int P_ILLEGAL_XML_NODE = 0x3001;
	
	// marker attribute keys
	public static final String BK_BUILD_ENTRY = "buildEntry.key"; //$NON-NLS-1$
	public static final String BK_BUILD_TOKEN = "buildEntry.tokenValue"; //$NON-NLS-1$
	public static final String PK_TREE_LOCATION_PATH = "xmlTree.locationPath"; //$NON-NLS-1$

	
	/**
	 * @see org.eclipse.pde.internal.core.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		return createMarker(file, NO_RESOLUTION);
	}
	
	public IMarker createMarker(IFile file, int id) throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("id", id); //$NON-NLS-1$
		return marker;
	}

}
