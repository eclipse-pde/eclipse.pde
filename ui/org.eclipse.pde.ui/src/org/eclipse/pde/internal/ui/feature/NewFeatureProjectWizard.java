package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.ui.actions.*;

import java.lang.reflect.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jdt.ui.wizards.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

public class NewFeatureProjectWizard extends NewWizard 
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewFeatureWizard.MainPage.title";
	public static final String CREATING_PROJECT = "NewFeatureWizard.creatingProject";
	public static final String CREATING_FOLDERS = "NewFeatureWizard.creatingFolders";
	public static final String CREATING_MANIFEST = "NewFeatureWizard.creatingManifest";
	public static final String MAIN_PAGE_DESC = "NewFeatureWizard.MainPage.desc";
	private WizardNewProjectCreationPage mainPage;
	private FeatureSpecPage specPage;
	private PluginListPage pluginListPage;
	private IConfigurationElement config;
	
	public NewFeatureProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
public void addPages() {
	mainPage = new WizardNewProjectCreationPage("main");
	mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
	mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
	addPage(mainPage);
	specPage = new FeatureSpecPage(mainPage);
	addPage(specPage);
	if (hasInterestingProjects(false)) {
		pluginListPage = new PluginListPage();
		addPage(pluginListPage);
	}
}

/*
public boolean canFinish() {
	IWizardPage page = getContainer().getCurrentPage();
	if (page==mainPage || page==specPage) return false;
	return super.canFinish();
}
*/

private IFolder createFeatureFolder(IProject project, String name, IProgressMonitor monitor) throws CoreException {
	// need to have install/feature/feature_version/
	IWorkspaceRoot root = project.getWorkspace().getRoot();
	IPath path = project.getFullPath();
	path = path.append("install");
	createFolder(root, path, monitor);
	path = path.append("features");
	createFolder(root, path, monitor);
	path = path.append(name);
	return createFolder(root, path, monitor);
}

private IFile createFeatureManifest(
	IFolder folder,
	FeatureData data,
	IPluginBase[] plugins)
	throws CoreException {
	IPath path = folder.getFullPath().append("feature.xml");
	IWorkspaceRoot root = folder.getProject().getWorkspace().getRoot();
	IFile file = root.getFile(path);
	WorkspaceFeatureModel model = new WorkspaceFeatureModel();
	model.setFile(file);
	IFeature feature = model.getFeature();
	String name = data.name;
	feature.setLabel(name);
	feature.setId(data.id);
	feature.setVersion(data.version);
	feature.setProviderName(data.provider);
	
	IFeaturePlugin [] added = new IFeaturePlugin[plugins.length];
	
	for (int i = 0; i < plugins.length; i++) {
		IPluginBase plugin = plugins[i];
		FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
		fplugin.loadFrom(plugin);
		added[i] = fplugin;
	}
	feature.addPlugins(added);
	feature.computeImports();
	// Save the model
	model.save();
	model.dispose();
	IWorkbench workbench = PlatformUI.getWorkbench();
	workbench.getEditorRegistry().setDefaultEditor(file, PDEPlugin.FEATURE_EDITOR_ID);
	return file;
}
private void createFeatureProject(
	IProject project,
	IPath location,
	FeatureData data,
	IPluginBase[] plugins,
	IProgressMonitor monitor)
	throws CoreException {
	monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 2);
	CoreUtility.createProject(project, location, monitor);
	project.open(monitor);
	CoreUtility.addNatureToProject(project, PDE.FEATURE_NATURE, monitor);
	// create initial folder structure
	monitor.subTask(PDEPlugin.getResourceString(CREATING_FOLDERS));
	String name = data.id + "_" + data.version;
	IFolder folder = createFeatureFolder(project, name, monitor);
	monitor.worked(1);
	monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
	// create install.xml
	IFile file = createFeatureManifest(folder, data, plugins);
	// open manifest for editing
	openFeatureManifest(file);
}
private IFolder createFolder(
	IWorkspaceRoot root,
	IPath path,
	IProgressMonitor monitor)
	throws CoreException {
	IFolder folder = root.getFolder(path);
	folder.create(false, true, monitor);
	return folder;
}
private boolean hasInterestingProjects(boolean fragments) {
	IWorkspace workspace = PDEPlugin.getWorkspace();
	IWorkspaceRoot root = workspace.getRoot();
	IProject[] projects = root.getProjects();
	for (int i = 0; i < projects.length; i++) {
		IProject project = projects[i];
		if (WorkspaceModelManager.isPluginProject(project)) return true;
	}
	return false;
}

private void openFeatureManifest(IFile manifestFile) {
	IWorkbenchPage page = PDEPlugin.getDefault().getActivePage();
	// Reveal the file first
	final ISelection selection = new StructuredSelection(manifestFile);
	final IWorkbenchPart activePart = page.getActivePart();

	if (activePart instanceof ISetSelectionTarget) {
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				((ISetSelectionTarget) activePart).selectReveal(selection);
			}
		});
	}
	// Open the editor

	FileEditorInput input = new FileEditorInput(manifestFile);
	String id = PDEPlugin.FEATURE_EDITOR_ID;
	try {
		page.openEditor(input, id);
	} catch (PartInitException e) {
		PDEPlugin.logException(e);
	}
}
public boolean performFinish() {
	final IProject project = mainPage.getProjectHandle();
	final IPath location = mainPage.getLocationPath();
	final FeatureData data = specPage.getFeatureData();
	final IPluginBase[] plugins =
		pluginListPage != null ? pluginListPage.getSelectedPlugins() : (new IPluginBase[0]);
	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				createFeatureProject(project, location, data, plugins, monitor);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
	try {
		getContainer().run(false, true, operation);
		BasicNewProjectResourceWizard.updatePerspective(config);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		return false;
	}
	return true;
}

public void setInitializationData(IConfigurationElement config, String property,	Object data)
										throws CoreException {
	this.config = config;
}


}
