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

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;

public class SchemaEditor extends PDEMultiPageXMLEditor {
	public static final String DEFINITION_PAGE = "definition";
	public static final String DOC_PAGE = "documentation";
	public static final String SOURCE_PAGE = "source";
	public static final String KEY_OLD_EXTENSION = "SchemaEditor.oldExtension";
	private ShowDescriptionAction previewAction;

	public SchemaEditor() {
		super();
	}

	protected Object createModel(Object input) {
		if (input instanceof File)
			return createExternalModel((File)input);
			
		if (!(input instanceof IFile)) {
			if (input instanceof IStorage)
				return createStorageModel((IStorage)input);
			return null;
		}

		IFile file = (IFile) input;
		FileSchemaDescriptor sd = new FileSchemaDescriptor(file);
		ISchema schema = sd.getSchema();
		if (schema.isValid() == false)
			return null;
		warnIfOldExtension(file.getName());
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}
	
	private Object createExternalModel(File file) {
		ExternalSchemaDescriptor sd = new ExternalSchemaDescriptor(file, "", false);

		ISchema schema = sd.getSchema();
		if (schema.isValid() == false)
			return null;
		warnIfOldExtension(file.getName());
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}
	
	private Object createStorageModel(IStorage storage) {
		StorageSchemaDescriptor sd = new StorageSchemaDescriptor(storage);
		ISchema schema = sd.getSchema();
		if (schema.isValid()==false)
		return null;
		warnIfOldExtension(storage.getName());
		return schema;
	}

	private void warnIfOldExtension(String name) {
		int dotLoc = name.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = name.substring(dotLoc + 1).toLowerCase();
			if (ext.equals("xsd")) {
				String title = getSite().getRegisteredName();
				String message = PDEPlugin.getResourceString(KEY_OLD_EXTENSION);
				MessageDialog.openWarning(
					PDEPlugin.getActiveWorkbenchShell(),
					title,
					message);
			}
		}
	}

	public void dispose() {
		PDECore.getDefault().getTempFileManager().disconnect(this);
		super.dispose();
	}

	void previewReferenceDocument() {
		ISchema schema = (ISchema) getModel();
		if (previewAction==null)
			previewAction = new ShowDescriptionAction(schema);
		else
			previewAction.setSchema(schema);
		previewAction.run();
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
	protected boolean isModelCorrect(Object model) {
		if (model == null)
			return false;
		ISchema schema = (ISchema) model;
		return schema.isValid();
	}
	protected boolean isModelDirty(Object model) {
		return model instanceof IEditable && ((IEditable) model).isDirty();
	}
	protected boolean updateModel() {
		Schema schema = (Schema) getModel();
		if (schema == null)
			return false;
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		String text = document.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			schema.reload(stream);
			if (schema instanceof IEditable)
			   ((IEditable)schema).setDirty(false);
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}
	/*
     * Overriding PDEMultiPageEditor to avoid 
     * a class cast exception on getModel() (bug 35691)
	 */
	public boolean validateModelSemantics() {
		ISchema schema = (ISchema)getModel();
		return schema!=null && schema.isValid();
	}
}
