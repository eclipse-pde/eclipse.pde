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
package org.eclipse.pde.api.tools.ui.internal.properties;

import org.eclipse.osgi.util.NLS;

public class PropertiesMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.ui.internal.properties.propertiesmessages"; //$NON-NLS-1$
	public static String ApiErrorWarningsPropertyPage_0;
	public static String ApiErrorWarningsPropertyPage_1;
	public static String ApiFiltersPropertyPage_55;
	public static String ApiFiltersPropertyPage_57;
	public static String ApiFiltersPropertyPage_58;
	public static String ApiFiltersPropertyPage_59;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PropertiesMessages.class);
	}

	private PropertiesMessages() {
	}
}
