/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.part.*;

public class SchemaEditor extends MultiSourceEditor {
	private ShowDescriptionAction previewAction;
	
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IFileEditorInput in = new FileEditorInput(file);
		manager.putContext(in, new SchemaInputContext(this, in, true));
		manager.monitorFile(file);		
	}
	
	protected InputContextManager createInputContextManager() {
		SchemaInputContextManager contextManager = new SchemaInputContextManager(this);
		//contextManager.setUndoManager(new SchemaUndoManager(this));
		return contextManager;
	}
	
	public boolean canCopy(ISelection selection) {
		return true;
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

	public void monitoredFileAdded(IFile file) {
		/*
		String name = file.getName();
		if (name.equalsIgnoreCase("site.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new SiteInputContext(this, in, false));
		}
		*/
	}

	public boolean monitoredFileRemoved(IFile file) {
		/*
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		 * */
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
		manager.putContext(input, new SchemaInputContext(this, input,
					true));
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		manager.putContext(input,
							new SchemaInputContext(this, input, true));
	}
	
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}
	
	void previewReferenceDocument() {
		ISchema schema = (ISchema) getAggregateModel();
		if (previewAction==null)
			previewAction = new ShowDescriptionAction(schema);
		else
			previewAction.setSchema(schema);
		previewAction.run();
	}	

	protected void addPages() {
		try {
			addPage(new SchemaFormPage(this));
			addPage(new SchemaDocPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SchemaInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(SchemaInputContext.CONTEXT_ID))
				firstPageId = SchemaFormPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = SchemaFormPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SchemaSourcePage(editor, title, name);
	}
	
	protected ISortableContentOutlinePage createContentOutline() {
		return new SchemaFormOutlinePage(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof ISchemaObject) {
			context = inputContextManager.findContext(SchemaInputContext.CONTEXT_ID);
		}		
		return context;
	}

}
