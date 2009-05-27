/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class JNLPTab {

	private static final String S_JNLP = "jnlp"; //$NON-NLS-1$
	private static final String S_URL = "siteURL"; //$NON-NLS-1$
	private static final String S_JRE = "jre"; //$NON-NLS-1$

	private Label fURLLabel;
	private Text fURLText;
	private Label fVersionLabel;
	private Text fVersionText;
	private Button fButton;

	private BaseExportWizardPage fPage;

	public JNLPTab(BaseExportWizardPage page) {
		fPage = page;
	}

	public Control createControl(Composite parent) {
		Composite jnlpGroup = new Composite(parent, SWT.NONE);
		jnlpGroup.setLayout(new GridLayout(2, false));
		jnlpGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fButton = new Button(jnlpGroup, SWT.CHECK);
		fButton.setText(PDEUIMessages.AdvancedFeatureExportPage_createJNLP);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fButton.setLayoutData(gd);
		fButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateGroup(fButton.getSelection());
				fPage.pageChanged();
			}
		});

		fURLLabel = createLabel(jnlpGroup, PDEUIMessages.AdvancedFeatureExportPage_siteURL);
		fURLText = createText(jnlpGroup);

		fVersionLabel = createLabel(jnlpGroup, PDEUIMessages.AdvancedFeatureExportPage_jreVersion);
		fVersionText = createText(jnlpGroup);

		Dialog.applyDialogFont(jnlpGroup);
		return jnlpGroup;
	}

	protected Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalIndent = 15;
		label.setLayoutData(gd);
		return label;
	}

	protected Text createText(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fPage.pageChanged();
			}
		});
		return text;
	}

	protected void initialize(IDialogSettings settings) {
		fURLText.setText(getString(settings, S_URL));
		fVersionText.setText(getString(settings, S_JRE));
		fButton.setSelection(settings.getBoolean(S_JNLP));
		updateGroup(fButton.getSelection());
	}

	private String getString(IDialogSettings settings, String key) {
		String value = settings.get(key);
		return value == null ? "" : value; //$NON-NLS-1$
	}

	private void updateGroup(boolean enabled) {
		fURLLabel.setEnabled(enabled);
		fURLText.setEnabled(enabled);
		fVersionLabel.setEnabled(enabled);
		fVersionText.setEnabled(enabled);
	}

	protected String validate() {
		String error = null;
		if (fButton.getSelection()) {
			if (fURLText.getText().trim().length() == 0) {
				error = PDEUIMessages.AdvancedFeatureExportPage_noSite;
			} else if (fVersionText.getText().trim().length() == 0) {
				error = PDEUIMessages.AdvancedFeatureExportPage_noVersion;
			}
		}
		return error;
	}

	protected void saveSettings(IDialogSettings settings) {
		settings.put(S_JNLP, fButton.getSelection());
		settings.put(S_URL, fURLText.getText());
		settings.put(S_JRE, fVersionText.getText());
	}

	protected String[] getJNLPInfo() {
		if (fButton.getSelection()) {
			return new String[] {fURLText.getText().trim(), fVersionText.getText().trim()};
		}
		return null;
	}
}
