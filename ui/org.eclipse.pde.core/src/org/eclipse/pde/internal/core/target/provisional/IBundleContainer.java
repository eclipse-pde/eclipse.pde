/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.provisional;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

/**
 * A collection of bundles. A bundle container abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.5
 */
public interface IBundleContainer {

	/**
	 * Resolves all bundles in this container in the context of the specified
	 * target and returns a status describing the resolution.
	 * 
	 * @param definition target being resolved for
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolution status
	 * @exception CoreException if unable to resolve this container
	 */
	public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns whether this container has resolved all of its contained bundles.
	 * 
	 * @return whether this container has resolved all of its contained bundles
	 */
	public boolean isResolved();

	/**
	 * Returns all bundles in this container or <code>null</code> if this container is
	 * not resolved.
	 * 
	 * @return resolved bundles or <code>null</code>
	 */
	public IResolvedBundle[] getBundles();

	/**
	 * Restricts the bundles in this container to the explicit set of bundles specified or
	 * all bundles in this container when <code>null</code>. When this container resolves
	 * bundles and source bundles the result will be limited to those bundles matching the
	 * bundles specified.
	 * <p>
	 * When bundle versions are specified, resolving will enforce version matching. When
	 * bundle versions are unspecified, resolving will select the bundle with the highest
	 * version identifier and matching symbolic name.
	 * </p>
	 * @param bundles bundle restriction or <code>null</code> for all bundles
	 */
	public void setIncludedBundles(BundleInfo[] bundles);

	/**
	 * Returns the explicit list of bundles to be included in the bundle container.  Any 
	 * bundles in the container that are not included in this list will not be included 
	 * in the result of {@link #resolveBundles(IProgressMonitor)}. This method will return 
	 * <code>null</code> if an explicit list has not been set, all bundles will be included
	 * in the container.
	 * 
	 * @return list of included bundles set or <code>null</code> if all bundles included
	 */
	public BundleInfo[] getIncludedBundles();

	/**
	 * Describes a set of bundles in this container that are optional. During resolution,
	 * missing optional bundles are not considered as resolution errors.
	 * 
	 * @param bundles optional bundles or <code>null</code>
	 */
	public void setOptionalBundles(BundleInfo[] bundles);

	/**
	 * Returns bundles that are optionally part of this container or <code>null</code> if none.
	 * 
	 * @return optional bundles or <code>null</code>
	 */
	public BundleInfo[] getOptionalBundles();

}
