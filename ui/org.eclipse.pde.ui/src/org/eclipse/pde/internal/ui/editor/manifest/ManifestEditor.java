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
package org.eclipse.pde.internal.ui.editor.manifest;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.wizards.templates.TemplateEditorInput;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class ManifestEditor
	extends PDEMultiPageXMLEditor
	implements IPropertyChangeListener {
	
	protected class SynchronizedUTF8FileDocumentProvider extends UTF8FileDocumentProvider {
		public IDocument createEmptyDocument() {
			return new PartiallySynchronizedDocument();
		}
	}
	
	public static final String TEMPLATE_PAGE = "TemplatePage";
	public static final String OVERVIEW_PAGE = "OverviewPage";
	public static final String EXTENSIONS_PAGE = "ExtensionsPage";
	public static final String RUNTIME_PAGE = "RuntimePage";
	public static final String EXTENSION_POINT_PAGE = "ExtensionPointPage";
	public static final String DEPENDENCIES_PAGE = "DependenciesPage";
	public static final String SOURCE_PAGE = "SourcePage";

	public static final String KEY_TEMPLATE =
		"ManifestEditor.TemplatePage.title";
	public static final String KEY_OVERVIEW =
		"ManifestEditor.OverviewPage.title";
	public static final String KEY_DEPENDENCIES =
		"ManifestEditor.DependenciesPage.title";
	public static final String KEY_RUNTIME = "ManifestEditor.RuntimePage.title";
	public static final String KEY_READ_ONLY = "ManifestEditor.readOnly";
	public static final String KEY_EXTENSIONS =
		"ManifestEditor.ExtensionsPage.title";
	public static final String KEY_EXTENSION_POINTS =
		"ManifestEditor.ExtensionPointsPage.title";
	public static final String NO_PLATFORM_HOME =
		"ManifestEditor.noPlatformHome";

	public ManifestEditor() {
		super();
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
			this);
	}
	private void checkPlatformHome() throws PartInitException {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String home = preferences.getString(ICoreConstants.PLATFORM_PATH);
		if (home == null || home.length() == 0) {
			throw new PartInitException(
				PDEPlugin.getResourceString(NO_PLATFORM_HOME));
		}
	}
	private IPluginModelBase createFileSystemModel(File file) {
		boolean fragment = file.getName().toLowerCase().equals("fragment.xml");
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ExternalPluginModelBase model = null;

		if (fragment)
			model = new ExternalFragmentModel();
		else
			model = new ExternalPluginModel();
		model.setReconcilingModel(true);
		String parentPath = file.getParentFile().getAbsolutePath();
		model.setInstallLocation("file:" + parentPath);
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
		return model;
	}
	private IPluginModelBase createStorageModel(IStorage storage) {
		String lname = storage.getName().toLowerCase();
		boolean fragment = lname.startsWith("fragment.xml");
		InputStream stream = null;
		try {
			stream = storage.getContents();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ExternalPluginModelBase model = null;

		if (fragment)
			model = new ExternalFragmentModel();
		else
			model = new ExternalPluginModel();
		model.setReconcilingModel(true);
		//String parentPath = file.getParentFile().getAbsolutePath();
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
		return model;
	}
	protected Object createModel(Object input) throws CoreException {
		if (input instanceof IFile)
			return createResourceModel((IFile) input);
		if (input instanceof IStorage)
			return createStorageModel((IStorage) input);
		if (input instanceof File)
			return createFileSystemModel((File) input);
		return null;
	}

	protected IModelUndoManager createModelUndoManager() {
		return new PluginUndoManager(this);
	}

	protected void createPages() {
		firstPageId = OVERVIEW_PAGE;
		formWorkbook.setFirstPageSelected(false);
		ManifestFormPage formPage =
			new ManifestFormPage(
				this,
				PDEPlugin.getResourceString(KEY_OVERVIEW));
		addPage(OVERVIEW_PAGE, formPage);
		addPage(
			DEPENDENCIES_PAGE,
			new ManifestDependenciesPage(
				formPage,
				PDEPlugin.getResourceString(KEY_DEPENDENCIES)));
		addPage(
			RUNTIME_PAGE,
			new ManifestRuntimePage(
				formPage,
				PDEPlugin.getResourceString(KEY_RUNTIME)));
		addPage(
			EXTENSIONS_PAGE,
			new ManifestExtensionsPage(
				formPage,
				PDEPlugin.getResourceString(KEY_EXTENSIONS)));
		addPage(
			EXTENSION_POINT_PAGE,
			new ManifestExtensionPointPage(
				formPage,
				PDEPlugin.getResourceString(KEY_EXTENSION_POINTS)));
		if (XMLCore.NEW_CODE_PATHS) {
			addPage(SOURCE_PAGE, new ManifestSourcePageNew(this));
		} else {
			addPage(SOURCE_PAGE, new ManifestSourcePage(this));
		}
	}

	private void addTemplatePage(IProject project) {
		IFile templateFile = project.getFile(".template");
		if (!templateFile.exists())
			return;
		ManifestFormPage parent = (ManifestFormPage) getPage(OVERVIEW_PAGE);
		ManifestTemplatePage page =
			new ManifestTemplatePage(
				parent,
				PDEPlugin.getResourceString(KEY_TEMPLATE),
				templateFile);
		addPage(TEMPLATE_PAGE, page, 0);
	}

	private IPluginModelBase createResourceModel(IFile file)
		throws CoreException {
		InputStream stream = null;

		stream = file.getContents(false);

		IModelProvider modelProvider =
			PDECore.getDefault().getWorkspaceModelManager();
		modelProvider.connect(file, this);
		WorkspacePluginModelBase model =
			(WorkspacePluginModelBase) modelProvider.getModel(file, this);
		model.setReconcilingModel(true);
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			// Errors in the file
		}
		String buildName = "build.properties";
		IPath buildPath = file.getProject().getFullPath().append(buildName);
		IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
		modelProvider.connect(buildFile, this);
		IBuildModel buildModel =
			(IBuildModel) modelProvider.getModel(buildFile, this);
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
		IPluginModelBase model = (IPluginModelBase) getModel();
		IModelProvider modelProvider =
			PDECore.getDefault().getWorkspaceModelManager();
		if (model instanceof WorkspacePluginModelBase) {
			IBuildModel buildModel = model.getBuildModel();
			modelProvider.disconnect(buildModel.getUnderlyingResource(), this);
			((WorkspacePluginModelBase) model).setBuildModel(null);
			modelProvider.disconnect(model.getUnderlyingResource(), this);
		} else {
			model.dispose();
		}
		PDEPlugin
			.getDefault()
			.getPreferenceStore()
			.removePropertyChangeListener(
			this);
	}

	public IPDEEditorPage getHomePage() {
		return getPage(OVERVIEW_PAGE);
	}
	protected java.lang.String getSourcePageId() {
		return SOURCE_PAGE;
	}
	public String getTitle() {
		if (!isModelCorrect(getModel()))
			return super.getTitle();
		IPluginModelBase model = (IPluginModelBase) getModel();
		String name = model.getPluginBase().getName();
		if (name == null)
			return super.getTitle();
		String value = model.getResourceString(name);
		if (value.startsWith("%")) {
			// could not find the translation - use the default
			return super.getTitle();
		}
		return value;
	}

	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		checkPlatformHome();
		if (input instanceof TemplateEditorInput) {
			firstPageId = ((TemplateEditorInput) input).getFirstPageId();
		}
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			IProject project = file.getProject();
			addTemplatePage(project);
		}
		super.init(site, input);
	}

	public boolean isFragmentEditor() {
		return false;
	}
	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IPluginModelBase) model).isValid() : false;
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
			if (isFragmentEditor()) {
				if (name.startsWith("fragment.xml"))
					return true;
			} else {
				if (name.startsWith("plugin.xml"))
					return true;
			}
		} else {
			if (isFragmentEditor()) {
				if (name.equals("fragment.xml"))
					return true;
			} else {
				if (name.equals("plugin.xml"))
					return true;
			}
		}
		return false;
	}
	private static ManifestEditor openExternalPlugin(IPluginBase pluginInfo) {
		boolean isFragment = pluginInfo.getPluginModel().isFragmentModel();
		String manifest =
			isFragment
				? "fragment.xml"
				: "plugin.xml";
		String fileName =
			pluginInfo.getModel().getInstallLocation()
				+ File.separator
				+ manifest;
		File file = new File(fileName);
		if (file.exists()) {
			String editorId = PDEPlugin.getPluginId() + (isFragment ? ".fragmentEditor" :".manifestEditor");
			try {
				SystemFileEditorInput input = new SystemFileEditorInput(file);
				return (ManifestEditor) PDEPlugin.getActivePage().openEditor(
					input,
					editorId);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		}
		return null;
	}

	public static void openPluginEditor(String pluginId) {
		openPluginEditor(pluginId, null);
	}

	public static void openPluginEditor(
		String pluginId,
		Object object) {
		IPlugin pluginToOpen = PDECore.getDefault().findPlugin(pluginId);
		if (pluginToOpen != null) {
			openPluginEditor(pluginToOpen, object);
		} else {
			Display.getCurrent().beep();
		}
	}

	public static void openPluginEditor(IPluginBase plugin) {
		openPluginEditor(plugin, null);
	}
	
	public static void openPluginEditor(
		IPluginBase plugin,
		Object object) {
		openPluginEditor(plugin, object, null);
	}

	public static void openPluginEditor(
		IPluginBase plugin,
		Object object,
		IMarker marker) {
		IEditorPart editor = null;
		IResource underlyingResource =
			plugin.getModel().getUnderlyingResource();
		if (underlyingResource == null) {
			editor = openExternalPlugin(plugin);
		} else {
			editor = openWorkspacePlugin((IFile) underlyingResource, plugin instanceof IFragment);
		}
		if (editor instanceof ManifestEditor && editor != null && object != null ) {
			((ManifestEditor)editor).openTo(object, marker);
		}
	}

	private static IEditorPart openWorkspacePlugin(IFile pluginFile, boolean fragment) {
		String editorId = fragment ? PDEPlugin.FRAGMENT_EDITOR_ID:PDEPlugin.MANIFEST_EDITOR_ID;
		try {
			FileEditorInput input = new FileEditorInput(pluginFile);
			return PDEPlugin.getActivePage().openEditor(
				input,
				editorId);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}
	protected boolean updateModel() {
		if (XMLCore.NEW_CODE_PATHS) {
			return updateModelNew();
		} else {
			return updateModelOrig();
		}
	}
	private boolean updateModelNew() {
		boolean result;
		ManifestSourcePageNew sourcePage = (ManifestSourcePageNew)getPage(getSourcePageId());
		if (sourcePage.tryGetModelUpdatingTicket()) {
			result= updateModelOrig();
		} else {
			result= sourcePage.containsError();
		}
		return result;
	}
	private boolean updateModelOrig() {
		IPluginModelBase model = (IPluginModelBase) getModel();
		IDocument document =
			getDocumentProvider().getDocument(getEditorInput());
		String text = document.get();
		boolean cleanModel = true;
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			try {
				model.reload(stream, false);
				if (model instanceof IEditable && model.isEditable())
					((IEditable)model).setDirty(false);
				cleanModel = containsError();
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

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.equals(MainPreferencePage.PROP_SHOW_OBJECTS)) {
			final IModelChangeProvider provider =
				(IModelChangeProvider) getModel();
			final ModelChangedEvent e =
				new ModelChangedEvent(
					IModelChangedEvent.WORLD_CHANGED,
					null,
					null);
			BusyIndicator
				.showWhile(
					formWorkbook.getControl().getDisplay(),
					new Runnable() {
				public void run() {
					provider.fireModelChanged(e);
				}
			});
		}
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.PDEMultiPageEditor#canCopy(ISelection)
	 */
	public boolean canCopy(ISelection selection) {
		return (getCurrentPage() instanceof ManifestFormPage)
			? true
			: super.canCopy(selection);
	}
	protected IPDEEditorPage getPageFor(Object object) {
		IPDEEditorPage overviewPage = getPage(OVERVIEW_PAGE);
		ManifestFormOutlinePage outline =
			(ManifestFormOutlinePage) overviewPage.getContentOutlinePage();
		if (outline != null) {
			return outline.getParentPage(object);
		}
		return null;
	}

	protected IDocumentProvider createDocumentProvider(Object input) {
		if (XMLCore.NEW_CODE_PATHS) {
			return createDocumentProviderNew(input);
		} else {
			return super.createDocumentProvider(input);
		}
	}
	private IDocumentProvider createDocumentProviderNew(Object input) {
		IDocumentProvider documentProvider= null;
		if (input instanceof IFile) {
			documentProvider= new SynchronizedUTF8FileDocumentProvider();
		} else if (input instanceof File) {
			documentProvider= new SynchronizedSystemFileDocumentProvider(createDocumentPartitioner(), "UTF8");
		} else if (input instanceof IStorage) {
			documentProvider= new SynchronizedStorageDocumentProvider(createDocumentPartitioner(), "UTF8");
		}
		return documentProvider;
	}

}
