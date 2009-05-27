/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

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
		return new VersionRange(getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));
	}

}
