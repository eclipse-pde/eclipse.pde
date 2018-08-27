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

public class LazyStartHeader extends SingleManifestHeader {

	private static final long serialVersionUID = 1L;

	public LazyStartHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public boolean isLazyStart() {
		return "true".equals(getMainComponent()); //$NON-NLS-1$
	}

	public void setLazyStart(boolean lazy) {
		setMainComponent(Boolean.toString(lazy));
	}

}
