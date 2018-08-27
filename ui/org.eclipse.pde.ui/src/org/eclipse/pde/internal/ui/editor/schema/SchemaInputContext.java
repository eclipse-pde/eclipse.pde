/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 *
 */
public class SchemaInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "schema-context"; //$NON-NLS-1$

	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public SchemaInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		if (input instanceof FileStoreEditorInput)
			return createExternalModel((FileStoreEditorInput) input);

		if (!(input instanceof IFileEditorInput)) {
			if (input instanceof IStorageEditorInput)
				return createStorageModel((IStorageEditorInput) input);
			return null;
		}

		IFile file = ((IFileEditorInput) input).getFile();
		SchemaDescriptor sd = new SchemaDescriptor(file, true);
		ISchema schema = sd.getSchema(false);
		if (schema instanceof EditableSchema) {
			((EditableSchema) schema).setNotificationEnabled(true);
		}
		return schema;
	}

	private IBaseModel createExternalModel(FileStoreEditorInput input) {
		File file = input.getAdapter(File.class);
		if (file == null) {
			URI uri = input.getURI();
			if (uri != null){
				file = new File(uri);
			}
			else{
				return null;
			}
		}

		SchemaDescriptor sd = new SchemaDescriptor(file);

		ISchema schema = sd.getSchema(false);
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
			return schema;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
	}

	@Override
	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			editableModel.save(writer);
			writer.flush();
			String content = swriter.toString();

			if (getInput() instanceof IFileEditorInput) {
				// If we are working with a file in the workspace, correct line delimeters based on workspace/file settings
				IFile file = ((IFileEditorInput) getInput()).getFile();
				content = AbstractModel.fixLineDelimiter(content, file);
			}

			doc.set(content);
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	public void flushEditorInput() {
		// Override parent, since this editor does not utilize edit operations
		IDocumentProvider provider = getDocumentProvider();
		IEditorInput input = getInput();
		IDocument doc = provider.getDocument(input);
		provider.aboutToChange(input);
		flushModel(doc);
		provider.changed(input);
		setValidated(false);
	}

	@Override
	protected boolean synchronizeModel(IDocument doc) {
		Schema schema = (Schema) getModel();
		if (schema == null) {
			// if model is null try to recreate it
			create();
			return getModel() == null;
		}
		String text = doc.get();
		try (InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
			schema.reload(stream);
			if (schema instanceof IEditable) {
				((IEditable) schema).setDirty(false);
			}
		} catch (IOException e) {
		}
		return true;
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
	}

	@Override
	protected String getPartitionName() {
		return "___schema_partition"; //$NON-NLS-1$
	}
}
