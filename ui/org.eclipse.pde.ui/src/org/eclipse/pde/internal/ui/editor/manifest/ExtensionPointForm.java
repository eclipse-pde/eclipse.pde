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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.*;

public class ExtensionPointForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "ManifestEditor.ExtensionPointForm.title";
	private ManifestExtensionPointPage page;
	private DetailExtensionPointSection extensionPointSection;
	private PointUsageSection usageSection;

public ExtensionPointForm(ManifestExtensionPointPage page) {
	this.page = page;
	setVerticalFit(true);
	setScrollable(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.makeColumnsEqualWidth=true;
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	extensionPointSection = new DetailExtensionPointSection(page);
	Control control = extensionPointSection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	usageSection = new PointUsageSection(page);
	control = usageSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(extensionPointSection, usageSection);

	registerSection(extensionPointSection);
	registerSection(usageSection);

	if (((ManifestEditor)page.getEditor()).isFragmentEditor()	)
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_FRAGMENT_EXT_POINTS);
	else
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_PLUGIN_EXT_POINTS);		

}
public void expandTo(Object object) {
   extensionPointSection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
