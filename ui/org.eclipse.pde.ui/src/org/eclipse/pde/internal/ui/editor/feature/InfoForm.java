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
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.ui.forms.internal.*;

public class InfoForm extends ScrollableSectionForm {
	private static final String KEY_TITLE = "FeatureEditor.InfoPage.heading";
	private InfoFormPage page;
	private IColorManager colorManager = new ColorManager();
	private InfoSection infoSection;

public InfoForm(InfoFormPage page) {
	this.page = page;
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.marginWidth = 10;
	GridData gd;
	
	infoSection = new InfoSection(page, colorManager);
	Control control = infoSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	registerSection(infoSection);
	
	WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_FEATURE_INFO);
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
