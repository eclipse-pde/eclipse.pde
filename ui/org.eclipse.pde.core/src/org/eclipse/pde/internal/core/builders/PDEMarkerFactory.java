/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public class PDEMarkerFactory implements IMarkerFactory {
	
	public static final String MARKER_ID = "org.eclipse.pde.core.problem"; //$NON-NLS-1$

	public static final int NO_RESOLUTION = -1;
	
	// manifest source fixes
	public static final int M_DEPRECATED_AUTOSTART = 0x1001; // other problem
	public static final int M_JAVA_PACKAGE__PORTED = 0x1002; // fatal error
	public static final int M_SINGLETON_DIR_NOT_SET = 0x1003; // other problem
	public static final int M_SINGLETON_ATT_NOT_SET = 0x1004; // other problem
	public static final int M_PROJECT_BUILD_ORDER_ENTRIES = 0x1005;
	public static final int M_EXPORT_PKG_NOT_EXIST = 0x1006;  // other problem
	public static final int M_IMPORT_PKG_NOT_AVAILABLE = 0x1007; // fatal error
	public static final int M_REQ_BUNDLE_NOT_AVAILABLE = 0x1008; // fatal error
	public static final int M_UNKNOWN_CLASS = 0x1009; // fatal error
	public static final int M_UNKNOWN_ACTIVATOR = 0x1010; // fatal error
	public static final int M_SINGLETON_DIR_NOT_SUPPORTED = 0x1011; // other problem
	public static final int M_DIRECTIVE_HAS_NO_EFFECT = 0x1012; // other problem
	public static final int M_MISMATCHED_EXEC_ENV = 0x1013; // fatal error
	public static final int M_UNKNOW_EXEC_ENV = 0x1014; // other problem
	public static final int M_DEPRECATED_IMPORT_SERVICE = 0x1015; // deprecation
	public static final int M_DEPRECATED_EXPORT_SERVICE = 0x1016; // deprecation
	public static final int M_UNECESSARY_DEP = 0x1017; // other problem
	public static final int M_MISSING_EXPORT_PKGS = 0x1018; // other problem
	public static final int M_DEPRECATED_PROVIDE_PACKAGE = 0x1019; // deprecation
	
	// build properties fixes
	public static final int B_APPEND_SLASH_FOLDER_ENTRY = 0x2001;
	public static final int B_REMOVE_SLASH_FILE_ENTRY = 0x2002;	
	public static final int B_ADDDITION = 0x2003;
	public static final int B_SOURCE_ADDITION = 0x2004;
	public static final int B_REMOVAL = 0x2005;
	
	// plugin.xml fixes
	public static final int P_ILLEGAL_XML_NODE = 0x3001;
	public static final int P_UNTRANSLATED_NODE = 0x3002;
	public static final int P_UNKNOWN_CLASS = 0x3003;
	
	// marker attribute keys
	public static final String BK_BUILD_ENTRY = "buildEntry.key"; //$NON-NLS-1$
	public static final String BK_BUILD_TOKEN = "buildEntry.tokenValue"; //$NON-NLS-1$
	public static final String MPK_LOCATION_PATH = "xmlTree.locationPath"; //$NON-NLS-1$
	
	// problem categories
	public static final String CAT_FATAL = "fatal"; //$NON-NLS-1$
	public static final String CAT_NLS = "nls"; //$NON-NLS-1$
	public static final String CAT_DEPRECATION = "deprecation"; //$NON-NLS-1$
	public static final String CAT_EE = "ee"; //$NON-NLS-1$
	public static final String CAT_OTHER = ""; //$NON-NLS-1$
	
	/**
	 * @see org.eclipse.pde.internal.core.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		return createMarker(file, NO_RESOLUTION, ""); //$NON-NLS-1$
	}
	
	public IMarker createMarker(IFile file, int id, String category) throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("id", id); //$NON-NLS-1$
		marker.setAttribute("categoryId", category); //$NON-NLS-1$
		return marker;
	}

}
