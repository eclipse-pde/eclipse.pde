/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.ui.*;
import org.eclipse.ui.IEditorInput;

/**
 *
 */
public class SchemaInputContext extends XMLInputContext {
	public static final String CONTEXT_ID="schema-context"; //$NON-NLS-1$
	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public SchemaInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		if (input instanceof SystemFileEditorInput)
			return createExternalModel((SystemFileEditorInput)input);

		if (!(input instanceof IFileEditorInput)) {
			if (input instanceof IStorageEditorInput)
				return createStorageModel((IStorageEditorInput)input);
			return null;
		}

		IFile file = ((IFileEditorInput)input).getFile();
		SchemaDescriptor sd = new SchemaDescriptor(file, true);
		ISchema schema = sd.getSchema(false);
		if (schema.isValid() == false)
			return null;
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}
		
	private IBaseModel createExternalModel(SystemFileEditorInput input) {
		File file = (File)input.getAdapter(File.class);
		SchemaDescriptor sd = new SchemaDescriptor(file); //$NON-NLS-1$

		ISchema schema = sd.getSchema(false);
		if (schema.isValid() == false)
			return null;
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}
		
	private IBaseModel createStorageModel(IStorageEditorInput input) {
		try {
			IStorage storage = input.getStorage();
			StorageSchemaDescriptor sd = new StorageSchemaDescriptor(storage);
			ISchema schema = sd.getSchema(false);
			if (schema.isValid()==false)
				return null;
			return schema;
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}
	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try {
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			doc.set(swriter.toString());
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		Schema schema = (Schema) getModel();
		if (schema == null)
			return false;
		String text = doc.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8")); //$NON-NLS-1$
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
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
	}
}
