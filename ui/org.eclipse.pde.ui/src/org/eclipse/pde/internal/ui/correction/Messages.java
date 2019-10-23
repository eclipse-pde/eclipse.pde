/*******************************************************************************
 *  Copyright (c) 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrew Obuchowicz <aobuchow@redhat.com>
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ui.correction.messages"; //$NON-NLS-1$
	public static String ReplaceExecEnvironment_Marker_Label;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
