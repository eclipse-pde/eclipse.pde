package org.eclipse.pde.internal.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.component.*;
import org.eclipse.ui.actions.*;
import java.lang.reflect.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jdt.ui.wizards.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.core.runtime.*;

public class NewComponentProjectWizard extends NewWizard {
	public static final String MAIN_PAGE_TITLE = "NewComponentWizard.MainPage.title";
	public static final String CREATING_PROJECT = "NewComponentWizard.creatingProject";
	public static final String CREATING_FOLDERS = "NewComponentWizard.creatingFolders";
	public static final String CREATING_MANIFEST = "NewComponentWizard.creatingManifest";
	public static final String MAIN_PAGE_DESC = "NewComponentWizard.MainPage.desc";
	private WizardNewProjectCreationPage mainPage;
	private ComponentSpecPage specPage;
	private PluginListPage pluginListPage;
	private FragmentListPage fragmentListPage;
	public NewComponentProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPCOMP_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
	}
public void addPages() {
	mainPage = new WizardNewProjectCreationPage("main");
	mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
	mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
	addPage(mainPage);
	specPage = new ComponentSpecPage();
	addPage(specPage);
	if (hasInterestingProjects(false)) {
		pluginListPage = new PluginListPage();
		addPage(pluginListPage);
	}
	if (hasInterestingProjects(true)) {
		fragmentListPage = new FragmentListPage();
		addPage(fragmentListPage);
	}
}
public boolean canFinish() {
	IWizardPage page = getContainer().getCurrentPage();
	if (page==mainPage || page==specPage) return false;
	return super.canFinish();
}
private IFolder createComponentFolder(IProject project, String name, IProgressMonitor monitor) throws CoreException {
	// need to have install/components/compid_version/
	IWorkspaceRoot root = project.getWorkspace().getRoot();
	IPath path = project.getFullPath();
	path = path.append("install");
	IFolder folder = createFolder(root, path, monitor);
	path = path.append("components");
	folder = createFolder(root, path, monitor);
	path = path.append(name);
	return createFolder(root, path, monitor);
}
private IFile createComponentManifest(
	IFolder folder,
	ComponentData data,
	IPlugin[] plugins,
	IFragment[] fragments)
	throws CoreException {
	IPath path = folder.getFullPath().append("install.xml");
	IWorkspaceRoot root = folder.getProject().getWorkspace().getRoot();
	IFile file = root.getFile(path);
	WorkspaceComponentModel model = new WorkspaceComponentModel();
	model.setEditable(true);
	model.setFile(file);
	IComponent component = model.getComponent();
	String name = folder.getProject().getName();
	component.setLabel(name);
	component.setId(data.id);
	component.setVersion(data.version);
	component.setProviderName(data.provider);
	component.setDescription(data.description);
	for (int i = 0; i < plugins.length; i++) {
		IPlugin plugin = plugins[i];
		IComponentPlugin cplugin = model.getFactory().createPlugin();
		cplugin.setId(plugin.getId());
		cplugin.setLabel(plugin.getName());
		cplugin.setVersion(plugin.getVersion());
		component.addPlugin(cplugin);
	}
	for (int i = 0; i < fragments.length; i++) {
		IFragment fragment = fragments[i];
		IComponentFragment cfragment = model.getFactory().createFragment();
		cfragment.setId(fragment.getId());
		cfragment.setLabel(fragment.getName());
		cfragment.setVersion(fragment.getVersion());
		component.addFragment(cfragment);
	}
	// Save the model
	model.save();
	model.dispose();
	IWorkbench workbench = PlatformUI.getWorkbench();
	workbench.getEditorRegistry().setDefaultEditor(file, PDEPlugin.COMPONENT_EDITOR_ID);
	return file;
}
private void createComponentProject(
	IProject project,
	IPath location,
	ComponentData data,
	IPlugin[] plugins,
	IFragment[] fragments,
	IProgressMonitor monitor)
	throws CoreException {
	monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 2);
	CoreUtility.createProject(project, location, monitor);
	project.open(monitor);
	CoreUtility.addNatureToProject(project, PDEPlugin.COMPONENT_NATURE, monitor);
	// create initial folder structure
	monitor.subTask(PDEPlugin.getResourceString(CREATING_FOLDERS));
	String name = data.id + "_" + data.version;
	IFolder folder = createComponentFolder(project, name, monitor);
	monitor.worked(1);
	monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
	// create install.xml
	IFile file = createComponentManifest(folder, data, plugins, fragments);
	// open manifest for editing
	openComponentManifest(file);
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
		try {
			if (project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
				String name = fragments ? "fragment.xml" : "plugin.xml";
				IPath path = project.getFullPath().append(name);
				IFile file = root.getFile(path);
				if (file.exists()) {
					return true;
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	return false;
}
private void openComponentManifest(IFile manifestFile) {
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
	String id = PDEPlugin.COMPONENT_EDITOR_ID;
	try {
		page.openEditor(input, id);
	} catch (PartInitException e) {
		PDEPlugin.logException(e);
	}
}
public boolean performFinish() {
	final IProject project = mainPage.getProjectHandle();
	final IPath location = mainPage.getLocationPath();
	final ComponentData data = specPage.getComponentData();
	final IPlugin[] plugins =
		pluginListPage != null ? pluginListPage.getSelectedPlugins() : (new IPlugin[0]);
	final IFragment[] fragments =
		fragmentListPage != null
			? fragmentListPage.getSelectedFragments()
			: (new IFragment[0]);
	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				createComponentProject(project, location, data, plugins, fragments, monitor);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
	try {
		getContainer().run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		return false;
	}
	return true;
}
}
