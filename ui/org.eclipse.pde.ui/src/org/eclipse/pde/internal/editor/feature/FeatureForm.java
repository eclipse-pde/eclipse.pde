package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class FeatureForm extends ScrollableSectionForm {
	private FeatureFormPage page;
	private URLSection urlSection;
	private FeatureSpecSection specSection;

public FeatureForm(FeatureFormPage page) {
	this.page = page;
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

	specSection = new FeatureSpecSection(page);
	Control control = specSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	urlSection = new URLSection(page);
	control = urlSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
/*
	descriptionSection = new DescriptionSection(page);
	control = descriptionSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	control.setLayoutData(gd);
*/
	registerSection(specSection);
	registerSection(urlSection);
	//registerSection(descriptionSection);
}
public void expandTo(Object object) {
	urlSection.expandTo(object);
}
public void initialize(Object modelObject) {
	IFeatureModel model = (IFeatureModel) modelObject;
	super.initialize(model);
	IFeature component = model.getFeature();
	setHeadingText(component.getLabel());
	((Composite)getControl()).layout(true);
}
}
