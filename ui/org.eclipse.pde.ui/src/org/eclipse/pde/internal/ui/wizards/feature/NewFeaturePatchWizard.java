/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.*;

/**
 * @author cgwong
 *  
 */
public class NewFeaturePatchWizard extends NewWizard implements IExecutableExtension {

	public static final String KEY_WTITLE = "FeaturePatch.wtitle"; //$NON-NLS-1$
	public static final String MAIN_PAGE_TITLE = "FeaturePatch.MainPage.title"; //$NON-NLS-1$
	public static final String MAIN_PAGE_DESC = "FeaturePatch.MainPage.desc"; //$NON-NLS-1$
	public static final String DEF_PROJECT_NAME = "project-name"; //$NON-NLS-1$
	public static final String DEF_FEATURE_ID = "feature-id"; //$NON-NLS-1$
	public static final String DEF_FEATURE_NAME = "feature-name"; //$NON-NLS-1$
	public static final String CREATING_PROJECT = "NewFeatureWizard.creatingProject"; //$NON-NLS-1$
	public static final String OVERWRITE_FEATURE = "NewFeatureWizard.overwriteFeature"; //$NON-NLS-1$
	public static final String CREATING_FOLDERS = "NewFeatureWizard.creatingFolders"; //$NON-NLS-1$
	public static final String CREATING_MANIFEST = "NewFeatureWizard.creatingManifest"; //$NON-NLS-1$

	private WizardNewProjectCreationPage mainPage;
	private PatchSpecPage specPage;
	private PatchPluginListPage pluginListPage;
	private IConfigurationElement config;
	private FeaturePatchProvider provider;

	public class FeaturePatchProvider implements IProjectProvider {

		public FeaturePatchProvider() {
			super();
		}

		public String getProjectName() {
			return mainPage.getProjectName();
		}

		public IProject getProject() {
			return mainPage.getProjectHandle();
		}

		public IPath getLocationPath() {
			return mainPage.getLocationPath();
		}

		public IFeatureModel getFeatureToPatch() {
			if (specPage != null)
				return specPage.getFeatureToPatch();
			return null;
		}

		public FeatureData getFeatureData() {
			return specPage.getFeatureData();
		}

	}

	public NewFeaturePatchWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPTCH_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	public void addPages() {
		mainPage = new WizardNewProjectCreationPage("main") { //$NON-NLS-1$
			public void createControl(Composite parent) {
				super.createControl(parent);
				WorkbenchHelp.setHelp(getControl(), IHelpContextIds.NEW_PATCH_MAIN);
			}
		};
		mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null)
			mainPage.setInitialProjectName(pname);
		addPage(mainPage);
		provider = new FeaturePatchProvider();

		specPage = new PatchSpecPage(mainPage);
		specPage.setInitialId(getDefaultValue(DEF_FEATURE_ID));
		specPage.setInitialName(getDefaultValue(DEF_FEATURE_NAME));
		addPage(specPage);
		pluginListPage = new PatchPluginListPage(provider);
		addPage(pluginListPage);
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return ((page == specPage && page.isPageComplete()) || (page == pluginListPage && page
				.isPageComplete()));
	}

	public boolean performFinish() {
		final IProject project = provider.getProject();
		final IPath location = provider.getLocationPath();
		final IFeaturePlugin[] plugins = pluginListPage.getSelectedPlugins() != null
				? (IFeaturePlugin[]) pluginListPage.getSelectedPlugins()
				: (new IFeaturePlugin[0]);
		final IFeatureModel featureModel = provider.getFeatureToPatch();
		final FeatureData data = provider.getFeatureData();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {

			public void execute(IProgressMonitor monitor) {
				try {
					createFeatureProject(project, location, plugins, featureModel, data,
							monitor);
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

	public void setInitializationData(IConfigurationElement config, String property,
			Object data) throws CoreException {
		this.config = config;
	}

	/* finish methods */

	private void createFeatureProject(IProject project, IPath location,
			IFeaturePlugin[] plugins, IFeatureModel featureModel, FeatureData data,
			IProgressMonitor monitor) throws CoreException {

		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 3);
		boolean overwrite = true;
		if (location.append(project.getName()).toFile().exists()) {
			overwrite = MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(KEY_WTITLE), PDEPlugin
							.getResourceString(OVERWRITE_FEATURE));
		}
		if (overwrite) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			IProjectDescription desc = project.getWorkspace().newProjectDescription(
					project.getName());
			desc.setLocation(provider.getLocationPath());
			if (!project.hasNature(PDE.FEATURE_BUILDER_ID))
				CoreUtility.addNatureToProject(project, PDE.FEATURE_NATURE, monitor);

			if (!project.hasNature(JavaCore.NATURE_ID) && data.hasCustomHandler()) {
				CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
				JavaCore.create(project).setOutputLocation(
						project.getFullPath().append(data.getJavaBuildFolderName()),
						monitor);
				JavaCore.create(project).setRawClasspath(
						new IClasspathEntry[]{
								JavaCore.newContainerEntry(new Path(
										JavaRuntime.JRE_CONTAINER)),
								JavaCore.newSourceEntry(project.getFullPath().append(
										data.getSourceFolderName()))}, monitor);
				addSourceFolder(data.getSourceFolderName(), project, monitor);
			}

			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			monitor.worked(1);
			createBuildProperties(project, data);
			monitor.worked(1);
			// create feature.xml
			IFile file = createFeatureManifest(project, plugins, featureModel, data);
			monitor.worked(1);
			// open manifest for editing
			openFeatureManifest(file);
		} else {
			project.create(monitor);
			project.open(monitor);
			IFile featureFile = project.getFile("feature.xml"); //$NON-NLS-1$
			if (featureFile.exists())
				openFeatureManifest(featureFile);
			monitor.worked(3);
		}

	}

	protected static void addSourceFolder(String name, IProject project,
			IProgressMonitor monitor) throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path, monitor);
		monitor.worked(1);
	}

	private void createBuildProperties(IProject project, FeatureData data)
			throws CoreException {
		String fileName = "build.properties"; //$NON-NLS-1$
		IPath path = project.getFullPath().append(fileName);
		IFile file = project.getWorkspace().getRoot().getFile(path);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry = model.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			ientry.addToken("feature.xml"); //$NON-NLS-1$
			String library = specPage.getInstallHandlerLibrary();
			if (library != null) {
				String source = data.getSourceFolderName();
				if (source != null) {
					IBuildEntry entry = model.getFactory().createEntry(
							IBuildEntry.JAR_PREFIX + library);
					if (!source.endsWith("/")) //$NON-NLS-1$
						source += "/"; //$NON-NLS-1$
					entry.addToken(source);
					ientry.addToken(library);
					model.getBuild().add(entry);
				}
				String output = data.getJavaBuildFolderName();
				if (output != null) {
					IBuildEntry entry = model.getFactory().createEntry(
							IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX + library);
					if (!output.endsWith("/")) //$NON-NLS-1$
						output += "/"; //$NON-NLS-1$
					entry.addToken(output);
					model.getBuild().add(entry);
				}
			}

			model.getBuild().add(ientry);
			model.save();
		}
		IDE.setDefaultEditor(file, PDEPlugin.BUILD_EDITOR_ID);
	}

	private IFile createFeatureManifest(IProject project, IFeaturePlugin[] plugins,
			IFeatureModel featureModel, FeatureData data) throws CoreException {
		IFile file = project.getFile("feature.xml"); //$NON-NLS-1$
		WorkspaceFeatureModel model = new WorkspaceFeatureModel();
		model.setFile(file);
		IFeature feature = model.getFeature();
		feature.setLabel(data.name);
		feature.setId(data.id);
		feature.setVersion("1.0.0"); //$NON-NLS-1$
		feature.setProviderName(data.provider);
		if(data.hasCustomHandler){
			feature.setInstallHandler(model.getFactory().createInstallHandler());
		}

		IFeaturePlugin[] added = new IFeaturePlugin[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			added[i] = model.getFactory().createPlugin();
			String name = feature.getId();
			int loc = name.lastIndexOf("."); //$NON-NLS-1$
			if (loc != -1 && loc != name.length())
				name = name.substring(loc + 1, name.length());
			String[] versionSegments = plugins[i].getVersion().split("\\."); //$NON-NLS-1$
			StringBuffer version = new StringBuffer();
			for (int j = 0; j < versionSegments.length; j++) {
				if (j < 3) {
					version.append(versionSegments[j]);
					version.append("."); //$NON-NLS-1$
				}
			}
			version.append(name);
			IStatus status = PluginVersionIdentifier.validateVersion(version.toString());
			if (status.isOK())
				added[i].setVersion(version.toString());
			else
				added[i].setVersion(plugins[i].getVersion());
			added[i].setId(plugins[i].getId());
			added[i].setDownloadSize(plugins[i].getDownloadSize());
			added[i].setArch(plugins[i].getArch());
			added[i].setInstallSize(plugins[i].getInstallSize());
			added[i].setLabel(plugins[i].getLabel());
			added[i].setNL(plugins[i].getNL());
			added[i].setOS(plugins[i].getOS());
			added[i].setWS(plugins[i].getWS());
		}
		feature.addPlugins(added);

		FeatureImport featureImport = (FeatureImport) model.getFactory().createImport();
		if (featureModel != null){
		    featureImport.loadFrom(featureModel.getFeature());
		    featureImport.setPatch(true);
		    featureImport.setVersion(featureModel.getFeature().getVersion());
		    featureImport.setId(featureModel.getFeature().getId());
		} else if (data.isPatch()){
		    featureImport.setPatch(true);
		    featureImport.setVersion(data.featureToPatchVersion);
		    featureImport.setId(data.featureToPatchId);
		}
		
		feature.addImports(new IFeatureImport[]{featureImport});
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler != null) {
			handler.setLibrary(specPage.getInstallHandlerLibrary());
		}

		IFeatureInfo info = model.getFactory().createInfo(IFeature.INFO_COPYRIGHT);
		feature.setFeatureInfo(info, IFeature.INFO_COPYRIGHT);

		info.setURL("http://www.yourdomain.com/copyright"); //$NON-NLS-1$
		info.setDescription(PDEPlugin
				.getResourceString("NewFeatureWizard.sampleCopyrightDesc")); //$NON-NLS-1$

		info = model.getFactory().createInfo(IFeature.INFO_LICENSE);
		feature.setFeatureInfo(info, IFeature.INFO_LICENSE);

		info.setURL("http://www.yourdomain.com/license"); //$NON-NLS-1$
		info.setDescription(PDEPlugin
				.getResourceString("NewFeatureWizard.sampleLicenseDesc")); //$NON-NLS-1$

		info = model.getFactory().createInfo(IFeature.INFO_DESCRIPTION);
		feature.setFeatureInfo(info, IFeature.INFO_DESCRIPTION);

		info.setURL("http://www.yourdomain.com/description"); //$NON-NLS-1$
		info.setDescription(PDEPlugin
				.getResourceString("NewFeatureWizard.sampleDescriptionDesc")); //$NON-NLS-1$

		// Save the model
		model.save();
		model.dispose();
		IDE.setDefaultEditor(file, PDEPlugin.FEATURE_EDITOR_ID);
		return file;
	}

	private void openFeatureManifest(IFile manifestFile) {
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
		String id = PDEPlugin.FEATURE_EDITOR_ID;
		try {
			page.openEditor(input, id);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	private static void ensureFolderExists(IProject project, IPath folderPath,
			IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
			monitor.worked(1);
		}

	}
}
