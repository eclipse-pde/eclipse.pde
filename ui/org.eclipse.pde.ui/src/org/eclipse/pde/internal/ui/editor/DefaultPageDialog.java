package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class DefaultPageDialog extends MessageDialog {
	private Button stopAskingButton;
	private boolean stopAsking;
	
	public DefaultPageDialog(Shell parent, String message) {
		super(
		parent,
		PDEPlugin.getResourceString("DefaultPageDialog.title"),  //$NON-NLS-1$
		null,	// accept the default window icon
		message, 
		QUESTION, 
		new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
		0); 	// yes is the default
	}
	
	public static boolean ask(Shell parent, String message) {
		DefaultPageDialog dialog = new DefaultPageDialog(parent, message);
		return dialog.open()== 0;
	}
	
	protected Control createCustomArea(Composite parent) {
		stopAskingButton = new Button(parent, SWT.CHECK);
		stopAskingButton.setText(PDEPlugin.getResourceString("DefaultPageDialog.stopAskingButton")); //$NON-NLS-1$
		return stopAskingButton;
	}
	
	public boolean getStopAsking() {
		return stopAsking;
	}
	
	public boolean close() {
		stopAsking = stopAskingButton.getSelection();
		return super.close();
	}
}
