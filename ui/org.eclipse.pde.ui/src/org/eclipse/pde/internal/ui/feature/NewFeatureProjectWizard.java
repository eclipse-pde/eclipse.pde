package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.CoreUtility;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewFeatureProjectWizard
	extends NewWizard
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewFeatureWizard.MainPage.title";
	public static final String CREATING_PROJECT =
		"NewFeatureWizard.creatingProject";
	public static final String CREATING_FOLDERS =
		"NewFeatureWizard.creatingFolders";
	public static final String CREATING_MANIFEST =
		"NewFeatureWizard.creatingManifest";
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

	private IFile createFeatureManifest(
		IProject project,
		FeatureData data,
		IPluginBase[] plugins)
		throws CoreException {
		IFile file = project.getFile("feature.xml");
		WorkspaceFeatureModel model = new WorkspaceFeatureModel();
		model.setFile(file);
		IFeature feature = model.getFeature();
		String name = data.name;
		feature.setLabel(name);
		feature.setId(data.id);
		feature.setVersion(data.version);
		feature.setProviderName(data.provider);

		IFeaturePlugin[] added = new IFeaturePlugin[plugins.length];

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
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.FEATURE_EDITOR_ID);
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
		monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
		// create install.xml
		IFile file = createFeatureManifest(project, data, plugins);
		// open manifest for editing
		openFeatureManifest(file);
	}
	private boolean hasInterestingProjects(boolean fragments) {
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (WorkspaceModelManager.isPluginProject(project))
				return true;
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
			pluginListPage != null
				? pluginListPage.getSelectedPlugins()
				: (new IPluginBase[0]);
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

	public void setInitializationData(
		IConfigurationElement config,
		String property,
		Object data)
		throws CoreException {
		this.config = config;
	}

}