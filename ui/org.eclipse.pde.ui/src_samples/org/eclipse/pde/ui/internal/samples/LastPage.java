/*
 * Created on Mar 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LastPage extends WizardPage {
	private IConfigurationElement selection;
	/**
	 * @param pageName
	 */
	public LastPage() {
		super("last");
		setTitle("Sample creation");
		setDescription("Review the sample to create and run.");
	}
	public void setSelection(IConfigurationElement selection) {
		this.selection = selection;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		Label label = new Label(container, SWT.NULL);
		String name = selection.getAttribute("name");
		if (name!=null)
			label.setText(name);
		else
			setErrorMessage("No samples found.");
		setControl(container);
	}
}