package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.pde.internal.base.model.*;

public class SchemaForm extends ScrollableForm implements IModelChangedListener {
	private ElementSection elementSection;
	private DescriptionSection descriptionSection;
	private GrammarSection grammarSection;
	private SchemaFormPage page;
	private IColorManager colorManager=new ColorManager();

public SchemaForm(SchemaFormPage page) {
	this.page = page;
	//setWidthHint(600);
	colorManager = new ColorManager();
    //setScrollable(false);
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

	elementSection = new ElementSection(page);
	control = elementSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	grammarSection = new GrammarSection(page);
	control = grammarSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	descriptionSection = new DescriptionSection(page, colorManager);
	control = descriptionSection.createControl(parent, factory);
	if (SWT.getPlatform().equals("motif"))
	   gd = new GridData(GridData.FILL_HORIZONTAL
	                            | GridData.VERTICAL_ALIGN_BEGINNING);
	else 
	   gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	control.setLayoutData(gd);

	// wire sections
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(elementSection, grammarSection);
	manager.linkSections(elementSection, descriptionSection);

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
	setTitle(schema.getName());
	getControl().layout(true);
	schema.addModelChangedListener(this);
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		ISchema schema = (ISchema) page.getModel();
		setTitle(schema.getName());
	}
}
}
