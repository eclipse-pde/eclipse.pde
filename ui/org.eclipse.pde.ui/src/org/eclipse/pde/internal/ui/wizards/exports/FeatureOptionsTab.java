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
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class FeatureOptionsTab extends ExportOptionsTab {

	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$

	private Button fMultiPlatform;

	public FeatureOptionsTab(FeatureExportWizardPage page) {
		super(page);
	}
	
	protected void addCrossPlatformOption(Composite comp) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel("org.eclipse.platform.launchers"); //$NON-NLS-1$
        if (model != null) {
			fMultiPlatform = new Button(comp, SWT.CHECK);
			fMultiPlatform.setText(PDEUIMessages.ExportWizard_multi_platform);
        }		
	}
	
	protected boolean getInitialJarButtonSelection(IDialogSettings settings){
       return settings.getBoolean(S_JAR_FORMAT);
	}

	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_fPackageJARs; 
	}
	
	protected void initialize(IDialogSettings settings) {
		super.initialize(settings);
		if (fMultiPlatform != null)
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));
	}
	
	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
        if (fMultiPlatform != null)
            settings.put(S_MULTI_PLATFORM, fMultiPlatform.getSelection());      
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
}
