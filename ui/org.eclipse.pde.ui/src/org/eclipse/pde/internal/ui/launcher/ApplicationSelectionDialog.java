/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ApplicationSelectionDialog extends TrayDialog {

	private final String fMode;
	private Combo applicationCombo;
	private final String[] fApplicationNames;
	private String fSelectedApplication;

	public ApplicationSelectionDialog(Shell parentShell, String[] applicationNames, String mode) {
		super(parentShell);
		fMode = mode;
		fApplicationNames = applicationNames;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.LAUNCHER_APPLICATION_SELECTION);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 9;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NONE);
		if (fMode.equals(ILaunchManager.DEBUG_MODE))
			label.setText(PDEUIMessages.ApplicationSelectionDialog_debug);
		else
			label.setText(PDEUIMessages.ApplicationSelectionDialog_run);

		applicationCombo = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
		applicationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		applicationCombo.setItems(fApplicationNames);

		String defaultApp = TargetPlatform.getDefaultApplication();
		if (applicationCombo.indexOf(defaultApp) == -1)
			applicationCombo.add(defaultApp);

		applicationCombo.setText(applicationCombo.getItem(0));

		getShell().setText(fMode.equals(ILaunchManager.DEBUG_MODE) ? PDEUIMessages.ApplicationSelectionDialog_dtitle : PDEUIMessages.ApplicationSelectionDialog_rtitle); //
		Dialog.applyDialogFont(container);
		return container;
	}

	@Override
	protected void okPressed() {
		fSelectedApplication = applicationCombo.getText();
		super.okPressed();
	}

	public String getSelectedApplication() {
		if (fSelectedApplication.equals(TargetPlatform.getDefaultApplication()))
			return null;
		return fSelectedApplication;
	}

}
