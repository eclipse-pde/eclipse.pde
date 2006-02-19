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
	public static final int DEPRECATED_AUTOSTART = 1;
	public static final int JAVA_PACKAGE__PORTED = 2;
	public static final int SINGLETON_DIR_NOT_SET = 3;
	public static final int SINGLETON_ATT_NOT_SET = 4;
	public static final int PROJECT_BUILD_ORDER_ENTRIES = 5;
	public static final int EXPORT_PKG_NOT_EXIST = 6; 
	public static final int IMPORT_PKG_NOT_AVAILABLE = 7;
	public static final int REQ_BUNDLE_NOT_AVAILABLE = 8;
	
	/**
	 * @see org.eclipse.pde.internal.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		return createMarker(file, NO_RESOLUTION);
	}
	
	/**
	 * @see org.eclipse.pde.internal.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file, int id) throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("id", id); //$NON-NLS-1$
		return marker;
	}

}
