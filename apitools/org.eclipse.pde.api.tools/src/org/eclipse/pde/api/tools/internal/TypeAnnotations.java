/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public int getVisibility() {
		return fAnnotations.getVisibility();
	}

	@Override
	public int getRestrictions() {
		return fAnnotations.getRestrictions();
	}

}
