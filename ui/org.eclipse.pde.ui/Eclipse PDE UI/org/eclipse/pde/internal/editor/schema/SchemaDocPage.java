package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.schema.*;
import java.util.*;
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


public class SchemaDocPage extends PDEChildFormPage {
	private IContentOutlinePage outlinePage;
	private Image topicImage;
	public static final String PAGE_TITLE = "SchemaEditor.DocPage.title";


public SchemaDocPage(SchemaFormPage mainPage) {
	super(mainPage, PDEPlugin.getResourceString(PAGE_TITLE));
}
protected Form createForm() {
	return new DocForm(this);
}
public IContentOutlinePage getContentOutlinePage() {
	return null;
}
private boolean isElementComplete(ISchemaElement element) {
	if (isObjectComplete(element)) return false;
	ISchemaType type = element.getType();
	if (!(type instanceof ISchemaComplexType)) return true;
	ISchemaComplexType complexType = (ISchemaComplexType)type;

	ISchemaAttribute [] attributes = complexType.getAttributes();
	for (int i=0; i<attributes.length; i++) {
		ISchemaAttribute att = attributes[i];
		if (isObjectComplete(att)==false) return false;
	}
	return true;
}
private boolean isMarkupComplete() {
	ISchema schema = (ISchema)getModel();
	ISchemaElement [] elements = schema.getElements();
	for (int i=0; i<elements.length; i++) {
		ISchemaElement element = elements[i];
		if (isElementComplete(element)==false) return false;
	}
	return true;
}
private boolean isObjectComplete(ISchemaObject object) {
	String text = object.getDescription();
	if (text != null)
		text.trim();
	return (text != null && text.length() > 0);
}
public void update() {
	super.update();
}
public void updateEditorInput(Object object) {
	((DocForm)getForm()).updateEditorInput(object);
}
}
