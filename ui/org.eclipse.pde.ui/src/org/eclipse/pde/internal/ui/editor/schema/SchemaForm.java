package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class SchemaForm extends ScrollableSectionForm implements IModelChangedListener {
	private SchemaSpecSection schemaSpecSection;
	private ElementSection elementSection;
	private DescriptionSection descriptionSection;
	private GrammarSection grammarSection;
	private SchemaFormPage page;
	private IColorManager colorManager=new ColorManager();

public SchemaForm(SchemaFormPage page) {
	this.page = page;
	colorManager = new ColorManager();
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	parent.setLayout(layout);

	GridData gd;
	Control control;
	
	schemaSpecSection = new SchemaSpecSection(page);
	control = schemaSpecSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	grammarSection = new GrammarSection(page);
	control = grammarSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
	gd.verticalSpan = 2;
	control.setLayoutData(gd);
	
	elementSection = new ElementSection(page);
	control = elementSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	descriptionSection = new DescriptionSection(page, colorManager);
	control = descriptionSection.createControl(parent, factory);
	//if (SWT.getPlatform().equals("motif"))
	   gd = new GridData(GridData.FILL_HORIZONTAL
	                            | GridData.VERTICAL_ALIGN_BEGINNING);
	//else 
	   //gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	gd.heightHint = 150;
	control.setLayoutData(gd);

	// wire sections
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(elementSection, grammarSection);
	manager.linkSections(elementSection, descriptionSection);

	registerSection(schemaSpecSection);
	registerSection(elementSection);
	registerSection(grammarSection);
	registerSection(descriptionSection);
}
public void dispose() {
	ISchema schema = (ISchema) page.getModel();
	colorManager.dispose();
	schema.removeModelChangedListener(this);
	super.dispose();
}
public void expandTo(Object object) {
   elementSection.expandTo(object);
}
public void initialize(Object model) {
	ISchema schema = (ISchema)model;
	super.initialize(model);
	setHeadingText(schema.getName());
	((Composite)getControl()).layout(true);
	schema.addModelChangedListener(this);
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		ISchema schema = (ISchema) page.getModel();
		setHeadingText(schema.getName());
	}
}
}
