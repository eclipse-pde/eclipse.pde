/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.exports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class ProductExportWizardPage extends AbstractExportWizardPage {

	private static final String S_SYNC_PRODUCT = "syncProduct"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE = "exportSource"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE_FORMAT = "exportSourceFormat"; //$NON-NLS-1$
	private static final String S_ALLOW_BINARY_CYCLES = "allowBinaryCycles"; //$NON-NLS-1$
	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$
	private static final String S_EXPORT_METADATA = "p2metadata"; //$NON-NLS-1$

	private Button fSyncButton;
	private final IStructuredSelection fSelection;
	private ProductDestinationGroup fExportGroup;
	private ProductConfigurationSection fConfigurationGroup;
	private Button fExportSourceButton;
	private Combo fExportSourceCombo;
	private Button fMultiPlatform;
	private Button fExportMetadata;
	private Button fAllowBinaryCycles;
	private boolean fPageInitialized = false;

	public ProductExportWizardPage(IStructuredSelection selection) {
		super("productExport"); //$NON-NLS-1$
		fSelection = selection;
		setTitle(PDEUIMessages.ProductExportWizardPage_title);
		setDescription(PDEUIMessages.ProductExportWizardPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		createConfigurationSection(container);
		createSynchronizationSection(container);
		createDestinationSection(container);
		createOptionsSection(container);

		initialize();
		pageChanged();

		setControl(container);
		hookHelpContext(container);
		Dialog.applyDialogFont(container);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PRODUCT_EXPORT_WIZARD);
		fPageInitialized = true;
	}

	private void createConfigurationSection(Composite parent) {
		fConfigurationGroup = new ProductConfigurationSection(this);
		fConfigurationGroup.createControl(parent);
	}

	private void createSynchronizationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 7;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.ProductExportWizardPage_sync);

		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.ProductExportWizardPage_syncText);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		label.setLayoutData(gd);

		fSyncButton = new Button(group, SWT.CHECK);
		fSyncButton.setText(PDEUIMessages.ProductExportWizardPage_syncButton);
		gd = new GridData();
		gd.horizontalIndent = 20;
		fSyncButton.setLayoutData(gd);
	}

	private void createDestinationSection(Composite container) {
		fExportGroup = new ProductDestinationGroup(this);
		fExportGroup.createControl(container);
	}

	private void createOptionsSection(Composite container) {
		Group group = new Group(container, SWT.NONE);
		group.setText(PDEUIMessages.ProductExportWizardPage_exportOptionsGroup);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite composite = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fExportSourceButton = new Button(composite, SWT.CHECK);
		fExportSourceButton.setText(PDEUIMessages.ExportWizard_includeSource);

		fExportSourceCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		fExportSourceCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		fExportMetadata = new Button(group, SWT.CHECK);
		fExportMetadata.setText(PDEUIMessages.ExportWizard_includesMetadata);

		if (getWizard().getPages().length > 1) {
			fMultiPlatform = new Button(group, SWT.CHECK);
			fMultiPlatform.setText(PDEUIMessages.ExportWizard_multi_platform);
			fMultiPlatform.addSelectionListener(widgetSelectedAdapter(e -> getContainer().updateButtons()));
		}

		fAllowBinaryCycles = new Button(group, SWT.CHECK);
		fAllowBinaryCycles.setText(PDEUIMessages.ExportOptionsTab_allowBinaryCycles);
	}

	protected void initialize() {
		IDialogSettings settings = getDialogSettings();
		fConfigurationGroup.initialize(fSelection, settings);

		String value = settings.get(S_SYNC_PRODUCT);
		fSyncButton.setSelection(value == null ? true : settings.getBoolean(S_SYNC_PRODUCT));

		fExportGroup.initialize(settings, fConfigurationGroup.getProductFile());

		fExportSourceButton.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		fExportSourceCombo.setItems(new String[] {PDEUIMessages.ExportWizard_generateAssociatedSourceBundles, PDEUIMessages.ExportWizard_includeSourceInBinaryBundles});
		String sourceComboValue = settings.get(S_EXPORT_SOURCE_FORMAT) != null ? settings.get(S_EXPORT_SOURCE_FORMAT) : PDEUIMessages.ExportWizard_generateAssociatedSourceBundles;
		fExportSourceCombo.setText(sourceComboValue);
		fExportSourceCombo.setEnabled(fExportSourceButton.getSelection());

		String selected = settings.get(S_EXPORT_METADATA);
		fExportMetadata.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));

		selected = settings.get(S_ALLOW_BINARY_CYCLES);
		fAllowBinaryCycles.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));

		if (fMultiPlatform != null)
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));

		hookListeners();
	}

	protected void hookListeners() {
		fExportSourceButton.addSelectionListener(widgetSelectedAdapter(e -> fExportSourceCombo.setEnabled(fExportSourceButton.getSelection())));
	}

	protected void updateProductFields() {
		fExportGroup.updateDestination(fConfigurationGroup.getProductFile());
	}

	@Override
	protected void pageChanged() {
		if (getMessage() != null)
			setMessage(null);
		String error = fConfigurationGroup.validate();
		if (error == null)
			error = fExportGroup.validate();
		if (fPageInitialized)
			setErrorMessage(error);
		else
			setMessage(error);
		setPageComplete(error == null);
	}

	@Override
	public IWizardPage getNextPage() {
		return doMultiPlatform() ? getWizard().getNextPage(this) : null;
	}

	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PRODUCT_EXPORT_WIZARD);
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		fConfigurationGroup.saveSettings(settings);
		settings.put(S_SYNC_PRODUCT, fSyncButton.getSelection());
		fExportGroup.saveSettings(settings);
		settings.put(S_EXPORT_SOURCE, doExportSource());
		settings.put(S_EXPORT_SOURCE_FORMAT, fExportSourceCombo.getItem(fExportSourceCombo.getSelectionIndex()));
		settings.put(S_EXPORT_METADATA, doExportMetadata());
		settings.put(S_ALLOW_BINARY_CYCLES, doBinaryCycles());

		if (fMultiPlatform != null)
			settings.put(S_MULTI_PLATFORM, fMultiPlatform.getSelection());
	}

	protected boolean doSync() {
		return fSyncButton.getSelection();
	}

	protected boolean doMultiPlatform() {
		return fMultiPlatform != null && fMultiPlatform.getSelection();
	}

	protected boolean doExportSource() {
		return fExportSourceButton.getSelection();
	}

	protected boolean doExportSourceBundles() {
		return PDEUIMessages.ExportWizard_generateAssociatedSourceBundles.equals(fExportSourceCombo.getText());
	}

	protected boolean doBinaryCycles() {
		return fAllowBinaryCycles.getSelection();
	}

	/**
	 * @return whether to generate p2 metadata on export
	 */
	protected boolean doExportMetadata() {
		return fExportMetadata.getSelection();
	}

	protected boolean doExportToDirectory() {
		return fExportGroup.doExportToDirectory();
	}

	protected String getFileName() {
		return fExportGroup.getFileName();
	}

	protected String getDestination() {
		return fExportGroup.getDestination();
	}

	protected String getRootDirectory() {
		return fConfigurationGroup.getRootDirectory();
	}

	protected IFile getProductFile() {
		return fConfigurationGroup.getProductFile();
	}

}
