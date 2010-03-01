/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.search.dialogs;

import org.eclipse.equinox.p2.metadata.*;

public class IUPackage implements IVersionedId {

	private final IInstallableUnit iu;
	private final String packageName;
	private final Version version;

	public IUPackage(IInstallableUnit iu, String packageName, Version version) {
		this.iu = iu;
		this.packageName = packageName;
		this.version = version;
	}

	/**
	 * @return the iu
	 */
	public IInstallableUnit getIU() {
		return iu;
	}

	/**
	 * @return the version
	 */
	public Version getVersion() {
		return version;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.metadata.IVersionedId#getId()
	 */
	public String getId() {
		return packageName;
	}

}
