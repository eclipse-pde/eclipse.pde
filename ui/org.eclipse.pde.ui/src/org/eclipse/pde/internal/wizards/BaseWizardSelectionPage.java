package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.parts.FormBrowser;


public abstract class BaseWizardSelectionPage
	extends WizardSelectionPage
	implements ISelectionChangedListener {
	private String label;
	private FormBrowser descriptionBrowser;

public BaseWizardSelectionPage(String name, String label) {
	super(name);
	this.label = label;
	descriptionBrowser = new FormBrowser();
	descriptionBrowser.setText("");
}
public void createDescriptionIn(Composite composite) {
	descriptionBrowser.createControl(composite);
	Control c = descriptionBrowser.getControl();	
	c.setLayoutData(new GridData(GridData.FILL_BOTH));
}

protected abstract IWizardNode createWizardNode(WizardElement element);
public String getLabel() {
	return label;
}
public void setDescriptionText(String text) {
	descriptionBrowser.setText(text);
}
}
