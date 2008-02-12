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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.osgi.util.NLS;

/**
 * 
 */
public class SearchMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.search.searchmessages"; //$NON-NLS-1$
	public static String SearchEngine_0;
	public static String SearchEngine_1;
	public static String SearchEngine_2;
	public static String SearchEngine_3;
	public static String SearchEngine_4;
	public static String SearchEngine_5;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
	}

	private SearchMessages() {
	}
}
