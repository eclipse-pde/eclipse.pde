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

import java.io.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;


public class SiteHTMLPage extends WizardPage {
	private boolean createSite = false;
	
	public static final String HTML_PAGE_TITLE = "SiteHTMLPage.HTMLPage.title";
	public static final String HTML_PAGE_DESC = "SiteHTMLPage.HTMLPage.desc";
	public static final String HTML_CHECK_LABEL = "SiteHTMLPage.HTMLPage.checkLabel";
	public static final String HTML_PLUGIN_LABEL = "SiteHTMLPage.HTMLPage.pluginLabel";
	public static final String HTML_FEATURE_LABEL = "SiteHTMLPage.HTMLPage.featureLabel";
	public static final String HTML_WEB_LABEL = "SiteHTMLPage.HTMLPage.webLabel";
	public static final String WEB_ERR = "SiteHTMLPage.HTMLPage.webError";
	public static final String PLUGIN_ERR = "SiteHTMLPage.HTMLPage.pluginError";
	public static final String FEATURE_ERR = "SiteHTMLPage.HTMLPage.featureError";
	private static final int SIZING_TEXT_FIELD_WIDTH = 350;
	protected Text pluginText;
	protected Text chkText;
	protected Text featureText;
	protected Text webText;
		
	private Listener textModifyListener = new Listener(){
		public void handleEvent(Event e){
			setPageComplete(validatePage());
			setErrorMessage(getStatusString());
		}
	};
	
	public SiteHTMLPage(WizardNewProjectCreationPage mainPage) {
		super("htmlPage");
		setTitle(PDEPlugin.getResourceString(HTML_PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(HTML_PAGE_DESC));
	}
	public void createControl(Composite parent){
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		
		initializeDialogUnits(parent);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		createSettingsGroup(composite);
		setPageComplete(validatePage());
		setControl(composite);
		Dialog.applyDialogFont(composite);
		
	}
	public void createSettingsGroup(Composite container) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		Label pluginLabel = new Label(container, SWT.NONE);
		pluginLabel.setText(PDEPlugin.getResourceString(HTML_PLUGIN_LABEL));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		pluginLabel.setLayoutData(gd);
		pluginText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		pluginText.setLayoutData(gd);
		pluginText.setText("plugins");
		pluginText.addListener(SWT.Modify, textModifyListener);
		
		Label featureLabel = new Label(container, SWT.NONE);
		featureLabel.setText(PDEPlugin.getResourceString(HTML_FEATURE_LABEL));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureLabel.setLayoutData(gd);
		featureText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		featureText.setLayoutData(gd);
		featureText.setText("features");
		featureText.addListener(SWT.Modify, textModifyListener);
		
		Label separator = new Label(container, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		separator.setLayoutData(gd);
		
		final Button htmlButton = new Button(container, SWT.CHECK | SWT.RIGHT);
		htmlButton.setText(PDEPlugin.getResourceString(HTML_CHECK_LABEL));
		gd = new GridData();
		gd.horizontalSpan=2;
		htmlButton.setLayoutData(gd);
		
		final Label webLabel = new Label(container, SWT.NULL);
		webLabel.setText(PDEPlugin.getResourceString(HTML_WEB_LABEL));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		webLabel.setLayoutData(gd);
		webText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		webText.setLayoutData(gd);
		webText.setText("web");
		webText.setEnabled(createSite);
		webLabel.setEnabled(createSite);
		webText.addListener(SWT.Modify, textModifyListener);

		htmlButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				createSite = htmlButton.getSelection();
				webLabel.setEnabled(createSite);
				webText.setEnabled(createSite);
				setPageComplete(validatePage());
				setErrorMessage(getStatusString());
			}
		});
	}

	public String getStatusString(){
		if (getPluginLocation().equals(""))
			return PDEPlugin.getResourceString(PLUGIN_ERR);
		else if (getFeaturesLocation().equals(""))
			return PDEPlugin.getResourceString(FEATURE_ERR);
		else if (createSite && getWebLocation().equals(""))
			return PDEPlugin.getResourceString(WEB_ERR);
		
		return null;
	}
	public boolean isCreateUpdateSiteHTML(){
		return createSite;
	}
	
	public String getPluginLocation(){
		String text = pluginText.getText();
		if (text.startsWith(File.separator) || text.startsWith("/"))
			text = text.substring(1);
		if (text.endsWith(File.separator) || text.endsWith("/"))
			text= text.substring(0,text.length()-1);
		return text;
	}
	
	public String getFeaturesLocation(){
		String text = featureText.getText();
		if (text.startsWith(File.separator) || text.startsWith("/"))
			text = text.substring(1);
		if (text.endsWith(File.separator) || text.endsWith("/"))
			text= text.substring(0,text.length()-1);
		return text;
	}
	
	public String getWebLocation(){
		String text = webText.getText();
		if (text.startsWith(File.separator) || text.startsWith("/"))
			text = text.substring(1);
		if (text.endsWith(File.separator) || text.endsWith("/"))
			text= text.substring(0,text.length()-1);
		return text;
	}

	protected boolean validatePage() {
		if (getPluginLocation().equals("") || getFeaturesLocation().equals(""))
			return false;
		
		if (createSite && getWebLocation().equals(""))
			return false;
		
		return true;
	}
}
