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
package org.eclipse.pde.core.project;

import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * Describes a required bundle. Instances of this class can be created
 * via {@link IBundleProjectService#newRequiredBundle(String, VersionRange, boolean, boolean)}.
 * 
 * @since 3.6
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRequiredBundleDescription {

	/**
	 * Returns the symbolic name of the required bundle.
	 * 
	 * @return symbolic name of the required bundle
	 */
	public String getName();

	/**
	 * Returns the version constraint of the required bundle or <code>null</code>
	 * if unspecified.
	 * 
	 * @return version constraint or <code>null</code>
	 */
	public VersionRange getVersionRange();

	/**
	 * Returns whether the required bundle is re-exported.
	 * 
	 * @return whether re-exported
	 */
	public boolean isExported();

	/**
	 * Returns whether the required bundle is optional.
	 * 
	 * @return whether optional
	 */
	public boolean isOptional();

}
