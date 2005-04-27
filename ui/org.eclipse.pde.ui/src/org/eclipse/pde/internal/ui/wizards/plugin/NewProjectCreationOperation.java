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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IFragmentFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.IPluginFieldData;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Constants;

public class NewProjectCreationOperation extends WorkspaceModifyOperation {
	private IFieldData fData;

	private IProjectProvider fProjectProvider;

	private WorkspacePluginModelBase fModel;

	private PluginClassCodeGenerator fGenerator;

	private IPluginContentWizard fContentWizard;

	private boolean fResult;

	public NewProjectCreationOperation(IFieldData data,
			IProjectProvider provider, IPluginContentWizard contentWizard) {
		fData = data;
		fProjectProvider = provider;
		fContentWizard = contentWizard;
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
		// copy jars
		if (fData instanceof LibraryPluginFieldData) {
			String[] paths = ((LibraryPluginFieldData) fData).getLibraryPaths();
			for (int i = 0; i < paths.length; i++) {
				File jarFile = new File(paths[i]);
				if (((LibraryPluginFieldData) fData).isUnzipLibraries()) {
					importJar(jarFile, project, monitor);
				} else {
					addJar(jarFile, project, monitor);
				}
				monitor.worked(1);
			}
			if (!fData.hasBundleStructure()) {
				// delete manifest.mf imported from libraries
				IFile importedManifest = project
						.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
				if (importedManifest.exists()) {
					importedManifest.delete(true, false, monitor);
				}
			}
		}
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

		if (fData.hasBundleStructure()) {
			PDEPluginConverter.convertToOSGIFormat(project,
					((AbstractFieldData) fData).getTargetVersion(), null,
					new SubProgressMonitor(monitor, 1));
			if (fData instanceof LibraryPluginFieldData
					&& ((LibraryPluginFieldData) fData).isUnzipLibraries()) {
				adjustExportRoot(monitor, project);
			}

			if (fModel.getPluginBase().getExtensions().length == 0) {
				project
						.getFile(
								fData instanceof IPluginFieldData ? "plugin.xml" : "fragment.xml").delete(true, null); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				trimModel(fModel.getPluginBase());
				fModel.save();
			}
			openFile(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
		} else {
			openFile((IFile) fModel.getUnderlyingResource());
		}
		monitor.worked(1);

		fResult = contentWizardResult;
	}

	private void addJar(File jarFile, IProject project, IProgressMonitor monitor)
			throws CoreException {
		String jarName = jarFile.getName();
		IFile file = project.getFile(jarName);
		monitor.subTask(NLS.bind(
				PDEUIMessages.NewProjectCreationOperation_copyingJar, jarName)); //$NON-NLS-1$
		InputStream in = null;
		try {
			in = new FileInputStream(jarFile);
			file.create(in, true, monitor);
		} catch (FileNotFoundException fnfe) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	private void importJar(File jar, IResource destination,
			IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		ZipFile input = null;
		try {
			try {
				input = new ZipFile(jar);
				ZipFileStructureProvider provider = new ZipFileStructureProvider(
						input);
				ImportOperation op = new ImportOperation(destination
						.getFullPath(), provider.getRoot(), provider,
						new IOverwriteQuery() {
							public String queryOverwrite(String pathString) {
								return IOverwriteQuery.ALL;
							}
						});
				op.run(monitor);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					PDEPlugin.PLUGIN_ID, IStatus.OK,
					PDEUIMessages.NewProjectCreationOperation_errorImportingJar
							+ jar, e));
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

	public boolean getResult() {
		return fResult;
	}

	private void generateTopLevelPluginClass(IProject project,
			IProgressMonitor monitor) throws CoreException {
		IPluginFieldData data = (IPluginFieldData) fData;
		fGenerator = new PluginClassCodeGenerator(project, data.getClassname(),
				data);
		fGenerator.generate(monitor);
		monitor.done();
	}

	private int getNumberOfWorkUnits() {
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
		if (fData instanceof LibraryPluginFieldData) {
			numUnits += ((LibraryPluginFieldData) fData).getLibraryPaths().length;
		}
		return numUnits;
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
			if (fData instanceof LibraryPluginFieldData
					&& ((LibraryPluginFieldData) fData).isUnzipLibraries()) {
				IPluginLibrary library = fModel.getPluginFactory()
						.createLibrary();
				library.setName("."); //$NON-NLS-1$
				library.setExported(true);
				pluginBase.add(library);

			} else if (fData.getLibraryName() != null) {
				IPluginLibrary library = fModel.getPluginFactory()
						.createLibrary();
				library.setName(fData.getLibraryName());
				if (fData instanceof LibraryPluginFieldData) {
					library.setExported(true);
				}
				pluginBase.add(library);
			}
		}
		if (fData instanceof LibraryPluginFieldData
				&& !((LibraryPluginFieldData) fData).isUnzipLibraries()) { //$NON-NLS-1$
			String[] paths = ((LibraryPluginFieldData) fData).getLibraryPaths();
			for (int i = 0; i < paths.length; i++) {
				File jarFile = new File(paths[i]);
				IPluginLibrary library = fModel.getPluginFactory()
						.createLibrary();
				library.setName(jarFile.getName());
				library.setExported(true);
				pluginBase.add(library);
			}
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

	private void createBuildPropertiesFile(IProject project)
			throws CoreException {
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildModelFactory factory = model.getFactory();

			// BIN.INCLUDES
			IBuildEntry binEntry = factory
					.createEntry(IBuildEntry.BIN_INCLUDES);
			if (!fData.hasBundleStructure() || fContentWizard != null)
				binEntry
						.addToken(fData instanceof IFragmentFieldData ? "fragment.xml" //$NON-NLS-1$
								: "plugin.xml"); //$NON-NLS-1$
			if (fData.hasBundleStructure())
				binEntry.addToken("META-INF/"); //$NON-NLS-1$

			if (fData instanceof LibraryPluginFieldData) {
				if (((LibraryPluginFieldData) fData).isUnzipLibraries()) {
					IResource[] resources = project.members(false);
					for (int j = 0; j < resources.length; j++) {
						if (resources[j] instanceof IFolder) {
							if (!binEntry
									.contains(resources[j].getName() + "/")) //$NON-NLS-1$
								binEntry.addToken(resources[j].getName() + "/"); //$NON-NLS-1$
						} else {
							if (".project".equals(resources[j].getName()) //$NON-NLS-1$
									|| ".classpath".equals(resources[j] //$NON-NLS-1$
											.getName())
									|| "build.properties".equals(resources[j] //$NON-NLS-1$
											.getName())) {
								continue;
							}
							if (!binEntry.contains(resources[j].getName()))
								binEntry.addToken(resources[j].getName());
						}
					}
				} else {
					String[] libraryPaths = ((LibraryPluginFieldData) fData)
							.getLibraryPaths();
					for (int j = 0; j < libraryPaths.length; j++) {
						File jarFile = new File(libraryPaths[j]);
						String name = jarFile.getName();
						if (!binEntry.contains(name))
							binEntry.addToken(name);
					}
				}
			}

			String libraryName = fData.getLibraryName();
			if (!fData.isSimple() && libraryName != null) {
				binEntry.addToken(libraryName);
				if (fContentWizard != null) {
					String[] files = fContentWizard.getNewFiles();
					for (int j = 0; j < files.length; j++) {
						if (!binEntry.contains(files[j]))
							binEntry.addToken(files[j]);
					}
				}

				// SOURCE.<LIBRARY_NAME>
				IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX
						+ libraryName);
				String srcFolder = fData.getSourceFolderName().trim();
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

	private void setClasspath(IProject project, IFieldData data)
			throws JavaModelException, CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		// Set output folder
		if (data.getOutputFolderName() != null) {
			IPath path = project.getFullPath().append(
					data.getOutputFolderName());
			javaProject.setOutputLocation(path, null);
		}
		// Set classpath
		IClasspathEntry[] entries = null;
		int i = 0;
		if (data.getSourceFolderName() != null) {
			entries = new IClasspathEntry[3];
			IPath path = project.getFullPath().append(
					data.getSourceFolderName());
			entries[i++] = JavaCore.newSourceEntry(path);
		} else {
			String[] libraryPaths = new String[0];
			if (fData instanceof LibraryPluginFieldData) {
				if (((LibraryPluginFieldData) fData).isUnzipLibraries()) {
					libraryPaths = new String[] { "" }; //$NON-NLS-1$
				} else {
					libraryPaths = ((LibraryPluginFieldData) fData)
							.getLibraryPaths();
				}
			}
			entries = new IClasspathEntry[2 + libraryPaths.length];
			for (int j = 0; j < libraryPaths.length; j++) {
				File jarFile = new File(libraryPaths[j]);
				String jarName = jarFile.getName();
				IPath path = project.getFullPath().append(jarName);
				entries[i++] = JavaCore.newLibraryEntry(path, null, null, true);
			}
		}
		entries[i++] = ClasspathUtilCore.createContainerEntry();
		entries[i++] = ClasspathUtilCore.createJREEntry();
		javaProject.setRawClasspath(entries, null);
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

	private void adjustExportRoot(IProgressMonitor monitor, IProject project)
			throws CoreException {
		IResource[] resources = project.members(false);
		for (int j = 0; j < resources.length; j++) {
			if (resources[j] instanceof IFile) {
				if (".project".equals(resources[j].getName()) //$NON-NLS-1$
						|| ".classpath".equals(resources[j] //$NON-NLS-1$
								.getName()) || "plugin.xml".equals(resources[j] //$NON-NLS-1$
								.getName())
						|| "build.properties".equals(resources[j] //$NON-NLS-1$
								.getName())) {
					continue;
				}
				// resource at the root, export root
				return;
			}
		}
		removeExportRoot(project.getFile("META-INF/MANIFEST.MF"), monitor); //$NON-NLS-1$
	}

	private void removeExportRoot(IFile file, IProgressMonitor monitor)
			throws CoreException {
		if (!file.exists())
			return;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file
					.getFullPath());

			IDocument document = buffer.getDocument();
			BundleModel model = new BundleModel(document, false);
			model.load();
			TextEdit edit = removeRootExportPackage(model);
			if (edit != null) {
				try {
					edit.apply(document);
				} catch (BadLocationException e) {
					PDEPlugin.logException(e);
				}
				buffer.commit(monitor, true);
			}
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}

	}

	private TextEdit removeRootExportPackage(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader) bundle
				.getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] packages = header.getPackages();
		for (int i = 0; i < packages.length; i++) {
			if (".".equals(packages[i].getName())) { //$NON-NLS-1$
				header.removePackage(packages[i]);
				return new ReplaceEdit(header.getOffset(), header.getLength(),
						header.write() + System.getProperty("line.separator")); //$NON-NLS-1$
			}
		}
		return null;
	}

}
