/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.internal.samples;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

public class ReviewPage extends WizardPage {
	private SampleWizard wizard;
	private ScrolledFormText formText;
	/**
	 * @param pageName
	 */
	public ReviewPage(SampleWizard wizard) {
		super("last"); //$NON-NLS-1$
		this.wizard = wizard;
		setTitle(PDEPlugin.getResourceString("ReviewPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("ReviewPage.desc")); //$NON-NLS-1$
	}
	public void setVisible(boolean visible) {
		setPageComplete(wizard.getSelection()!=null);			
		if (formText!=null)
			updateContent();
		super.setVisible(visible);
	}
	
	private void updateContent() {
		StringBuffer buf = new StringBuffer();
		buf.append("<form>"); //$NON-NLS-1$
		IConfigurationElement selection = wizard.getSelection();
		if (selection!=null) {
			setMessage(null);
			IConfigurationElement [] desc = selection.getChildren("description");  //$NON-NLS-1$
			if (desc.length==1)
				buf.append(PDEPlugin.getFormattedMessage("ReviewPage.descContent", new String[]{selection.getAttribute("name"), desc[0].getValue()})); //$NON-NLS-1$ //$NON-NLS-2$
			else
				buf.append(PDEPlugin.getFormattedMessage("ReviewPage.content", selection.getAttribute("name"))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			setMessage(PDEPlugin.getResourceString("ReviewPage.noSampleFound"), WizardPage.WARNING); //$NON-NLS-1$
		}
		buf.append("</form>"); //$NON-NLS-1$
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
