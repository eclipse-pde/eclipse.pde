/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class RefactoringMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.api.tools.ui.internal.refactoring.refactoringmessages"; //$NON-NLS-1$
	public static String CreateFileChange_0;
	public static String CreateFileChange_1;
	public static String CreateFileChange_2;
	public static String CreateFileChange_3;
	public static String FilterChange_add_filter;
	public static String FilterChange_remove_used_filter;
	public static String FilterDeleteParticipant_remove_unused_filters_for_0;
	public static String RefactoringUtils_remove_usused_filters;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, RefactoringMessages.class);
	}

	private RefactoringMessages() {
	}
}
