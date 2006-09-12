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

public class CommandComposerDialog extends Dialog implements IDialogButtonCreator {
	
	private CommandComposerPart fCSP;
	private ParameterizedCommand fPC;
	private Button fOKButton;
	
	public CommandComposerDialog(Shell parentShell, int filterType) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.DIALOG_TRIM);
		fCSP = new CommandComposerPart();
		fCSP.setFilterType(filterType);
		fCSP.setNotifier(this);
	}
	
	protected void configureShell(Shell newShell) {
		newShell.setText(PDEUIMessages.CommandSerializerPart_name);
		super.configureShell(newShell);
	}
	
	protected Control createDialogArea(Composite parent) {
		fCSP.createPartControl(parent);
		Control c = fCSP.getForm().getBody();
		applyDialogFont(c);
		return c;
	}
	
	protected Control createButtonBar(Composite parent) {
		// custom buttons in part control
		return null;
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
				2, false);
		
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
