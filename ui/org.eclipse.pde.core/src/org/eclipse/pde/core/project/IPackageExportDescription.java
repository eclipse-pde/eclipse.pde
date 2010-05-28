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

import org.osgi.framework.Version;

/**
 * Describes a package export. Instances of this class can be created
 * via {@link IBundleProjectService#newPackageExport(String, Version, boolean, String[])}.
 * 
 * @since 3.6
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPackageExportDescription {

	/**
	 * Returns the fully qualified name of the exported package.
	 * 
	 * @return fully qualified name of the exported package
	 */
	public String getName();

	/**
	 * Returns the version of the exported package or <code>null</code>
	 * if unspecified.
	 * 
	 * @return version or <code>null</code>
	 */
	public Version getVersion();

	/**
	 * Returns the declared friends of this package or <code>null</code> if none.
	 *  
	 * @return friends as bundle symbolic names or <code>null</code>
	 */
	public String[] getFriends();

	/**
	 * Returns whether the package is exported as API, or is internal.
	 * 
	 * @return whether the package is exported as API
	 */
	public boolean isApi();
}
