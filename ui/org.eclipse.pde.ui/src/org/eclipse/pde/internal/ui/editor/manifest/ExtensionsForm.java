package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.*;

public class ExtensionsForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "ManifestEditor.ExtensionForm.title";
	private ManifestExtensionsPage page;
	private DetailExtensionSection extensionSection;
	//private DetailChildrenSection childrenSection;
	private BodyTextSection bodyTextSection;

public ExtensionsForm(ManifestExtensionsPage page) {
	this.page = page;
	setVerticalFit(true);
	setScrollable(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 1;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	extensionSection = new DetailExtensionSection(page);
	Control control = extensionSection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	bodyTextSection = new BodyTextSection(page);
	bodyTextSection.setCollapsable(true);
	bodyTextSection.setCollapsed(true);
	control = bodyTextSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(extensionSection, bodyTextSection);

	// Register
	registerSection(extensionSection);
	registerSection(bodyTextSection);
}

public void expandTo(Object object) {
   extensionSection.expandTo(object);
}

public void openNewExtensionWizard() {
	extensionSection.handleNew();
}

public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
