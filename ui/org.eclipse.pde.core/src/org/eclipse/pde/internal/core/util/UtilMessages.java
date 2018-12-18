/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
 *     Les Jones <lesojones@gmail.com> - Bug 214457
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import org.eclipse.osgi.util.NLS;

public class UtilMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.core.util.UtilMessages"; //$NON-NLS-1$
	public static String ErrorReadingManifest;
	public static String ErrorManifestFileAbsent;
	public static String ErrorReadingOldStyleManifest;
	public static String VMHelper_noJreForExecEnv;
	public static String BundleErrorReporter_InvalidFormatInBundleVersion;
	public static String BundleErrorReporter_invalidVersionRangeFormat;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, UtilMessages.class);
	}

	private UtilMessages() {
	}
}
