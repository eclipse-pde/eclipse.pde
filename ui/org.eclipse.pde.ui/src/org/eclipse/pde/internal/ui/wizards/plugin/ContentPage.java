/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.pde.internal.ui.wizards.BundleProviderHistoryUtil;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public abstract class ContentPage extends WizardPage {

	protected boolean fInitialized = false;
	protected Text fIdText;
	protected Text fVersionText;
	protected Text fNameText;
	protected Combo fProviderCombo;

	protected NewProjectCreationPage fMainPage;
	protected AbstractFieldData fData;
	protected IProjectProvider fProjectProvider;

	protected final static int PROPERTIES_GROUP = 1;

	protected int fChangedGroups = 0;

	protected ModifyListener propertiesListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			if (fInitialized)
				fChangedGroups |= PROPERTIES_GROUP;
			validatePage();
		}
	};

	public ContentPage(String pageName, IProjectProvider provider, NewProjectCreationPage page, AbstractFieldData data) {
		super(pageName);
		fMainPage = page;
		fProjectProvider = provider;
		fData = data;
	}

	protected Text createText(Composite parent, ModifyListener listener) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(listener);
		return text;
	}

	protected Text createText(Composite parent, ModifyListener listener, int horizSpan) {
		Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = horizSpan;
		text.setLayoutData(data);
		text.addModifyListener(listener);
		return text;
	}

	protected Combo createProviderCombo(Composite parent, ModifyListener listener, int horizSpan) {
		Combo combo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = horizSpan;
		combo.setLayoutData(data);
		BundleProviderHistoryUtil.loadHistory(combo, getDialogSettings());
		// Add listener only now, otherwise combo.select(0) would trigger it
		// and cause a NPE during validation.
		combo.addModifyListener(listener);
		return combo;
	}

	protected abstract void validatePage();

	protected String validateProperties() {

		if (!fInitialized) {
			if (!fIdText.getText().trim().equals(fProjectProvider.getProjectName())) {
				setMessage(PDEUIMessages.ContentPage_illegalCharactersInID, INFORMATION);
			} else {
				setMessage(null);
			}
			return null;
		}

		setMessage(null);
		String errorMessage = null;

		// Validate ID
		errorMessage = validateId();
		if (errorMessage != null) {
			return errorMessage;
		}

		// Validate Version
		errorMessage = validateVersion(fVersionText);
		if (errorMessage != null) {
			return errorMessage;
		}

		return null;
	}

	/**
	 * @param text
	 */
	protected String validateVersion(Text text) {
		if (text.getText().trim().length() == 0) {
			return PDELabelUtility.qualifyMessage(PDELabelUtility.getFieldLabel(text), PDEUIMessages.ControlValidationUtility_errorMsgValueMustBeSpecified);
		} else if (!isVersionValid(text.getText().trim())) {
			return PDELabelUtility.qualifyMessage(PDELabelUtility.getFieldLabel(text), PDECoreMessages.BundleErrorReporter_InvalidFormatInBundleVersion);
		}
		return null;
	}

	private String validateId() {
		String id = fIdText.getText().trim();
		if (id.length() == 0)
			return PDEUIMessages.ContentPage_noid;

		if (!IdUtil.isValidCompositeID3_0(id)) {
			return PDEUIMessages.ContentPage_invalidId;
		}
		return null;
	}

	protected boolean isVersionValid(String version) {
		return VersionUtil.validateVersion(version).getSeverity() == IStatus.OK;
	}

	public IWizardPage getNextPage() {
		updateData();
		return super.getNextPage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			String id = computeId();
			// properties group
			if ((fChangedGroups & PROPERTIES_GROUP) == 0) {
				int oldfChanged = fChangedGroups;
				fIdText.setText(id);
				fVersionText.setText("1.0.0.qualifier"); //$NON-NLS-1$
				fNameText.setText(IdUtil.getValidName(id));
				if (0 == fProviderCombo.getText().length()) {
					fProviderCombo.setText(IdUtil.getValidProvider(id));
				}
				fChangedGroups = oldfChanged;
			}
			if (fInitialized)
				validatePage();
			else
				fInitialized = true;
		}
		super.setVisible(visible);
	}

	protected String computeId() {
		return IdUtil.getValidId(fProjectProvider.getProjectName());
	}

	public void saveSettings(IDialogSettings settings) {
		BundleProviderHistoryUtil.saveHistory(fProviderCombo, settings);
	}

	public void updateData() {
		fData.setId(fIdText.getText().trim());
		fData.setVersion(fVersionText.getText().trim());
		fData.setName(fNameText.getText().trim());
		fData.setProvider(fProviderCombo.getText().trim());
	}

	public IFieldData getData() {
		return fData;
	}

	public String getId() {
		return fIdText.getText().trim();
	}
}
