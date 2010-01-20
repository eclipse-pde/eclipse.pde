/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;

public abstract class AbstractCreateFeatureOperation extends WorkspaceModifyOperation {

	protected IProject fProject;
	protected IPath fLocation;
	protected FeatureData fFeatureData;
	private Shell fShell;

	public AbstractCreateFeatureOperation(IProject project, IPath location, FeatureData featureData, Shell shell) {
		fProject = project;
		fLocation = location;
		fFeatureData = featureData;
		fShell = shell;
	}

	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		try {
			createFeature(monitor);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			monitor.done();
		}
	}

	protected void createFeature(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(PDEUIMessages.NewFeatureWizard_creatingProject, 3);
		IFile file;
		if (shouldOverwriteFeature()) {
			createProject(monitor);
			monitor.worked(1);
			createBuildProperties();
			monitor.worked(1);

			// create feature.xml
			monitor.subTask(PDEUIMessages.NewFeatureWizard_creatingManifest);
			file = createFeature();
			monitor.worked(1);
		} else {
			fProject.create(monitor);
			fProject.open(monitor);
			file = PDEProject.getFeatureXml(fProject);
			monitor.worked(3);
		}
		if (file.exists())
			openFeatureEditor(file);
	}

	private void createProject(IProgressMonitor monitor) throws CoreException {
		CoreUtility.createProject(fProject, fLocation, monitor);
		fProject.open(monitor);
		IProjectDescription desc = fProject.getWorkspace().newProjectDescription(fProject.getName());
		desc.setLocation(fLocation);
		if (!fProject.hasNature(PDE.FEATURE_NATURE))
			CoreUtility.addNatureToProject(fProject, PDE.FEATURE_NATURE, monitor);

		if (fFeatureData.hasCustomHandler()) {
			if (!fProject.hasNature(JavaCore.NATURE_ID))
				CoreUtility.addNatureToProject(fProject, JavaCore.NATURE_ID, monitor);

			if (fFeatureData.getSourceFolderName() != null && fFeatureData.getSourceFolderName().trim().length() > 0) {
				IFolder folder = fProject.getFolder(fFeatureData.getSourceFolderName());
				if (!folder.exists())
					CoreUtility.createFolder(folder);
			}

			IJavaProject jproject = JavaCore.create(fProject);
			jproject.setOutputLocation(fProject.getFullPath().append(fFeatureData.getJavaBuildFolderName()), monitor);
			jproject.setRawClasspath(new IClasspathEntry[] {JavaCore.newSourceEntry(fProject.getFullPath().append(fFeatureData.getSourceFolderName())), JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER))}, monitor);
		}
	}

	protected void createBuildProperties() throws CoreException {
		IFile file = PDEProject.getBuildProperties(fProject);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry = model.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			ientry.addToken(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			String library = fFeatureData.library;
			if (library != null) {
				String source = fFeatureData.getSourceFolderName();
				if (source != null) {
					IBuildEntry entry = model.getFactory().createEntry(IBuildEntry.JAR_PREFIX + library);
					if (!source.endsWith("/")) //$NON-NLS-1$
						source += "/"; //$NON-NLS-1$
					entry.addToken(source);
					ientry.addToken(library);
					model.getBuild().add(entry);
				}
				String output = fFeatureData.getJavaBuildFolderName();
				if (output != null) {
					IBuildEntry entry = model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX + library);
					if (!output.endsWith("/")) //$NON-NLS-1$
						output += "/"; //$NON-NLS-1$
					entry.addToken(output);
					model.getBuild().add(entry);
				}
			}

			model.getBuild().add(ientry);
			model.save();
		}
		IDE.setDefaultEditor(file, IPDEUIConstants.BUILD_EDITOR_ID);
	}

	protected IFile createFeature() throws CoreException {
		IFile file = PDEProject.getFeatureXml(fProject);
		WorkspaceFeatureModel model = new WorkspaceFeatureModel();
		model.setFile(file);
		IFeature feature = model.getFeature();
		feature.setLabel(fFeatureData.name);
		feature.setId(fFeatureData.id);
		feature.setVersion(fFeatureData.version);
		feature.setProviderName(fFeatureData.provider);
		if (fFeatureData.hasCustomHandler())
			feature.setInstallHandler(model.getFactory().createInstallHandler());

		configureFeature(feature, model);

		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler != null)
			handler.setLibrary(fFeatureData.library);

		IFeatureInfo info = model.getFactory().createInfo(IFeature.INFO_COPYRIGHT);
		feature.setFeatureInfo(info, IFeature.INFO_COPYRIGHT);
		info.setURL("http://www.example.com/copyright"); //$NON-NLS-1$
		info.setDescription(PDEUIMessages.NewFeatureWizard_sampleCopyrightDesc);

		info = model.getFactory().createInfo(IFeature.INFO_LICENSE);
		feature.setFeatureInfo(info, IFeature.INFO_LICENSE);
		info.setURL("http://www.example.com/license"); //$NON-NLS-1$
		info.setDescription(PDEUIMessages.NewFeatureWizard_sampleLicenseDesc);

		info = model.getFactory().createInfo(IFeature.INFO_DESCRIPTION);
		feature.setFeatureInfo(info, IFeature.INFO_DESCRIPTION);
		info.setURL("http://www.example.com/description"); //$NON-NLS-1$
		info.setDescription(PDEUIMessages.NewFeatureWizard_sampleDescriptionDesc);

		// Save the model
		model.save();
		model.dispose();
		IDE.setDefaultEditor(file, IPDEUIConstants.FEATURE_EDITOR_ID);
		return file;
	}

	protected abstract void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException;

	protected void openFeatureEditor(IFile manifestFile) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		// Reveal the file first
		final ISelection selection = new StructuredSelection(manifestFile);
		final IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof ISetSelectionTarget) {
			fShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					((ISetSelectionTarget) activePart).selectReveal(selection);
				}
			});
		}
		// Open the editor
		try {
			page.openEditor(new FileEditorInput(manifestFile), IPDEUIConstants.FEATURE_EDITOR_ID);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	protected boolean shouldOverwriteFeature() {
		return !fLocation.append(fProject.getName()).toFile().exists() || MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(), this instanceof CreateFeaturePatchOperation ? PDEUIMessages.FeaturePatch_wtitle : PDEUIMessages.NewFeatureWizard_wtitle, PDEUIMessages.NewFeatureWizard_overwriteFeature);
	}
}
