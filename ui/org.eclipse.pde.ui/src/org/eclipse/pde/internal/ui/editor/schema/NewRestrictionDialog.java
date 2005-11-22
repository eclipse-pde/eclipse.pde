package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewRestrictionDialog extends MessageDialog {

	private Text fText;
	private String fRestriction;
	public NewRestrictionDialog(Shell parent) {
		super(parent, PDEUIMessages.NewRestrictionDialog_title, null, PDEUIMessages.NewRestrictionDialog_message, 
				QUESTION,
				new String[] { 
					IDialogConstants.OK_LABEL, 
					IDialogConstants.CANCEL_LABEL},
				0);
		
	}
	protected Control createCustomArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fText = new Text(parent, SWT.BORDER);
		fText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return comp;
	}
	
	public String getNewRestriction() {
		return fRestriction;
	}
	
	public boolean close() {
		fRestriction = fText.getText();
		return super.close();
	}
}
