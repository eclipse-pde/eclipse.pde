/*******************************************************************************
 * Copyright (c) 2009, 2019 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 546803
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
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class CategoryEditor extends MultiSourceEditor {

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.CATEGORY_EDITOR_ID;
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		manager.putContext(input, new CategoryInputContext(this, input, true));
		manager.monitorFile(input.getFile());
	}

	@Override
	protected InputContextManager createInputContextManager() {
		CategoryInputContextManager contextManager = new CategoryInputContextManager(this);
		contextManager.setUndoManager(new CategoryUndoManager(this));
		return contextManager;
	}

	@Override
	public void monitoredFileAdded(IFile file) {
		// do nothing
	}

	@Override
	public boolean monitoredFileRemoved(IFile file) {
		return true;
	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	@Override
	public void contextRemoved(InputContext context) {
		close(false);
	}

	@Override
	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			manager.putContext(in, new CategoryInputContext(this, in, true));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		manager.putContext(input, new CategoryInputContext(this, input, true));
	}

	@Override
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new IUsPage(this));
			addPage(new RepositoryMetadataPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(CategoryInputContext.CONTEXT_ID);
	}

	@Override
	protected String computeInitialPageId() {
		return IUsPage.PAGE_ID;
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new CategoryOutlinePage(this);
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new CategorySourcePage(editor, title, name);
	}

	@Override
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof ISiteObject) {
			context = fInputContextManager.findContext(CategoryInputContext.CONTEXT_ID);
		}
		return context;
	}

}