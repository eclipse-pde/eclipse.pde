/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import java.io.*;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSWorkspaceModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.UTF8InputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

public class CompCSInputContext extends UTF8InputContext {

	public static final String CONTEXT_ID = "compcs-context"; //$NON-NLS-1$

	public CompCSInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops, IModelChangedEvent event) {
		// NO-OP
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		ICompCSModel model = null;
		if (input instanceof IStorageEditorInput) {
			try {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					model = new CompCSWorkspaceModel(file, true);
					model.load();
				} else if (input instanceof IStorageEditorInput) {
					InputStream is = new BufferedInputStream(
							((IStorageEditorInput) input).getStorage()
									.getContents());
					model = new CompCSModel();
					model.load(is, false);
				}
			} catch (CoreException e) {
				PDEUserAssistanceUIPlugin.logException(e);
				return null;
			}
		}
		return model;
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected String getPartitionName() {
		return "___compcs_partition"; //$NON-NLS-1$
	}

	@Override
	protected void flushModel(IDocument doc) {
		if ((getModel() instanceof IEditable) == false) {
			return;
		}
		IEditable editableModel = (IEditable) getModel();
		// No need to flush the model if it is not dirty
		if (editableModel.isDirty() == false) {
			return;
		}
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			String content = swriter.toString();
			content = AbstractModel.fixLineDelimiter(content,
					(IFile) ((IModel) getModel()).getUnderlyingResource());
			doc.set(content);
		} catch (IOException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
	}

}
