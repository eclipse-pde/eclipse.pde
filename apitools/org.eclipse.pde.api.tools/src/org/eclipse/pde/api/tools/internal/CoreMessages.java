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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.osgi.util.NLS;

/**
 * 
 */
public class CoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.coremessages"; //$NON-NLS-1$
	public static String JavadocTagManager_class;
	public static String JavadocTagManager_extended;
	public static String JavadocTagManager_field;
	public static String JavadocTagManager_implemented;
	public static String JavadocTagManager_instantiated;
	public static String JavadocTagManager_interface;
	public static String JavadocTagManager_method;
	public static String JavadocTagManager_not_intended_to_be;
	public static String JavadocTagManager_referenced;
	public static String JavadocTagManager_subclassed;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
