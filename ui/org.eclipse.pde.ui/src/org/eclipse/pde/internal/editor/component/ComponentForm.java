package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class ComponentForm extends ScrollableSectionForm {
	private ComponentFormPage page;
	private URLSection urlSection;
	private ComponentSpecSection specSection;
	private DescriptionSection descriptionSection;

public ComponentForm(ComponentFormPage page) {
	this.page = page;
	//setScrollable(false);
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	layout.verticalSpacing=15;
	GridData gd;

	specSection = new ComponentSpecSection(page);
	Control control = specSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	urlSection = new URLSection(page);
	control = urlSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	descriptionSection = new DescriptionSection(page);
	control = descriptionSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	control.setLayoutData(gd);

	registerSection(specSection);
	registerSection(urlSection);
	registerSection(descriptionSection);
}
public void expandTo(Object object) {
	urlSection.expandTo(object);
}
public void initialize(Object modelObject) {
	IComponentModel model = (IComponentModel) modelObject;
	super.initialize(model);
	IComponent component = model.getComponent();
	setHeadingText(component.getLabel());
	((Composite)getControl()).layout(true);
}
}
