/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.p2inf;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String P2InfHeader_provides;
	public static String P2InfHeader_requires;
	public static String P2InfHeader_metaRequirements;
	public static String P2InfHeader_properties;
	public static String P2InfHeader_update;
	public static String P2InfHeader_instructions;
	public static String P2InfHeader_units;

	public static String P2InfHeader_filter;
	public static String P2InfHeader_greedy;
	public static String P2InfHeader_matchExp;
	public static String P2InfHeader_multiple;
	public static String P2InfHeader_name;
	public static String P2InfHeader_namespace;
	public static String P2InfHeader_optional;
	public static String P2InfHeader_range;
	public static String P2InfHeader_version;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
