package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.*;

public class SiteEditor extends PDEMultiPageXMLEditor {
	public static final String SITE_PAGE = "SitePage";
	public static final String BUILD_PAGE = "BuildPage";
	public static final String FEATURE_PAGE = "FeaturePage";
	public static final String ARCHIVE_PAGE = "ArchivePage";
	public static final String SOURCE_PAGE = "SourcePage";
	public static final String SITE_PAGE_TITLE =
		"MultiPageSiteEditor.SitePage.title";
	public static final String BUILD_PAGE_TITLE =
		"MultiPageSiteEditor.BuildPage.title";
	public static final String FEATURE_PAGE_TITLE =
		"MultiPageSiteEditor.FeaturePage.title";
	public static final String ARCHIVE_PAGE_TITLE =
		"MultiPageSiteEditor.ArchivePage.title";
	public static final String SOURCE_PAGE_TITLE =
		"MultiPageSiteEditor.SourcePage.title";
	private boolean storageModel = false;
	private StateListener stateListener;

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
		if (input instanceof IStorage)
			return createStorageModel((IStorage) input);
		return null;
	}
	protected void createPages() {
		firstPageId = SITE_PAGE;
		formWorkbook.setFirstPageSelected(false);
		SitePage sitePage =
			new SitePage(this, PDEPlugin.getResourceString(SITE_PAGE_TITLE));
		BuildPage buildPage =
			new BuildPage(
				sitePage,
				PDEPlugin.getResourceString(BUILD_PAGE_TITLE));
		FeaturePage featurePage =
			new FeaturePage(
				sitePage,
				PDEPlugin.getResourceString(FEATURE_PAGE_TITLE));
		ArchivePage archivePage =
			new ArchivePage(
				sitePage,
				PDEPlugin.getResourceString(ARCHIVE_PAGE_TITLE));
		addPage(SITE_PAGE, sitePage);
		addPage(BUILD_PAGE, buildPage);
		addPage(FEATURE_PAGE, featurePage);
		addPage(ARCHIVE_PAGE, archivePage);
		addPage(SOURCE_PAGE, new SiteSourcePage(this));
	}
	private ISiteModel createResourceModel(IFile file) throws CoreException {
		InputStream stream = null;
		stream = file.getContents(false);

		IModelProvider provider =
			PDECore.getDefault().getWorkspaceModelManager();
		provider.connect(file, this);
		ISiteModel model = (ISiteModel) provider.getModel(file, this);
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
		provider.connect(buildFile, this);
		ISiteBuildModel buildModel =
			(ISiteBuildModel) provider.getModel(buildFile, this);
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
		stateListener = new StateListener(model);
		return model;
	}
	
	private ISiteModel createStorageModel(IStorage storage) {
		/*
		InputStream stream = null;
		try {
			stream = storage.getContents();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ExternalSiteModel model = new ExternalSiteModel();
		model.setInstallLocation("");
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			// Errors in the file
			return null;
		}
		try {
			stream.close();
		} catch (IOException e) {
		}
		storageModel=true;
		return model;
		*/
		return null;
	}
	
	public StateListener getStateListener() {
		return stateListener;
	}
	
	public void dispose() {
		super.dispose();
		IModelProvider provider =
			PDECore.getDefault().getWorkspaceModelManager();
		ISiteModel model = (ISiteModel) getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (storageModel) {
			model.dispose();
			if (buildModel != null)
				buildModel.dispose();
		} else {
			provider.disconnect(model.getUnderlyingResource(), this);
			if (buildModel != null)
				provider.disconnect(buildModel.getUnderlyingResource(), this);
		}
		if (stateListener!=null)
			stateListener.dispose();
	}

	public IPDEEditorPage getHomePage() {
		return getPage(SITE_PAGE);
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
			Object data =
				getClipboard().getContents(TextTransfer.getInstance());
			return (data != null);
		} catch (SWTError e) {
			return false;
		}
	}
	protected boolean isModelCorrect(Object model) {
		return model != null ? ((ISiteModel) model).isLoaded() : false;
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
}