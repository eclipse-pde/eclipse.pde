/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPluginConverter;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.plugin.ClasspathComputer;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public class PluginImportOperation extends JarImportOperation {

	public static final int IMPORT_BINARY = 1;

	public static final int IMPORT_BINARY_WITH_LINKS = 2;

	public static final int IMPORT_WITH_SOURCE = 3;

	private IPluginModelBase[] fModels;

	private int fImportType;

	private IImportQuery fReplaceQuery;
	
	private Hashtable fProjectClasspaths = new Hashtable();

	private boolean fForceAutobuild;

	private IImportQuery fExecutionQuery;

	public interface IImportQuery {
		public static final int CANCEL = 0;

		public static final int NO = 1;

		public static final int YES = 2;

		int doQuery(String message);
	}

	public PluginImportOperation(IPluginModelBase[] models, int importType, IImportQuery replaceQuery, IImportQuery executionQuery) {
		fModels = models;
		fImportType = importType;
		fReplaceQuery = replaceQuery;
		fExecutionQuery = executionQuery;
	}

	public PluginImportOperation(IPluginModelBase[] models, int importType, IImportQuery replaceQuery, IImportQuery executionQuery, boolean forceAutobuild) {
		this(models, importType, replaceQuery, executionQuery);
		fForceAutobuild = forceAutobuild;
	}

	public void run(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException{
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_creating, fModels.length + 1);
		try {
			MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEUIMessages.ImportWizard_operation_multiProblem, 
					null);

			for (int i = 0; i < fModels.length; i++) {
				try {
					importPlugin(fModels[i], new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					setClasspaths(new SubProgressMonitor(monitor, 1));
					throw new OperationCanceledException();
				}
			}
			setClasspaths(new SubProgressMonitor(monitor, 1));
			if (!ResourcesPlugin.getWorkspace().isAutoBuilding() && fForceAutobuild)
				runBuildJob();
			if (!multiStatus.isOK())
				throw new CoreException(multiStatus);
		} finally {
			monitor.done();
		}
	}
	
	private void runBuildJob() {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) { 
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_AUTO_BUILD == family;
			}

			protected IStatus run(IProgressMonitor monitor) {
				try {
					PDEPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
				} catch (CoreException e) {
				}
				return Status.OK_STATUS;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.schedule();
	}
	
	private void setClasspaths(IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask("", fProjectClasspaths.size()); //$NON-NLS-1$
		Enumeration keys = fProjectClasspaths.keys();
		while (keys.hasMoreElements()) {
			IProject project = (IProject)keys.nextElement();
			IClasspathEntry[] classpath = (IClasspathEntry[])fProjectClasspaths.get(project);
			monitor.subTask(project.getName());
			JavaCore.create(project).setRawClasspath(classpath, new SubProgressMonitor(monitor, 1));
		}		
	}

	private void importPlugin(IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		String id = model.getPluginBase().getId();
		monitor.beginTask(NLS.bind(PDEUIMessages.ImportWizard_operation_creating2, id), 6);
		try {
			BundleDescription desc = model.getBundleDescription();
			if (desc != null) {
				IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
				String[] envs = desc.getExecutionEnvironments();
				boolean found = false; 
				for (int i = 0; i < envs.length; i++) {
					if (manager.getEnvironment(envs[i]) != null) {
						found = true;
						break;
					}
				}
				if (envs.length > 0 && !found) {
					String message = NLS.bind(PDEUIMessages.PluginImportOperation_executionEnvironment, id, envs[0]);
					if (!queryExecutionEnvironment(message))
						return;					
				}
			}
			
			IProject project = findProject(model.getPluginBase().getId());

			if (project.exists()) {
				if (!queryReplace(project))
					return;
				if (RepositoryProvider.isShared(project))
					RepositoryProvider.unmap(project);
				project.delete(true, true, monitor);
			}

			project.create(monitor);
			if (!project.isOpen())
				project.open(monitor);			
			monitor.worked(1);

			switch (fImportType) {
				case IMPORT_BINARY:
					importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					break;
				case IMPORT_BINARY_WITH_LINKS:
					if (model.getPluginBase().getId().startsWith("org.eclipse.swt") && !isJARd(model)) { //$NON-NLS-1$
						importAsBinary(project, model, true, monitor);
					} else {
						importAsBinaryWithLinks(project, model, new SubProgressMonitor(monitor, 4));
					}
					break;
				case IMPORT_WITH_SOURCE:
					if (isExempt(model)) {
						importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					} else {
						importAsSource(project, model, new SubProgressMonitor(monitor, 4));
					}
			}

			setProjectDescription(project, model);

			if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
				fProjectClasspaths .put(project, ClasspathComputer.getClasspath(project, model, true));
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}
	
	private void importAsBinaryWithLinks(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		if (isJARd(model)) {
			extractJARdPlugin(
					project,
					model,
					monitor);
		} else {
			File[] items = new File(model.getInstallLocation()).listFiles();
			if (items != null) {
				monitor.beginTask(PDEUIMessages.PluginImportOperation_linking, items.length + 1); 
				for (int i = 0; i < items.length; i++) {
					File sourceFile = items[i];
					String name = sourceFile.getName();
					if (sourceFile.isDirectory()) {
						project.getFolder(name).createLink(
							new Path(sourceFile.getPath()),
							IResource.NONE,
							new SubProgressMonitor(monitor, 1));
					} else {
						if (!name.equals(".project")) { //$NON-NLS-1$ 
							project.getFile(name).createLink(
								new Path(sourceFile.getPath()),
								IResource.NONE,
								new SubProgressMonitor(monitor, 1));
						}
					}
				}
			}
			linkSourceArchives(project, model, new SubProgressMonitor(monitor, 1));
		}
		try {
			RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
		} catch (TeamException e) {
		}
	}

	private void importAsBinary(IProject project, IPluginModelBase model, boolean markAsBinary, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 2); //$NON-NLS-1$
		if (isJARd(model)) {
			extractJARdPlugin(
					project,
					model,
					new SubProgressMonitor(monitor, 1));
		} else {
			importContent(
					new File(model.getInstallLocation()),
					project.getFullPath(),
					FileSystemStructureProvider.INSTANCE,
					null,
					new SubProgressMonitor(monitor, 1));
			importSourceArchives(
					project,
					model,
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
		
		if (markAsBinary)
			project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					PDECore.BINARY_PROJECT_VALUE);		
	}

	private void importAsSource(IProject project, IPluginModelBase model, SubProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 3); //$NON-NLS-1$
		importAsBinary(project, model, false, new SubProgressMonitor(monitor, 2));
		
		WorkspaceBuildModel buildModel = new WorkspaceBuildModel(project.getFile("build.properties")); //$NON-NLS-1$
		if (!isJARd(model) || containsCode(new File(model.getInstallLocation()))) {
			String[] libraries = getLibraryNames(model, false);
			for (int i = 0; i < libraries.length; i++) {
				if (ClasspathUtilCore.containsVariables(libraries[i]))
					continue;
				String name = ClasspathUtilCore.expandLibraryName(libraries[i]);
				IPath libraryPath = (name.equals(".") && isJARd(model)) //$NON-NLS-1$
										? new Path(new File(model.getInstallLocation()).getName())
										: new Path(name);
				IResource jarFile = project.findMember(libraryPath);
				if (jarFile != null) {
					IResource srcZip = jarFile.getProject().findMember(ClasspathUtilCore.getSourceZipName(jarFile.getName()));
					if (srcZip != null) {
						String jarName = libraries[i].equals(".") ? "" : libraryPath.removeFileExtension().lastSegment(); //$NON-NLS-1$ //$NON-NLS-2$
						String folder = addBuildEntry(buildModel, "source." + libraries[i], "src" + (jarName.length() == 0 ? "/" : "-" + jarName + "/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						IFolder dest = jarFile.getProject().getFolder(folder); 
						if (!dest.exists()) {
							dest.create(true, true, null);
						}
						extractZipFile(srcZip.getLocation().toFile(), dest.getFullPath(), monitor);
						if (isJARd(model)) {
							extractJavaResources(jarFile.getLocation().toFile(), dest, monitor);
						} else {
							extractResources(jarFile.getLocation().toFile(), dest, monitor);
						}
						srcZip.delete(true, null);
						jarFile.delete(true, null);
					}
				} else if (name.equals(".") && project.getFolder("src").exists()) { //$NON-NLS-1$ //$NON-NLS-2$
					addBuildEntry(buildModel, "source..", "src/"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}	
		}
		configureBinIncludes(buildModel, model);
		buildModel.save();
	}
	
	private void configureBinIncludes(WorkspaceBuildModel buildModel, IPluginModelBase model) throws CoreException {
		IBuildEntry entry = buildModel.getBuild(true).getEntry("bin.includes"); //$NON-NLS-1$
		if (entry == null) {
			entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			File location = new File(model.getInstallLocation());
			if (location.isDirectory()) {
				File[] files = location.listFiles();
				for (int i = 0; i < files.length; i++) {
					String token = files[i].getName();
					if (files[i].isDirectory())
						token = token + "/"; //$NON-NLS-1$
					entry.addToken(token);
				}
			} else {
				String[] tokens = getTopLevelResources(location);
				for (int i = 0; i < tokens.length; i++) {
					entry.addToken(tokens[i]);
				}
			}
			buildModel.getBuild().add(entry);
		}
	}
	
	private String addBuildEntry(WorkspaceBuildModel model, String key, String value) throws CoreException {
		IBuild build = model.getBuild(true);
		IBuildEntry entry = build.getEntry(key);
		if (entry == null) {
			entry = model.getFactory().createEntry(key);
			entry.addToken(value);
			build.add(entry);
		}
		String[] tokens = entry.getTokens();
		return (tokens.length > 0) ? tokens[0] : "src/"; //$NON-NLS-1$
	}

	private void linkSourceArchives(IProject project, IPluginModelBase model,
			IProgressMonitor monitor) throws CoreException {

		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource,
				libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
				if (srcPath != null) {
					if ("src.zip".equals(zipName) && isJARd(model)) { //$NON-NLS-1$
						path = new Path(ClasspathUtilCore.getSourceZipName(new File(model
								.getInstallLocation()).getName()));
					}
					IFile zipFile = project.getFile(path.lastSegment());
					if (!zipFile.exists()) {
						zipFile.createLink(
								srcPath,
								IResource.NONE,
								new SubProgressMonitor(monitor, 1));
					}
				}
			}
			monitor.worked(1);
		}
		monitor.done();
	}

	private void importSourceArchives(IProject project, IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		
		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource, libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
				if (srcPath != null) {
					if ("src.zip".equals(zipName) && isJARd(model)) { //$NON-NLS-1$
						path = new Path(ClasspathUtilCore.getSourceZipName(new File(model.getInstallLocation()).getName()));
					}
					importArchive(project, new File(srcPath.toOSString()), path);						
				}
			}
			monitor.worked(1);
		}
		monitor.done();
	}
	
	private String[] getLibraryNames(IPluginModelBase model, boolean expand) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		ArrayList list = new ArrayList();
		for (int i = 0; i < libraries.length; i++) {
			if (expand)
				list.add(ClasspathUtilCore.expandLibraryName(libraries[i].getName()));
			else
				list.add(libraries[i].getName());
		}
		if (libraries.length == 0 && isJARd(model))
			list.add("."); //$NON-NLS-1$
		return (String[])list.toArray(new String[list.size()]);
	}
	
	private void extractJARdPlugin(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(model.getInstallLocation());
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			if (!containsCode(provider)) {
				extractZipFile(new File(model.getInstallLocation()), project.getFullPath(), monitor);
				return;
			}
			ArrayList collected = new ArrayList();
			collectNonJavaResources(provider, provider.getRoot(), collected);
			importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);

			File file = new File(model.getInstallLocation());
			if (hasEmbeddedSource(provider) && fImportType == IMPORT_WITH_SOURCE) {
				collected = new ArrayList();
				collectJavaFiles(provider, provider.getRoot(), collected);
				importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
				collected = new ArrayList();
				collectJavaResources(provider, provider.getRoot(), collected);
				importContent(provider.getRoot(), project.getFullPath().append("src"), provider, collected, monitor); //$NON-NLS-1$
			} else {
				if (fImportType == IMPORT_BINARY_WITH_LINKS) {
					 project.getFile(file.getName()).createLink(
						new Path(file.getAbsolutePath()),
						IResource.NONE,
					 	null);
				} else {
					importArchive(project, file, new Path(file.getName()));				
				}
				if (!hasEmbeddedSource(provider)) {
					if (fImportType == IMPORT_BINARY_WITH_LINKS) {
						linkSourceArchives(
								project, 
								model, 
								new SubProgressMonitor(monitor, 1));
					} else {
						importSourceArchives(
								project,
								model,
								new SubProgressMonitor(monitor, 1));
					}
				}
			}
			if (fImportType != IMPORT_WITH_SOURCE) {
				PDEPluginConverter.modifyBundleClasspathHeader(project, model);
			}
			setPermissions(model, project);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
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



	private IProject findProject(String id) {
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getWorkspaceModel();
			if (model != null)
				return model.getUnderlyingResource().getProject();
		}
		return PDEPlugin.getWorkspace().getRoot().getProject(id);
	}

	private boolean queryReplace(IProject project) throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(
				NLS.bind(PDEUIMessages.ImportWizard_messages_exists, project.getName()))) {
			case IImportQuery.CANCEL:
				throw new OperationCanceledException();
			case IImportQuery.NO:
				return false;
		}
		return true;
	}
	
	private boolean queryExecutionEnvironment(String message) throws OperationCanceledException {
		switch (fExecutionQuery.doQuery(message)) {
			case IImportQuery.CANCEL:
				throw new OperationCanceledException();
			case IImportQuery.NO:
				return false;
		}
		return true;
	}
	
	private void setProjectDescription(IProject project, IPluginModelBase model)
			throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature(project, model))
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID, PDE.PLUGIN_NATURE });
		else
			desc.setNatureIds(new String[] { PDE.PLUGIN_NATURE });
		project.setDescription(desc, null);
	}

	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		if (model.getPluginBase().getLibraries().length > 0)
			return true;
		
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			if (desc.getExportPackages().length > 0)
				return true;
			if (desc.getRequiredBundles().length > 0)
				return true;
			if (desc.getImportPackages().length > 0)
				return true;
		}		
		return false;
	}
	
	private boolean isExempt(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		if ("org.apache.ant".equals(id) //$NON-NLS-1$
			|| "org.eclipse.osgi.util".equals(id) //$NON-NLS-1$
			|| "org.eclipse.osgi.services".equals(id)) //$NON-NLS-1$
			return true;
		
		if ("org.eclipse.swt".equals(id) && !isJARd(model)) //$NON-NLS-1$
			return true;
		return false;
	}

	private boolean isJARd(IPluginModelBase model) {
		return new File(model.getInstallLocation()).isFile();
	}
	
	private void setPermissions(IPluginModelBase model, IProject project) {
		try {
			if (!Platform.getOS().equals(Constants.OS_WIN32) && model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel)model).getFragment();
				if ("org.eclipse.swt".equals(fragment.getPluginId())) { //$NON-NLS-1$
					IResource[] children = project.members();
					for (int i = 0; i < children.length; i++) {
						if (children[i] instanceof IFile && isInterestingResource(children[i].getName())) {
							Runtime.getRuntime().exec(new String[] {"chmod", "755", children[i].getLocation().toOSString()}).waitFor();						 //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}			
			}
		} catch (CoreException e) {
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}
	}
	
	private boolean isInterestingResource(String name) {
		return name.endsWith(".jnilib") //$NON-NLS-1$
		|| name.endsWith(".sl") //$NON-NLS-1$
		|| name.endsWith(".a") //$NON-NLS-1$
		|| name.indexOf(".so") != -1; //$NON-NLS-1$
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
		File jar =
			new File(fragment.getModel().getInstallLocation(), jarPath.toString());
		if (jar.exists()) {
			importArchive(project, jar, jarPath);
		}
	}
	
	private void importSourceFromFragment(IProject project, IFragment fragment, String name)
		throws CoreException {
		IPath jarPath = new Path(ClasspathUtilCore.expandLibraryName(name));
		IPath srcPath = new Path(ClasspathUtilCore.getSourceZipName(jarPath.toString()));
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		File srcFile = manager.findSourceFile(fragment, srcPath);
		if (srcFile != null) {
			importArchive(project, srcFile, srcPath);
		}
	}
	


	
}
