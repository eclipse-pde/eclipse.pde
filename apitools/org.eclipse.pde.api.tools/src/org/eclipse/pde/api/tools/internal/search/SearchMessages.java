/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
	public static String ApiSearchEngine_api;
	public static String ApiSearchEngine_api_internal;
	public static String ApiSearchEngine_extracting_refs_from;
	public static String ApiSearchEngine_internal;
	public static String ApiSearchEngine_searching_for_use_from;
	public static String ApiSearchEngine_searching_project;
	public static String ApiSearchEngine_searching_projects;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SearchMessages.class);
	}

	private SearchMessages() {
	}
}
