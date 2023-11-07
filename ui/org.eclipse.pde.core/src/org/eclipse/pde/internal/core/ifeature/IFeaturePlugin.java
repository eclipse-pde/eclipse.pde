/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;

/**
 * A base class for plug-in and data entires
 */
public interface IFeaturePlugin extends IFeatureObject, IVersionable, IFeatureEntry {
	/**
	 * A property that will be carried by the change event
	 * if 'unpack' field of this object is changed.
	 */
	public static final String P_UNPACK = "unpack"; //$NON-NLS-1$

	/**
	 * Returns whether this is a reference to a fragment.
	 * @return <samp>true</samp> if this is a fragment, <samp>false</samp> otherwise.
	 */
	public boolean isFragment();

	public boolean isUnpack();

	public void setUnpack(boolean unpack) throws CoreException;
}
