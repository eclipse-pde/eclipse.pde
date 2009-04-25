/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.net.URI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class FeatureOptionsTab extends ExportOptionsTab {

	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$
	private static final String S_EXPORT_METADATA = "p2metadata"; //$NON-NLS-1$
	private static final String S_CATEGORY_FILE = "category_file"; //$NON-NLS-1$
	private static final String S_CREATE_CATEGORIES = "create_categories"; //$NON-NLS-1$

	private Button fMultiPlatform;
	private Button fExportMetadata;

	private Button fCategoryButton;
	private Combo fCategoryCombo;
	private Button fCategoryBrowse;

	public FeatureOptionsTab(FeatureExportWizardPage page) {
		super(page);
	}

	protected void addAdditionalOptions(Composite comp) {
		fJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fExportMetadata.setEnabled(fJarButton.getSelection());
				fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
				updateCategoryGeneration();
			}
		});
		fExportMetadata = SWTFactory.createCheckButton(comp, PDEUIMessages.ExportWizard_includesMetadata, null, false, 1);
		GridData data = (GridData) fExportMetadata.getLayoutData();
		data.horizontalIndent = 20;

		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.getDeltaPackFeature();
		if (model != null) {
			fMultiPlatform = new Button(comp, SWT.CHECK);
			fMultiPlatform.setText(PDEUIMessages.ExportWizard_multi_platform);
		}
		Composite categoryComposite = new Composite(comp, SWT.NONE);

		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalIndent = 20;
		categoryComposite.setLayoutData(data);
		GridLayout layout = new GridLayout(3, false);
		layout.marginRight = 0;
		layout.marginLeft = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		categoryComposite.setLayout(layout);

		fCategoryButton = new Button(categoryComposite, SWT.CHECK);
		fCategoryButton.setText(PDEUIMessages.ExportWizard_generateCategories + ":"); //$NON-NLS-1$
		fCategoryButton.setSelection(true);

		fCategoryCombo = new Combo(categoryComposite, SWT.NONE);
		fCategoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fCategoryBrowse = new Button(categoryComposite, SWT.PUSH);
		fCategoryBrowse.setText(PDEUIMessages.ExportWizard_browse);
		fCategoryBrowse.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fCategoryBrowse);

	}

	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_fPackageJARs;
	}

	/**
	 * @return whether to generate p2 metadata on export
	 */
	protected boolean doExportMetadata() {
		return fExportMetadata.isEnabled() && fExportMetadata.getSelection();
	}

	protected URI getCategoryDefinition() {
		if (doExportCategories()) {
			File f = new File(fCategoryCombo.getText().trim());
			if (f.exists())
				return f.toURI();
		}
		return null;
	}

	/**
	 * @return whether to publish categories when exporting
	 */
	private boolean doExportCategories() {
		return doExportMetadata() && fCategoryButton.getSelection() && fCategoryCombo.getText().trim().length() > 0;
	}

	protected void initialize(IDialogSettings settings) {
		super.initialize(settings);
		if (fMultiPlatform != null) {
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));
		}

		String selected = settings.get(S_EXPORT_METADATA);
		fExportMetadata.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));
		fExportMetadata.setEnabled(fJarButton.getSelection());
		selected = settings.get(S_CREATE_CATEGORIES);

		fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
		fCategoryButton.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));
		if (settings.get(S_CATEGORY_FILE) != null)
			fCategoryCombo.setText(settings.get(S_CATEGORY_FILE));
		updateCategoryGeneration();
	}

	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fMultiPlatform != null) {
			settings.put(S_MULTI_PLATFORM, fMultiPlatform.getSelection());
		}
		settings.put(S_EXPORT_METADATA, doExportMetadata());
		settings.put(S_CREATE_CATEGORIES, fCategoryButton.getSelection());
		settings.put(S_CATEGORY_FILE, fCategoryCombo.getText());
	}

	protected void hookListeners() {
		super.hookListeners();
		if (fMultiPlatform != null) {
			fMultiPlatform.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fPage.pageChanged();
				}
			});
		}

		fExportMetadata.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
				updateCategoryGeneration();
			}
		});
		fCategoryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateCategoryGeneration();
			}
		});

		fCategoryBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openFile(fCategoryCombo, new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}

	protected void updateCategoryGeneration() {
		fCategoryBrowse.setEnabled(fExportMetadata.getSelection() && fCategoryButton.getSelection() && fJarButton.getSelection());
		fCategoryCombo.setEnabled(fExportMetadata.getSelection() && fCategoryButton.getSelection() && fJarButton.getSelection());
	}

	protected boolean doMultiplePlatform() {
		return fMultiPlatform != null && fMultiPlatform.getSelection();
	}

	protected void setEnabledForInstall(boolean enabled) {
		super.setEnabledForInstall(enabled);
		fExportMetadata.setEnabled(enabled);
		fCategoryButton.setEnabled(enabled);
		fCategoryCombo.setEnabled(enabled);
		fCategoryBrowse.setEnabled(enabled);
	}

	protected void openFile(Combo combo, String[] filter) {
		FileDialog dialog = new FileDialog(fPage.getShell(), SWT.OPEN);
		String path = combo.getText();
		if (path.trim().length() == 0)
			path = PDEPlugin.getWorkspace().getRoot().getLocation().toString();
		dialog.setFilterExtensions(filter);
		dialog.setFileName(path);
		String res = dialog.open();
		if (res != null) {
			if (combo.indexOf(res) == -1)
				combo.add(res, 0);
			combo.setText(res);
		}
	}

}
