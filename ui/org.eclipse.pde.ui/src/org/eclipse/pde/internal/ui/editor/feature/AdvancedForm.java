package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class AdvancedForm extends ScrollableSectionForm {
	private static final String KEY_HEADING = "FeatureEditor.AdvancedPage.heading";
	private FeatureAdvancedPage page;
	private IncludedFeaturesSection includedSection;
	private DataSection dataSection;
	private HandlerSection handlerSection;

public AdvancedForm(FeatureAdvancedPage page) {
	this.page = page;
	setVerticalFit(true);
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
	
	Composite left = factory.createComposite(parent);
	layout = new GridLayout();
	layout.marginWidth = layout.marginHeight = 0;
	left.setLayout(layout);
	gd = new GridData(GridData.FILL_BOTH);
	left.setLayoutData(gd);
	
	Composite right = factory.createComposite(parent);
	layout = new GridLayout();
	layout.marginWidth = layout.marginHeight = 0;
	right.setLayout(layout);
	gd = new GridData(GridData.FILL_BOTH);
	right.setLayoutData(gd);

	Control control;
	
	includedSection = new IncludedFeaturesSection(page);
	control = includedSection.createControl(left, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	dataSection = new DataSection(page);
	control = dataSection.createControl(right, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	handlerSection = new HandlerSection(page);
	control = handlerSection.createControl(right, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	registerSection(includedSection);
	registerSection(dataSection);
	registerSection(handlerSection);
	
	WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FEATURE_ADVANCED);
}

public void expandTo(Object object) {
	if (object instanceof IFeatureChild)
		includedSection.expandTo(object);
	if (object instanceof IFeatureData)
		dataSection.expandTo(object);
}

public void initialize(Object modelObject) {
	IFeatureModel model = (IFeatureModel) modelObject;
	super.initialize(model);
	setHeadingText(PDEPlugin.getResourceString(KEY_HEADING));
	((Composite)getControl()).layout(true);
}
}
