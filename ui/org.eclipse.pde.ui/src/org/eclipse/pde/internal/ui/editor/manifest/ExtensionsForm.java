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

public class ExtensionsForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "ManifestEditor.ExtensionForm.title";
	private ManifestExtensionsPage page;
	private DetailExtensionSection extensionSection;
	//private DetailChildrenSection childrenSection;
	private BodyTextSection bodyTextSection;

public ExtensionsForm(ManifestExtensionsPage page) {
	this.page = page;
	setVerticalFit(true);
	setScrollable(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 1;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	extensionSection = new DetailExtensionSection(page);
	Control control = extensionSection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);
	
	bodyTextSection = new BodyTextSection(page);
	bodyTextSection.setCollapsable(true);
	bodyTextSection.setCollapsed(true);
	control = bodyTextSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(extensionSection, bodyTextSection);

	// Register
	registerSection(extensionSection);
	registerSection(bodyTextSection);

	if (((ManifestEditor)page.getEditor()).isFragmentEditor()	)
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_FRAGMENT_EXTENSIONS);
	else
		WorkbenchHelp.setHelp(parent,IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS);		

}

public void expandTo(Object object) {
   extensionSection.expandTo(object);
}

public void openNewExtensionWizard() {
	extensionSection.handleNew();
}

public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
