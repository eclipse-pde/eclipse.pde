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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.BundleProviderHistoryUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class PatchSpecPage extends AbstractFeatureSpecPage {

	private Combo fPatchProviderCombo;
	private Button fBrowseButton;
	private Text fPatchIdText;
	private Text fPatchNameText;
	private Text fFeatureIdText;

	public PatchSpecPage() {
		super();
		setTitle(PDEUIMessages.PatchSpec_title);
		setDescription(PDEUIMessages.NewFeatureWizard_SpecPage_desc);
	}

	@Override
	protected void initialize() {
		String projectName = getProjectName();
		if (fInitialId == null)
			fPatchIdText.setText(IdUtil.getValidId(projectName));
		if (fInitialName == null)
			fPatchNameText.setText(projectName);
		setMessage(PDEUIMessages.FeaturePatch_MainPage_desc);
	}

	@Override
	protected String validateContent() {
		fFeatureToPatch = PDECore.getDefault().getFeatureModelManager().findFeatureModel(fFeatureIdText.getText(), fFeatureVersionText.getText());
		if (fFeatureToPatch != null) {
			setMessage(null);
			return null;
		}

		setMessage(NLS.bind(PDEUIMessages.NewFeaturePatch_SpecPage_notFound, fFeatureIdText.getText(), fFeatureVersionText.getText()), IMessageProvider.WARNING);
		getContainer().updateButtons();
		return null;
	}

	@Override
	public IWizardPage getNextPage() {
		if (fFeatureToPatch == null)
			return null;
		return super.getNextPage();
	}

	private String getPatchId() {
		if (fPatchIdText == null)
			return ""; //$NON-NLS-1$
		return fPatchIdText.getText();
	}

	private String getPatchName() {
		if (fPatchNameText == null)
			return ""; //$NON-NLS-1$
		return fPatchNameText.getText();
	}

	private String getPatchProvider() {
		if (fPatchProviderCombo == null)
			return ""; //$NON-NLS-1$
		return fPatchProviderCombo.getText();
	}

	@Override
	public FeatureData getFeatureData() {
		FeatureData data = new FeatureData();
		data.id = getPatchId();
		data.version = "1.0.0"; //$NON-NLS-1$
		data.provider = getPatchProvider();
		data.name = getPatchName();
		data.library = getInstallHandlerLibrary();
		data.isPatch = true;
		data.featureToPatchId = fFeatureIdText.getText();
		data.featureToPatchVersion = fFeatureVersionText.getText();
		return data;
	}

	@Override
	protected String verifyIdRules() {
		String id = fPatchIdText.getText();
		if (id == null || id.length() == 0)
			return PDEUIMessages.NewFeatureWizard_SpecPage_pmissing;
		if (!IdUtil.isValidCompositeID(id)) {
			return PDEUIMessages.NewFeatureWizard_SpecPage_invalidId;
		}
		return super.verifyIdRules();
	}

	@Override
	protected String getHelpId() {
		return IHelpContextIds.NEW_PATCH_REQUIRED_DATA;
	}

	protected void createTopGroup(Composite container) {
		Group patchGroup = new Group(container, SWT.NULL);
		patchGroup.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 10;
		patchGroup.setLayoutData(gd);
		patchGroup.setText(PDEUIMessages.NewFeatureWizard_SpecPage_patchProperties);
		Label label = new Label(patchGroup, SWT.NULL);
		label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_id);
		fPatchIdText = new Text(patchGroup, SWT.BORDER);
		fPatchIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(patchGroup, SWT.NULL);
		label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_name);
		fPatchNameText = new Text(patchGroup, SWT.BORDER);
		fPatchNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(patchGroup, SWT.NULL);
		label.setText(PDEUIMessages.NewFeaturePatch_SpecPage_provider);
		fPatchProviderCombo = new Combo(patchGroup, SWT.BORDER | SWT.DROP_DOWN);
		fPatchProviderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		BundleProviderHistoryUtil.loadHistory(fPatchProviderCombo, getDialogSettings());

		createInstallHandlerText(patchGroup);
	}

	@Override
	protected void createContents(Composite container) {

		createTopGroup(container);

		Group group = new Group(container, SWT.NULL);
		group.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 10;
		group.setLayoutData(gd);
		group.setText(PDEUIMessages.BaseFeatureSpecPage_patchGroup_title);

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_id);

		Composite patchcontainer = new Composite(group, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		patchcontainer.setLayout(layout);
		patchcontainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fFeatureIdText = new Text(patchcontainer, SWT.BORDER);
		fFeatureIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseButton = new Button(patchcontainer, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.BaseFeatureSpecPage_browse);
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(getShell(), PDECore.getDefault().getFeatureModelManager().getModels(), false);
			dialog.create();
			if (dialog.open() == Window.OK) {
				Object[] result = dialog.getResult();
				IFeatureModel selectedModel = (IFeatureModel) result[0];

				// block auto validation till last setText
				fSelfModification = true;
				fFeatureIdText.setText(selectedModel.getFeature().getId());
				fFeatureNameText.setText(selectedModel.getFeature().getLabel());
				fSelfModification = false;
				fFeatureVersionText.setText(selectedModel.getFeature().getVersion());

				fFeatureToPatch = selectedModel;
			}
		}));
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		createCommonInput(group);
	}

	@Override
	protected void attachListeners(ModifyListener listener) {
		fPatchIdText.addModifyListener(listener);
		fPatchNameText.addModifyListener(listener);
		fPatchProviderCombo.addModifyListener(listener);
		fFeatureIdText.addModifyListener(listener);
	}

	@Override
	protected String getFeatureId() {
		return fFeatureIdText.getText();
	}

	@Override
	protected void updateNameRelativeFields() {
		if (fPatchIdText == null || fPatchNameText == null)
			return;
		fSelfModification = true;
		String id = IdUtil.getValidId(getProjectName());
		fPatchIdText.setText(id);
		fPatchNameText.setText(IdUtil.getValidName(id));
		if (0 == fPatchProviderCombo.getText().length()) {
			fPatchProviderCombo.setText(IdUtil.getValidProvider(id));
		}
		fSelfModification = false;
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		BundleProviderHistoryUtil.saveHistory(fPatchProviderCombo, settings);
	}
}
