/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SchemaMarkerFactory implements IMarkerFactory {
	public static final String MARKER_ID = "org.eclipse.pde.validation-marker";
	private String point;
	
	public SchemaMarkerFactory() {
	}

	public SchemaMarkerFactory(String point) {
		this.point = point;
	}
	
	public void setPoint(String point) {
		this.point = point;
	}

	/**
	 * @see org.eclipse.pde.internal.builders.IMarkerFactory#createMarker(org.eclipse.core.resources.IFile)
	 */
	public IMarker createMarker(IFile file) throws CoreException {
		IMarker marker = file.createMarker(MARKER_ID);
		marker.setAttribute("point", point);
		return marker;
	}
}
