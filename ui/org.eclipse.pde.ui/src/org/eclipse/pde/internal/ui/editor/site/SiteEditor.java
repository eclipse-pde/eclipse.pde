/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 27, 2004
 */
package org.eclipse.pde.internal.ui.editor.site;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;

public class SiteEditor extends MultiSourceEditor {
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IFile siteFile = null;
		String name = file.getName().toLowerCase();
		if (name.equals("site.xml")) { //$NON-NLS-1$
			siteFile = file;
		}
		if (siteFile.exists()) {
			IEditorInput in = new FileEditorInput(siteFile);
			manager.putContext(in, new SiteInputContext(this, in, file==siteFile));
		}
		manager.monitorFile(siteFile);
	}
	
	protected InputContextManager createInputContextManager() {
		SiteInputContextManager contextManager = new SiteInputContextManager(this);
		contextManager.setUndoManager(new SiteUndoManager(this));
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
		File file = (File) input.getAdapter(File.class);
		File siteFile = null;

		String name = file.getName().toLowerCase();
		if (name.equals("site.xml")) { //$NON-NLS-1$
			siteFile = file;
		}
		if (siteFile.exists()) {
			IEditorInput in = new SystemFileEditorInput(siteFile);
			manager.putContext(in, new SiteInputContext(this, in,
					file == siteFile));
		}
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase();
		if (name.startsWith("site.xml")) { //$NON-NLS-1$
			manager.putContext(input,
							new SiteInputContext(this, input, true));
		}
	}
	
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	protected void addPages() {
		try {
			addPage(new FeaturesPage(this));
			addPage(new ArchivePage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SiteInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(SiteInputContext.CONTEXT_ID))
				firstPageId = FeaturesPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = FeaturesPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SiteSourcePage(editor, title, name);
	}
	
	protected ISortableContentOutlinePage createContentOutline() {
		return new SiteOutlinePage(this);
	}
		
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof ISiteObject) {
			context = inputContextManager
					.findContext(SiteInputContext.CONTEXT_ID);
		}
		return context;
	}

}
