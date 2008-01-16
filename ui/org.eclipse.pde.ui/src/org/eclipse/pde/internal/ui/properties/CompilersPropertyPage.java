/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.preferences.CompilersConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The PDE manifest compiler options property page for plugin projects
 */
public class CompilersPropertyPage extends PropertyPage {

	private ControlEnableState blockEnableState;
	private CompilersConfigurationTab configurationBlock;
	private Control configurationBlockControl;

	/**
	 * If project specific settings are being used or not
	 */
	private Button fProjectSpecific = null;

	/**
	 * A link to configure workspace settings
	 */
	private Link fWorkspaceLink = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		configurationBlock = new CompilersConfigurationTab(getProject());
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);

		fProjectSpecific = new Button(composite, SWT.CHECK);
		fProjectSpecific.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));
		fProjectSpecific.setText(PDEUIMessages.CompilersPropertyPage_useprojectsettings_label);
		fProjectSpecific.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateEnableState();
			}
		});

		fWorkspaceLink = new Link(composite, SWT.NONE);
		fWorkspaceLink.setText(PDEUIMessages.CompilersPropertyPage_useworkspacesettings_change);
		fWorkspaceLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String id = "org.eclipse.pde.ui.CompilersPreferencePage"; //$NON-NLS-1$
				PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] {id}, null).open();
			}
		});

		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		data.horizontalSpan = 2;

		configurationBlockControl = configurationBlock.createContents(composite);
		configurationBlockControl.setLayoutData(data);

		boolean useProjectSettings = CompilerFlags.getBoolean(getProject(), CompilerFlags.USE_PROJECT_PREF);

		fProjectSpecific.setEnabled(useProjectSettings);
		fWorkspaceLink.setEnabled(!useProjectSettings);

		updateEnableState();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.COMPILERS_PROPERTY_PAGE);
	}

	/**
	 * @return the backing project for this property page
	 */
	private IProject getProject() {
		return (IProject) getElement().getAdapter(IProject.class);
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (useProjectSettings()) {
			fProjectSpecific.setEnabled(false);
			updateEnableState();
			configurationBlock.performDefaults();
		}
		super.performDefaults();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (!configurationBlock.performOk(useProjectSettings())) {
			getContainer().updateButtons();
			return false;
		}
		return super.performOk();
	}

	/**
	 * Updates the enabled state of the controls based on the project specific settings
	 */
	private void updateEnableState() {
		if (useProjectSettings()) {
			if (blockEnableState != null) {
				fWorkspaceLink.setEnabled(false);
				blockEnableState.restore();
				blockEnableState = null;
			}
		} else {
			if (blockEnableState == null) {
				fWorkspaceLink.setEnabled(true);
				blockEnableState = ControlEnableState.disable(configurationBlockControl);
			}
		}
	}

	/**
	 * @return if project specific settings are being configured
	 */
	private boolean useProjectSettings() {
		return fProjectSpecific.getSelection();
	}
}
