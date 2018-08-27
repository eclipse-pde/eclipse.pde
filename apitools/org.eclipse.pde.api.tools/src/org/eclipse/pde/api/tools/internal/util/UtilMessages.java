/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import org.eclipse.osgi.util.NLS;

public class UtilMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.util.utilmessages"; //$NON-NLS-1$
	public static String Util_0;
	public static String Util_4;
	public static String Util_5;
	public static String Util_6;
	public static String Util_builder_errorMessage;
	public static String Util_couldNotFindFilterFile;
	public static String Util_problemWithFilterFile;
	public static String comparison_invalidRegularExpression;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, UtilMessages.class);
	}

	private UtilMessages() {
	}
}
