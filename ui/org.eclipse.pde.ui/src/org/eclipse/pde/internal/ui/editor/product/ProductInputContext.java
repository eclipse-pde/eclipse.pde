/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 97149, bug 268363
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 547322
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.product.ProductModel;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.*;

public class ProductInputContext extends XMLInputContext {

	public static final String CONTEXT_ID = "product-context"; //$NON-NLS-1$

	public ProductInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		IProductModel model = null;
		if (input instanceof IStorageEditorInput) {
			try {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					model = new WorkspaceProductModel(file, true);
					model.load();
				} else if (input instanceof IStorageEditorInput) {
					InputStream is = new BufferedInputStream(((IStorageEditorInput) input).getStorage().getContents());
					model = new ProductModel();
					model.load(is, false);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
				return null;
			}
		} else if (input instanceof IURIEditorInput) {
			IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
			InputStream is = store.openInputStream(EFS.CACHE, new NullProgressMonitor());
			model = new ProductModel();
			model.load(is, false);
		}
		return model;
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
	}

	@Override
	protected void flushModel(IDocument doc) {
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			editableModel.save(writer);
			writer.flush();
			String content = swriter.toString();
			content = AbstractModel.fixLineDelimiter(content, (IFile) ((IModel) getModel()).getUnderlyingResource());
			doc.set(content);
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected boolean synchronizeModel(IDocument doc) {
		IProductModel model = (IProductModel) getModel();
		boolean cleanModel = true;
		String text = doc.get();
		try (InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
			model.reload(stream, false);
		} catch (CoreException e) {
			cleanModel = false;
		} catch (IOException e) {
		}
		return cleanModel;
	}

	@Override
	protected void reorderInsertEdits(ArrayList<TextEdit> ops) {
	}

	@Override
	protected String getPartitionName() {
		return "___prod_partition"; //$NON-NLS-1$
	}

}
