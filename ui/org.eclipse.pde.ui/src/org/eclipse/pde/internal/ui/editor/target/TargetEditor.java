/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;


public class TargetEditor extends PDEFormEditor {
	
	protected static String LAST_PATH;
	
	static {
		Location installLoc = Platform.getInstallLocation();
		if (installLoc == null) {
			LAST_PATH = ""; //$NON-NLS-1$
		}
		URL url = installLoc.getURL();
		LAST_PATH = new Path(url.getPath()).toOSString();
	}

	public TargetEditor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getContextIDForSaveAs()
	 */
	public String getContextIDForSaveAs() {
		return TargetInputContext.CONTEXT_ID;
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContextManager()
	 */
	protected InputContextManager createInputContextManager() {
		return new TargetInputContextManager(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createResourceContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IFileEditorInput)
	 */
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		manager.putContext(input, new TargetInputContext(this, input, true));
		manager.monitorFile(input.getFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createSystemFileContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.pde.internal.ui.editor.SystemFileEditorInput)
	 */
	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		if (file != null) {
			String name = file.getName();
			if (name.endsWith(".target")) {  //$NON-NLS-1$
				IEditorInput in = new SystemFileEditorInput(file);
				manager.putContext(in, new TargetInputContext(this, in, true));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createStorageContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IStorageEditorInput)
	 */
	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		if (input.getName().endsWith(".target")) { //$NON-NLS-1$
			manager.putContext(input, new TargetInputContext(this, input, true));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createContentOutline()
	 */
	protected ISortableContentOutlinePage createContentOutline() {
		return new TargetOutlinePage(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(TargetInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	protected void addEditorPages() {
		try {
			addPage(new OverviewPage(this));
			addPage(new ContentPage(this));
			addPage(new EnvironmentPage(this));
			addPageChangedListener(new IPageChangedListener() {

				public void pageChanged(PageChangedEvent event) {
					Object o = event.getSelectedPage();
					if (o instanceof EnvironmentPage)
						((EnvironmentPage)o).updateChoices();
				}
				
			});
		} catch (PartInitException e) {
			PDEPlugin.log(e);
		}
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void editorContextAdded(InputContext context) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		close(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

}