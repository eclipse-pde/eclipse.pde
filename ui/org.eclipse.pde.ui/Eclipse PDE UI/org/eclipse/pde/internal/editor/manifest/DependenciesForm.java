package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.w3c.dom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.w3c.dom.Document;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class DependenciesForm extends ScrollableForm {
	public static final String TITLE = "ManifestEditor.DependenciesForm.title";
	private ManifestDependenciesPage page;
	private ReqGraphSection reqGraphSection;
	private PluginListSection pluginListSection;

public DependenciesForm(ManifestDependenciesPage page) {
	this.page = page;
	setVerticalFit(true);
}
public void commitChanges(boolean onSave) {
	if (pluginListSection==null) return;
	if (onSave || pluginListSection.isDirty()) pluginListSection.commitChanges(onSave);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	parent.setLayout(layout);
	//layout.makeColumnsEqualWidth=true;

	FormSection section;
	GridData gd;
	Control control;

	pluginListSection = new PluginListSection(page);
	control = pluginListSection.createControl(parent, getFactory());
	gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING);
	//gd.widthHint = 250;
	control.setLayoutData(gd);

	reqGraphSection = new ReqGraphSection(page);
	control =reqGraphSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
	//gd.widthHint = 250;
	control.setLayoutData(gd);

	// Link forms
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(pluginListSection, reqGraphSection);

	registerSection(pluginListSection);
	registerSection(reqGraphSection);
}
public void initialize(Object input) {
	IPluginModel model = (IPluginModel)input;
	setTitle(PDEPlugin.getResourceString(TITLE));
	super.initialize(model);
	reqGraphSection.sectionChanged(pluginListSection, pluginListSection.SELECTION, null);
	getControl().layout(true);
}
}
