/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.provisional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

/**
 * A resolved bundle contained in a bundle container of a target definition.
 * A resolved bundle is created by resolving a bundle container or target
 * definition.
 * 
 * @since 3.5
 */
public interface IResolvedBundle {

	/**
	 * Status code indicating that a required bundle does not exist.
	 */
	public static final int STATUS_DOES_NOT_EXIST = 100;

	/**
	 * Status code indicating that a required bundle version does not exist (a bundle
	 * with the correct symbolic name is present, but the specified version was not
	 * found).
	 */
	public static final int STATUS_VERSION_DOES_NOT_EXIST = 101;

	/**
	 * Status code indicating that a bundle's manifest could not be read, or did not exist. 
	 */
	public static final int STATUS_INVALID_MANIFEST = 102;

	/**
	 * Returns the underlying bundle this resolution describes.
	 * 
	 * @return the underlying bundle this resolution describes
	 */
	public BundleInfo getBundleInfo();

	/**
	 * Returns <code>true</code> if this bundle is a source bundle and 
	 * <code>false</code> if this bundle is an executable bundle.
	 * 
	 * @return whether the resolved bundle is a source bundle
	 */
	public boolean isSourceBundle();

	/**
	 * Returns the resolution status of this bundle.
	 * 
	 * @return resolution status
	 */
	public IStatus getStatus();

	/**
	 * Returns whether this bundle is optional in its target definition.
	 * Returns <code>true</code> if optional and <code>false</code> if it is required.
	 *  
	 * @return whether this bundle is optional in its target definition
	 */
	public boolean isOptional();

	/**
	 * Returns whether this bundle is a fragment.
	 * 
	 * @return whether this bundle is a fragment
	 */
	public boolean isFragment();
}
