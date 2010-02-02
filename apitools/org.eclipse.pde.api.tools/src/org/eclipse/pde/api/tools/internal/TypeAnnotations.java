/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;

/**
 * Extends annotations with relative build time stamp
 */
public class TypeAnnotations implements IApiAnnotations {
	
	private IApiAnnotations fAnnotations;
	private long fBuildStamp;

	/**
	 * @param visibility
	 * @param restrictions
	 */
	public TypeAnnotations(IApiAnnotations annotations, long stamp) {
		fAnnotations = annotations;
		fBuildStamp = stamp;
	}
	
	public long getBuildStamp() {
		return fBuildStamp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations#getVisibility()
	 */
	public int getVisibility() {
		return fAnnotations.getVisibility();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations#getRestrictions()
	 */
	public int getRestrictions() {
		return fAnnotations.getRestrictions();
	}

}
