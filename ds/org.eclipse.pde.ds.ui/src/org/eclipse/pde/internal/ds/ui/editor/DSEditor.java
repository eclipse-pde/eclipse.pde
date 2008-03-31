/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

// see TOCEditor as an example in org.eclipse.pde.ui

public class DSEditor extends MultiSourceEditor {

	public DSEditor() {
		// TODO Auto-generated constructor stub
	}

	protected void addEditorPages() {
		// TODO Auto-generated method stub

	}

	protected ISortableContentOutlinePage createContentOutline() {
		// TODO Auto-generated method stub
		return null;
	}

	protected InputContextManager createInputContextManager() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		// TODO Auto-generated method stub

	}

	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
		// TODO Auto-generated method stub

	}

	protected void createSystemFileContexts(InputContextManager contexts,
			SystemFileEditorInput input) {
		// TODO Auto-generated method stub

	}

	public void editorContextAdded(InputContext context) {
		// TODO Auto-generated method stub

	}

	protected String getEditorID() {
		// TODO Auto-generated method stub
		return null;
	}

	protected InputContext getInputContext(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	public void contextRemoved(InputContext context) {
		// TODO Auto-generated method stub

	}

	public void monitoredFileAdded(IFile monitoredFile) {
		// TODO Auto-generated method stub

	}

	public boolean monitoredFileRemoved(IFile monitoredFile) {
		// TODO Auto-generated method stub
		return false;
	}

}
