/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.feature;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;


public class SiteHTMLPage extends WizardPage {
	private IConfigurationElement config;
	private boolean createSite = false;
	public static final String HTML_PAGE_TITLE = "NewSiteWizard.HTMLPage.title";
	public static final String HTML_PAGE_DESC = "NewSiteWizard.HTMLPage.desc";
	public static final String HTML_CHECK_TITLE = "NewSiteWizard.HTMLPage.checkTitle";
	public static final String HTML_CHECK_DESC = "NewSiteWizard.HTMLPage.checkDesc";
	private WizardNewProjectCreationPage mainPage;
	
	public SiteHTMLPage(WizardNewProjectCreationPage mainPage) {
		super("htmlPage");
		setTitle(PDEPlugin.getResourceString(HTML_PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(HTML_PAGE_DESC));
		this.mainPage = mainPage;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 9;
		container.setLayout(layout);

		GridData gd = new GridData();
		gd.horizontalSpan=2;
		gd.widthHint=500;
		Label description = new Label(container, SWT.WRAP);
		description.setText(PDEPlugin.getFormattedMessage(HTML_CHECK_TITLE, "index"));
		description.setLayoutData(gd);
		final Button htmlButton = new Button(container, SWT.CHECK);
		htmlButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				createSite = htmlButton.getSelection();
			}
		});
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getFormattedMessage(HTML_CHECK_DESC, "index"));
		setControl(container);
		Dialog.applyDialogFont(container);
//		WorkbenchHelp.setHelp(container, IHelpContextIds.{some help id here});
	}

	public boolean isCreateUpdateSiteHTML(){
		return createSite;
	}
	public void setInitializationData(
		IConfigurationElement config,
		String property,
		Object data)
		throws CoreException {
		this.config = config;
	}
}
