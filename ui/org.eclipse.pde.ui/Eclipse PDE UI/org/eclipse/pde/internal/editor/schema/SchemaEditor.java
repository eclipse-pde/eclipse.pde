package org.eclipse.pde.internal.editor.schema;

import org.eclipse.ui.views.properties.*;
import java.io.*;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;

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
	InputStream stream = new ByteArrayInputStream(text.getBytes());
	schema.reload(stream);
	try {
		stream.close();
	} catch (IOException e) {
	}
	return true;
}
}
