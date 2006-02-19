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
package org.eclipse.pde.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.preferences.CompilersConfigurationTab;
import org.eclipse.pde.internal.ui.preferences.CompilersPreferencePage;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

public class CompilersPropertyPage extends PropertyPage {

	private ControlEnableState blockEnableState;

	private Button changeWorkspaceSettingsButton;

	private CompilersConfigurationTab configurationBlock;

	private Control configurationBlockControl;

	private Button useProjectButton;

	private Button useWorkspaceButton;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		configurationBlock = new CompilersConfigurationTab(getProject());

		SelectionListener listener = new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					doDialogFieldChanged((Button) e.getSource());
				}
			}
		};

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		composite.setLayout(layout);

		// Use Workspace Settings radio button
		useWorkspaceButton = new Button(composite, SWT.RADIO);
		useWorkspaceButton.addSelectionListener(listener);
		useWorkspaceButton
				.setText(PDEUIMessages.CompilersPropertyPage_useworkspacesettings_label); 
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		// if (fButtonStyle == SWT.PUSH) {
		// gd.heightHint = SWTUtil.getButtonHeightHint(button);
		// gd.widthHint = SWTUtil.getButtonWidthHint(button);
		// }
		gd.grabExcessHorizontalSpace = true;
		useWorkspaceButton.setLayoutData(gd);

		// Change Workspace Settings push button
		changeWorkspaceSettingsButton = new Button(composite, SWT.PUSH);
		changeWorkspaceSettingsButton
				.setText(PDEUIMessages.CompilersPropertyPage_useworkspacesettings_change); 
		changeWorkspaceSettingsButton.addSelectionListener(listener);
		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		useWorkspaceButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(changeWorkspaceSettingsButton);

		// Use Project Settings radio button
		useProjectButton = new Button(composite, SWT.RADIO);
		useProjectButton.addSelectionListener(listener);
		useProjectButton
				.setText(PDEUIMessages.CompilersPropertyPage_useprojectsettings_label); 
		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		useProjectButton.setLayoutData(gd);

		//
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_FILL);
		data.horizontalSpan = 2;

		configurationBlockControl = configurationBlock
				.createContents(composite);
		configurationBlockControl.setLayoutData(data);

		boolean useProjectSettings = CompilerFlags.getBoolean(getProject(),
				CompilerFlags.USE_PROJECT_PREF);

		useProjectButton.setSelection(useProjectSettings);
		useWorkspaceButton.setSelection(!useProjectSettings);
		changeWorkspaceSettingsButton.setEnabled(!useProjectSettings);

		updateEnableState();
		Dialog.applyDialogFont(composite);
		return composite;
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				IHelpContextIds.COMPILERS_PROPERTY_PAGE);
	}

	private void doDialogFieldChanged(Button button) {
		if (button == changeWorkspaceSettingsButton) {
			showPreferencePage();
		} else {
			updateEnableState();
		}
	}

	private IProject getProject() {
		return (IProject) getElement().getAdapter(IProject.class);
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (useProjectSettings()) {
			useProjectButton.setSelection(false);
			useWorkspaceButton.setSelection(true);
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

	private boolean showPreferencePage() {
		final IPreferenceNode targetNode = new PreferenceNode(
				"org.eclipse.pde.ui.CompilersPreferencePage", //$NON-NLS-1$
				new CompilersPreferencePage());

		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(getShell(),
				manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				result[0] = (dialog.open() == Window.OK);
			}
		});
		return result[0];
	}

	private void updateEnableState() {
		if (useProjectSettings()) {
			if (blockEnableState != null) {
				changeWorkspaceSettingsButton.setEnabled(false);
				blockEnableState.restore();
				blockEnableState = null;
			}
		} else {
			if (blockEnableState == null) {
				changeWorkspaceSettingsButton.setEnabled(true);
				blockEnableState = ControlEnableState
						.disable(configurationBlockControl);
			}
		}
	}

	private boolean useProjectSettings() {
		return useProjectButton.getSelection();
	}
}
