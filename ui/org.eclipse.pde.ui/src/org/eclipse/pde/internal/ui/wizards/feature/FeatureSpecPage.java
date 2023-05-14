/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.BundleProviderHistoryUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FeatureSpecPage extends AbstractFeatureSpecPage {

	private Combo fFeatureProviderCombo;
	private Text fFeatureIdText;

	public FeatureSpecPage() {
		super();
		setTitle(PDEUIMessages.NewFeatureWizard_SpecPage_title);
		setDescription(PDEUIMessages.NewFeatureWizard_SpecPage_desc);
	}

	@Override
	protected void initialize() {
		String projectName = getProjectName();
		if (fInitialId == null)
			fFeatureIdText.setText(IdUtil.getValidId(projectName));
		if (fInitialName == null)
			fFeatureNameText.setText(projectName);
		fFeatureVersionText.setText("1.0.0.qualifier"); //$NON-NLS-1$
		setMessage(PDEUIMessages.NewFeatureWizard_MainPage_desc);
	}

	@Override
	public FeatureData getFeatureData() {
		FeatureData data = new FeatureData();
		data.id = fFeatureIdText.getText();
		data.version = fFeatureVersionText.getText();
		data.provider = fFeatureProviderCombo.getText();
		data.name = fFeatureNameText.getText();
		data.library = getInstallHandlerLibrary();
		return data;
	}

	@Override
	protected String validateContent() {
		setMessage(null);
		return null;
	}

	@Override
	protected String getHelpId() {
		return IHelpContextIds.NEW_FEATURE_DATA;
	}

	@Override
	protected void createContents(Composite container) {
		Group group = new Group(container, SWT.NULL);
		group.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 10;
		group.setLayoutData(gd);
		group.setText(PDEUIMessages.BaseFeatureSpecPage_featurePropertiesGroup_title);

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_id);
		fFeatureIdText = new Text(group, SWT.BORDER);
		fFeatureIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createCommonInput(group);

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_provider);
		fFeatureProviderCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
		fFeatureProviderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		BundleProviderHistoryUtil.loadHistory(fFeatureProviderCombo, getDialogSettings());

		createInstallHandlerText(group);
	}

	@Override
	protected void attachListeners(ModifyListener listener) {
		fFeatureProviderCombo.addModifyListener(listener);
		fFeatureIdText.addModifyListener(listener);
	}

	@Override
	protected String getFeatureId() {
		return fFeatureIdText.getText();
	}

	@Override
	protected void updateNameRelativeFields() {
		if (fFeatureIdText == null || fFeatureNameText == null)
			return;
		fSelfModification = true;
		String id = IdUtil.getValidId(getProjectName());
		fFeatureIdText.setText(id);
		fFeatureNameText.setText(IdUtil.getValidName(id));
		if (0 == fFeatureProviderCombo.getText().length()) {
			fFeatureProviderCombo.setText(IdUtil.getValidProvider(id));
		}
		fSelfModification = false;
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		BundleProviderHistoryUtil.saveHistory(fFeatureProviderCombo, settings);
	}
}
