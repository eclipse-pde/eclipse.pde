/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public class DSMarkerFactory {
	
	public static final String MARKER_ID = "org.eclipse.pde.ds.core.problem"; //$NON-NLS-1$
	public static final int NO_RESOLUTION = -1;
	
	public static final String CAT_OTHER = ""; //$NON-NLS-1$

	/**
	 * @param file
	 * @return
	 * @throws CoreException
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		return createMarker(file, NO_RESOLUTION, ""); //$NON-NLS-1$
	}

	public IMarker createMarker(IFile file, int id, String category)
			throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("id", id); //$NON-NLS-1$
		marker.setAttribute("categoryId", category); //$NON-NLS-1$
		return marker;
	}
	
}
