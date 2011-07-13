/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public abstract class AbstractFeatureSpecPage extends WizardNewProjectCreationPage {

	protected Text fFeatureNameText;
	protected Text fFeatureVersionText;
	protected Text fLibraryText;
	protected String fInitialId;
	protected String fInitialName;
	protected IFeatureModel fFeatureToPatch;
	protected boolean fSelfModification;
	private boolean fUpdateName = true;

	public AbstractFeatureSpecPage() {
		super("specPage"); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite comp = (Composite) getControl();

		createContents(comp);

		initialize();
		attachListeners();

		Dialog.applyDialogFont(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, getHelpId());
	}

	protected abstract void createContents(Composite container);

	protected abstract void initialize();

	protected abstract void attachListeners(ModifyListener listener);

	protected abstract String getHelpId();

	protected abstract void saveSettings(IDialogSettings settings);

	protected void createCommonInput(Composite common) {
		Label label = new Label(common, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_name);
		fFeatureNameText = new Text(common, SWT.BORDER);
		fFeatureNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(common, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_version);
		fFeatureVersionText = new Text(common, SWT.BORDER);
		fFeatureVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	protected void createInstallHandlerText(Composite parent) {
		Label libraryLabel = new Label(parent, SWT.NULL);
		libraryLabel.setText(PDEUIMessages.NewFeatureWizard_SpecPage_library);
		fLibraryText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		fLibraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	protected abstract void updateNameRelativeFields();

	protected boolean validatePage() {
		boolean valid = super.validatePage();
		if (!valid)
			return valid;
		if (fUpdateName)
			updateNameRelativeFields();
		return validateBaseContent(false);
	}

	private boolean validateBaseContent(boolean validateSuper) {
		if (validateSuper && !super.validatePage())
			return false;
		if (!setValidationMessage(verifyIdRules()))
			return false;
		if (!setValidationMessage(verifyVersion()))
			return false;
		if (!setValidationMessage(validateContent()))
			return false;

		setPageComplete(true);
		setErrorMessage(null);
		return true;
	}

	private boolean setValidationMessage(String message) {
		if (message == null)
			return true;
		setPageComplete(false);
		setErrorMessage(message);
		return false;
	}

	protected abstract String validateContent();

	public String getInitialName() {
		return fInitialName;
	}

	public void setInitialName(String initialName) {
		fInitialName = initialName;
	}

	public void setInitialId(String initialId) {
		fInitialId = initialId;
	}

	public String getInitialId() {
		return fInitialId;
	}

	protected String verifyVersion() {
		String value = fFeatureVersionText.getText();
		if (VersionUtil.validateVersion(value).getSeverity() != IStatus.OK)
			return PDEUIMessages.NewFeatureWizard_SpecPage_versionFormat;
		return null;
	}

	protected abstract String getFeatureId();

	protected String verifyIdRules() {
		String id = getFeatureId();
		if (id == null || id.length() == 0)
			return PDEUIMessages.NewFeatureWizard_SpecPage_missing;
		if (!IdUtil.isValidCompositeID(id)) {
			return PDEUIMessages.NewFeatureWizard_SpecPage_invalidId;
		}
		return null;
	}

	public IFeatureModel getFeatureToPatch() {
		return fFeatureToPatch;
	}

	protected String getInstallHandlerLibrary() {
		String library = fLibraryText.getText();
		if (library == null || library.length() == 0)
			return null;
		if (!library.endsWith(".jar") && !library.endsWith("/") && !library.equals(".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			library += "/"; //$NON-NLS-1$
		return library;
	}

	private void attachListeners() {
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fSelfModification) {
					fUpdateName = false;
					setPageComplete(validateBaseContent(true));
				}
			}
		};
		attachListeners(listener);
		fFeatureNameText.addModifyListener(listener);
		fFeatureVersionText.addModifyListener(listener);
		fLibraryText.addModifyListener(listener);
	}

	public abstract FeatureData getFeatureData();
}
