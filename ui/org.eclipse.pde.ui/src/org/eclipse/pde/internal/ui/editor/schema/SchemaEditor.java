package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.core.ischema.ISchema;

public class SchemaEditor extends PDEMultiPageXMLEditor {
	public static final String DEFINITION_PAGE = "definition";
	public static final String DOC_PAGE = "documentation";
	public static final String SOURCE_PAGE = "source";

public SchemaEditor() {
	super();
}
protected Object createModel(Object input) {
	if (!(input instanceof IFile))
		return null;

	IFile file = (IFile) input;
	FileSchemaDescriptor sd = new FileSchemaDescriptor(file);
	ISchema schema=sd.getSchema();
	if (schema instanceof EditableSchema) {
		((EditableSchema)schema).setNotificationEnabled(true);
	}
	return schema;
}
protected void createPages() {
	firstPageId = DEFINITION_PAGE;
	SchemaFormPage form = new SchemaFormPage(this);
	SchemaDocPage doc = new SchemaDocPage(form);
	addPage(DEFINITION_PAGE, form);
	addPage(DOC_PAGE, doc);
	addPage(SOURCE_PAGE, new SchemaSourcePage(this));
}
public IPDEEditorPage getHomePage() {
	return getPage(DEFINITION_PAGE);
}
protected String getSourcePageId() {
	return SOURCE_PAGE;
}
protected boolean isModelDirty(Object model) {
	return model instanceof IEditable && ((IEditable)model).isDirty();
}
protected boolean updateModel() {
	Schema schema = (Schema)getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	String text = document.get();
	try {
		InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8"));
		schema.reload(stream);
		try {
			stream.close();
		} catch (IOException e) {
		}
	}
	catch (UnsupportedEncodingException e) {
		PDEPlugin.logException(e);
	}
	return true;
}
}
