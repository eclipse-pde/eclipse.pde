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
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class ExtensionPointForm extends ScrollableForm {
	public static final String FORM_TITLE = "ManifestEditor.ExtensionPointForm.title";
	private ManifestExtensionPointPage page;
	private DetailExtensionPointSection extensionPointSection;
	private PointGraphSection graphSection;

public ExtensionPointForm(ManifestExtensionPointPage page) {
	this.page = page;
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	//layout.makeColumnsEqualWidth=true;
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	extensionPointSection = new DetailExtensionPointSection(page);
	Control control = extensionPointSection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	//gd.widthHint = 250;
	//gd.heightHint = 300;
	control.setLayoutData(gd);

	graphSection = new PointGraphSection(page);
	control = graphSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(extensionPointSection, graphSection);

	registerSection(extensionPointSection);
	registerSection(graphSection);
}
public void expandTo(Object object) {
   extensionPointSection.expandTo(object);
}
public void initialize(Object model) {
	setTitle(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
	getControl().layout(true);
}
}
