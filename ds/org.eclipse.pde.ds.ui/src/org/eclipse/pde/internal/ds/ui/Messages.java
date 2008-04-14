/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String DSPage_title;

	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ds.ui.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
