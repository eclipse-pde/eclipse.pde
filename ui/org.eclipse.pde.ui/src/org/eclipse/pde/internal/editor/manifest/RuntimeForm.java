package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.*;
import org.w3c.dom.Document;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;

public class RuntimeForm extends ScrollableSectionForm {
	private ManifestRuntimePage page;
	private LibrarySection librarySection;
	public static final String TITLE = "ManifestEditor.RuntimeForm.title";
	private JarsSection jarsSection;
	private ExportSection exportSection;

public RuntimeForm(ManifestRuntimePage page) {
	this.page = page;
	//setScrollable(false);
	setVerticalFit(true);
}
protected void createFormClient(Composite parent) {
	GridLayout layout = new GridLayout();
	parent.setLayout(layout);
	layout.numColumns = 2;
	layout.marginWidth = 10;
	layout.horizontalSpacing=15;
	layout.makeColumnsEqualWidth=true;
	librarySection = new LibrarySection(page);
	Control control = librarySection.createControl(parent, getFactory());
	GridData gd = new GridData(GridData.FILL_BOTH);
	//gd.widthHint = 250;
	//gd.heightHint = 300;
	control.setLayoutData(gd);

	exportSection = new ExportSection(page);
	control = exportSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	gd.verticalSpan = 2;
	control.setLayoutData(gd);

	jarsSection = new JarsSection(page);
	control = jarsSection.createControl(parent, getFactory());
	gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	// Link
	SectionChangeManager manager = new SectionChangeManager();
	manager.linkSections(librarySection, exportSection);
	manager.linkSections(librarySection, jarsSection);

	registerSection(librarySection);
	registerSection(exportSection);
	registerSection(jarsSection);
}
public void expandTo(Object object) {
   librarySection.expandTo(object);
}
public void initialize(Object model) {
	setHeadingText(PDEPlugin.getResourceString(TITLE));
	super.initialize(model);
	((Composite)getControl()).layout(true);
}
}
