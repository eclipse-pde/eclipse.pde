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

import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;
import org.osgi.framework.VersionRange;

public class FragmentHostHeader extends SingleManifestHeader {

	private static final long serialVersionUID = 1L;

	public FragmentHostHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public void setHostId(String id) {
		setMainComponent(id);
	}

	public String getHostId() {
		return getMainComponent();
	}

	public void setHostRange(String range) {
		setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, range);
	}

	public VersionRange getHostRange() {
		return Utils.parseVersionRange(getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));
	}

}
