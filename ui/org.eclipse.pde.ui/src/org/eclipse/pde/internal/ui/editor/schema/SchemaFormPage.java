package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;


public class SchemaFormPage extends PDEFormPage {
	public static final String PAGE_TITLE = "SchemaEditor.FormPage.title";

public SchemaFormPage(PDEMultiPageEditor editor) {
	super(editor, PDEPlugin.getResourceString(PAGE_TITLE));
}
public SchemaFormPage(PDEMultiPageEditor editor, String title) {
	super(editor, title);
}
public IContentOutlinePage createContentOutlinePage() {
	return new SchemaFormOutlinePage(this);
}
protected AbstractSectionForm createForm() {
	return new SchemaForm(this);
}
public IPropertySheetPage createPropertySheetPage() {
	return new SchemaPropertySheet();
}
public Object getModel() {
	return getEditor().getModel();
}
}
