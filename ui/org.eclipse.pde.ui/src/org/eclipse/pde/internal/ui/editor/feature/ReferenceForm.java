/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;

public class ReferenceForm extends ScrollableSectionForm {
	private static final String KEY_HEADING = "FeatureEditor.ReferencePage.heading";
	private FeatureReferencePage page;
	private PluginSection pluginSection;
	private RequiresSection requiresSection;
	private FeatureMatchSection matchSection;
//	private PortabilitySection portabilitySection;

public ReferenceForm(FeatureReferencePage page) {
	this.page = page;
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	//layout.makeColumnsEqualWidth=true;
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

	pluginSection = new PluginSection(page);
	Control control = pluginSection.createControl(left, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	requiresSection = new RequiresSection(page);
	control = requiresSection.createControl(right, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);	

	matchSection = new FeatureMatchSection(page);
	control = matchSection.createControl(right, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	control.setLayoutData(gd);

/*	
	String title = PDEPlugin.getResourceString(KEY_P_TITLE);
	String desc = PDEPlugin.getResourceString(KEY_P_DESC);

	portabilitySection = new PortabilitySection(page, title, desc, true);
	control = portabilitySection.createControl(right, factory);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	control.setLayoutData(gd);
*/

	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(requiresSection, matchSection);
//	manager.linkSections(requiresSection, portabilitySection);

	registerSection(pluginSection);
	registerSection(requiresSection);
	registerSection(matchSection);
//	registerSection(portabilitySection);
	
	WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FEATURE_CONTENT);
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
