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
