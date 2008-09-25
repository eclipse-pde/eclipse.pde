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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.osgi.util.NLS;
/**
 * 
 */
public class MarkerMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.ui.internal.markers.markermessages"; //$NON-NLS-1$
	public static String AddNoReferenceTagResolution_add_noreference_tag;
	public static String CreateApiFilterOperation_0;
	public static String DefaultApiProfileResolution_0;
	public static String DefaultApiProfileResolution_1;
	public static String DefaultApiProfileResolution_2;
	public static String FilterProblemResolution_0;
	public static String FilterProblemResolution_compatible;
	public static String FilterProblemResolution_default_profile;
	public static String FilterProblemResolution_since_tag;
	public static String FilterProblemResolution_usage;
	public static String FilterProblemResolution_version_number;
	public static String RemoveUnsupportedTagOperation_removeing_unsupported_tag;

	public static String SinceTagResolution_missing0;
	public static String SinceTagResolution_missing1;
	public static String SinceTagResolution_missing2;

	public static String SinceTagResolution_malformed0;
	public static String SinceTagResolution_malformed1;
	public static String SinceTagResolution_malformed2;

	public static String SinceTagResolution_invalid0;
	public static String SinceTagResolution_invalid1;
	public static String SinceTagResolution_invalid2;

	public static String UnsupportedTagResolution_remove_unsupported_tag;
	public static String UpdateSinceTagOperation_title;

	public static String VersionNumberingResolution_major0;
	public static String VersionNumberingResolution_minor0;
	public static String VersionNumberingResolution_major1;
	public static String VersionNumberingResolution_minor1;
	public static String VersionNumberingResolution_major2;
	public static String VersionNumberingResolution_minor2;
	public static String VersionNumberingResolution_minorNoNewAPI0;
	public static String VersionNumberingResolution_minorNoNewAPI1;
	public static String VersionNumberingResolution_minorNoNewAPI2;

	public static String VersionNumberingResolution_reexportedMajor0;
	public static String VersionNumberingResolution_reexportedMajor1;
	public static String VersionNumberingResolution_reexportedMajor2;
	public static String VersionNumberingResolution_reexportedMinor0;
	public static String VersionNumberingResolution_reexportedMinor1;
	public static String VersionNumberingResolution_reexportedMinor2;
	
	public static String UpdateVersionNumberingOperation_title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MarkerMessages.class);
	}

	private MarkerMessages() {
	}
}
