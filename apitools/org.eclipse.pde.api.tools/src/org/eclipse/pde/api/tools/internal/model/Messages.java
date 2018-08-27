/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.internal.model.Messages"; //$NON-NLS-1$
	public static String adding_component__0;
	public static String ApiType_0;
	public static String ApiType_1;
	public static String ApiType_2;
	public static String ApiType_3;
	public static String ApiScope_0;
	public static String BundleApiComponent_baseline_disposed;
	public static String BundleComponent_failed_to_lookup_fragment;
	public static String configuring_baseline;
	public static String resolving_target_definition;
	public static String TypeStructureBuilder_badClassFileEncountered;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
