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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class BuildEditor extends MultiSourceEditor {
	public BuildEditor() {
	}
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();

		manager.putContext(input, new BuildInputContext(this, input, true));
		manager.monitorFile(file);
	}
	
	protected InputContextManager createInputContextManager() {
		BuildInputContextManager manager =  new BuildInputContextManager(this);
		manager.setUndoManager(new BuildUndoManager(this));
		return manager;
	}
	
	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		if (name.equalsIgnoreCase("build.properties")) { //$NON-NLS-1$
			if (!inputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				inputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}
	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}
	public void contextRemoved(InputContext context) {
		if (context.isPrimary()) {
			close(true);
			return;
		}
		IFormPage page = findPage(context.getId());
		if (page!=null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	public boolean canCopy(ISelection selection) {
		return true;
	}
	
	protected void addPages() {
		try {
			if (getEditorInput() instanceof IFileEditorInput)
				addPage(new BuildPage(this));			
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(BuildInputContext.CONTEXT_ID))
				firstPageId = BuildPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = BuildPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new BuildSourcePage(editor, title, name);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new BuildOutlinePage(this);
	}
	
	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return null;
	}

	public String getTitle() {
		return super.getTitle();
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IBuildModel) model).isValid() : false;
	}
	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers =
				new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
			for (int i = 0; i < types.length; i++) {
				for (int j = 0; j < transfers.length; j++) {
					if (transfers[j].isSupportedType(types[i]))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	public Object getAdapter(Class key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}	
}
