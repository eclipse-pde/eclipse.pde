/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractEditor;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class SimpleCSEditor extends CSAbstractEditor {

	public SimpleCSEditor() {
		super();
	}

	@Override
	protected String getEditorID() {
		return IConstants.SIMPLE_CHEAT_SHEET_EDITOR_ID;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public String getContextIDForSaveAs() {
		return SimpleCSInputContext.CONTEXT_ID;
	}

	@Override
	protected void addEditorPages() {
		// Add form pages
		try {
			addPage(new SimpleCSDefinitionPage(this));
		} catch (PartInitException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
		// Add source page
		addSourcePage(SimpleCSInputContext.CONTEXT_ID);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new SimpleCSFormOutlinePage(this);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		return new SimpleCSInputContextManager(this);
	}

	@Override
	protected void createResourceContexts(InputContextManager contexts, IFileEditorInput input) {
		contexts.putContext(input, new SimpleCSInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	@Override
	protected void createStorageContexts(InputContextManager contexts, IStorageEditorInput input) {
		contexts.putContext(input, new SimpleCSInputContext(this, input, true));
	}

	@Override
	protected void createSystemFileContexts(InputContextManager contexts, FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			contexts.putContext(in, new SimpleCSInputContext(this, in, true));
		} catch (CoreException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
	}

	@Override
	public void editorContextAdded(InputContext context) {
		// Add the source page
		addSourcePage(context.getId());
	}

	@Override
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(SimpleCSInputContext.CONTEXT_ID);
	}

	@Override
	public void contextRemoved(InputContext context) {
		close(false);
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {
		// NO-OP
	}

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	@Override
	public ISelection getSelection() {
		// Override the parent getSelection because it doesn't work.
		// The selection provider operates at the form level and does not
		// track selections made in the master tree view.
		// The selection is required to synchronize the master tree view with
		// the outline view
		IFormPage formPage = getActivePageInstance();
		if ((formPage != null) && (formPage instanceof SimpleCSDefinitionPage)) {
			// Synchronizes the selection made in the master tree view with the
			// selection in the outline view when the link with editor button
			// is toggled on
			return ((SimpleCSDefinitionPage) formPage).getSelection();
		}
		return super.getSelection();
	}

	@Override
	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			firstPageId = SimpleCSDefinitionPage.PAGE_ID;
		}
		return firstPageId;
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SimpleCSSourcePage(editor, title, name);
	}

	@Override
	public boolean canCut(ISelection selection) {
		IFormPage page = getActivePageInstance();
		if (page instanceof PDEFormPage) {
			return ((PDEFormPage) page).canCut(selection);
		}
		return super.canCut(selection);
	}

}
