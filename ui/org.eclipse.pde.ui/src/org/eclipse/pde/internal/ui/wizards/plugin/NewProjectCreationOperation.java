/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDEPluginConverter;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IFragmentFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.IPluginFieldData;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

public class NewProjectCreationOperation extends WorkspaceModifyOperation {
	private IPluginContentWizard fContentWizard;

	private IFieldData fData;

	private PluginClassCodeGenerator fGenerator;

	private WorkspacePluginModelBase fModel;

	private IProjectProvider fProjectProvider;

	private boolean fResult;

	public NewProjectCreationOperation(IFieldData data,
			IProjectProvider provider, IPluginContentWizard contentWizard) {
		fData = data;
		fProjectProvider = provider;
		fContentWizard = contentWizard;
	}

	protected void adjustManifests(IProgressMonitor monitor, IProject project)
			throws CoreException {
		if (fData.hasBundleStructure()) {
			PDEPluginConverter.convertToOSGIFormat(project,
					((AbstractFieldData) fData).getTargetVersion(), null,
					new SubProgressMonitor(monitor, 1));
			if (fModel.getPluginBase().getExtensions().length == 0) {
				project
						.getFile(
								fData instanceof IPluginFieldData ? "plugin.xml" : "fragment.xml").delete(true, null); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				trimModel(fModel.getPluginBase());
				fModel.save();
			}
		}
	}

	private void createBuildPropertiesFile(IProject project)
			throws CoreException {
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildModelFactory factory = model.getFactory();

			// BIN.INCLUDES
			IBuildEntry binEntry = factory.createEntry(IBuildEntry.BIN_INCLUDES);
			fillBinIncludes(project, binEntry);
			String srcFolder = fData.getSourceFolderName();
			if (!fData.isSimple() && srcFolder != null) {
				String libraryName = fData.getLibraryName();
				if (libraryName == null)
					libraryName = "."; //$NON-NLS-1$
				// SOURCE.<LIBRARY_NAME>
				IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX + libraryName);
				if (srcFolder.length() > 0)
					entry.addToken(new Path(srcFolder).addTrailingSeparator()
							.toString());
				else
					entry.addToken("."); //$NON-NLS-1$
				model.getBuild().add(entry);

				// OUTPUT.<LIBRARY_NAME>
				entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX
						+ libraryName);
				String outputFolder = fData.getOutputFolderName().trim();
				if (outputFolder.length() > 0)
					entry.addToken(new Path(outputFolder)
							.addTrailingSeparator().toString());
				else
					entry.addToken("."); //$NON-NLS-1$
				model.getBuild().add(entry);
			}
			model.getBuild().add(binEntry);
			model.save();
		}
	}

	protected void createContents(IProgressMonitor monitor, IProject project)
			throws CoreException, JavaModelException,
			InvocationTargetException, InterruptedException {
	}

	private void createManifest(IProject project) throws CoreException {
		if (fData instanceof IFragmentFieldData) {
			fModel = new WorkspaceFragmentModel(
					project.getFile("fragment.xml"), false); //$NON-NLS-1$
		} else {
			fModel = new WorkspacePluginModel(
					project.getFile("plugin.xml"), false); //$NON-NLS-1$
		}
		PluginBase pluginBase = (PluginBase) fModel.getPluginBase();
		if (!fData.isLegacy())
			pluginBase.setSchemaVersion("3.0"); //$NON-NLS-1$
		pluginBase.setId(fData.getId());
		pluginBase.setVersion(fData.getVersion());
		pluginBase.setName(fData.getName());
		pluginBase.setProviderName(fData.getProvider());
		pluginBase.setTargetVersion(((AbstractFieldData) fData)
				.getTargetVersion());
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			FragmentFieldData data = (FragmentFieldData) fData;
			fragment.setPluginId(data.getPluginId());
			fragment.setPluginVersion(data.getPluginVersion());
			fragment.setRule(data.getMatch());
		} else {
			if (((IPluginFieldData) fData).doGenerateClass())
				((IPlugin) pluginBase).setClassName(((IPluginFieldData) fData)
						.getClassname());
		}
		if (!fData.isSimple()) {
			setPluginLibraries(fModel);
		}

		IPluginReference[] dependencies = getDependencies();
		for (int i = 0; i < dependencies.length; i++) {
			IPluginReference ref = dependencies[i];
			IPluginImport iimport = fModel.getPluginFactory().createImport();
			iimport.setId(ref.getId());
			iimport.setVersion(ref.getVersion());
			iimport.setMatch(ref.getMatch());
			pluginBase.add(iimport);
		}
	}

	private IProject createProject() throws CoreException {
		IProject project = fProjectProvider.getProject();
		if (!project.exists()) {
			CoreUtility.createProject(project, fProjectProvider
					.getLocationPath(), null);
			project.open(null);
		}
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
		if (!fData.isSimple() && !project.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
		if (!fData.isSimple() && fData.getSourceFolderName() != null
				&& fData.getSourceFolderName().trim().length() > 0) {
			IFolder folder = project.getFolder(fData.getSourceFolderName());
			if (!folder.exists())
				CoreUtility.createFolder(folder);
		}
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {

		// start task
		monitor.beginTask(PDEUIMessages.NewProjectCreationOperation_creating,
				getNumberOfWorkUnits()); //$NON-NLS-1$
		monitor.subTask(PDEUIMessages.NewProjectCreationOperation_project); //$NON-NLS-1$

		// create project
		IProject project = createProject();
		monitor.worked(1);
		createContents(monitor, project);
		// set classpath if project has a Java nature
		if (project.hasNature(JavaCore.NATURE_ID)) {
			monitor
					.subTask(PDEUIMessages.NewProjectCreationOperation_setClasspath); //$NON-NLS-1$
			setClasspath(project, fData);
			monitor.worked(1);
		}

		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData) fData;

			// generate top-level Java class if that option is selected
			if (data.doGenerateClass()) {
				generateTopLevelPluginClass(project, new SubProgressMonitor(
						monitor, 1));
			}
		}
		// generate the manifest file
		monitor.subTask(PDEUIMessages.NewProjectCreationOperation_manifestFile); //$NON-NLS-1$
		createManifest(project);
		monitor.worked(1);

		// generate the build.properties file
		monitor
				.subTask(PDEUIMessages.NewProjectCreationOperation_buildPropertiesFile); //$NON-NLS-1$
		createBuildPropertiesFile(project);
		monitor.worked(1);

		// generate content contributed by template wizards
		boolean contentWizardResult = true;
		if (fContentWizard != null) {
			contentWizardResult = fContentWizard.performFinish(project, fModel,
					new SubProgressMonitor(monitor, 1));
		}

		fModel.save();

		adjustManifests(monitor, project);
		if (fData.hasBundleStructure()) {
			openFile(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
		} else {
			openFile((IFile) fModel.getUnderlyingResource());
		}
		monitor.worked(1);

		fResult = contentWizardResult;
	}

	protected void fillBinIncludes(IProject project, IBuildEntry binEntry)
			throws CoreException {
		if (!fData.hasBundleStructure() || fContentWizard != null)
			binEntry.addToken(fData instanceof IFragmentFieldData ? "fragment.xml" //$NON-NLS-1$
							: "plugin.xml"); //$NON-NLS-1$
		if (fData.hasBundleStructure())
			binEntry.addToken("META-INF/"); //$NON-NLS-1$
		if (!fData.isSimple()) {
			String libraryName = fData.getLibraryName();
			binEntry.addToken(libraryName == null ? "." : libraryName); //$NON-NLS-1$
		}
		if (fContentWizard != null) {
			String[] files = fContentWizard.getNewFiles();
			for (int j = 0; j < files.length; j++) {
				if (!binEntry.contains(files[j]))
					binEntry.addToken(files[j]);
			}
		}
	}

	private void generateTopLevelPluginClass(IProject project,
			IProgressMonitor monitor) throws CoreException {
		IPluginFieldData data = (IPluginFieldData) fData;
		fGenerator = new PluginClassCodeGenerator(project, data.getClassname(),
				data);
		fGenerator.generate(monitor);
		monitor.done();
	}

	private IClasspathEntry[] getClassPathEntries(IProject project,
			IFieldData data) {
		IClasspathEntry[] internalClassPathEntries = getInternalClassPathEntries(
				project, data);
		IClasspathEntry[] entries = new IClasspathEntry[internalClassPathEntries.length + 2];
		System.arraycopy(internalClassPathEntries, 0, entries, 0,
				internalClassPathEntries.length);
		entries[entries.length - 2] = ClasspathUtilCore
				.createContainerEntry();
		entries[entries.length - 1] = ClasspathUtilCore
				.createJREEntry();
		return entries;
	}

	private IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fGenerator != null) {
			IPluginReference[] refs = fGenerator.getDependencies();
			for (int i = 0; i < refs.length; i++) {
				result.add(refs[i]);
			}
		}

		if (fContentWizard != null) {
			IPluginReference[] refs = fContentWizard.getDependencies(fData
					.isLegacy() ? null : "3.0"); //$NON-NLS-1$
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}

	protected IClasspathEntry[] getInternalClassPathEntries(IProject project,
			IFieldData data) {
		if (data.getSourceFolderName() == null) {
			return new IClasspathEntry[0];
		}
		IClasspathEntry[] entries = new IClasspathEntry[1];
		IPath path = project.getFullPath().append(data.getSourceFolderName());
		entries[0] = JavaCore.newSourceEntry(path);
		return entries;
	}

	protected int getNumberOfWorkUnits() {
		int numUnits = 4;
		if (fData.hasBundleStructure())
			numUnits++;
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData) fData;
			if (data.doGenerateClass())
				numUnits++;
			if (fContentWizard != null)
				numUnits++;
		}
		return numUnits;
	}

	public boolean getResult() {
		return fResult;
	}

	private void openFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
		final IWorkbenchPage page = ww.getActivePage();
		if (page == null)
			return;
		final IWorkbenchPart focusPart = page.getActivePart();
		ww.getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(file);
					((ISetSelectionTarget) focusPart).selectReveal(selection);
				}
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
	}

	private void setClasspath(IProject project, IFieldData data)
			throws JavaModelException, CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		// Set output folder
		if (data.getOutputFolderName() != null) {
			IPath path = project.getFullPath().append(
					data.getOutputFolderName());
			javaProject.setOutputLocation(path, null);
		}
		IClasspathEntry[] entries = getClassPathEntries(project, data);
		javaProject.setRawClasspath(entries, null);
	}

	protected void setPluginLibraries(WorkspacePluginModelBase model)
			throws CoreException {
		PluginBase pluginBase = (PluginBase) fModel.getPluginBase();
		String libraryName = fData.getLibraryName();
		if (libraryName == null && !fData.hasBundleStructure()) {
			libraryName = "."; //$NON-NLS-1$
		}
		if (libraryName != null) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName(libraryName);
			library.setExported(!fData.hasBundleStructure());
			pluginBase.add(library);
		}
	}

	private void trimModel(IPluginBase base) throws CoreException {
		base.setId(null);
		base.setVersion(null);
		base.setName(null);
		base.setProviderName(null);
		if (base instanceof IFragment) {
			((IFragment) base).setPluginId(null);
			((IFragment) base).setPluginVersion(null);
			((IFragment) base).setRule(0);
		} else {
			((IPlugin) base).setClassName(null);
		}
		IPluginImport[] imports = base.getImports();
		for (int i = 0; i < imports.length; i++) {
			base.remove(imports[i]);
		}
		IPluginLibrary[] libraries = base.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			base.remove(libraries[i]);
		}
	}

}
