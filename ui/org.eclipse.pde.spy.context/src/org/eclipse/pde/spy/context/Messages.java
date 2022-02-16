/*******************************************************************************
 * Copyright (c) 2022 OPCoach and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #577963)
 *******************************************************************************/
package org.eclipse.pde.spy.context;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String ContextSpyPart_10;
	public static String ContextSpyPart_11;
	public static String ContextSpyPart_4;
	public static String ContextSpyPart_5;
	public static String ContextSpyPart_6;
	public static String ContextSpyPart_7;
	public static String ContextSpyPart_8;
	public static String ContextSpyPart_9;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
