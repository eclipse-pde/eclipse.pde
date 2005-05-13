/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class AdvancedPluginExportPage extends ExportWizardPage {

	private static final String S_SIGN_JARS = "signJAR"; //$NON-NLS-1$
	private static final String S_KEYSTORE = "keystore"; //$NON-NLS-1$
	private static final String S_ALIAS = "alias"; //$NON-NLS-1$
	private static final String S_PASSWORD = "password"; //$NON-NLS-1$
	
	private Button fButton;
	private Label fKeystoreLabel;
	private Text fKeystoreText;
	private Label fAliasLabel;
	private Text fAliasText;
	private Label fPasswordLabel;
	private Text fPasswordText;

	public AdvancedPluginExportPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.AdvancedPluginExportPage_title); //$NON-NLS-1$
		setDescription(getDescriptionText()); //$NON-NLS-1$
	}
	
	protected String getDescriptionText() {
		return PDEUIMessages.AdvancedPluginExportPage_desc; //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 15;
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		
		createSigningSection(container);
		createJNLPSection(container);
		
		Dialog.applyDialogFont(container);
		validatePage();
		setControl(container);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.ADVANCED_PLUGIN_EXPORT);
	}
	
	private void createSigningSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.AdvancedPluginExportPage_signJar); //$NON-NLS-1$
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		IDialogSettings settings = getDialogSettings();
		
		fButton = createbutton(group, PDEUIMessages.AdvancedPluginExportPage_signButton); //$NON-NLS-1$
		fButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fButton.getSelection();
				updateGroup(selected);
				validatePage();
			}
		});
		
		fKeystoreLabel = createLabel(group, PDEUIMessages.AdvancedPluginExportPage_keystore);	 //$NON-NLS-1$
		fKeystoreText = createText(group);
		fKeystoreText.setText(getString(S_KEYSTORE));
		
		fAliasLabel = createLabel(group, PDEUIMessages.AdvancedPluginExportPage_alias); //$NON-NLS-1$
		fAliasText = createText(group);
		fAliasText.setText(getString(S_ALIAS));
		
		fPasswordLabel = createLabel(group, PDEUIMessages.AdvancedPluginExportPage_password);	 //$NON-NLS-1$
		fPasswordText = createText(group);
		fPasswordText.setEchoChar('*');
		fPasswordText.setText(getString(S_PASSWORD));

		fButton.setSelection(settings.getBoolean(S_SIGN_JARS));
		updateGroup(fButton.getSelection());
	}
	
	protected String getString(String key) {
		String value = getDialogSettings().get(key);
		return value == null ? "" : value; //$NON-NLS-1$
	}
	
	protected Button createbutton(Composite group, String text) {
		Button button = new Button(group, SWT.CHECK);
		button.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		button.setLayoutData(gd);
		return button;
	}
	
	protected Label createLabel(Composite group, String text) {
		Label label = new Label(group, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 30;
		label.setLayoutData(gd);
		return label;
	}
	
	protected Text createText(Composite group) {
		Text text = new Text(group, SWT.SINGLE|SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage();
			}
		});
		return text;
	}
	
	protected void createJNLPSection(Composite parent) {		
	}
	
	protected void validatePage() {
		if (!isCurrentPage())
			return;
		String error = null;
		if (fButton.getSelection()) {
			if (fKeystoreText.getText().trim().length() == 0) {
				error = PDEUIMessages.AdvancedPluginExportPage_noKeystore; //$NON-NLS-1$
			} else if (fAliasText.getText().trim().length() == 0) {
				error = PDEUIMessages.AdvancedPluginExportPage_noAlias; //$NON-NLS-1$
			} else if (fPasswordText.getText().trim().length() == 0) {
				error = PDEUIMessages.AdvancedPluginExportPage_noPassword; //$NON-NLS-1$
			}
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}
	
	private void updateGroup(boolean enabled) {
		fKeystoreLabel.setEnabled(enabled);
		fKeystoreText.setEnabled(enabled);
		fAliasLabel.setEnabled(enabled);
		fAliasText.setEnabled(enabled);
		fPasswordLabel.setEnabled(enabled);
		fPasswordText.setEnabled(enabled);
	}
	
	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_SIGN_JARS, fButton.getSelection());
		settings.put(S_KEYSTORE, fKeystoreText.getText().trim());
		settings.put(S_ALIAS, fAliasText.getText().trim());
		settings.put(S_PASSWORD, fPasswordText.getText().trim());
	}
	
	public String[] getSigningInfo() {
		if (fButton.getSelection()) {
			return new String[] { fAliasText.getText().trim(),
					fKeystoreText.getText().trim(),
					fPasswordText.getText().trim() };
		}
		return null;
	}
	
	public String[] getJNLPInfo() {
		return null;
	}
	
}
