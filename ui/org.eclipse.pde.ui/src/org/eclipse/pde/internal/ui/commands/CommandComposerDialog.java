/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.commands;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class CommandComposerDialog extends FormDialog {

	private final CommandComposerPart fCCP;
	private ParameterizedCommand fPC;
	private Button fOKButton;

	public CommandComposerDialog(Shell parentShell, int filterType, ParameterizedCommand preselectedCommand, IEvaluationContext snapshot) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.SHELL_TRIM | SWT.BORDER);
		fCCP = new CommandComposerPart();
		fCCP.setFilterType(filterType);
		fCCP.setPresetCommand(preselectedCommand);
		fCCP.setSnapshotContext(snapshot);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		mform.getToolkit().decorateFormHeading(form.getForm());
		initializeDialogUnits(form);
		fCCP.createCC(form, mform.getToolkit(), event -> updateOkButtonEnablement(event.getSelection()));
		applyDialogFont(form);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		// Update the button enablement only after the button is created
		fOKButton = getButton(IDialogConstants.OK_ID);

		CommandList list = fCCP.getCommandList();
		// Ensure the tree viewer was created
		if (list == null) {
			updateOkButtonEnablement(false);
			return;
		}
		// Retrieve the current selection
		ISelection selection = list.getSelection();
		// Update the OK button based on the current selection
		updateOkButtonEnablement(selection);
	}

	/**
	 * @param selection
	 */
	private void updateOkButtonEnablement(Object selection) {
		// Ensure there is a selection
		if (selection == null) {
			updateOkButtonEnablement(false);
			return;
		}
		// Ensure the selection is structured
		if ((selection instanceof IStructuredSelection) == false) {
			updateOkButtonEnablement(false);
			return;
		}
		IStructuredSelection sSelection = (IStructuredSelection) selection;
		// Ensure the selection is a command
		if (sSelection.getFirstElement() instanceof Command) {
			// Enable button
			updateOkButtonEnablement(true);
			return;
		}
		// Disable button
		updateOkButtonEnablement(false);
	}

	/**
	 * @param enabled
	 */
	private void updateOkButtonEnablement(boolean enabled) {
		if (fOKButton != null) {
			fOKButton.setEnabled(enabled);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText(PDEUIMessages.CommandSerializerPart_name);
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.COMMAND_COMPOSER_DIALOG);
	}

	@Override
	public void okPressed() {
		fPC = fCCP.getParameterizedCommand();
		super.okPressed();
	}

	@Override
	public boolean close() {
		fCCP.dispose();
		return super.close();
	}

	public ParameterizedCommand getCommand() {
		return fPC;
	}
}
