/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.team.core.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

public class PluginImportOperation implements IWorkspaceRunnable {
		
	public static final int IMPORT_BINARY = 1;
	public static final int IMPORT_BINARY_WITH_LINKS = 2;
	public static final int IMPORT_WITH_SOURCE = 3;

	private IPluginModelBase[] fModels;
	private int fImportType;
	private IReplaceQuery fReplaceQuery;
	private WorkspaceBuildModel buildModel;

	public interface IReplaceQuery {
		public static final int CANCEL = 0;
		public static final int NO = 1;
		public static final int YES = 2;

		int doQuery(IProject project);
	}
	
	public PluginImportOperation(
		IPluginModelBase[] models,
		int importType,
		IReplaceQuery replaceQuery) {
		this.fModels = models;
		this.fImportType = importType;
		this.fReplaceQuery = replaceQuery;
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor)
		throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(
			PDEPlugin.getResourceString("ImportWizard.operation.creating"), //$NON-NLS-1$
			fModels.length);
		try {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString("ImportWizard.operation.multiProblem"), //$NON-NLS-1$
					null);

			for (int i = 0; i < fModels.length; i++) {
				try {
					importPlugin(fModels[i], new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			if (!multiStatus.isOK()) {
				throw new CoreException(multiStatus);
			}
		} finally {
			monitor.done();
		}
	}
	
	private void importPlugin(IPluginModelBase model, IProgressMonitor monitor)
		throws CoreException {

		String id = model.getPluginBase().getId();
		String task =
			PDEPlugin.getFormattedMessage("ImportWizard.operation.creating2", id); //$NON-NLS-1$
		monitor.beginTask(task, 6);
		try {
			buildModel = null;
			
			IProject project = findProject(model.getPluginBase().getId());

			if (project.exists()) {
				if (!queryReplace(project))
					return;
				deleteProject(project, new SubProgressMonitor(monitor, 1));
			}

			createProject(project, new SubProgressMonitor(monitor, 1));
			
			File file = new File(model.getInstallLocation());
			if (file.isFile()) {
				// Plugin-in-Jar format
				extractZipFile(file, project.getFullPath(), new SubProgressMonitor(monitor, 4));
				if (fImportType != IMPORT_WITH_SOURCE)
					project.setPersistentProperty(
							PDECore.EXTERNAL_PROJECT_PROPERTY,
							PDECore.BINARY_PROJECT_VALUE);
			} else {
				switch (fImportType) {
					case IMPORT_BINARY :
						importAsBinary(project, model, new SubProgressMonitor(monitor, 4));
						break;
					case IMPORT_BINARY_WITH_LINKS :
						importAsBinaryWithLinks(
								project,
								model,
								new SubProgressMonitor(monitor, 4));
						break;
					case IMPORT_WITH_SOURCE :
						if (id.equals("org.apache.ant") || id.equals("org.eclipse.osgi.util") //$NON-NLS-1$ //$NON-NLS-2$
								|| id.equals("org.eclipse.osgi.services") || id.equals("org.eclipse.swt")) { //$NON-NLS-1$ //$NON-NLS-2$
							importAsBinary(project, model, new SubProgressMonitor(monitor, 4));
						} else {
							importWithSource(project, model, new SubProgressMonitor(monitor, 4));
						}
				}
			}
			
			setProjectDescription(project, model);

			if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
				setClasspath(project, model);
		} finally {
			monitor.done();
		}
	}
	
	private IProject findProject(String id) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getWorkspaceModel();
			if (model != null)
				return model.getUnderlyingResource().getProject();
		}
		return PDEPlugin.getWorkspace().getRoot().getProject(id);
	}
	
	private void deleteProject(IProject project, IProgressMonitor monitor)
		throws CoreException {
		if (RepositoryProvider.getProvider(project) != null)
			RepositoryProvider.unmap(project);
		project.delete(true, true, monitor);
	}
	
	private void createProject(IProject project, IProgressMonitor monitor)
		throws CoreException {
		project.create(monitor);
		if (!project.isOpen()) {
			project.open(null);
		}
	}
	
	private void importAsBinary(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {

		importPluginContent(project, model, monitor);

		project.setPersistentProperty(
				PDECore.EXTERNAL_PROJECT_PROPERTY,
				PDECore.BINARY_PROJECT_VALUE);
		
	}
	
	private void importAsBinaryWithLinks(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		
		File[] items = new File(model.getInstallLocation()).listFiles();
		if (items != null) {
			monitor.beginTask(PDEPlugin.getResourceString("PluginImportOperation.linking"), items.length); //$NON-NLS-1$
			for (int i = 0; i < items.length; i++) {
				File sourceFile = items[i];
				if (sourceFile.isDirectory()) {
					IFolder folder = project.getFolder(sourceFile.getName());
					folder.createLink(
						new Path(sourceFile.getPath()),
						IResource.NONE,
						new SubProgressMonitor(monitor, 1));
				} else {
					String fileName = sourceFile.getName();
					// Ignore .project in the plug-in.
					// This file will be created, so ignore the imported one.
					if (!fileName.equals(".project")) { //$NON-NLS-1$ //$NON-NLS-2$
						IFile file = project.getFile(fileName);
						file.createLink(
							new Path(sourceFile.getPath()),
							IResource.NONE,
							new SubProgressMonitor(monitor, 1));
					}
				}
			}
		}

		try {
			RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
		} catch (TeamException e) {
		}
		
	}

	private void importWithSource(
			IProject project,
			IPluginModelBase model,
			IProgressMonitor monitor)
	throws CoreException {
		
		monitor.beginTask("", 3); //$NON-NLS-1$
		
		importPluginContent(project, model, new SubProgressMonitor(monitor, 2));
		
		buildModel = configureBinIncludes(project, model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		
		for (int i = 0; i < libraries.length; i++) {
			if (ClasspathUtilCore.containsVariables(libraries[i].getName()))
				continue;
			IPath libraryPath = new Path(libraries[i].getName());
			IResource jarFile = project.findMember(libraryPath);
			if (jarFile != null) {
				IPath srcPath = getSourcePath(libraryPath);
				IResource srcZip = jarFile.getProject().findMember(srcPath);
				if (srcZip != null) {
					String jarName = libraryPath.removeFileExtension().lastSegment();
					IFolder dest = jarFile.getProject().getFolder("src-" + jarName); //$NON-NLS-1$
					IBuildEntry entry =
					buildModel.getFactory().createEntry(
							"source." + libraries[i].getName()); //$NON-NLS-1$
					entry.addToken(dest.getName() + "/"); //$NON-NLS-1$
					buildModel.getBuild().add(entry);
					if (!dest.exists()) {
						dest.create(true, true, null);
					}
					extractZipFile(srcZip.getLocation().toFile(), dest.getFullPath(), monitor);
					extractResources(jarFile, dest, monitor);
					srcZip.delete(true, null);
					jarFile.delete(true, null);
				}
			}
		}
		buildModel.save();
		monitor.done();
	}
	
	private WorkspaceBuildModel configureBinIncludes(IProject project, IPluginModelBase model) {
		WorkspaceBuildModel buildModel =
			new WorkspaceBuildModel(project.getFile("build.properties")); //$NON-NLS-1$
		IBuild build = buildModel.getBuild(true);
		IBuildEntry entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$

		File[] files = new File(model.getInstallLocation()).listFiles();
		try {
			for (int i = 0; i < files.length; i++) {
				String token = files[i].getName();
				if (files[i].isDirectory())
					token = token + "/"; //$NON-NLS-1$
				entry.addToken(token);
			}
			build.add(entry);
			buildModel.save();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return buildModel;
	}
	
	private void importPluginContent(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {		
		monitor.beginTask("", 2); //$NON-NLS-1$
		
		importContent(
			new File(model.getInstallLocation()),
			project.getFullPath(),
			FileSystemStructureProvider.INSTANCE,
			null,
			new SubProgressMonitor(monitor, 1));
		
		importSource(
					project,
					model.getPluginBase(),
					new Path(model.getInstallLocation()),
					new SubProgressMonitor(monitor, 1));
		
		// make sure all libraries have been imported
		// if any are missing, check in fragments		
		IFragment[] fragments = getFragmentsFor(model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		
		for (int i = 0; i < libraries.length; i++) {
			String libraryName = libraries[i].getName();
			if (ClasspathUtilCore.containsVariables(libraryName) &&
					!project.exists(new Path(ClasspathUtilCore.expandLibraryName(libraryName)))) {
				for (int j = 0; j < fragments.length; j++) {
					importJarFromFragment(project, fragments[j], libraryName);
					importSourceFromFragment(project, fragments[j], libraryName);
				}
			}
		}
	}
	
	private void importContent(
		Object source,
		IPath destPath,
		IImportStructureProvider provider,
		List filesToImport,
		IProgressMonitor monitor)
		throws CoreException {
		IOverwriteQuery query = new IOverwriteQuery() {
			public String queryOverwrite(String file) {
				return ALL;
			}
		};
		ImportOperation op = new ImportOperation(destPath, source, provider, query);
		op.setCreateContainerStructure(false);
		if (filesToImport != null) {
			op.setFilesToImport(filesToImport);
		}

		try {
			op.run(monitor);
		} catch (InvocationTargetException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}

	private void importSource(
		IProject project,
		IPluginBase plugin,
		IPath pluginPath,
		IProgressMonitor monitor)
		throws CoreException {
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		IPluginLibrary[] libraries = plugin.getLibraries();
		monitor.beginTask(
			PDEPlugin.getResourceString("ImportWizard.operation.copyingSource"), //$NON-NLS-1$
			libraries.length);
		for (int i = 0; i < libraries.length; i++) {
			IPath libPath = new Path(libraries[i].getName());
			IPath srcPath = getSourcePath(libPath);
			if (srcPath != null && !project.getFile(srcPath).exists()) {
				File srcZip = manager.findSourceFile(plugin, srcPath);
				if (srcZip != null) {
					importArchive(project, srcZip, srcPath);
				}
			}
			monitor.worked(1);
		}
	}

	private void importArchive(IProject project, File archive, IPath destPath)
		throws CoreException {
		try {
			if (destPath.segmentCount() > 2) {
				for (int i = 1; i < destPath.segmentCount(); i++) {
					IFolder folder = project.getFolder(destPath.uptoSegment(i));
					if (!folder.exists())
						folder.create(true, true, null);
				}
			}
			IFile file = project.getFile(destPath);
			FileInputStream fstream = new FileInputStream(archive);
			if (file.exists())
				file.setContents(fstream, true, false, null);
			else
				file.create(fstream, true, null);
			fstream.close();
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					e.getMessage(),
					e);
			throw new CoreException(status);
		}
	}

	private void extractZipFile(File file, IPath destPath, IProgressMonitor monitor)
		throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			importContent(
				provider.getRoot(),
				destPath,
				provider,
				null,
				monitor);
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void extractResources(IResource res, IFolder dest, IProgressMonitor monitor)
		throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectResources(provider, provider.getRoot(), collected);

			importContent(
				provider.getRoot(),
				dest.getFullPath(),
				provider,
				collected,
				monitor);
		} catch (IOException e) {
			IStatus status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void collectResources(
		ZipFileStructureProvider provider,
		Object element,
		ArrayList collected) {
		List children = provider.getChildren(element);
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr)) {
					if (!provider.getLabel(curr).equals("META-INF")) { //$NON-NLS-1$
						collectResources(provider, curr, collected);
					}					
				} else if (!provider.getLabel(curr).endsWith(".class")) { //$NON-NLS-1$
					collected.add(curr);
				}
			}
		}
	}

	private boolean queryReplace(IProject project)
		throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL :
				throw new OperationCanceledException();
			case IReplaceQuery.NO :
				return false;
		}
		return true;
	}

	private void setProjectDescription(
		IProject project,
		IPluginModelBase model)
		throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature(project, model))
			desc.setNatureIds(
				new String[] { JavaCore.NATURE_ID, PDE.PLUGIN_NATURE });
		else
			desc.setNatureIds(new String[] { PDE.PLUGIN_NATURE });
		project.setDescription(desc, null);
	}
	
	private void setClasspath(IProject project, IPluginModelBase model)
		throws JavaModelException {
		IJavaProject jProject = JavaCore.create(project);
		Vector entries = new Vector();
		if (new File(model.getInstallLocation()).isFile()) {
			IClasspathEntry entry = JavaCore.newLibraryEntry(project.getFullPath(), project.getFullPath(), null, true);
			if (!entries.contains(entry))
				entries.add(entry);
		} else if (fImportType == IMPORT_BINARY_WITH_LINKS) {
			getLinkedLibraries(project, model, entries);
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (buildModel != null) {
					IBuildEntry buildEntry = buildModel.getBuild().getEntry("source." + libraries[i].getName()); //$NON-NLS-1$
					if (buildEntry != null) {
						IPath path = new Path(buildEntry.getTokens()[0]);
						entries.add(JavaCore.newSourceEntry(project.getFullPath().append(path)));
						continue;
					}
				}
				IClasspathEntry entry = getLibraryEntry(project, libraries[i]);
				if (entry != null)
					entries.add(entry);
			}
		}
		entries.add(ClasspathUtilCore.createContainerEntry());
		entries.add(ClasspathUtilCore.createJREEntry());
		jProject.setRawClasspath((IClasspathEntry[]) entries
				.toArray(new IClasspathEntry[entries.size()]), jProject
				.getOutputLocation(), null);
	}
	
	private void getLinkedLibraries(IProject project, IPluginModelBase model, Vector entries) {
		ClasspathUtilCore.addLibraries(model, true, entries);
		for (int i = 0; i < entries.size(); i++) {
			IPath path = new Path(model.getInstallLocation());
			IClasspathEntry entry = (IClasspathEntry)entries.get(i);	
			if (PDEPlugin.getWorkspace().getRoot().findMember(entry.getPath()) != null)
				continue;
			if (entry.getPath().matchingFirstSegments(path) == path.segmentCount()) {
				path = entry.getPath().removeFirstSegments(path.segmentCount());
				path = project.getFullPath().append(path).setDevice(null);
			} else {
				if (!(model instanceof IFragmentModel)) {
					IFragment[] fragments = getFragmentsFor(model);
					for (int j = 0; j < fragments.length; j++) {
						IPath fragPath = new Path(fragments[j].getModel().getInstallLocation());
						if (entry.getPath().matchingFirstSegments(fragPath) == fragPath.segmentCount()) {
							path = PDEPlugin.getWorkspace().getRoot().getFullPath();
							path = path.append(fragments[j].getId());
							path = path.append(entry.getPath().removeFirstSegments(fragPath.segmentCount())).setDevice(null);
							break;
						}
					}
				}
			}
			IPath srcAttachment = entry.getSourceAttachmentPath();
			IPath srcAttRoot = entry.getSourceAttachmentRootPath();
			entries.setElementAt(JavaCore.newLibraryEntry(path, srcAttachment, srcAttRoot, entry.isExported()), i);	
		}		
	}
	
	private IClasspathEntry getLibraryEntry(IProject project, IPluginLibrary library) {
		if (IPluginLibrary.RESOURCE.equals(library.getType()))
			return null;
		
		String libraryName = ClasspathUtilCore.expandLibraryName(library.getName());
		if (!project.exists(new Path(libraryName)))
			return null;
		
		IPath srcAttach = getSourceAttachmentPath(project, project.getFullPath().append(libraryName));
		IPath srcRoot = srcAttach != null ? Path.EMPTY : null;
		return JavaCore.newLibraryEntry(project.getFullPath().append(libraryName), srcAttach, srcRoot, library.isExported());
	}

	private IPath getSourceAttachmentPath(IProject project, IPath jarPath) {
		IPath sourcePath = getSourcePath(jarPath);
		if (sourcePath == null)
			return null;
		IWorkspaceRoot root = project.getWorkspace().getRoot();
		if (root.findMember(sourcePath) != null) {
			return sourcePath;
		}
		return null;
	}
	
	private IPath getSourcePath(IPath jarPath) {
		jarPath = new Path(ClasspathUtilCore.expandLibraryName(jarPath
				.toString()));
		String libName = jarPath.lastSegment();
		if (libName != null) {
			int idx = libName.lastIndexOf('.');
			if (idx != -1) {
				String srcName = libName.substring(0, idx) + "src.zip"; //$NON-NLS-1$
				IPath path = jarPath.removeLastSegments(1).append(srcName);
				return path;
			}
		}
		return null;
	}
	
	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		// Always return true when the model is in the plugin-in-jar format
		if (new File(model.getInstallLocation()).isFile())
			return true;
		boolean isJavaProject = false;
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			if (!IPluginLibrary.RESOURCE.equals(libraries[i].getType())) {
				isJavaProject = true;
				break;
			}
		}
		if (!isJavaProject) {
			IPluginImport[] imports = model.getPluginBase().getImports();
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isReexported()) {
					isJavaProject = true;
					break;
				}
			}
		}
		return isJavaProject;
	}
	
	private IFragment[] getFragmentsFor(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			if (fModels[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) fModels[i]).getFragment();
				if (PDECore.compare(
						model.getPluginBase().getId(),
						model.getPluginBase().getVersion(),
						fragment.getPluginId(),
						fragment.getVersion(),
						fragment.getRule())) {
					result.add(fragment);
				}
			}
		}
		return (IFragment[])result.toArray(new IFragment[result.size()]);
	}
	
	private void importJarFromFragment(IProject project, IFragment fragment, String name)
		throws CoreException {
		IPath jarPath = new Path(ClasspathUtilCore.expandLibraryName(name));
		File swtJar =
			new File(fragment.getModel().getInstallLocation(), jarPath.toString());
		if (swtJar.exists()) {
			importArchive(project, swtJar, jarPath);
		}
	}
	
	private void importSourceFromFragment(IProject project, IFragment fragment, String name)
		throws CoreException {
		IPath jarPath = new Path(ClasspathUtilCore.expandLibraryName(name));
		IPath srcPath = getSourcePath(jarPath);
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		File srcFile = manager.findSourceFile(fragment, srcPath);
		if (srcFile != null) {
			importArchive(project, srcFile, srcPath);
		}
	}
}
