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
package org.eclipse.pde.core.project;

import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * Describes a package import. Instances of this class can be created
 * via {@link IBundleProjectService#newPackageImport(String, VersionRange, boolean)}.
 *
 * @since 3.6
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPackageImportDescription {

	/**
	 * Returns the fully qualified name of the imported package.
	 *
	 * @return fully qualified name of the imported package
	 */
	public String getName();

	/**
	 * Returns the version constraint of the imported package or <code>null</code>
	 * if unspecified.
	 *
	 * @return version constraint or <code>null</code>
	 */
	public VersionRange getVersionRange();

	/**
	 * Returns whether the package import is optional.
	 *
	 * @return whether optional
	 */
	public boolean isOptional();

}
