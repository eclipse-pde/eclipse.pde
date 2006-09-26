package org.eclipse.pde.internal.ui.commands;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.internal.provisional.forms.FormDialog;

public class CommandComposerDialog extends FormDialog {
	
	private CommandComposerPart fCCP;
	private ParameterizedCommand fPC;
	private Button fOKButton;
	
	public CommandComposerDialog(Shell parentShell, int filterType, ParameterizedCommand preselectedCommand) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.SHELL_TRIM | SWT.BORDER);
		fCCP = new CommandComposerPart();
		fCCP.setFilterType(filterType);
		fCCP.setPresetCommand(preselectedCommand);
	}
	
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		fOKButton = getButton(IDialogConstants.OK_ID);
		fOKButton.setEnabled(fCCP.getPresetCommand() != null);
		return control;
	}
	
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		initializeDialogUnits(form);
		fCCP.createCC(form, mform.getToolkit(), new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (fOKButton != null) {
					Object obj = event.getSelection();
					fOKButton.setEnabled(obj instanceof IStructuredSelection &&
							((IStructuredSelection)obj).getFirstElement() instanceof Command);
				}
			}
		});
		applyDialogFont(form);
	}
	
	protected void configureShell(Shell newShell) {
		newShell.setText(PDEUIMessages.CommandSerializerPart_name);
		super.configureShell(newShell);
	}
	
	public void okPressed() {
		fPC = fCCP.getParameterizedCommand();
		super.okPressed();
	}
	
	public boolean close() {
		fCCP.dispose();
		return super.close();
	}
	
	public ParameterizedCommand getCommand() {
		return fPC;
	}
}
