/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.IConstants;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;

public class DSEditor extends MultiSourceEditor {

	public DSEditor() {
		super();
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new DSOverviewPage(this));
			addPage(new DSServicesPage(this));
		} catch (PartInitException e) {
			Activator.logException(e);
		}
		// Add source page
		addSourcePage(DSInputContext.CONTEXT_ID);

	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		// TODO add help icon here maybe?
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new DSFormOutlinePage(this);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		return new DSInputContextManager(this);
		}

	@Override
	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		contexts.putContext(input, new DSInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	@Override
	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
		contexts.putContext(input, new DSInputContext(this, input, true));
	}

	@Override
	protected void createSystemFileContexts(InputContextManager contexts,
			FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			contexts.putContext(in, new DSInputContext(this, in, true));
		} catch (CoreException e) {
			Activator.logException(e);
		}
	}

	private void addDSBuilder(IFile file) {
		try {
			// Add builder
			IProject project = file.getProject();
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();

			for (ICommand command : commands) {
				if (command.getBuilderName().equals(IConstants.ID_BUILDER)) {
					return;
				}
			}

			ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			ICommand command = description.newCommand();
			command.setBuilderName(IConstants.ID_BUILDER);
			newCommands[newCommands.length - 1] = command;
			description.setBuildSpec(newCommands);
			project.setDescription(description, null);

		} catch (CoreException e) {
			Activator.logException(e, null, null);
		}

	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	@Override
	protected String getEditorID() {
		return IConstants.ID_EDITOR;
	}

	@Override
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(DSInputContext.CONTEXT_ID);
	}

	@Override
	public void contextRemoved(InputContext context) {
		close(false);
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {
		// no op
	}

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFileEditorInput fileInput = (IFileEditorInput) input;
			addDSBuilder(fileInput.getFile());
		}
		super.doSave(monitor);
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor,
			String title, String name, String contextId) {
		return new DSSourcePage(editor, title, name);
	}
}
