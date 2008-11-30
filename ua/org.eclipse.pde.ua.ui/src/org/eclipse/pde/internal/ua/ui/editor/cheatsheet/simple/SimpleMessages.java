/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@code9.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.osgi.util.NLS;

public class SimpleMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.messages"; //$NON-NLS-1$
	public static String SimpleCSDefinitionPage_errorMessage;
	public static String SimpleCSDefinitionPage_loadFailure;
	public static String SimpleCSDefinitionPage_title;
	public static String SimpleCSMasterTreeSection_addStep;
	public static String SimpleCSMasterTreeSection_addSubStep;
	public static String SimpleCSMasterTreeSection_collapseAll;
	public static String SimpleCSMasterTreeSection_descriptionText1;
	public static String SimpleCSMasterTreeSection_descriptionText2;
	public static String SimpleCSMasterTreeSection_down;
	public static String SimpleCSMasterTreeSection_new;
	public static String SimpleCSMasterTreeSection_preview;
	public static String SimpleCSMasterTreeSection_remove;
	public static String SimpleCSMasterTreeSection_sectionDescription;
	public static String SimpleCSMasterTreeSection_sectionTitle;
	public static String SimpleCSMasterTreeSection_up;
	public static String SimpleCSSourcePage_title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SimpleMessages.class);
	}

	private SimpleMessages() {
	}
}
