/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.ui.parts.FormBrowser;


public abstract class BaseWizardSelectionPage
	extends WizardSelectionPage
	implements ISelectionChangedListener {
	private String label;
	private FormBrowser descriptionBrowser;

public BaseWizardSelectionPage(String name, String label) {
	super(name);
	this.label = label;
	descriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
	descriptionBrowser.setText("");
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
	descriptionBrowser.setText(text);
}
}
