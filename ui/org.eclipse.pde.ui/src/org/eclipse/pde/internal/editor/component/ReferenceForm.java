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

public class ReferenceForm extends ScrollableSectionForm {
	private ComponentReferencePage page;
	private ComponentSpecSection specSection;
	private PluginSection referenceSection;
	private PluginSection fragmentSection;
	private PluginSection pluginSection;
	private DescriptionSection descriptionSection;

public ReferenceForm(ComponentReferencePage page) {
	this.page = page;
	setScrollable(false);
	//setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.makeColumnsEqualWidth=true;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	layout.verticalSpacing=15;
	GridData gd;

	pluginSection = new PluginSection(page, false);
	Control control = pluginSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	fragmentSection = new PluginSection(page, true);
	control = fragmentSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	registerSection(pluginSection);
	registerSection(fragmentSection);
}
public void expandTo(Object object) {
	if (object instanceof IComponentPlugin)
		pluginSection.expandTo(object);
	else
		fragmentSection.expandTo(object);
}
public void initialize(Object modelObject) {
	IComponentModel model = (IComponentModel) modelObject;
	super.initialize(model);
	IComponent component = model.getComponent();
	setHeadingText(component.getLabel());
	((Composite)getControl()).layout(true);
}
}
