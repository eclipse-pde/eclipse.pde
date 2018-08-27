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
 *     Chris Aniszczyk <zx@code9.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.messages"; //$NON-NLS-1$
	public static String CompCSFileValidator_0;
	public static String CompCSMasterTreeSection_addGroup;
	public static String CompCSMasterTreeSection_addTask;
	public static String CompCSMasterTreeSection_collapseAll;
	public static String CompCSMasterTreeSection_content;
	public static String CompCSMasterTreeSection_Content;
	public static String CompCSMasterTreeSection_Down;
	public static String CompCSMasterTreeSection_new;
	public static String CompCSMasterTreeSection_Preview;
	public static String CompCSMasterTreeSection_Remove;
	public static String CompCSMasterTreeSection_sectionDesc;
	public static String CompCSMasterTreeSection_Up;
	public static String CompCSPage_definition;
	public static String CompCSPage_error;
	public static String CompCSPage_loadFailure;
	public static String CompCSGroupValidator_errorChildlessGroup;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
