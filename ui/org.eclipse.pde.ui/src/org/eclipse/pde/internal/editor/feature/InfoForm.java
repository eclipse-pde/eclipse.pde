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
import org.eclipse.pde.internal.editor.manifest.MatchSection;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.pde.internal.PDEPlugin;

public class InfoForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "FeatureEditor.InfoPage.heading";
	private InfoFormPage page;
	private IColorManager colorManager = new ColorManager();
	private InfoSection infoSection;

public InfoForm(InfoFormPage page) {
	this.page = page;
	setScrollable(false);
	//setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.marginWidth = 10;
	GridData gd;
	
	infoSection = new InfoSection(page, colorManager);
	Control control = infoSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	registerSection(infoSection);
}

public void dispose() {
	colorManager.dispose();
	super.dispose();
}

public void expandTo(Object object) {
	if (object instanceof IFeatureInfo)
		infoSection.expandTo(object);
}

public void initialize(Object modelObject) {
	IFeatureModel model = (IFeatureModel) modelObject;
	super.initialize(model);
	setHeadingText(PDEPlugin.getResourceString(KEY_TITLE));
	((Composite)getControl()).layout(true);
}
}
