package org.eclipse.pde.internal.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.model.build.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.model.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.*;

public class BuildForm extends ScrollableSectionForm {
	public static final String FORM_TITLE = "BuildEditor.Form.title";
	public static final String FORM_RTITLE = "BuildEditor.Form.rtitle";
	private BuildPage page;
	private VariableSection variableSection;
	private TokenSection tokenSection;

public BuildForm(BuildPage page) {
	this.page = page;
	setScrollable(false);
	//setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	layout.makeColumnsEqualWidth = true;
	
	variableSection = new VariableSection(page);
	Control control = variableSection.createControl(parent, factory);
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	tokenSection = new TokenSection(page);
	control = tokenSection.createControl(parent, factory);
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(variableSection, tokenSection);

	registerSection(variableSection);
	registerSection(tokenSection);
}
public void expandTo(Object object) {
	variableSection.expandTo(object);
}
public void initialize(Object modelObject) {
	IBuildModel model = (IBuildModel) modelObject;

	super.initialize(model);
	String title = "";
	if (model instanceof IEditable && model.isEditable() == false) {
		title =
			PDEPlugin.getResourceString(FORM_RTITLE);
	} else
		title =
			PDEPlugin.getResourceString(FORM_TITLE);
	setHeadingText(title);
	((Composite)getControl()).layout(true);
}
}
