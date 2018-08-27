/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;

/**
 * DSInputContext
 */
public class DSInputContext extends XMLInputContext {

	public static final String CONTEXT_ID = "ds-context"; //$NON-NLS-1$

	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public DSInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
		// no op

	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);

			DSModel model = new DSModel(document, isReconciling);

			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(Charset.forName(file.getCharset()));
			} else if (input instanceof IURIEditorInput) {
				IFileStore store = EFS.getStore(((IURIEditorInput) input)
						.getURI());
				model.setInstallLocation(store.getParent().toString());
				model.setCharset(getDefaultCharset());
			} else if (input instanceof JarEntryEditorInput) {
				File file = ((JarEntryEditorInput) input)
						.getAdapter(File.class);
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
	protected String getPartitionName() {
		return "___ds_partition"; //$NON-NLS-1$
	}

}
