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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.text.*;


public class DocForm extends ScrollableSectionForm {
	private IColorManager colorManager = new ColorManager();
	private DocSection docSection;
	public static final String FORM_TITLE = "SchemaEditor.DocForm.title";
	private SchemaDocPage page;

public DocForm(SchemaDocPage page) {
	this.page = page;
	//setWidthHint(600);
	//this.setVerticalFit(true);
	this.setScrollable(false);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	//layout.setMarginWidth 
	parent.setLayout(layout);

	GridData gd;
	Control control;

	docSection = new DocSection(page, colorManager);
	control = docSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	registerSection(docSection);
	
	WorkbenchHelp.setHelp(parent, IHelpContextIds.SCHEMA_EDITOR_DOC);
}
public void dispose() {
	colorManager.dispose();
	super.dispose();
}
public void expandTo(Object object) {
	docSection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(FORM_TITLE));
	super.initialize(model);
}
public void updateEditorInput(Object obj) {
	docSection.updateEditorInput(obj);
}
}
