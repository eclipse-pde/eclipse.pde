package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.*;

public class JarsForm extends ScrollableForm {
	public static final String FORM_TITLE = "JarsEditor.JarsForm.title";
	public static final String FORM_RTITLE = "JarsEditor.JarsForm.rtitle";
	private JarsPage page;
	private LibrarySection librarySection;
	private FolderSection folderSection;

public JarsForm(JarsPage page) {
	this.page = page;
	setScrollable(false);
}
protected void createFormClient(Composite parent) {
	FormWidgetFactory factory = getFactory();
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	
	librarySection = new LibrarySection(page);
	Control control = librarySection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	folderSection = new FolderSection(page);
	control = folderSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(librarySection, folderSection);

	registerSection(librarySection);
	registerSection(folderSection);
}
public void expandTo(Object object) {
	librarySection.expandTo(object);
}
public void initialize(Object modelObject) {
	IJarsModel model = (IJarsModel) modelObject;

	super.initialize(model);
	if (model instanceof IEditable && model.isEditable() == false) {
		setTitle(PDEPlugin.getResourceString(FORM_RTITLE));
	} else
		setTitle(PDEPlugin.getResourceString(FORM_TITLE));
	getControl().layout(true);
}
}
