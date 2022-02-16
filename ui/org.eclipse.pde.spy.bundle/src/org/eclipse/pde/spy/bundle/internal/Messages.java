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
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #577962)
 *******************************************************************************/
package org.eclipse.pde.spy.bundle.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String BundleDataProvider_1;
	public static String BundleDataProvider_2;
	public static String BundleDataProvider_3;
	public static String BundleDataProvider_4;
	public static String BundleDataProvider_5;
	public static String BundleDataProvider_6;
	public static String BundleDataProvider_7;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
