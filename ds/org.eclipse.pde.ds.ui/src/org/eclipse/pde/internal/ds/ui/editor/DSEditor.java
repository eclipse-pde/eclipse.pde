/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.ds.ui.IConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;

// see TOCEditor or SimpleCSEditor as an example in org.eclipse.pde.ui

public class DSEditor extends MultiSourceEditor {

	public DSEditor() {
		super();
	}

	protected void addEditorPages() {
		try {
			addPage(new DSPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		// Add source page
		addSourcePage(DSInputContext.CONTEXT_ID);

	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new DSFormOutlinePage(this);
	}

	protected InputContextManager createInputContextManager() {
		return new DSInputContextManager(this);
		}

	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		contexts.putContext(input, new DSInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
		contexts.putContext(input, new DSInputContext(this, input, true));
	}

	protected void createSystemFileContexts(InputContextManager contexts,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		if (file != null) {
			IEditorInput in = new SystemFileEditorInput(file);
			contexts.putContext(in, new DSInputContext(this, in, true));
		}
	}

	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	protected String getEditorID() {
		return IConstants.ID_EDITOR;
	}

	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(DSInputContext.CONTEXT_ID);
	}

	public void contextRemoved(InputContext context) {
		close(false);
	}

	public void monitoredFileAdded(IFile monitoredFile) {
		// no op
	}

	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
}
