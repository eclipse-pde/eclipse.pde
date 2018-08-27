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

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class BundleVendorHeader extends SingleManifestHeader {

	private static final long serialVersionUID = 1L;

	public BundleVendorHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public void setVendor(String vendor) {
		setMainComponent(vendor);
	}

	public String getVendor() {
		return getMainComponent();
	}

}
