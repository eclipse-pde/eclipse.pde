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
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.action.*;

public class NewDependenciesForm extends ScrollableSectionForm {
	public static final String TITLE = "ManifestEditor.DependenciesForm.title";
	private ManifestDependenciesPage page;
	private ReqGraphSection reqGraphSection;
	private ImportListSection importListSection;

public NewDependenciesForm(ManifestDependenciesPage page) {
	this.page = page;
	//setVerticalFit(true);
	setScrollable(false);
}
public void commitChanges(boolean onSave) {
	if (importListSection==null) return;
	if (onSave || importListSection.isDirty()) importListSection.commitChanges(onSave);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	//layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	parent.setLayout(layout);
	//layout.makeColumnsEqualWidth=true;

	FormSection section;
	GridData gd;
	Control control;

	importListSection = new ImportListSection(page);
	control = importListSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
/*
	reqGraphSection = new ReqGraphSection(page);
	control =reqGraphSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
	//gd.widthHint = 250;
	control.setLayoutData(gd);

*/	// Link forms
//	SectionChangeManager manager = new SectionChangeManager();
//	manager.linkSections(pluginListSection, reqGraphSection);

	registerSection(importListSection);
//	registerSection(reqGraphSection);
}
public void initialize(Object input) {
	IPluginModel model = (IPluginModel)input;
	setHeadingText(PDEPlugin.getResourceString(TITLE));
	super.initialize(model);
//	reqGraphSection.sectionChanged(pluginListSection, pluginListSection.SELECTION, null);
	((Composite)getControl()).layout(true);
}

public boolean fillContextMenu(IMenuManager manager) {
	//return importListSection.fillContextMenu(manager);
	return true;
}
}
