/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;

public class TocInputContext extends XMLInputContext {

	public static final String CONTEXT_ID = "toc-context"; //$NON-NLS-1$

	public TocInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);

			TocModel model = new TocModel(document, isReconciling);

			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(Charset.forName(file.getCharset()));
			} else if (input instanceof IURIEditorInput) {
				IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
				model.setInstallLocation(store.getParent().toString());
				model.setCharset(getDefaultCharset());
			} else if (input instanceof JarEntryEditorInput) {
				File file = ((JarEntryEditorInput) input).getAdapter(File.class);
				model.setInstallLocation(file.toString());
				model.setCharset(getDefaultCharset());
			} else {
				model.setCharset(getDefaultCharset());
			}

			model.load();

			return model;
		}

		return null;
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
		// NO-OP
	}

	@Override
	public void doRevert() {
		fEditOperations.clear();
		fOperationTable.clear();
		fMoveOperations.clear();
		AbstractEditingModel model = (AbstractEditingModel) getModel();
		model.reconciled(model.getDocument());
	}

	@Override
	protected String getPartitionName() {
		return "___toc_partition"; //$NON-NLS-1$
	}
}
