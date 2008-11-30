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
package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.osgi.util.NLS;

public class TocMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.editor.toc.messages"; //$NON-NLS-1$
	
	public static String TocEditor_link;
	public static String TocFileValidator_errorMessage1;
	public static String TocFileValidator_errorMessage2;
	public static String TocPage_errorMessage;
	public static String TocPage_errorMessage2;
	public static String TocPage_title;
	public static String TocSourcePage_title;
	public static String TocTreeSection_addAnchor;
	public static String TocTreeSection_addLink;
	public static String TocTreeSection_addTopic;
	public static String TocTreeSection_collapseAll;
	public static String TocTreeSection_down;
	public static String TocTreeSection_errorMessage1;
	public static String TocTreeSection_errorMessage2;
	public static String TocTreeSection_New;
	public static String TocTreeSection_open;
	public static String TocTreeSection_openFile;
	public static String TocTreeSection_openFileMessage;
	public static String TocTreeSection_openFileMessage2;
	public static String TocTreeSection_remove;
	public static String TocTreeSection_sectionDesc;
	public static String TocTreeSection_sectionText;
	public static String TocTreeSection_showIn;
	public static String TocTreeSection_topic;
	public static String TocTreeSection_up;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TocMessages.class);
	}

	private TocMessages() {
	}
}
