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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class FeatureOptionsTab extends ExportOptionsTab {

	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$
	private static final String S_EXPORT_METADATA = "p2metadata"; //$NON-NLS-1$

	private Button fMultiPlatform;
	private Button fExportMetadata;

	public FeatureOptionsTab(FeatureExportWizardPage page) {
		super(page);
	}

	protected void addAdditionalOptions(Composite comp) {
		fJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fExportMetadata.setEnabled(fJarButton.getSelection());
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

	}

	protected boolean getInitialJarButtonSelection(IDialogSettings settings) {
		return settings.getBoolean(S_JAR_FORMAT);
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

	protected void initialize(IDialogSettings settings) {
		super.initialize(settings);
		if (fMultiPlatform != null) {
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));
		}
		fExportMetadata.setSelection(settings.getBoolean(S_EXPORT_METADATA));
		fExportMetadata.setEnabled(fJarButton.getSelection());
	}

	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fMultiPlatform != null) {
			settings.put(S_MULTI_PLATFORM, fMultiPlatform.getSelection());
		}
		settings.put(S_EXPORT_METADATA, doExportMetadata());
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
	}

	protected boolean doMultiplePlatform() {
		return fMultiPlatform != null && fMultiPlatform.getSelection();
	}

	protected void setEnabledForInstall(boolean enabled) {
		super.setEnabledForInstall(enabled);
		fExportMetadata.setEnabled(enabled);
	}

}
