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
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class ManifestForm extends ScrollableSectionForm {
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
	FormWidgetFactory factory = getFactory();
	factory.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_ROLLOVER);

	alertSection = new AlertSection(page);
	alertSection.setCollapsable(true);
	control = alertSection.createControl(parent, factory);
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
	pluginSection.setCollapsable(true);
	control = pluginSection.createControl(leftColumn, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	extensionSection = new ExtensionSection(page);
	extensionSection.setCollapsable(true);
	control = extensionSection.createControl(leftColumn, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	runtimeSection = new RuntimeSection(page);
	runtimeSection.setCollapsable(true);
	control = runtimeSection.createControl(rightColumn, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	boolean fragment = ((ManifestEditor)page.getEditor()).isFragmentEditor();

	if (!fragment) {
		requiresSection = new RequiresSection(page);
		requiresSection.setCollapsable(true);
		control = requiresSection.createControl(rightColumn, factory);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		control.setLayoutData(gd);
	}

	extensionPointSection = new ExtensionPointSection(page);
	extensionPointSection.setCollapsable(true);
	control = extensionPointSection.createControl(rightColumn, factory);
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
	String name = plugin.getName();
	if (model.isEditable()==false) {
		name = PDEPlugin.getFormattedMessage(ManifestEditor.KEY_READ_ONLY, name);
	}
	setHeadingText(name);

	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
