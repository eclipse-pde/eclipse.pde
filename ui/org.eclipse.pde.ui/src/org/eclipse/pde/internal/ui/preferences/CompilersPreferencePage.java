/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class CompilersPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private CompilersConfigurationTab configurationBlock;

	/**
	 *  
	 */
	public CompilersPreferencePage() {
		super();
		setDescription(PDEUIMessages.CompilersPreferencePage_desc); 
		// only used when page is shown programatically
		setTitle(PDEUIMessages.CompilersPreferencePage_title); 

		configurationBlock = new CompilersConfigurationTab(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control result = configurationBlock.createContents(parent);
		Dialog.applyDialogFont(result);
		return result;
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, IHelpContextIds.COMPILERS_PREFERENCE_PAGE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		configurationBlock.performDefaults();
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (!configurationBlock.performOk(true)) {
			getContainer().updateButtons();
			return false;
		}
		return super.performOk();
	}

}
