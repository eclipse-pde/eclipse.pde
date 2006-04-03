/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

public class ProductExportWizardPage extends AbstractExportWizardPage {
	
	private static final String S_SYNC_PRODUCT = "syncProduct"; //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE = "exportSource"; //$NON-NLS-1$
	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$

	private Button fSyncButton;
	private IStructuredSelection fSelection;
	private ProductDestinationGroup fExportGroup;
	private ProductConfigurationSection fConfigurationGroup;
	private Button fExportSource;
	private Button fMultiPlatform;

	public ProductExportWizardPage(IStructuredSelection selection) {
		super("productExport"); //$NON-NLS-1$
		fSelection = selection;
		setTitle(PDEUIMessages.ProductExportWizardPage_title); 
		setDescription(PDEUIMessages.ProductExportWizardPage_desc); 
	}

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
		if (getErrorMessage() != null) {
			setMessage(getErrorMessage());
			setErrorMessage(null);
		}
		setControl(container);
		hookHelpContext(container);
		Dialog.applyDialogFont(container);				
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
		
		fExportSource = new Button(group, SWT.CHECK);
		fExportSource.setText(PDEUIMessages.ExportWizard_includeSource);
		
		if (getWizard().getPages().length > 1) {
			fMultiPlatform = new Button(group, SWT.CHECK);
			fMultiPlatform.setText(PDEUIMessages.ExportWizard_multi_platform);
			fMultiPlatform.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					getContainer().updateButtons();
				}
			});
		}
	}

	protected void initialize() {
		IDialogSettings settings = getDialogSettings();
		fConfigurationGroup.initialize(fSelection, settings);
		
		String value = settings.get(S_SYNC_PRODUCT);
		fSyncButton.setSelection(value == null ? true : settings.getBoolean(S_SYNC_PRODUCT));
		
		fExportGroup.initialize(settings, fConfigurationGroup.getProductFile());
		
		fExportSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		if (fMultiPlatform != null)
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));
	}
	
	protected void updateProductFields() {
		fExportGroup.updateDestination(fConfigurationGroup.getProductFile());
	}

	protected void pageChanged() {
		String error = fConfigurationGroup.validate();
		if (error == null)
			error = fExportGroup.validate();
		setErrorMessage(error);
		setPageComplete(error == null);
	}
	
	public IWizardPage getNextPage() {
		return doMultiPlatform() ? getWizard().getNextPage(this) : null;
	}
	
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PRODUCT_EXPORT_WIZARD);
	}
	
	protected void saveSettings(IDialogSettings settings) {
		fConfigurationGroup.saveSettings(settings);
		settings.put(S_SYNC_PRODUCT, fSyncButton.getSelection());
		fExportGroup.saveSettings(settings);
		settings.put(S_EXPORT_SOURCE, doExportSource());
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
		return fExportSource.getSelection();
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
