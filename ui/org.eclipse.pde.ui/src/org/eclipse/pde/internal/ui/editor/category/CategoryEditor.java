/******************************************************************************* 
 * Copyright (c) 2009 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.ui.editor.category;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class CategoryEditor extends PDEFormEditor {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IPDEUIConstants.CATEGORY_EDITOR_ID;
	}

	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		manager.putContext(input, new CategoryInputContext(this, input, true));
		manager.monitorFile(input.getFile());
	}

	protected InputContextManager createInputContextManager() {
		CategoryInputContextManager contextManager = new CategoryInputContextManager(this);
		contextManager.setUndoManager(new CategoryUndoManager(this));
		return contextManager;
	}

	public void monitoredFileAdded(IFile file) {
		// do nothing
	}

	public boolean monitoredFileRemoved(IFile file) {
		return true;
	}

	public void editorContextAdded(InputContext context) {
	}

	public void contextRemoved(InputContext context) {
		close(false);
	}

	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			manager.putContext(in, new CategoryInputContext(this, in, true));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		manager.putContext(input, new CategoryInputContext(this, input, true));
	}

	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	protected void addEditorPages() {
		try {
			addPage(new FeaturesPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	protected String computeInitialPageId() {
		return FeaturesPage.PAGE_ID;
	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new CategoryOutlinePage(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof ISiteObject) {
			context = fInputContextManager.findContext(CategoryInputContext.CONTEXT_ID);
		}
		return context;
	}

}