package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.model.*;
import org.w3c.dom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.w3c.dom.Document;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class ManifestForm extends ScrollableForm {
	private PluginSpecSection pluginSection;
	private ExtensionSection extensionSection;
	private RuntimeSection runtimeSection;
	private AlertSection alertSection;
	private ExtensionPointSection extensionPointSection;
	private RequiresSection requiresSection;
	private ManifestFormPage page;

public ManifestForm(ManifestFormPage page) {
	this.page = page;
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing = 15;
	//layout.makeColumnsEqualWidth=true;
	parent.setLayout(layout);

	FormSection section;
	GridData gd;
	Control control;

	alertSection = new AlertSection(page);
	control = alertSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	control.setLayoutData(gd);

	Composite leftColumn = getFactory().createComposite(parent);
	gd = new GridData(GridData.FILL_BOTH);
	leftColumn.setLayoutData(gd);
	GridLayout leftLayout = new GridLayout();
	leftLayout.verticalSpacing = 10;
	leftLayout.marginWidth = 0;
	leftColumn.setLayout(leftLayout);

	Composite rightColumn = getFactory().createComposite(parent);
	gd = new GridData(GridData.FILL_BOTH);
	rightColumn.setLayoutData(gd);
	GridLayout rightLayout = new GridLayout();
	rightLayout.verticalSpacing = 10;
	rightLayout.marginWidth = 0;
	rightColumn.setLayout(rightLayout);

	pluginSection = new PluginSpecSection(page);
	pluginSection.setTitleAsHyperlink(true);
	control = pluginSection.createControl(leftColumn, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	extensionSection = new ExtensionSection(page);
	extensionSection.setTitleAsHyperlink(true);
	control = extensionSection.createControl(leftColumn, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	runtimeSection = new RuntimeSection(page);
	runtimeSection.setTitleAsHyperlink(true);
	control = runtimeSection.createControl(rightColumn, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	boolean fragment = ((ManifestEditor)page.getEditor()).isFragmentEditor();

	if (!fragment) {
		requiresSection = new RequiresSection(page);
		requiresSection.setTitleAsHyperlink(true);
		control = requiresSection.createControl(rightColumn, getFactory());
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);
	}

	extensionPointSection = new ExtensionPointSection(page);
	extensionPointSection.setTitleAsHyperlink(true);
	control = extensionPointSection.createControl(rightColumn, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	gd.grabExcessVerticalSpace = true;
	control.setLayoutData(gd);

	registerSection(alertSection);
	registerSection(pluginSection);
	registerSection(runtimeSection);
	if (!fragment)
		registerSection(requiresSection);
	registerSection(extensionSection);
	registerSection(extensionPointSection);
}
public void initialize(Object modelObject) {
	IPluginModelBase model = (IPluginModelBase)modelObject;
	IPluginBase plugin = model.getPluginBase();
	setTitle(plugin.getName());

	super.initialize(model);
	getControl().layout(true);
}
}
