/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.editor.plugin.PluginExportAction;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class BuildEditor extends MultiSourceEditor {

	private PluginExportAction fExportAction;

	public BuildEditor() {
	}

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.BUILD_EDITOR_ID;
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();

		manager.putContext(input, new BuildInputContext(this, input, true));
		manager.monitorFile(file);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		BuildInputContextManager manager = new BuildInputContextManager(this);
		manager.setUndoManager(new BuildUndoManager(this));
		return manager;
	}

	@Override
	public void monitoredFileAdded(IFile file) {
		if (fInputContextManager == null)
			return;
		String name = file.getName();
		if (name.equalsIgnoreCase(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}

	@Override
	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
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
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	@Override
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		manager.putContext(input, new BuildInputContext(this, input, true));
	}

	@Override
	protected void addEditorPages() {
		try {
			if (getEditorInput() instanceof IFileEditorInput)
				addPage(new BuildPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

	@Override
	protected String computeInitialPageId() {
		// Retrieve the initial page
		String firstPageId = super.computeInitialPageId();
		// If none is defined, return the default
		if (firstPageId == null) {
			return BuildPage.PAGE_ID;
		}

		return firstPageId;
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new BuildSourcePage(editor, title, name);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new BuildOutlinePage(this);
	}

	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return null;
	}

	@Override
	public String getTitle() {
		return super.getTitle();
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IBuildModel) model).isValid() : false;
	}

	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
			for (TransferData type : types) {
				for (Transfer transfer : transfers) {
					if (transfer.isSupportedType(type))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	@Override
	public <T> T getAdapter(Class<T> key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	@Override
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		}
		return context;
	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		manager.add(getExportAction());
	}

	private PluginExportAction getExportAction() {
		if (fExportAction == null) {
			fExportAction = new PluginExportAction(this);
			fExportAction.setToolTipText(PDEUIMessages.PluginEditor_exportTooltip);
			fExportAction.setImageDescriptor(PDEPluginImages.DESC_EXPORT_PLUGIN_TOOL);
		}
		return fExportAction;
	}
}
