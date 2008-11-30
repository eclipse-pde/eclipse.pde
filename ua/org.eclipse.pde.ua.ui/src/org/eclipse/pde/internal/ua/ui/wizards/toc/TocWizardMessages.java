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
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import org.eclipse.osgi.util.NLS;

public class TocWizardMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.ua.ui.wizards.toc.messages"; //$NON-NLS-1$
	
	public static String NewTocFileWizard_title;
	public static String RegisterTocOperation_errorMessage1;
	public static String RegisterTocOperation_errorMessage2;
	public static String RegisterTocOperation_task;
	public static String RegisterTocOperation_task2;
	public static String RegisterTocOperation_task3;
	public static String RegisterTocOperation_task4;
	public static String RegisterTocWizard_link;
	public static String RegisterTocWizardPage_description;
	public static String RegisterTocWizardPage_makePrimary;
	public static String RegisterTocWizardPage_title;
	public static String TocHTMLWizard_description;
	public static String TocHTMLWizard_title;
	public static String TocHTMLWizardPage_errorMessage1;
	public static String TocHTMLWizardPage_errorMessage2;
	public static String TocOperation_topic;
	public static String TocWizardPage_description;
	public static String TocWizardPage_errorMessage;
	public static String TocWizardPage_name;
	public static String TocWizardPage_title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TocWizardMessages.class);
	}

	private TocWizardMessages() {
	}
}
