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
package org.eclipse.pde.internal.ui.neweditor.feature;

import java.io.*;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext;
import org.eclipse.ui.*;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FeatureInputContext extends XMLInputContext {
	public static final String CONTEXT_ID="feature";
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
		model.setInstallLocation("");
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
				new ByteArrayInputStream(text.getBytes("UTF8"));
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
}
