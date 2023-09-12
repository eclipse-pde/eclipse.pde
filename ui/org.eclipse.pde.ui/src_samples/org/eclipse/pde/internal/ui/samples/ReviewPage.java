/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

public class ReviewPage extends WizardPage {
	private final SampleWizard wizard;
	private ScrolledFormText formText;

	public ReviewPage(SampleWizard wizard) {
		super("last"); //$NON-NLS-1$
		this.wizard = wizard;
		setTitle(PDEUIMessages.ReviewPage_title);
		setDescription(PDEUIMessages.ReviewPage_desc);
	}

	@Override
	public void setVisible(boolean visible) {
		setPageComplete(wizard.getSelection() != null);
		if (formText != null)
			updateContent();
		super.setVisible(visible);
	}

	private void updateContent() {
		StringBuilder buf = new StringBuilder();
		buf.append("<form>"); //$NON-NLS-1$
		IConfigurationElement selection = wizard.getSelection();
		if (selection != null) {
			setMessage(null);
			IConfigurationElement[] desc = selection.getChildren("description"); //$NON-NLS-1$
			if (desc.length == 1)
				buf.append(NLS.bind(PDEUIMessages.ReviewPage_descContent, (new String[] {selection.getAttribute("name"), desc[0].getValue()}))); //$NON-NLS-1$
			else
				buf.append(NLS.bind(PDEUIMessages.ReviewPage_content, selection.getAttribute("name"))); //$NON-NLS-1$
		} else {
			setMessage(PDEUIMessages.ReviewPage_noSampleFound, IMessageProvider.WARNING);
		}
		buf.append("</form>"); //$NON-NLS-1$
		formText.setText(buf.toString());
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
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

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.REVIEW);
	}
}
