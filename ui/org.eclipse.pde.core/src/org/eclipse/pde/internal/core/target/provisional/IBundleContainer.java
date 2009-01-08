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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;

/**
 * A collection of bundles. A bundle container abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.5
 */
public interface IBundleContainer {

	/**
	 * Resolves and returns the executable bundles in this container, possibly empty.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @return executable bundles
	 * @exception CoreException if unable to resolve bundles
	 */
	public BundleInfo[] resolveBundles(IProgressMonitor monitor) throws CoreException;

	/**
	 * Resolves and returns the source bundles in this container, possibly empty.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @return source bundles
	 * @exception CoreException if unable to resolve bundles
	 */
	public BundleInfo[] resolveSourceBundles(IProgressMonitor monitor) throws CoreException;

}
