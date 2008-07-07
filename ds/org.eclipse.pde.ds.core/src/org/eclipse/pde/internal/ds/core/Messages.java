/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/

package org.eclipse.pde.internal.ds.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ds.core.messages"; //$NON-NLS-1$

	public static String DSErrorReporter_cannotResolveResource;
	public static String DSErrorReporter_requiredElement;
	public static String DSErrorReporter_requiredAttribute;
	public static String DSErrorReporter_attrValue;
	
	public static String DSBuilder_verifying;
	public static String DSBuilder_updating;

	

	

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
