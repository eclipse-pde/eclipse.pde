/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

/**
 * 
 */
public class FeatureInputContext extends XMLInputContext {
	public static final String CONTEXT_ID="feature-context"; //$NON-NLS-1$
	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public FeatureInputContext(PDEFormEditor editor, IEditorInput input,
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
		if (input instanceof IFileEditorInput)
			return createResourceModel((IFileEditorInput) input);
		if (input instanceof IStorageEditorInput)
			return createStorageModel((IStorageEditorInput)input);
		return null;		
	}
	
	private IBaseModel createResourceModel(IFileEditorInput input)
				throws CoreException {
		IFile file = input.getFile();
		WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
		model.load();
		return model;
	}
	
	private IBaseModel createStorageModel(IStorageEditorInput input) throws CoreException {
		InputStream stream = null;
		IStorage storage = input.getStorage();
		try {
			stream = storage.getContents();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(""); //$NON-NLS-1$
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			// Errors in the file
			return null;
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
			}
		}
		return model;
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
		IFeatureModel model = (IFeatureModel) getModel();

		boolean cleanModel = true;
		String text = doc.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8")); //$NON-NLS-1$
			try {
				model.reload(stream, false);
			} catch (CoreException e) {
				cleanModel = false;
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
		}
		return cleanModel;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
	}
	protected String getPartitionName() {
		return "___feature_partition"; //$NON-NLS-1$
	}
}
