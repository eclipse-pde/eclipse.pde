/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
	private ArrayList fModelIds;
	private int fImportType;
	private IReplaceQuery fReplaceQuery;

	public interface IReplaceQuery {
		public static final int CANCEL = 0;
		public static final int NO = 1;
		public static final int YES = 2;

		int doQuery(IProject project);
	}
	
	public PluginImportOperation(
		IPluginModelBase[] models,
		ArrayList modelIds,
		int importType,
		IReplaceQuery replaceQuery) {
		this.fModels = models;
		this.fModelIds = modelIds;
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
			PDEPlugin.getResourceString("ImportWizard.operation.creating"),
			fModels.length);
		try {
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEPlugin.getResourceString("ImportWizard.operation.multiProblem"),
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
			PDEPlugin.getFormattedMessage("ImportWizard.operation.creating2", id);
		monitor.beginTask(task, 6);
		try {
			IProject project = findProject(model.getPluginBase().getId());

			if (project.exists()) {
				if (!queryReplace(project))
					return;
				deleteProject(project, new SubProgressMonitor(monitor, 1));
			}

			createProject(project, new SubProgressMonitor(monitor, 1));

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
					if (model.getPluginBase().getId().equals("org.apache.ant")) {
						importAsBinary(project, model, new SubProgressMonitor(monitor, 4));
					} else {
						importWithSource(project, model, new SubProgressMonitor(monitor, 4));
					}
			}

			setProjectDescription(project, model);

			if (project.hasNature(JavaCore.NATURE_ID))
				resetClasspath(project, model);
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
		project.delete(true, true, monitor);
		try {
			RepositoryProvider.unmap(project);
		} catch (TeamException e) {
		}
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
		monitor.beginTask("Linking content...", items.length);
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
				// Ignore .classpath and .project in the plug-in.
				// These files will be created, so ignore the imported ones.
				if (!fileName.equals(".classpath") && !fileName.equals(".project")) {
					IFile file = project.getFile(fileName);
					file.createLink(
						new Path(sourceFile.getPath()),
						IResource.NONE,
						new SubProgressMonitor(monitor, 1));
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
		
		monitor.beginTask("", 3);
		importPluginContent(project, model, new SubProgressMonitor(monitor, 2));
		
		WorkspaceBuildModel buildModel = configureBinIncludes(project, model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		
		boolean sourceFound = false;
		for (int i = 0; i < libraries.length; i++) {
			IPath libraryPath = UpdateClasspathOperation.getExpandedPath(new Path(libraries[i].getName()));
			IResource jarFile = project.findMember(libraryPath);
			if (jarFile != null) {
				IPath srcPath = UpdateClasspathOperation.getSourcePath(libraryPath);
				IResource srcZip = jarFile.getProject().findMember(srcPath);
				if (srcZip != null) {
					String jarName = libraryPath.removeFileExtension().lastSegment();
					IFolder dest = jarFile.getProject().getFolder("src-" + jarName);
					IBuildEntry entry =
					buildModel.getFactory().createEntry(
							"source." + libraries[i].getName());
					entry.addToken(dest.getName() + "/");
					buildModel.getBuild().add(entry);
					if (!dest.exists()) {
						dest.create(true, true, null);
					}
					extractZipFile(srcZip, dest, monitor);
					extractResources(jarFile, dest, monitor);
					srcZip.delete(true, null);
					jarFile.delete(true, null);
					sourceFound = true;
				}
			}
		}
		buildModel.save();
		if (!sourceFound)
			project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					PDECore.BINARY_PROJECT_VALUE);
		monitor.done();
	}
	
	private WorkspaceBuildModel configureBinIncludes(IProject project, IPluginModelBase model) {
		WorkspaceBuildModel buildModel =
			new WorkspaceBuildModel(project.getFile("build.properties"));
		IBuild build = buildModel.getBuild(true);
		IBuildEntry entry = buildModel.getFactory().createEntry("bin.includes");

		File[] files = new File(model.getInstallLocation()).listFiles();
		try {
			for (int i = 0; i < files.length; i++) {
				String token = files[i].getName();
				if (files[i].isDirectory())
					token = token + "/";
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
		boolean isSWTPlugin = model.getPluginBase().getId().equals("org.eclipse.swt");
		
		monitor.beginTask("", 2);
		
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
		
		if (isSWTPlugin) {
			IFragment swtFragment = getSWTFragment(model);
			if (swtFragment != null) {
				String libraryName = model.getPluginBase().getLibraries()[0].getName();
				importSWTJar(project, swtFragment, libraryName);
				importSWTSource(project, swtFragment, libraryName);
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
			PDEPlugin.getResourceString("ImportWizard.operation.copyingSource"),
			libraries.length);
		for (int i = 0; i < libraries.length; i++) {
			IPath libPath = new Path(libraries[i].getName());
			IPath srcPath = UpdateClasspathOperation.getSourcePath(libPath);
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

	private void extractZipFile(IResource res, IFolder dest, IProgressMonitor monitor)
		throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(res.getLocation().toFile());
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			importContent(
				provider.getRoot(),
				dest.getFullPath(),
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
					if (!provider.getLabel(curr).equals("META-INF")) {
						collectResources(provider, curr, collected);
					}					
				} else if (!provider.getLabel(curr).endsWith(".class")) {
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
	
	private void resetClasspath(IProject project, IPluginModelBase model)
		throws JavaModelException {
		IJavaProject jProject = JavaCore.create(project);
		jProject.setRawClasspath(
			new IClasspathEntry[0],
			jProject.getOutputLocation(),
			null);
		fModelIds.add(model.getPluginBase().getId());
	}
	
	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		boolean isJavaProject = model.getPluginBase().getLibraries().length > 0;
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
	
	private IFragment getSWTFragment(IPluginModelBase model) {
		for (int i = 0; i < fModels.length; i++) {
			if (fModels[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) fModels[i]).getFragment();
				if (PDECore.compare(
						model.getPluginBase().getId(),
						model.getPluginBase().getVersion(),
						fragment.getPluginId(),
						fragment.getVersion(),
						fragment.getRule())) {
					return fragment;
				}
			}
		}
		return null;
	}
	
	private void importSWTJar(IProject project, IFragment fragment, String name)
		throws CoreException {
		IPath jarPath = UpdateClasspathOperation.getExpandedPath(new Path(name));
		File swtJar =
			new File(fragment.getModel().getInstallLocation(), jarPath.toString());
		if (swtJar.exists()) {
			importArchive(project, swtJar, jarPath);
		}
	}
	
	private void importSWTSource(IProject project, IFragment fragment, String name)
		throws CoreException {
		IPath jarPath = UpdateClasspathOperation.getExpandedPath(new Path(name));
		IPath srcPath = UpdateClasspathOperation.getSourcePath(jarPath);
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		File srcFile = manager.findSourceFile(fragment, srcPath);
		if (srcFile != null) {
			importArchive(project, srcFile, srcPath);
		}
	}
}
