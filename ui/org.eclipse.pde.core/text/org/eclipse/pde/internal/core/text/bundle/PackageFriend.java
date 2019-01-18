/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text.bundle;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.bundle.BundleObject;

public class PackageFriend extends BundleObject {

	private static final long serialVersionUID = 1L;

	private final String fName;

	private transient PackageObject fPackageObject;

	public PackageFriend(PackageObject object, String name) {
		fName = name;
		fPackageObject = object;
	}

	public String getName() {
		return fName;
	}

	@Override
	public String toString() {
		return fName;
	}

	public ManifestHeader getHeader() {
		return fPackageObject.getHeader();
	}

	/**
	 * @param object
	 */
	public void reconnect(PackageObject object) {
		fPackageObject = object;
	}

	@Override
	public void writeDelimeter(PrintWriter writer) {
		writer.print(',');
	}

}
