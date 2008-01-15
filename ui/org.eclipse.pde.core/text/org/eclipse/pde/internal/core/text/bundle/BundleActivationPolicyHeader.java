/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public class BundleActivationPolicyHeader extends LazyStartHeader {

	private static final long serialVersionUID = 1L;

	public BundleActivationPolicyHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public boolean isLazyStart() {
		return Constants.ACTIVATION_LAZY.equals(getMainComponent());
	}

	public void setLazyStart(boolean lazy) {
		setMainComponent(lazy ? Constants.ACTIVATION_LAZY : null);
	}

	// Need to overwrite the write() method incase user has directives.  If we did not, we would continue to write directives even when we aren't lazy starting 
	public String write() {
		if (isLazyStart())
			return super.write();
		return ""; //$NON-NLS-1$
	}

}
