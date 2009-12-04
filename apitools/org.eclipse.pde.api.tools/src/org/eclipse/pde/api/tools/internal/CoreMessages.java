/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
	public static String ApiBaseline_0;
	public static String ApiBaseline_1;
	public static String ApiBaseline_2;
	public static String ApiBaseline_3;
	public static String ApiBaseline_4;
	public static String ApiBaseline_5;
	public static String ProjectComponent_could_not_locate_model;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
