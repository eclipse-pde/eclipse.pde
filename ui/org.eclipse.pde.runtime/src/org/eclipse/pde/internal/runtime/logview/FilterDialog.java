/*
 * Created on Jun 12, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author wassimm
 */
public class FilterDialog extends Dialog {

	public FilterDialog(Shell parentShell) {
		super(parentShell);
	}
	
	protected Control createDialogArea(Composite parent) {
		return super.createDialogArea(parent);
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
	}

}
