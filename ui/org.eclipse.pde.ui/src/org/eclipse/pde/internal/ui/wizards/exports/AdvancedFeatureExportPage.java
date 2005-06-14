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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AdvancedFeatureExportPage extends AdvancedPluginExportPage {
	
	private static final String S_JNLP = "jnlp"; //$NON-NLS-1$
	private static final String S_URL = "siteURL"; //$NON-NLS-1$
	private static final String S_JRE = "jre"; //$NON-NLS-1$
	

	private Label fURLLabel;
	private Text fURLText;
	private Label fVersionLabel;
	private Text fVersionText;
	private Button fButton;
	private Group jnlpGroup;
	
	
	public AdvancedFeatureExportPage() {
		super("feature-sign"); //$NON-NLS-1$
	}
	
	protected String getDescriptionText() {
		return PDEUIMessages.AdvancedFeatureExportPage_desc; //$NON-NLS-1$
	}
	
	protected void createJNLPSection(Composite parent) {
		jnlpGroup = new Group(parent, SWT.NONE);
		jnlpGroup.setText(PDEUIMessages.AdvancedFeatureExportPage_jnlp); //$NON-NLS-1$
		jnlpGroup.setLayout(new GridLayout(2, false));
		jnlpGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fButton = createbutton(jnlpGroup, PDEUIMessages.AdvancedFeatureExportPage_createJNLP); //$NON-NLS-1$
		fButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fButton.getSelection();
				updateGroup(selected);
				validatePage();
			}
		});

		fURLLabel = createLabel(jnlpGroup, PDEUIMessages.AdvancedFeatureExportPage_siteURL);		 //$NON-NLS-1$
		fURLText = createText(jnlpGroup);
		fURLText.setText(getString(S_URL));
		
		fVersionLabel = createLabel(jnlpGroup, PDEUIMessages.AdvancedFeatureExportPage_jreVersion);	 //$NON-NLS-1$
		fVersionText = createText(jnlpGroup);
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
		forceValidatePage(false);
	}
	
	protected void forceValidatePage(boolean forceSuper) {
		if (forceSuper) super.forceValidatePage();
		else super.validatePage();
			
		if (isPageComplete()) {
			String error = null;
			
			if (jnlpGroup.getVisible() && fButton.getSelection()) {
				if (fURLText.getText().trim().length() == 0) {
					error = PDEUIMessages.AdvancedFeatureExportPage_noSite; //$NON-NLS-1$
				} else if (fVersionText.getText().trim().length() == 0) {
					error = PDEUIMessages.AdvancedFeatureExportPage_noVersion; //$NON-NLS-1$
				}
			}
			setErrorMessage(error);
			setPageComplete(error == null);
			// setPageComplete does not update buttons since we are checking a different page
			// and so we must updateButtons explicitly.
			if (forceSuper) getContainer().updateButtons();
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
		if (jnlpGroup.getVisible() && fButton.getSelection()) {
			return new String[] { fURLText.getText().trim(),
					fVersionText.getText().trim() };
		}
		return null;
	}

	public void hideJNLP(boolean hide) {
        jnlpGroup.setVisible(!hide);
        fButton.setEnabled(!hide);
    }
}
