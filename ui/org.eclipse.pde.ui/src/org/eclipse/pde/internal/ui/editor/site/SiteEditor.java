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
package org.eclipse.pde.internal.ui.editor.site;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;

public class SiteEditor extends PDEMultiPageXMLEditor {
	public static final String FEATURES_PAGE = "FeaturesPage";
	public static final String ARCHIVE_PAGE = "ArchivePage";
	public static final String SOURCE_PAGE = "SourcePage";
	private boolean storageModel = false;

	public SiteEditor() {
		super();
	}

	protected IModelUndoManager createModelUndoManager() {
		return new SiteUndoManager(this);
	}

	public boolean canCopy(ISelection selection) {
		return true;
	}
	protected Object createModel(Object input) throws CoreException {
		if (input instanceof IFile)
			return createResourceModel((IFile) input);
		return null;
	}
	protected void createPages() {
		firstPageId = FEATURES_PAGE;
		formWorkbook.setFirstPageSelected(false);
		FeaturesPage sitePage =
			new FeaturesPage(this, PDEPlugin.getResourceString("SiteEditor.page1"));
		addPage(FEATURES_PAGE, sitePage);
		addPage(ARCHIVE_PAGE, new ArchivePage(this, "Site Layout"));
		addPage(SOURCE_PAGE, new SiteSourcePage(this));
	}
	private ISiteModel createResourceModel(IFile file) throws CoreException {
		InputStream stream = null;
		stream = file.getContents(false);

		WorkspaceModelManager provider =
			PDECore.getDefault().getWorkspaceModelManager();
		ISiteModel model = (ISiteModel) provider.getModel(file);
		//boolean cleanModel = true;
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			//cleanModel = false;
		}
		IPath buildPath =
			file.getProject().getFullPath().append(
				PDECore.SITEBUILD_DIR).append(
				PDECore.SITEBUILD_PROPERTIES);
		IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
		ISiteBuildModel buildModel =
			(ISiteBuildModel) provider.getModel(buildFile);
		try {
			buildModel.load();
		} catch (CoreException e) {
		}
		model.setBuildModel(buildModel);
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}
	
	
	public void dispose() {
		super.dispose();
		ISiteModel model = (ISiteModel) getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (storageModel) {
			model.dispose();
			if (buildModel != null)
				buildModel.dispose();
		}
	}

	public IPDEEditorPage getHomePage() {
		return getPage(FEATURES_PAGE);
	}
	protected String getSourcePageId() {
		return SOURCE_PAGE;
	}
	public String getTitle() {
		if (!isModelCorrect(getModel()))
			return super.getTitle();
		ISiteModel model = (ISiteModel) getModel();
		String name = model.getSite().getLabel();
		if (name == null)
			return super.getTitle();
		return model.getResourceString(name);
	}
	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = clipboard.getAvailableTypes();
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
	protected boolean isModelCorrect(Object model) {
		return model != null ? ((ISiteModel) model).isValid() : false;
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
			if (name.startsWith("site.xml"))
				return true;
		} else {
			if (name.equals("site.xml"))
				return true;
		}
		return false;
	}
	protected boolean updateModel() {
		ISiteModel model = (ISiteModel) getModel();
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		boolean cleanModel = true;
		String text = document.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			try {
				model.reload(stream, false);
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
	public void updateTitle() {
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEMultiPageEditor#doRevert()
	 */
	public void doRevert() {
		try {
			((ISiteModel)getModel()).getBuildModel().load();
			super.doRevert();
		} catch (CoreException e) {
		}
	}
}
