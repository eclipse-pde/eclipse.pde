/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public static String ErrorReadingOldStyleManifest;
	public static String ManifestUtils_NeedCompatFragmentToConvertManifest;
	public static String ManifestUtils_PluginConverterOnlyAvailableWithOSGi;
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
