package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class FeatureForm extends ScrollableSectionForm {
	private FeatureFormPage page;
	private URLSection urlSection;
	private FeatureSpecSection specSection;
	private PortabilitySection portabilitySection;
	private HandlerSection handlerSection;

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
	Control control = specSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	urlSection = new URLSection(page);
	control = urlSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL| GridData.VERTICAL_ALIGN_FILL);
	control.setLayoutData(gd);

	portabilitySection = new PortabilitySection(page);
	control = portabilitySection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	handlerSection = new HandlerSection(page);
	control = handlerSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	registerSection(specSection);
	registerSection(urlSection);
	registerSection(portabilitySection);
	registerSection(handlerSection);
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
