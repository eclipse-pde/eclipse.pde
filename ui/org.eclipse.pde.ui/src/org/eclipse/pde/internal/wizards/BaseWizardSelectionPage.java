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


public abstract class BaseWizardSelectionPage
	extends WizardSelectionPage
	implements ISelectionChangedListener {
	private String label;
	private Label descriptionText;

public BaseWizardSelectionPage(String name, String label) {
	super(name);
	this.label = label;
}
public void createDescriptionIn(Composite composite) {
	descriptionText = new Label(composite, SWT.WRAP);
	descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); 
}

protected abstract IWizardNode createWizardNode(WizardElement element);
public String getLabel() {
	return label;
}
public void setDescriptionText(String text) {
	descriptionText.setText(text);
}
}
