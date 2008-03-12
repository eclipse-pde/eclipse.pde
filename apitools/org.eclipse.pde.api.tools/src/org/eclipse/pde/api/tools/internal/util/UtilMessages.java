/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import org.eclipse.osgi.util.NLS;

public class UtilMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.util.utilmessages"; //$NON-NLS-1$
	public static String Util_0;
	public static String Util_2;
	public static String Util_3;
	public static String Util_4;
	public static String Util_5;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, UtilMessages.class);
	}

	private UtilMessages() {
	}
}
