/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM - Further improvements
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Wrapper containing a java package and the installable unit that represents it in the
 * p2 metadata.
 * 
 * @since 3.6
 */
public class IUPackage {

	private final IInstallableUnit iu;
	private final String packageName;
	private final Version version;

	/**
	 * Creates a new wrapper
	 * 
	 * @param packageName name of the package, must not be <code>null</code>
	 * @param version version of the package, must not be <code>null</code>
	 * @param iu installable unit associated with this package, must not be <code>null</code>
	 */
	public IUPackage(String packageName, Version version, IInstallableUnit iu) {
		this.packageName = packageName;
		this.version = version;
		this.iu = iu;
	}

	/**
	 * @return the iu representing this package
	 */
	public IInstallableUnit getIU() {
		return iu;
	}

	/**
	 * @return the version of this package
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * @return the id of this package
	 */
	public String getId() {
		return packageName;
	}

}
