package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.ui.part.*;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.model.Plugin;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.preferences.*;

public class ManifestEditor extends PDEMultiPageXMLEditor {
	public static final String OVERVIEW_PAGE = "OverviewPage";
	public static final String EXTENSIONS_PAGE = "ExtensionsPage";
	public static final String RUNTIME_PAGE = "RuntimePage";
	public static final String EXTENSION_POINT_PAGE = "ExtensionPointPage";
	public static final String DEPENDENCIES_PAGE = "DependenciesPage";
	public static final String SOURCE_PAGE = "SourcePage";

	public static final String KEY_OVERVIEW = "ManifestEditor.OverviewPage.title";
	public static final String KEY_DEPENDENCIES =
		"ManifestEditor.DependenciesPage.title";
	public static final String KEY_RUNTIME = "ManifestEditor.RuntimePage.title";
	public static final String KEY_READ_ONLY = "ManifestEditor.readOnly";
	public static final String KEY_EXTENSIONS =
		"ManifestEditor.ExtensionsPage.title";
	public static final String KEY_EXTENSION_POINTS =
		"ManifestEditor.ExtensionPointsPage.title";
	public static final String NO_PLATFORM_HOME = "ManifestEditor.noPlatformHome";

public ManifestEditor() {
	super();
}
private void checkPlatformHome() throws PartInitException {
   IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
   String home = store.getString(TargetPlatformPreferencePage.PROP_PLATFORM_PATH);
   if (home==null || home.length()==0) {
	   throw new PartInitException(PDEPlugin.getResourceString(NO_PLATFORM_HOME));
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
protected Object createModel(Object input) throws CoreException {
	if (input instanceof IFile) return createResourceModel((IFile)input);
	if (input instanceof File) return createFileSystemModel((File)input);
	return null;
}

protected IModelUndoManager createModelUndoManager() {
	return new PluginUndoManager(this);
}

protected void createPages() {
	firstPageId = OVERVIEW_PAGE;
	formWorkbook.setFirstPageSelected(false);
	ManifestFormPage formPage =
		new ManifestFormPage(this, PDEPlugin.getResourceString(KEY_OVERVIEW));
	addPage(OVERVIEW_PAGE, formPage);
	addPage(
		DEPENDENCIES_PAGE,
		new ManifestDependenciesPage(
			formPage,
			PDEPlugin.getResourceString(KEY_DEPENDENCIES)));
	addPage(
		RUNTIME_PAGE,
		new ManifestRuntimePage(formPage, PDEPlugin.getResourceString(KEY_RUNTIME)));
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
	addPage(SOURCE_PAGE, new ManifestSourcePage(this));
}
private IPluginModelBase createResourceModel(IFile file) throws CoreException {
	boolean fragment = file.getName().toLowerCase().equals("fragment.xml");
	InputStream stream = null;

	stream = file.getContents(false);

	IModelProvider modelProvider =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	modelProvider.connect(file, this);
	WorkspacePluginModelBase model =
		(WorkspacePluginModelBase) modelProvider.getModel(file, this);
	try {
		model.load(stream, false);
	} catch (CoreException e) {
		// Errors in the file
	}
	String buildName = "build.properties";
	IPath buildPath = file.getProject().getFullPath().append(buildName);
	IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
	modelProvider.connect(buildFile, this);
	IBuildModel buildModel = (IBuildModel) modelProvider.getModel(buildFile, this);
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
	IPluginModelBase model = (IPluginModelBase)getModel();
	IModelProvider modelProvider = PDEPlugin.getDefault().getWorkspaceModelManager();
	if (model instanceof WorkspacePluginModelBase) {
		IBuildModel buildModel = model.getBuildModel();
		modelProvider.disconnect(buildModel.getUnderlyingResource(), this);
		((WorkspacePluginModelBase)model).setBuildModel(null);
		modelProvider.disconnect(model.getUnderlyingResource(), this);
	}
}
public IPDEEditorPage getHomePage() {
	return getPage(OVERVIEW_PAGE);
}
protected java.lang.String getSourcePageId() {
	return SOURCE_PAGE;
}
public String getTitle() {
	if (!isModelCorrect(getModel())) return super.getTitle();
	IPluginModelBase model = (IPluginModelBase)getModel();
	String name = model.getPluginBase().getName();
	if (name==null) return super.getTitle();
	return model.getResourceString(name);
}

public void init(IEditorSite site, IEditorInput input)
	throws PartInitException {
	checkPlatformHome();
	super.init(site, input);
}
public boolean isFragmentEditor() {
	return false;
}
protected boolean isModelCorrect(Object model) {
	return model!=null?((IPluginModelBase)model).isLoaded():false;
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
	if (isFragmentEditor()) {
		if (name.equals("fragment.xml"))
			return true;
	} else {
		if (name.equals("plugin.xml"))
			return true;
	}
	return false;
}
private void openExternalPlugin(IPluginBase pluginInfo) {
	String manifest = pluginInfo.getModel().isFragmentModel() ? "fragment.xml" : "plugin.xml";
	String fileName =
		pluginInfo.getModel().getInstallLocation() + File.separator + manifest;
	File file = new File(fileName);
	if (file.exists()) {
		String editorId = PDEPlugin.getPluginId() + ".manifestEditor";
		try {
			SystemFileEditorInput input = new SystemFileEditorInput(file);
			PDEPlugin.getActivePage().openEditor(input, editorId);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}


public void openPluginEditor(String pluginId) {
	IPlugin pluginToOpen = PDEPlugin.getDefault().findPlugin(pluginId);
	if (pluginToOpen != null) {
		openPluginEditor(pluginToOpen);
	} else {
		Display.getCurrent().beep();
	}
}

public void openPluginEditor(IPluginBase plugin) {
	IResource underlyingResource = plugin.getModel().getUnderlyingResource();
	if (underlyingResource == null) {
		openExternalPlugin(plugin);
	} else {
		openWorkspacePlugin((IFile) underlyingResource);
	}	
}

private void openWorkspacePlugin(IFile pluginFile) {
	String editorId = PDEPlugin.MANIFEST_EDITOR_ID;
	try {
		FileEditorInput input = new FileEditorInput(pluginFile);
		PDEPlugin.getActivePage().openEditor(input, editorId);
	} catch (PartInitException e) {
		PDEPlugin.logException(e);
	}
}
protected boolean updateModel() {
	IPluginModelBase model = (IPluginModelBase) getModel();
	IDocument document = getDocumentProvider().getDocument(getEditorInput());
	String text = document.get();
	boolean cleanModel = true;
	try {
		InputStream stream = new ByteArrayInputStream(text.getBytes("UTF8"));
		try {
			model.reload(stream, false);
		} catch (CoreException e) {
			cleanModel = false;
		}
		try {
			stream.close();
		} catch (IOException e) {
		}
	}
	catch (UnsupportedEncodingException e) {
		PDEPlugin.logException(e);
	}
	return cleanModel;
}
public void updateTitle() {
	firePropertyChange(IWorkbenchPart.PROP_TITLE);
}
}
