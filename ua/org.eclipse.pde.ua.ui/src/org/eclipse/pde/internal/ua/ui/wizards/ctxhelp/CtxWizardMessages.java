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
package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import org.eclipse.osgi.util.NLS;

public class CtxWizardMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.wizards.ctxhelp.messages"; //$NON-NLS-1$
	
	public static String NewCtxHelpOperation_context;
	public static String NewCtxHelpOperation_topic;
	public static String NewCtxHelpWizard_title;
	public static String NewCtxHelpWizardPage_description;
	public static String NewCtxHelpWizardPage_title;
	public static String RegisterCtxHelpOperation_errorMessage1;
	public static String RegisterCtxHelpOperation_errorMessage2;
	public static String RegisterCtxHelpOperation_task;
	public static String RegisterCtxHelpWizard_pageMessage;
	public static String RegisterCtxHelpWizard_pageTitle;
	public static String RegisterCtxHelpWizard_plugin;
	public static String RegisterCtxHelpWizard_pluginDesc;
	public static String RegisterCtxHelpWizard_title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CtxWizardMessages.class);
	}

	private CtxWizardMessages() {
	}
}
