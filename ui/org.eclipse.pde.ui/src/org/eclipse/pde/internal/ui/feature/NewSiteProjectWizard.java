package org.eclipse.pde.internal.ui.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.CoreUtility;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewSiteProjectWizard
	extends NewWizard
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewSiteWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewSiteWizard.MainPage.title";
	public static final String CREATING_PROJECT =
		"NewSiteWizard.creatingProject";
	public static final String CREATING_FOLDERS =
		"NewSiteWizard.creatingFolders";
	public static final String CREATING_MANIFEST =
		"NewSiteWizard.creatingManifest";
	public static final String MAIN_PAGE_DESC = "NewSiteWizard.MainPage.desc";
	public static final String OVERWRITE_SITE = "NewFeatureWizard.overwriteSite";
	private WizardNewProjectCreationPage mainPage;
	private IConfigurationElement config;

	public NewSiteProjectWizard() {
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
	}

	private IFile createSiteManifest(
		IProject project,
		SiteData data)
		throws CoreException {
		IFile file = project.getFile("site.xml");
		WorkspaceSiteModel model = new WorkspaceSiteModel();
		model.setFile(file);
		ISite site = model.getSite();
		String name = project.getName();
		site.setLabel(name);
		site.setType(data.type);
		site.setURL(data.url);

		// Save the model
		model.save();
		model.dispose();
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.SITE_EDITOR_ID);
		return file;
	}
	private void createSiteProject(
		IProject project,
		IPath location,
		SiteData data,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 4);

		boolean overwrite = true;
		if (location.append(project.getName()).toFile().exists()) {
			overwrite =
				MessageDialog.openQuestion(
					PDEPlugin.getActiveWorkbenchShell(),
					getWindowTitle(),
					PDEPlugin.getResourceString(OVERWRITE_SITE));
		}
		if (overwrite) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			CoreUtility.addNatureToProject(
				project,
				PDE.SITE_NATURE,
				monitor);
			createFolders(project, monitor);
			monitor.worked(2);
			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			// create site.xml
			IFile file = createSiteManifest(project, data);
			monitor.worked(1);
			// open manifest for editing
			openSiteManifest(file);
			monitor.worked(1);
		} else {
			project.create(monitor);
			project.open(monitor);
			IFile siteFile = project.getFile("site.xml");
			if (siteFile.exists())
				openSiteManifest(siteFile);
			monitor.worked(4);
		}

	}
	
	private void createFolders(IProject project, IProgressMonitor monitor) throws CoreException {
		createFolder(project, "features", monitor);
		createFolder(project, "plugins", monitor);
	}
	
	private void createFolder(IProject project, String name, IProgressMonitor monitor) throws CoreException {
		IFolder plugins = project.getFolder(name);
		if (!plugins.exists())
			plugins.create(true, true, new SubProgressMonitor(monitor, 1));
		else
			monitor.worked(1);
	}

	private void openSiteManifest(IFile manifestFile) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
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
		String id = PDEPlugin.SITE_EDITOR_ID;
		try {
			page.openEditor(input, id);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean performFinish() {
		final IProject project = mainPage.getProjectHandle();
		final IPath location = mainPage.getLocationPath();
		final SiteData data = new SiteData();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					createSiteProject(project, location, data, monitor);
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