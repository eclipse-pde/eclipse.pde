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
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ReviewPage extends WizardPage {
	private SampleWizard wizard;
	private ScrolledFormText formText;
	/**
	 * @param pageName
	 */
	public ReviewPage(SampleWizard wizard) {
		super("last");
		this.wizard = wizard;
		setTitle("Review");
		setDescription("Review the selected sample.");
	}
	public void setVisible(boolean visible) {
		setPageComplete(wizard.getSelection()!=null);			
		if (formText!=null)
			updateContent();
		super.setVisible(visible);
	}
	
	private void updateContent() {
		StringBuffer buf = new StringBuffer();
		buf.append("<form>");
		IConfigurationElement selection = wizard.getSelection();
		if (selection!=null) {
			setMessage(null);
			IConfigurationElement [] desc = selection.getChildren("description");
			buf.append("<p>You have selected the following sample:</p>");
			buf.append("<p><b>");
			buf.append(selection.getAttribute("name"));
			buf.append("</b></p>");
			if (desc.length==1) {
				buf.append("<p>");
				buf.append(desc[0].getValue());
				buf.append("</p>");
			}
			buf.append("<p>If the selection is correct, press <b>Finish</b> to create the sample.</p>");
		}
		else {
			setMessage("No sample has been selected.", WizardPage.WARNING);
		}
		buf.append("</form>");
		formText.setText(buf.toString());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		formText = new ScrolledFormText(container, true);
		formText.setBackground(parent.getBackground());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 300;
		formText.setLayoutData(gd);
		HyperlinkSettings settings = new HyperlinkSettings(parent.getDisplay());
		formText.getFormText().setHyperlinkSettings(settings);
		setControl(container);
		updateContent();
	}
}