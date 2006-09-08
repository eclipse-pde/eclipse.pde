package org.eclipse.pde.internal.ui.commands;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.commands.IParameter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.commands.CommandSerializerPart.IDialogButtonCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class CommandSerializerDialog extends Dialog implements IDialogButtonCreator {

	public class CommandResult {
		String fCommandName, fSerializedCommand;
		HashMap fParamMap;
		public String getCommandName()
			{ return fCommandName; }
		public String getSerializedString()
			{ return fSerializedCommand; }
		public HashMap getParameterMap()
			{ return fParamMap; }
	}
	
	private CommandSerializerPart fCSP;
	private CommandResult fCR;
	
	public CommandSerializerDialog(Shell parentShell, int filterType) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.DIALOG_TRIM);
		fCSP = new CommandSerializerPart();
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
		fCR = new CommandResult();
		fCR.fCommandName = fCSP.getSelectedCommandName();
		fCR.fSerializedCommand = fCSP.getSelectedSerializedString();
		HashMap map = fCSP.getSelectedCommandsParameters();
		HashMap stringMap = new HashMap();
		for (Iterator i = map.keySet().iterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof IParameter)
				stringMap.put(((IParameter)o).getId(), map.get(o));
		}
		fCR.fParamMap = stringMap;
		super.okPressed();
	}
	
	public boolean close() {
		fCSP.dispose();
		return super.close();
	}
	
	public CommandResult getCommand() {
		return fCR;
	}

	public void createButtons(Composite parent) {
		Composite comp = fCSP.createComposite(
				parent, 
				GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END, 
				2, false);
		
		createButtonsForButtonBar(comp);
	}
}
