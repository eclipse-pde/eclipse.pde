/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;

public class SchemaEditor extends MultiSourceEditor {
	private ShowDescriptionAction fPreviewAction;
	
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
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

	protected void createSystemFileContexts(InputContextManager manager, SystemFileEditorInput input) {
		manager.putContext(input, new SchemaInputContext(this, input, true));
	}

	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		manager.putContext(input, new SchemaInputContext(this, input, true));
	}
	
	void previewReferenceDocument() {
		ISchema schema = (ISchema) getAggregateModel();
		if (fPreviewAction == null)
			fPreviewAction = new ShowDescriptionAction(schema);
		else
			fPreviewAction.setSchema(schema);
		fPreviewAction.run();
	}	

	protected void addPages() {
		try {
		 	addPage(new SchemaOverviewPage(this));
			addPage(new SchemaFormPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SchemaInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			firstPageId = SchemaOverviewPage.PAGE_ID;
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
			context = fInputContextManager.findContext(SchemaInputContext.CONTEXT_ID);
		}		
		return context;
	}
	
	public static boolean openSchema(IFile file) {
		if (file != null && file.exists()) {
			IEditorInput input = new FileEditorInput(file);
			try {
				return PDEPlugin.getActivePage().openEditor(input, IPDEUIConstants.SCHEMA_EDITOR_ID) != null;
			} catch (PartInitException e) {
			}
		}
		return false;
	}

	public static boolean openSchema(IPath path) {
		String pluginId = path.segment(0);
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null && model.getUnderlyingResource() != null) {
			IProject project = model.getUnderlyingResource().getProject();
			IFile file = project.getFile(path.removeFirstSegments(1));
			return openSchema(file);
		}
		return false;
	}
	
	public static void openToElement(IPath path, ISchemaElement element) {
		if (openSchema(path)) {
			IEditorPart editorPart = PDEPlugin.getActivePage().getActiveEditor();
			if (!(editorPart instanceof SchemaEditor))
				return; // something messed up, schema editor should be open
			SchemaEditor schemaEditor = (SchemaEditor)editorPart;
			schemaEditor.selectReveal(element);
		}
	}

}
