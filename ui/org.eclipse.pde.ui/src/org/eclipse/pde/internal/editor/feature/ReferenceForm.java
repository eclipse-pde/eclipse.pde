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
import org.eclipse.pde.internal.PDEPlugin;

public class ReferenceForm extends ScrollableSectionForm {
	private static final String KEY_HEADING = "FeatureEditor.ReferencePage.heading";
	private FeatureReferencePage page;
	private PluginSection pluginSection;
	private RequiresSection requiresSection;
	private MatchSection matchSection;

public ReferenceForm(FeatureReferencePage page) {
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

	pluginSection = new PluginSection(page);
	Control control = pluginSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
	gd.verticalSpan = 2;
	control.setLayoutData(gd);
	
	requiresSection = new RequiresSection(page);
	control = requiresSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);	

	matchSection = new MatchSection(page, false);
	control = matchSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	control.setLayoutData(gd);

	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(requiresSection, matchSection);

	registerSection(pluginSection);
	registerSection(requiresSection);
	registerSection(matchSection);
}

public void expandTo(Object object) {
	if (object instanceof IFeaturePlugin)
		pluginSection.expandTo(object);
	if (object instanceof IFeatureImport)
		requiresSection.expandTo(object);
}

public void initialize(Object modelObject) {
	IFeatureModel model = (IFeatureModel) modelObject;
	super.initialize(model);
	setHeadingText(PDEPlugin.getResourceString(KEY_HEADING));
	((Composite)getControl()).layout(true);
}
}
