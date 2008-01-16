/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class JavaArgumentsTab {

	private Text fProgramArgs;
	private Text fVMArgs;
	private TargetPlatformPreferencePage fPage;
	private Button fAppendLauncherArgs;

	public JavaArgumentsTab(TargetPlatformPreferencePage page) {
		fPage = page;
	}

	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		Label description = new Label(container, SWT.WRAP);
		description.setText(PDEUIMessages.JavaArgumentsTab_description);
		GridData gd = new GridData();
		gd.widthHint = 450;
		description.setLayoutData(gd);

		Group programGroup = new Group(container, SWT.NONE);
		programGroup.setLayout(new GridLayout());
		programGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		programGroup.setText(PDEUIMessages.JavaArgumentsTab_progamArgsGroup);

		fProgramArgs = new Text(programGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		gd.heightHint = 60;
		fProgramArgs.setLayoutData(gd);

		Button programVars = new Button(programGroup, SWT.NONE);
		programVars.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		programVars.setText(PDEUIMessages.JavaArgumentsTab_programVariables);
		programVars.addSelectionListener(getListener(fProgramArgs));

		Group vmGroup = new Group(container, SWT.NONE);
		vmGroup.setLayout(new GridLayout(2, false));
		vmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		vmGroup.setText(PDEUIMessages.JavaArgumentsTab_vmArgsGroup);

		fVMArgs = new Text(vmGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		gd.heightHint = 60;
		gd.horizontalSpan = 2;
		fVMArgs.setLayoutData(gd);

		fAppendLauncherArgs = new Button(vmGroup, SWT.CHECK);
		fAppendLauncherArgs.setText(PDEUIMessages.JavaArgumentsTab_appendLauncherIni);

		Button vmVars = new Button(vmGroup, SWT.NONE);
		vmVars.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		vmVars.setText(PDEUIMessages.JavaArgumentsTab_vmVariables);
		vmVars.addSelectionListener(getListener(fVMArgs));

		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.LAUNCHING_ARGS_PREFERENCE_PAGE);
		return container;
	}

	protected SelectionListener getListener(final Text textControl) {
		return new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(fPage.getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					textControl.insert(variable);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	protected void initialize() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		fProgramArgs.setText(preferences.getString(ICoreConstants.PROGRAM_ARGS));
		fVMArgs.setText(preferences.getString(ICoreConstants.VM_ARGS));
		fAppendLauncherArgs.setSelection(preferences.getBoolean(ICoreConstants.VM_LAUNCHER_INI));
	}

	public void performOk() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setValue(ICoreConstants.PROGRAM_ARGS, fProgramArgs.getText());
		preferences.setValue(ICoreConstants.VM_ARGS, fVMArgs.getText());
		preferences.setValue(ICoreConstants.VM_LAUNCHER_INI, fAppendLauncherArgs.getSelection());
	}

	protected void performDefaults() {
		fProgramArgs.setText(""); //$NON-NLS-1$
		fVMArgs.setText(""); //$NON-NLS-1$
	}

	protected void loadTargetProfile(ITarget target) {
		IArgumentsInfo info = target.getArguments();
		if (info == null) {
			fProgramArgs.setText(""); //$NON-NLS-1$
			fVMArgs.setText(""); //$NON-NLS-1$
			return;
		}
		String progArgs = (info.getProgramArguments() == null) ? "" : info.getProgramArguments(); //$NON-NLS-1$
		fProgramArgs.setText(progArgs);
		String vmArgs = (info.getVMArguments() == null) ? "" : info.getVMArguments(); //$NON-NLS-1$
		fVMArgs.setText(vmArgs);
	}
}
