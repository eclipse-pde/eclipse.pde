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
package org.eclipse.pde.internal.ui.editor.site;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

public class SiteInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "site-context"; //$NON-NLS-1$
	private boolean storageModel=false;
	/**
	 * @param editor
	 * @param input
	 */
	public SiteInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	protected IBaseModel createModel(IEditorInput input) {
		IBaseModel model = null;
		if (input instanceof IStorageEditorInput) {
			InputStream is = null;
			try {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					is = file.getContents();
					model = createWorkspaceModel(file, is, true);
				} else if (input instanceof IStorageEditorInput) {
					is = ((IStorageEditorInput) input).getStorage()
							.getContents();
					model =  createStorageModel(is);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
				return null;
			}
		}
		return model;
	}

	private IBaseModel createWorkspaceModel(IFile file, InputStream stream,
			boolean editable) {
		WorkspaceSiteModel model = new WorkspaceSiteModel(file);
		try {
			model.setEditable(editable);
			model.load(stream, false);
		} catch (CoreException e) {
		}
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}
	
	private IBaseModel createStorageModel(InputStream stream) {
		ExternalSiteModel model = new ExternalSiteModel();
		try {
			model.load(stream, true);
		} catch (CoreException e) {
		} finally {
			try {
				stream.close();
			} catch (IOException e1) {
			}
		}
		return model;
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getModel();
		if (storageModel) {
			model.dispose();
		}
		super.dispose();
	}
	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isEditable()==false) return;
		if (editableModel.isDirty() == false) return;
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
		ISiteModel model = (ISiteModel) getModel();
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList,
	 *      org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
	}

	protected String getPartitionName() {
		return "___site_partition"; //$NON-NLS-1$
	}
}
