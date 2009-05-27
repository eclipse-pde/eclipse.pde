/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class BaseWizardSelectionPage extends WizardSelectionPage implements ISelectionChangedListener {
	private String label;
	private FormBrowser descriptionBrowser;

	public BaseWizardSelectionPage(String name, String label) {
		super(name);
		this.label = label;
		descriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
		descriptionBrowser.setText(""); //$NON-NLS-1$
	}

	public void createDescriptionIn(Composite composite) {
		descriptionBrowser.createControl(composite);
		Control c = descriptionBrowser.getControl();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		c.setLayoutData(gd);
	}

	protected abstract IWizardNode createWizardNode(WizardElement element);

	public String getLabel() {
		return label;
	}

	public void setDescriptionText(String text) {
		if (text == null)
			text = PDEUIMessages.BaseWizardSelectionPage_noDesc;
		descriptionBrowser.setText(text);
	}

	public void setDescriptionEnabled(boolean enabled) {
		Control dcontrol = descriptionBrowser.getControl();
		if (dcontrol != null)
			dcontrol.setEnabled(enabled);
	}
}
