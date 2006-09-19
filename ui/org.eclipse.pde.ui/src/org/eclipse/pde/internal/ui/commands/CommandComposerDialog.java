package org.eclipse.pde.internal.ui.commands;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandComposerPart.IDialogButtonCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class CommandComposerDialog extends Dialog implements IDialogButtonCreator {
	
	private CommandComposerPart fCSP;
	private ParameterizedCommand fPC;
	private Button fOKButton;
	
	public CommandComposerDialog(Shell parentShell, int filterType, ParameterizedCommand preselectedCommand) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.SHELL_TRIM | SWT.BORDER);
		fCSP = new CommandComposerPart();
		fCSP.setFilterType(filterType);
		fCSP.setButtonCreator(this);
		fCSP.setPresetCommand(preselectedCommand);
	}
	
	protected void configureShell(Shell newShell) {
		newShell.setText(PDEUIMessages.CommandSerializerPart_name);
		super.configureShell(newShell);
	}
	
	protected Control createContents(Composite parent) {
		ScrolledForm form = fCSP.createForm(parent);
		initializeDialogUnits(form);
		fCSP.createPartControl();
		applyDialogFont(form);
		return form;
	}
	
	public void okPressed() {
		fPC = fCSP.getParameterizedCommand();
		super.okPressed();
	}
	
	public boolean close() {
		fCSP.dispose();
		return super.close();
	}
	
	public ParameterizedCommand getCommand() {
		return fPC;
	}

	public void createButtons(Composite parent) {
		Composite comp = fCSP.createComposite(
				parent, 
				GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END, 
				0, false); // 0 columns since createButton(...) increments col num
		
		fOKButton = createButton(comp, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		fOKButton.setEnabled(false);
		fCSP.getToolkit().adapt(fOKButton, true, true);
		fCSP.getToolkit().adapt(createButton(comp, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false), true, true);
	}
	
	public ISelectionChangedListener getButtonEnablementListener() {
		return new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (fOKButton != null) {
					Object obj = event.getSelection();
					fOKButton.setEnabled(obj instanceof IStructuredSelection &&
							((IStructuredSelection)obj).getFirstElement() instanceof Command);
				}
			}
		};
	}
}
