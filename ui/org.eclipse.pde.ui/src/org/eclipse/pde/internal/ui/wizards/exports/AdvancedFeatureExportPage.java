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

import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class AdvancedFeatureExportPage extends AdvancedPluginExportPage {
	
	private static final String S_JNLP = "jnlp"; //$NON-NLS-1$
	private static final String S_URL = "siteURL"; //$NON-NLS-1$
	private static final String S_JRE = "jre"; //$NON-NLS-1$
	

	private Label fURLLabel;
	private Text fURLText;
	private Label fVersionLabel;
	private Text fVersionText;
	private Button fButton;

	public AdvancedFeatureExportPage(String pageName) {
		super(pageName);
	}
	
	protected void createJNLPSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("AdvancedFeatureExportPage.jnlp")); //$NON-NLS-1$
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fButton = createbutton(group, PDEPlugin.getResourceString("AdvancedFeatureExportPage.createJNLP")); //$NON-NLS-1$
		fButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fButton.getSelection();
				updateGroup(selected);
				validatePage();
			}
		});

		fURLLabel = createLabel(group, PDEPlugin.getResourceString("AdvancedFeatureExportPage.siteURL"));		 //$NON-NLS-1$
		fURLText = createText(group);
		fURLText.setText(getString(S_URL));
		
		fVersionLabel = createLabel(group, PDEPlugin.getResourceString("AdvancedFeatureExportPage.jreVersion"));	 //$NON-NLS-1$
		fVersionText = createText(group);
		fVersionText.setText(getString(S_JRE));
		
		fButton.setSelection(getDialogSettings().getBoolean(S_JNLP));
		updateGroup(fButton.getSelection());
	}
	
	private void updateGroup(boolean enabled) {
		fURLLabel.setEnabled(enabled);
		fURLText.setEnabled(enabled);
		fVersionLabel.setEnabled(enabled);
		fVersionText.setEnabled(enabled);
	}
	
	protected void validatePage() {
		if (!isCurrentPage())
			return;
		super.validatePage();
		if (isPageComplete()) {
			String error = null;
			if (fButton.getSelection()) {
				if (fURLText.getText().trim().length() == 0) {
					error = PDEPlugin.getResourceString("AdvancedFeatureExportPage.noSite"); //$NON-NLS-1$
				} else if (fVersionText.getText().trim().length() == 0) {
					error = PDEPlugin.getResourceString("AdvancedFeatureExportPage.noVersion"); //$NON-NLS-1$
				}
			}
			setErrorMessage(error);
			setPageComplete(error == null);
		}
	}
	
	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_JNLP, fButton.getSelection());
		settings.put(S_URL, fURLText.getText());
		settings.put(S_JRE, fVersionText.getText());
		super.saveSettings();
	}
	
	public String[] getJNLPInfo() {
		if (fButton.getSelection()) {
			return new String[] { fURLText.getText().trim(),
					fVersionText.getText().trim() };
		}
		return null;
	}

}
