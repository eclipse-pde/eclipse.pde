/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.ExternalBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.*;

public class BuildPropertiesEditor extends PDEMultiPageEditor {
	public static final String BUILD_PAGE_TITLE = "BuildEditor.BuildPage.title";
	public static final String BUILD_PAGE = "BuildPage";
	public static final String SOURCE_PAGE = "SourcePage";
	private boolean storageModel=false;

	public BuildPropertiesEditor() {
		super();
	}
	protected Object createModel(Object input) throws CoreException {
		if (input instanceof IFile)
			return createResourceModel((IFile) input);
		if (input instanceof IStorage)
			return createStorageModel((IStorage) input);
		return null;
	}

	protected void createPages() {
		firstPageId = BUILD_PAGE;
		formWorkbook.setFirstPageSelected(false);
		BuildPage buildPage =
			new BuildPage(this, PDEPlugin.getResourceString(BUILD_PAGE_TITLE));
		addPage(BUILD_PAGE, buildPage);
		addPage(SOURCE_PAGE, new BuildSourcePage(this));
	}
	private IBuildModel createResourceModel(IFile file) throws CoreException {
		InputStream stream = null;

		stream = file.getContents(false);

		IModelProvider provider =
			PDECore.getDefault().getWorkspaceModelManager();
		provider.connect(file, this);
		IBuildModel model = (IBuildModel) provider.getModel(file, this);
		try {
			model.load(stream, false);
		} catch (CoreException e) {
		}
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}

	private IBuildModel createStorageModel(IStorage storage)
		throws CoreException {
		InputStream stream = null;

		stream = storage.getContents();

		ExternalBuildModel model = new ExternalBuildModel("");
		model.load(stream, false);
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		storageModel = true;
		return model;
	}

	public void dispose() {
		super.dispose();
		IModelProvider provider =
			PDECore.getDefault().getWorkspaceModelManager();
		IModel model = (IModel) getModel();
		if (storageModel)
			model.dispose();
		else
			provider.disconnect(model.getUnderlyingResource(), this);
	}
	public IPDEEditorPage getHomePage() {
		return getPage(BUILD_PAGE);
	}
	protected String getSourcePageId() {
		return SOURCE_PAGE;
	}
	public String getTitle() {
		IEditorInput input = getEditorInput();
		if (input instanceof IStorageEditorInput
			&& !(input instanceof IFileEditorInput)) {
			return ((IStorageEditorInput) input).getName();
		}
		return super.getTitle();
	}
	protected boolean isModelDirty(Object model) {
		return model != null
			&& model instanceof IEditable
			&& model instanceof IModel
			&& ((IModel) model).isEditable()
			&& ((IEditable) model).isDirty();
	}
	protected boolean isValidContentType(IEditorInput input) {
		String name = input.getName().toLowerCase();
		if (input instanceof IStorageEditorInput
			&& !(input instanceof IFileEditorInput)) {
			if (name.startsWith("build.properties"))
				return true;
			else
				return false;
		}
		if (name.equals("build.properties"))
			return true;
		return false;
	}
	protected boolean updateModel() {
		IBuildModel model = (IBuildModel) getModel();
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		String text = document.get();
		boolean cleanModel = true;
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			try {
				model.reload(stream, false);
				if (model instanceof IEditable) {
					((IEditable)model).setDirty(false);
					fireSaveNeeded();
				}
			} catch (CoreException e) {
				cleanModel = false;
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
		}
		return cleanModel;
	}
}
