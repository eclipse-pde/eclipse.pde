package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.text.*;


public class DocForm extends ScrollableSectionForm {
	private IColorManager colorManager = new ColorManager();
	private DocSection docSection;
	public static final String FORM_TITLE = "SchemaEditor.DocForm.title";
	private SchemaDocPage page;

public DocForm(SchemaDocPage page) {
	this.page = page;
	//setWidthHint(600);
	//this.setVerticalFit(true);
	this.setScrollable(false);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	//layout.setMarginWidth 
	parent.setLayout(layout);

	GridData gd;
	Control control;

	docSection = new DocSection(page, colorManager);
	control = docSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	registerSection(docSection);
}
public void dispose() {
	colorManager.dispose();
	super.dispose();
}
public void expandTo(Object object) {
	docSection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
}
public void updateEditorInput(Object obj) {
	docSection.updateEditorInput(obj);
}
}
