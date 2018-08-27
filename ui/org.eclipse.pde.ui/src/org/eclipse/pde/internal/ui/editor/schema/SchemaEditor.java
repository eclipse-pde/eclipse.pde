/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.schema;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class SchemaEditor extends MultiSourceEditor {
	private ShowDescriptionAction fPreviewAction;

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.SCHEMA_EDITOR_ID;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public String getContextIDForSaveAs() {
		return SchemaInputContext.CONTEXT_ID;
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();
		IFileEditorInput in = new FileEditorInput(file);
		manager.putContext(in, new SchemaInputContext(this, in, true));
		manager.monitorFile(file);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		SchemaInputContextManager contextManager = new SchemaInputContextManager(this);
		//contextManager.setUndoManager(new SchemaUndoManager(this));
		return contextManager;
	}

	@Override
	public void monitoredFileAdded(IFile file) {
		/*
		String name = file.getName();
		if (name.equalsIgnoreCase("site.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new SiteInputContext(this, in, false));
		}
		*/
	}

	@Override
	public boolean monitoredFileRemoved(IFile file) {
		/*
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		 * */
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
		manager.putContext(input, new SchemaInputContext(this, input, true));
	}

	@Override
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

	@Override
	protected void addEditorPages() {
		try {
			addPage(new SchemaOverviewPage(this));
			addPage(new SchemaFormPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SchemaInputContext.CONTEXT_ID);
	}

	@Override
	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			firstPageId = SchemaOverviewPage.PAGE_ID;
		}
		return firstPageId;
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SchemaSourcePage(editor, title, name);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new SchemaFormOutlinePage(this);
	}

	@Override
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
		IPluginModelBase model = PluginRegistry.findModel(pluginId);
		if (model != null && model.getUnderlyingResource() != null) {
			IProject project = model.getUnderlyingResource().getProject();
			IFile file = project.getFile(path.removeFirstSegments(1));
			return openSchema(file);
		}
		return false;
	}

	public static boolean openSchema(File file) {
		// Ensure the file exists
		if ((file == null) || (file.exists() == false)) {
			return false;
		}
		// Create the editor input
		IEditorInput input = null;
		try {
			IFileStore store = EFS.getStore(file.toURI());
			input = new FileStoreEditorInput(store);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return openEditor(input);
	}

	private static boolean openEditor(IEditorInput input) {
		IEditorPart part = null;
		try {
			// Open the schema editor using the editor input
			part = PDEPlugin.getActivePage().openEditor(input, IPDEUIConstants.SCHEMA_EDITOR_ID);
		} catch (PartInitException e) {
			PDEPlugin.log(e);
			return false;
		}
		// Ensure the schema editor was opened properly
		if (part == null) {
			return false;
		}
		return true;
	}

	public static boolean openSchema(File jarFile, String schemaJarFileEntry) {
		// Ensure the file exists
		if ((jarFile == null) || (jarFile.exists() == false)) {
			return false;
		}
		// Open the jar archive
		try (ZipFile zipFile = new ZipFile(jarFile);) {
			// Ensure the schema file exists in the jar archive
			if ((schemaJarFileEntry == null) || zipFile.getEntry(schemaJarFileEntry) == null) {
				return false;
			}
			// Create the editor input
			IStorage storage = new JarEntryFile(zipFile, schemaJarFileEntry);
			IEditorInput input = new JarEntryEditorInput(storage);
			return openEditor(input);
		} catch (ZipException e) {
			PDEPlugin.log(e);
		} catch (IOException e) {
			PDEPlugin.log(e);
		}
		return false;
	}

	public static void openToElement(IPath path, ISchemaElement element) {
		if (openSchema(path)) {
			IEditorPart editorPart = PDEPlugin.getActivePage().getActiveEditor();
			if (!(editorPart instanceof SchemaEditor))
				return; // something messed up, schema editor should be open
			SchemaEditor schemaEditor = (SchemaEditor) editorPart;
			schemaEditor.selectReveal(element);
		}
	}

	@Override
	public boolean canCut(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object selected = ((IStructuredSelection) selection).getFirstElement();
			if (selected instanceof ISchemaRootElement) {
				return false;
			} else if (selected instanceof ISchemaAttribute) {
				if (((ISchemaAttribute) selected).getParent() instanceof ISchemaRootElement) {
					return false;
				}
			}
		}

		return super.canCut(selection);
	}

}
