package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.w3c.dom.Document;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class ExtensionsForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "ManifestEditor.ExtensionForm.title";
	private ManifestExtensionsPage page;
	private DetailExtensionSection extensionSection;
	private DetailChildrenSection childrenSection;

public ExtensionsForm(ManifestExtensionsPage page) {
	this.page = page;
	//setVerticalFit(true);
	setScrollable(false);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.makeColumnsEqualWidth=true;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	extensionSection = new DetailExtensionSection(page);
	Control control = extensionSection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	//gd.widthHint = 300;
	//gd.heightHint = 300;
	control.setLayoutData(gd);

	childrenSection = new DetailChildrenSection(page);
	control = childrenSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(extensionSection, childrenSection);

	// Register
	registerSection(extensionSection);
	registerSection(childrenSection);
}
public void expandTo(Object object) {
   extensionSection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
