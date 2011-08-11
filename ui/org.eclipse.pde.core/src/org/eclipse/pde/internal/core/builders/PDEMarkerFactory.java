/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 214156
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public class PDEMarkerFactory {

	public static final String MARKER_ID = "org.eclipse.pde.core.problem"; //$NON-NLS-1$

	public static final int NO_RESOLUTION = -1;

	// manifest source fixes
	public static final int M_DEPRECATED_AUTOSTART = 0x1001; // other problem
	public static final int M_JAVA_PACKAGE__PORTED = 0x1002; // fatal error
	public static final int M_SINGLETON_DIR_NOT_SET = 0x1003; // other problem
	public static final int M_SINGLETON_ATT_NOT_SET = 0x1004; // other problem
	public static final int M_PROJECT_BUILD_ORDER_ENTRIES = 0x1005;
	public static final int M_EXPORT_PKG_NOT_EXIST = 0x1006; // other problem
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
	public static final int M_EXECUTION_ENVIRONMENT_NOT_SET = 0x1020; // other problem
	public static final int M_MISSING_BUNDLE_CLASSPATH_ENTRY = 0x1021; // fatal problem
	public static final int M_LAZYLOADING_HAS_NO_EFFECT = 0x1022; //other problem
	public static final int M_DISCOURAGED_CLASS = 0x1023; //other problem
	public static final int M_NO_LINE_TERMINATION = 0x1024; // fatal problem
	public static final int M_R4_SYNTAX_IN_R3_BUNDLE = 0x1025; // other problem

	// build properties fixes
	public static final int B_APPEND_SLASH_FOLDER_ENTRY = 0x2001;
	public static final int B_REMOVE_SLASH_FILE_ENTRY = 0x2002;
	public static final int B_ADDITION = 0x2003;
	public static final int B_SOURCE_ADDITION = 0x2004;
	public static final int B_REMOVAL = 0x2005;
	public static final int B_REPLACE = 0x2006;
	public static final int B_JAVA_ADDDITION = 0x2007;

	// plugin.xml fixes
	public static final int P_ILLEGAL_XML_NODE = 0x3001;
	public static final int P_UNTRANSLATED_NODE = 0x3002;
	public static final int P_UNKNOWN_CLASS = 0x3003;
	public static final int P_USELESS_FILE = 0x3004;

	// marker attribute keys
	public static final String BK_BUILD_ENTRY = "buildEntry.key"; //$NON-NLS-1$
	public static final String BK_BUILD_TOKEN = "buildEntry.tokenValue"; //$NON-NLS-1$
	public static final String MPK_LOCATION_PATH = "xmlTree.locationPath"; //$NON-NLS-1$
	public static final String ATTR_CAN_ADD = "deprecatedAutostart.canAdd"; //$NON-NLS-1$
	public static final String ATTR_HEADER = "deprecatedAutostart.header"; //$NON-NLS-1$
	/**
	 * Boolean attribute for marker added when no newline is found at the end of a manifest. Value is
	 * <code>true</code> if there is character content on the last line that should be
	 * saved or <code>false</code> if the line only contains whitespace characters.
	 */
	public static final String ATTR_HAS_CONTENT = "noLineTermination.hasContent"; //$NON-NLS-1$

	// problem categories
	public static final String CAT_FATAL = "fatal"; //$NON-NLS-1$
	public static final String CAT_NLS = "nls"; //$NON-NLS-1$
	public static final String CAT_DEPRECATION = "deprecation"; //$NON-NLS-1$
	public static final String CAT_EE = "ee"; //$NON-NLS-1$
	public static final String CAT_OTHER = ""; //$NON-NLS-1$
	public static final String CAT_ID = "categoryId"; //$NON-NLS-1$

	public IMarker createMarker(IFile file, int id, String category) throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("id", id); //$NON-NLS-1$
		marker.setAttribute(CAT_ID, category);
		return marker;
	}

}
