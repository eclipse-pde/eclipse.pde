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

import java.util.Set;
import java.util.SortedSet;

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
	 * @since 3.19
	 */
	String name();

	/** @deprecated Instead use {@link #name()} */
	@Deprecated(since = "3.19")
	default String getName() {
		return name();
	}

	/**
	 * Returns the version of the exported package or <code>null</code> if
	 * unspecified.
	 *
	 * @return version or <code>null</code>
	 * @since 3.19
	 */
	Version version();

	/** @deprecated Instead use {@link #version()} */
	@Deprecated(since = "3.19")
	default Version getVersion() {
		return version();
	}

	/**
	 * Returns the declared friends of this package.
	 *
	 * @return friends as bundle symbolic names, may be empty
	 * @since 3.19
	 */
	SortedSet<String> friends();

	/** @deprecated Instead use {@link #friends()} */
	@Deprecated(since = "3.19")
	default String[] getFriends() {
		Set<String> friends = friends();
		return friends.isEmpty() ? null : friends.toArray(String[]::new);
	}

	/**
	 * Returns whether the package is exported as API, or is internal.
	 *
	 * @return whether the package is exported as API
	 */
	boolean isApi();
}
