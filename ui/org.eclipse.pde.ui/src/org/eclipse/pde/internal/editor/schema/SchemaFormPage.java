package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.schema.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;


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
protected Form createForm() {
	return new SchemaForm(this);
}
public IPropertySheetPage createPropertySheetPage() {
	return new SchemaPropertySheet();
}
public Object getModel() {
	return getEditor().getModel();
}
}
