package org.eclipse.pde.internal.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.*;

public class ComponentSpecPage extends WizardPage {
	public static final String PAGE_TITLE = "NewComponentWizard.SpecPage.title";
	public static final String PAGE_ID = "NewComponentWizard.SpecPage.id";
	public static final String PAGE_VERSION = "NewComponentWizard.SpecPage.version";
	public static final String PAGE_PROVIDER = "NewComponentWizard.SpecPage.provider";
	public static final String PAGE_DESCRIPTION = "NewComponentWizard.SpecPage.description";
	public static final String PAGE_DESC = "NewComponentWizard.SpecPage.desc";
	private Text idText;
	private Text versionText;
	private Text providerText;
	private Text descriptionText;

protected ComponentSpecPage() {
	super("specPage");
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	layout.horizontalSpacing = 9;
	container.setLayout(layout);

	ModifyListener listener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			verifyComplete();
		}
	};

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_ID));
	idText = new Text(container, SWT.BORDER);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	idText.setLayoutData(gd);
	idText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_VERSION));
	versionText = new Text(container, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	versionText.setLayoutData(gd);
	versionText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_PROVIDER));
	providerText = new Text(container, SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	providerText.setLayoutData(gd);
	providerText.addModifyListener(listener);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(PAGE_DESCRIPTION));
	descriptionText =
		new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
	gd = new GridData(GridData.FILL_BOTH);
	descriptionText.setLayoutData(gd);
	descriptionText.addModifyListener(listener);

	idText.setFocus();
	verifyComplete();
	setControl(container);
}
public boolean finish() {
	return true;
}
public ComponentData getComponentData() {
	ComponentData data = new ComponentData();
	data.id = idText.getText();
	try {
	   PluginVersionIdentifier pvi = new PluginVersionIdentifier(versionText.getText());
	   data.version = pvi.toString();
	}
	catch (NumberFormatException e) {
	   data.version = versionText.getText();
	}
	data.provider = providerText.getText();
	data.description = descriptionText.getText();
	return data;
}
private void verifyComplete() {
	if (verifyVersion()) {
	   boolean complete =
		   idText.getText().length() > 0
			&& providerText.getText().length() > 0;
	   setPageComplete(complete);
	}
}

private boolean verifyVersion() {
	String value = versionText.getText();
	if (value.length()==0) return false;
	try {
	   PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
	}
	catch (Exception e) {
		return false;
	}
	return true;
}
}
