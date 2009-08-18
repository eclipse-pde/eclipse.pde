/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.BundleException;

/**
 * Imports one or more plug-ins into the workspace.  There are three different
 * ways to import a plugin: as binary, as binary with linked source,
 * and as source. 
 */
public class PluginImportOperation extends WorkspaceJob {

	public static final int IMPORT_BINARY = 1;
	public static final int IMPORT_BINARY_WITH_LINKS = 2;
	public static final int IMPORT_WITH_SOURCE = 3;

	private static final String DEFAULT_SOURCE_DIR = "src"; //$NON-NLS-1$
	private static final String DEFAULT_LIBRARY_NAME = "."; //$NON-NLS-1$

	private IPluginModelBase[] fModels;
	private int fImportType;
	private ImportQuery fReplaceQuery;
	private ImportQuery fExecutionQuery;
	private Hashtable fProjectClasspaths = new Hashtable();
	private boolean fForceAutobuild;

	/**
	 * Used to find source locations when not found in default locations.
	 * Possibly <code>null</code>
	 */
	private SourceLocationManager fAlternateSource;

	private boolean fPluginsAreInUse = false;

	/**
	 * Constructor
	 * @param models models of plug-ins to import
	 * @param importType one of three types specified by constants, binary, binary with links, source
	 * @param replaceQuery defines what to do if the project already exists in the workspace
	 * @param executionQuery defines what to do if the project requires an unsupported execution environment
	 * @param forceAutobuild whether to force a build after the import
	 */
	public PluginImportOperation(IPluginModelBase[] models, int importType, boolean forceAutobuild) {
		super(PDEUIMessages.ImportWizard_title);
		fModels = models;
		fImportType = importType;
		fReplaceQuery = new ImportQuery();
		fExecutionQuery = new ImportQuery();
		fForceAutobuild = forceAutobuild;
	}

	/**
	 * Sets whether some of the plug-ins being imported are currently in use by a launched
	 * Eclipse instance.  Setting this to true will force an additional check before deleting
	 * a plug-in.
	 * @param pluginsInUse
	 */
	public void setPluginsInUse(boolean pluginsInUse) {
		fPluginsAreInUse = pluginsInUse;
	}

	/**
	 * Sets a source location manager to use to find source attachments. This should
	 * be specified when importing plug-ins that are not from the active target platform
	 * so source attachments can be found.
	 * 
	 * @param alternate source location manager.
	 */
	public void setAlternateSource(SourceLocationManager alternate) {
		fAlternateSource = alternate;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(PDEUIMessages.ImportWizard_operation_creating, fModels.length + 1);
			MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(), IStatus.OK, PDEUIMessages.ImportWizard_operation_multiProblem, null);

			for (int i = 0; i < fModels.length; i++) {
				monitor.setTaskName(NLS.bind(PDEUIMessages.PluginImportOperation_Importing_plugin, fModels[i].getPluginBase().getId()));
				try {
					importPlugin(fModels[i], fImportType, new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					try {
						setClasspaths(new SubProgressMonitor(monitor, 1));
					} catch (JavaModelException e) {
						/* Do nothing as we are already cancelled */
					}
					return Status.CANCEL_STATUS;
				}
			}
			monitor.setTaskName(PDEUIMessages.PluginImportOperation_Set_up_classpaths);
			try {
				setClasspaths(new SubProgressMonitor(monitor, 1));
			} catch (JavaModelException e) {
				multiStatus.merge(e.getStatus());
			}
			if (!ResourcesPlugin.getWorkspace().isAutoBuilding() && fForceAutobuild)
				runBuildJob();
			return multiStatus;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the raw classpath of projects that need to be updated
	 * @param monitor
	 * @throws JavaModelException if a classpath could not be set
	 */
	private void setClasspaths(IProgressMonitor monitor) throws JavaModelException {
		try {
			monitor.beginTask("", fProjectClasspaths.size()); //$NON-NLS-1$
			Enumeration keys = fProjectClasspaths.keys();
			while (keys.hasMoreElements()) {
				IProject project = (IProject) keys.nextElement();
				IClasspathEntry[] classpath = (IClasspathEntry[]) fProjectClasspaths.get(project);
				monitor.subTask(project.getName());
				JavaCore.create(project).setRawClasspath(classpath, new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * This method starts the import of a specific plugin.  Checks if the execution
	 * environment is supported and also checks if the project already exists and 
	 * needs to be replaced.
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @throws CoreException if a problem occurs while importing a plugin
	 */
	private void importPlugin(IPluginModelBase model, int importType, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 5); //$NON-NLS-1$

			// Test is the required execution environment is supported
			if (!testExecutionEnvironment(model)) {
				return;
			}

			// Create the project or ask to overwrite if project exists
			IProject project = createProject(model, new SubProgressMonitor(monitor, 1));
			if (project == null) {
				return;
			}

			// Target Weaving: if we are importing plug-ins in the runtime workbench from the host workbench, import everything as-is and return
			if (Platform.inDevelopmentMode()) {
				File location = new File(model.getInstallLocation());
				if (location.isDirectory()) {
					File classpathFile = new File(location, ".classpath"); //$NON-NLS-1$
					File projectFile = new File(location, ".project"); //$NON-NLS-1$
					if (classpathFile.exists() && classpathFile.isFile() && projectFile.exists() && projectFile.isFile()) {
						PluginImportHelper.importContent(location, project.getFullPath(), FileSystemStructureProvider.INSTANCE, null, new SubProgressMonitor(monitor, 4));
						return;
					}
				}
			}

			// Perform the import
			Map sourceMap = null;
			if (importType == IMPORT_BINARY || isExempt(model, importType) || (importType == IMPORT_WITH_SOURCE && !canFindSource(model))) {
				sourceMap = importAsBinary(project, model, new SubProgressMonitor(monitor, 4));
			} else if (importType == IMPORT_BINARY_WITH_LINKS) {
				sourceMap = importAsBinaryWithLinks(project, model, new SubProgressMonitor(monitor, 4));
			} else if (importType == IMPORT_WITH_SOURCE) {
				importAsSource(project, model, new SubProgressMonitor(monitor, 4));
			}

			setProjectNatures(project, model);

			// Set the classpath
			if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
				fProjectClasspaths.put(project, ClasspathComputer.getClasspath(project, model, sourceMap, true, false));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Imports the contents of the plugin and imports source files as binary files that will not be compiled.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @return a mapping of libraries to source locations to use in the classpath
	 * @throws CoreException if there is a problem completing the import
	 */
	private Map importAsBinary(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$

			// Import the plug-in content
			File srcFile = new File(model.getInstallLocation());
			if (isJARd(model)) {
				PluginImportHelper.copyArchive(srcFile, project.getFile(srcFile.getName()), new SubProgressMonitor(monitor, 1));
			} else {
				PluginImportHelper.importContent(new File(model.getInstallLocation()), project.getFullPath(), FileSystemStructureProvider.INSTANCE, null, new SubProgressMonitor(monitor, 1));
			}

			// Import source from known source locations
			Map sourceMap = importSourceArchives(project, model, IMPORT_BINARY, new SubProgressMonitor(monitor, 1));

			// Import additional source files such as schema files for easy access, see bug 139161
			importAdditionalSourceFiles(project, model, new SubProgressMonitor(monitor, 1));

			// Extract the required bundle files and modify the imported manifest to have the correct classpath
			importRequiredPluginFiles(project, model, new SubProgressMonitor(monitor, 1));
			modifyBundleClasspathHeader(project, model);

			// Mark the project as binary
			RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
			project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);

			return sourceMap;

		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates links to remote plugin and source locations and sets up the project
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @return mapping of library name to path to source library (relative to project)
	 * @throws CoreException if there is a problem completing the import
	 */
	private Map importAsBinaryWithLinks(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 3); //$NON-NLS-1$

			// Link the plug-in content
			File srcFile = new File(model.getInstallLocation());
			if (srcFile.isFile()) {
				IFile dstFile = project.getFile(new Path(srcFile.getName()));
				dstFile.createLink(srcFile.toURI(), IResource.NONE, new SubProgressMonitor(monitor, 1));
			} else {
				IFolder dstFile = project.getFolder(new Path(srcFile.getName()));
				dstFile.createLink(srcFile.toURI(), IResource.NONE, new SubProgressMonitor(monitor, 1));
			}

			// Link source from known source locations
			Map sourceMap = importSourceArchives(project, model, IMPORT_BINARY_WITH_LINKS, new SubProgressMonitor(monitor, 1));

			// Import additional source files such as schema files for easy access, see bug 139161
			importAdditionalSourceFiles(project, model, new SubProgressMonitor(monitor, 1));

			// Extract the required bundle files and modify the imported manifest to have the correct classpath
			importRequiredPluginFiles(project, model, new SubProgressMonitor(monitor, 1));
			modifyBundleClasspathHeader(project, model);

			// Mark the project as binary
			RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
			project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);

			return sourceMap;

		} finally {
			monitor.done();
		}
	}

	/**
	 * Imports the contents of the plugin and imports source files to source folders that will be compiled.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void importAsSource(IProject project, IPluginModelBase model, SubProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$

			// Extract the source, track build entries and package locations
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(project.getFile("build.properties")); //$NON-NLS-1$
			Map packageLocations = new HashMap(); // maps package path to a src folder 
			extractSourceFolders(project, model, buildModel, packageLocations, new SubProgressMonitor(monitor, 1));

			// Extract additional non-java files from the source bundles
			importAdditionalSourceFiles(project, model, new SubProgressMonitor(monitor, 1));

			// Extract the binary plug-in (for non-class files)
			// Use the package locations map to put files that belong in the package directory structure into the proper source directory
			if (isJARd(model)) {
				Map collected = new HashMap();
				ZipFileStructureProvider provider = new ZipFileStructureProvider(new ZipFile(new File(model.getInstallLocation())));
				PluginImportHelper.collectBinaryFiles(provider, provider.getRoot(), packageLocations, collected);
				for (Iterator iterator = collected.keySet().iterator(); iterator.hasNext();) {
					IPath currentDestination = (IPath) iterator.next();
					IPath destination = project.getFullPath();
					destination = destination.append(currentDestination);
					PluginImportHelper.importContent(provider.getRoot(), destination, provider, (List) collected.get(currentDestination), new NullProgressMonitor());
				}
				monitor.worked(1);
			} else {
				Map collected = new HashMap();
				File srcFile = new File(model.getInstallLocation());
				PluginImportHelper.collectBinaryFiles(FileSystemStructureProvider.INSTANCE, srcFile, packageLocations, collected);
				for (Iterator iterator = collected.keySet().iterator(); iterator.hasNext();) {
					IPath currentDestination = (IPath) iterator.next();
					IPath destination = project.getFullPath();
					destination = destination.append(currentDestination);
					PluginImportHelper.importContent(srcFile, destination, FileSystemStructureProvider.INSTANCE, (List) collected.get(currentDestination), new NullProgressMonitor());
				}
				monitor.worked(1);

			}

			// Create the build.properties file
			configureBinIncludes(buildModel, model, project);
			buildModel.save();
			monitor.worked(1);

		} catch (ZipException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			monitor.done();
		}

	}

	/**
	 * Creates the project to add to the workspace.  If the project already exists in 
	 * the workspace ask the user if it is ok to overwrite.  Will return <code>null</code>
	 * if no project could be created for the import (i.e. the user chooses to not overwrite).
	 * 
	 * @param model plug-in being imported
	 * @param monitor progress monitor
	 * @return the project to use or <code>null</code> if no project could be created/overwritten
	 * @throws TeamException if an existing project is shared and an error occurs disconnecting it
	 * @throws CoreException if an error occurs when working with the project
	 */
	private IProject createProject(IPluginModelBase model, IProgressMonitor monitor) throws TeamException, CoreException {
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			IProject project = findProject(model.getPluginBase().getId());
			if (project.exists() || new File(project.getParent().getLocation().toFile(), project.getName()).exists()) {
				// Query the user to see if we should overwrite
				switch (fReplaceQuery.doQuery(NLS.bind(PDEUIMessages.ImportWizard_messages_exists, project.getName()))) {
					case IDialogConstants.CANCEL_ID :
						throw new OperationCanceledException();
					case IDialogConstants.NO_ID :
						return null;
				}
				if (RepositoryProvider.isShared(project))
					RepositoryProvider.unmap(project);
				if (!project.exists())
					project.create(new SubProgressMonitor(monitor, 1));
				if (!safeDeleteCheck(project, monitor)) {
					throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), NLS.bind(PDEUIMessages.PluginImportOperation_could_not_delete_project, project.getName())));
				}
				project.delete(true, true, monitor);
			}

			project.create(monitor);
			if (!project.isOpen())
				project.open(monitor);
			monitor.worked(1);

			return project;

		} finally {
			monitor.done();
		}
	}

	private IProject findProject(String id) {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null)
				return resource.getProject();
		}
		return PDEPlugin.getWorkspace().getRoot().getProject(id);
	}

	/**
	 * Returns true if it is safe to delete the project.  It is not safe to delete if
	 * one of its libraries is locked by a running launch configuration.
	 * 
	 * @param project project to test
	 * @param monitor progress monitor
	 * @return true is it is safe to delete the project, false otherwise
	 */
	private boolean safeDeleteCheck(IProject project, IProgressMonitor monitor) {
		if (!fPluginsAreInUse)
			return true;
		IPluginModelBase base = PluginRegistry.findModel(project);
		if (base != null) {
			IPluginLibrary[] libraries = base.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				IResource res = project.findMember(libraries[i].getName());
				if (res != null)
					try {
						if (!(ResourcesPlugin.getWorkspace().delete(new IResource[] {res}, true, monitor).isOK()))
							return false;
					} catch (CoreException e) {
						return false;
					}
			}
		}
		return true;
	}

	/**
	 * Checks if we have a source location for the given model
	 * @param model model to lookup source for
	 * @return true if source was found for at least one library, false otherwise
	 */
	private boolean canFindSource(IPluginModelBase model) {
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		if (manager.hasBundleManifestLocation(model.getPluginBase())) {
			return true;
		}
		String[] libraries = getLibraryNames(model);
		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath srcPath = manager.findSourcePath(model.getPluginBase(), new Path(zipName));
			if (srcPath != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tests whether the required execution environment of the given plugin is supported by
	 * the current known JREs.  If not, ask the user whether to continue.
	 * @param model the plug-in model to test
	 * @return true is the import should continue, false if the plug-in should be skipped
	 * @throws OperationCanceledException if the user chooses to cancel the operation
	 */
	private boolean testExecutionEnvironment(IPluginModelBase model) throws OperationCanceledException {
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
				switch (fExecutionQuery.doQuery(NLS.bind(PDEUIMessages.PluginImportOperation_executionEnvironment, model.getPluginBase().getId(), envs[0]))) {
					case IDialogConstants.CANCEL_ID :
						throw new OperationCanceledException();
					case IDialogConstants.NO_ID :
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns true if the given plugin must be imported as
	 * binary instead of the setting defined by fImportType
	 * @param model
	 * @return true is the plugin must be imported as binary, false otherwise
	 */
	private boolean isExempt(IPluginModelBase model, int importType) {
		String id = model.getPluginBase().getId();
		if (importType == IMPORT_WITH_SOURCE) {
			if ("org.apache.ant".equals(id) //$NON-NLS-1$
					|| "org.eclipse.osgi.util".equals(id) //$NON-NLS-1$
					|| "org.eclipse.osgi.services".equals(id) //$NON-NLS-1$
					|| "org.eclipse.core.runtime.compatibility.registry".equals(id)) { //$NON-NLS-1$
				return true;
			}
		}

		if ("org.eclipse.swt".equals(id) && !isJARd(model)) //$NON-NLS-1$
			return true;
		return false;
	}

	/**
	 * Starts a job that will build the workspace
	 */
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

	/**
	 * Imports the source archives required by the project either by copying or linking, based on the mode
	 * constant that is passed to the method (IMPORT_BINARY or IMPORT_BINARY_WITH_LINKS).
	 * @param project project destination
	 * @param model model we are importing
	 * @param mode either IMPORT_BINARY (copies source) or MPORT_BINARY_WITH_LINKS (links source)
	 * @param monitor progress monitor
	 * @return mapping of library name to the source location
	 * @throws CoreException if there are problems importing an archive
	 */
	private Map importSourceArchives(IProject project, IPluginModelBase model, int mode, IProgressMonitor monitor) throws CoreException {
		String[] libraries = getLibraryNames(model);
		try {
			monitor.beginTask(PDEUIMessages.ImportWizard_operation_importingSource, libraries.length);

			Map sourceMap = new HashMap(libraries.length);
			SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
			for (int i = 0; i < libraries.length; i++) {
				String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
				IPluginBase pluginBase = model.getPluginBase();
				// check default locations
				IPath srcPath = manager.findSourcePath(pluginBase, new Path(zipName));
				// check alternate locations
				if (srcPath == null && fAlternateSource != null) {
					srcPath = fAlternateSource.findSourcePath(pluginBase, new Path(zipName));
				}
				if (srcPath != null) {
					zipName = srcPath.lastSegment();
					IPath dstPath = new Path(zipName);
					sourceMap.put(libraries[i], dstPath);
					if (project.findMember(dstPath) == null) {
						if (mode == IMPORT_BINARY) {
							PluginImportHelper.copyArchive(new File(srcPath.toOSString()), project.getFile(dstPath), new SubProgressMonitor(monitor, 1));
						} else if (mode == IMPORT_BINARY_WITH_LINKS) {
							IFile dstFile = project.getFile(dstPath);
							dstFile.createLink(srcPath, IResource.NONE, new SubProgressMonitor(monitor, 1));
						}
					}
				}
			}
			return sourceMap;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Looks up the source locations for the plug-in and imports the source for each library.  Each source root is
	 * extracted to a source folder in the project and a build model containing the source entries is returned.
	 * 
	 * @param project destination project
	 * @param model plug-in being imported
	 * @param buildModel a workspace build model that entries for each created source folder will be added to
	 * @param packageLocations map that will be updated with package locations (package path to a source foldeR) 
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem extracting the source or creating a build entry
	 */
	private void extractSourceFolders(IProject project, IPluginModelBase model, WorkspaceBuildModel buildModel, Map packageLocations, IProgressMonitor monitor) throws CoreException {
		try {
			String[] libraries = getLibraryNames(model);
			monitor.beginTask(PDEUIMessages.ImportWizard_operation_importingSource, libraries.length);

			SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();

			// Need to do different things based on whether we have a individual source bundle or the old style separated zips
			if (manager.hasBundleManifestLocation(model.getPluginBase())) {
				File srcFile = manager.findSourcePlugin(model.getPluginBase());
				Set sourceRoots = manager.findSourceRoots(model.getPluginBase());
				for (int i = 0; i < libraries.length; i++) {
					if (libraries[i].equals(DEFAULT_LIBRARY_NAME)) {
						// Need to pull out any java source that is not in another source root
						IResource destination = project.getFolder(DEFAULT_SOURCE_DIR);
						if (!destination.exists()) {
							List excludeFolders = new ArrayList(sourceRoots.size());
							for (Iterator iterator = sourceRoots.iterator(); iterator.hasNext();) {
								String root = (String) iterator.next();
								if (!root.equals(DEFAULT_LIBRARY_NAME)) {
									excludeFolders.add(new Path(root));
								}
							}
							Set collectedPackages = new HashSet();
							PluginImportHelper.extractJavaSourceFromArchive(srcFile, excludeFolders, destination.getFullPath(), collectedPackages, new SubProgressMonitor(monitor, 1));
							addBuildEntry(buildModel, "source." + DEFAULT_LIBRARY_NAME, DEFAULT_SOURCE_DIR + "/"); //$NON-NLS-1$ //$NON-NLS-2$
							addPackageEntries(collectedPackages, new Path(DEFAULT_SOURCE_DIR), packageLocations);

						}
					} else if (sourceRoots.contains(getSourceDirName(libraries[i]))) {
						IPath sourceDir = new Path(getSourceDirName(libraries[i]));
						if (!project.getFolder(sourceDir).exists()) {
							Set collectedPackages = new HashSet();
							PluginImportHelper.extractFolderFromArchive(srcFile, sourceDir, project.getFullPath(), collectedPackages, new SubProgressMonitor(monitor, 1));
							addBuildEntry(buildModel, "source." + libraries[i], sourceDir.toString()); //$NON-NLS-1$
							addPackageEntries(collectedPackages, sourceDir, packageLocations);
						}
					}
				}
			} else {
				// Old style, zips in folders, determine the source zip name/location and extract it to the project
				for (int i = 0; i < libraries.length; i++) {
					String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
					IPath srcPath = manager.findSourcePath(model.getPluginBase(), new Path(zipName));
					if (srcPath != null) {
						IPath dstPath = new Path(getSourceDirName(libraries[i]));
						IResource destination = project.getFolder(dstPath);
						if (!destination.exists()) {
							Set collectedPackages = new HashSet();
							PluginImportHelper.extractArchive(new File(srcPath.toOSString()), destination.getFullPath(), collectedPackages, new SubProgressMonitor(monitor, 1));
							addBuildEntry(buildModel, "source." + libraries[i], dstPath.toString()); //$NON-NLS-1$
							addPackageEntries(collectedPackages, dstPath, packageLocations);
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adds a set of packages to the package location map.  For each package in the given set
	 * an entry will be added to the map pointing to the destination.  In additional, any parent 
	 * package fragments that do not have entries in the map will be added (also pointing to the
	 * destination path).
	 *  
	 * @param packages set of packages to add to the map
	 * @param destination the destination directory that the packages belong to
	 * @param packageLocations the map to add the entries to
	 */
	private void addPackageEntries(Set packages, IPath destination, Map packageLocations) {
		for (Iterator iterator = packages.iterator(); iterator.hasNext();) {
			IPath currentPackage = (IPath) iterator.next();
			packageLocations.put(currentPackage, destination);

			// Add package fragment locations
			while (currentPackage.segmentCount() > 1) {
				currentPackage = currentPackage.removeLastSegments(1);
				if (packageLocations.containsKey(currentPackage)) {
					break; // Don't overwrite existing entries, we assume that if one parent has an entry, all further parents already have an entry
				}
				packageLocations.put(currentPackage, destination);
			}
		}
	}

	/**
	 * Extracts any additional files and folders that exist in the source location
	 * @param project the destination project
	 * @param model the plugin being imported
	 * @param monitor progress monitor
	 * @throws CoreException is there is a problem importing the files
	 */
	private void importAdditionalSourceFiles(IProject project, IPluginModelBase model, SubProgressMonitor monitor) throws CoreException {
		File sourceLocation = PDECore.getDefault().getSourceLocationManager().findSourcePlugin(model.getPluginBase());
		if (sourceLocation != null) {
			if (sourceLocation.isFile()) {
				ArrayList collected = new ArrayList();
				ZipFileStructureProvider provider = null;
				try {
					provider = new ZipFileStructureProvider(new ZipFile(sourceLocation));
				} catch (IOException e) {
					IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
					throw new CoreException(status);
				}
				PluginImportHelper.collectNonJavaNonBuildFiles(provider, provider.getRoot(), collected);
				PluginImportHelper.importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
			} else {
				ArrayList collected = new ArrayList();
				PluginImportHelper.collectNonJavaNonBuildFiles(FileSystemStructureProvider.INSTANCE, sourceLocation, collected);
				PluginImportHelper.importContent(sourceLocation, project.getFullPath(), FileSystemStructureProvider.INSTANCE, collected, monitor);
			}
		}
	}

	/**
	 * Imports files from the plug-in that are necessary to make the created project a plug-in project.
	 * Specifically the manifest and related file are extracted.  
	 * @param project
	 * @param model
	 * @param monitor
	 * @throws CoreException if there is a problem importing the content
	 */
	private void importRequiredPluginFiles(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		if (isJARd(model)) {
			ArrayList collected = new ArrayList();
			ZipFileStructureProvider provider = null;
			try {
				provider = new ZipFileStructureProvider(new ZipFile(new File(model.getInstallLocation())));
			} catch (IOException e) {
				IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
				throw new CoreException(status);
			}
			PluginImportHelper.collectRequiredBundleFiles(provider, provider.getRoot(), collected);
			PluginImportHelper.importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
		} else {
			ArrayList collected = new ArrayList();
			File file = new File(model.getInstallLocation());
			PluginImportHelper.collectRequiredBundleFiles(FileSystemStructureProvider.INSTANCE, file, collected);
			PluginImportHelper.importContent(file, project.getFullPath(), FileSystemStructureProvider.INSTANCE, collected, monitor);
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

	private void configureBinIncludes(WorkspaceBuildModel buildModel, IPluginModelBase model, IProject project) throws CoreException {
		IBuild build = buildModel.getBuild(true);
		IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
		HashMap libraryDirs = getSourceDirectories(build);
		if (entry == null) {
			entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			File location = new File(model.getInstallLocation());
			if (location.isDirectory()) {
				File[] files = location.listFiles();
				for (int i = 0; i < files.length; i++) {
					String token = files[i].getName();
					if ((project.findMember(token) == null) && (build.getEntry(IBuildEntry.JAR_PREFIX + token) == null))
						continue;
					if (files[i].isDirectory()) {
						token = token + "/"; //$NON-NLS-1$
						if (libraryDirs.containsKey(token))
							token = libraryDirs.get(token).toString();
					}
					entry.addToken(token);
				}
			} else {
				String[] tokens = PluginImportHelper.getTopLevelResources(location);
				for (int i = 0; i < tokens.length; i++) {
					IResource res = project.findMember(tokens[i]);
					if ((res == null) && (build.getEntry(IBuildEntry.JAR_PREFIX + tokens[i]) == null))
						continue;
					if ((res instanceof IFolder) && (libraryDirs.containsKey(tokens[i])))
						continue;
					entry.addToken(tokens[i]);
				}
			}
			buildModel.getBuild().add(entry);
		}
	}

	private HashMap getSourceDirectories(IBuild build) {
		HashMap set = new HashMap();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			String name = entries[i].getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
				name = name.substring(7);
				String[] tokens = entries[i].getTokens();
				for (int j = 0; j < tokens.length; j++) {
					set.put(tokens[j], name);
				}
			}
		}
		return set;
	}

	/**
	 * Creates a model for an existing manifest file, replacing the classpath entry with the
	 * new location of the referenced library.  Also removes any extra entries such as signing
	 * headers. 
	 * @param project
	 * @param base
	 */
	private void modifyBundleClasspathHeader(IProject project, IPluginModelBase base) {
		IFile file = project.getFile(JarFile.MANIFEST_NAME);
		if (file.exists()) {
			WorkspaceBundleModel bmodel = new WorkspaceBundleModel(file);
			IBundle bundle = bmodel.getBundle();
			String classpath = bundle.getHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH);
			if (classpath == null) {
				bundle.setHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, ClasspathUtilCore.getFilename(base));
			} else {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, classpath);
					StringBuffer buffer = new StringBuffer();
					for (int i = 0; i < elements.length; i++) {
						if (buffer.length() > 0) {
							buffer.append(","); //$NON-NLS-1$
							buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
							buffer.append(" "); //$NON-NLS-1$
						}
						if (elements[i].getValue().equals(".")) //$NON-NLS-1$
							buffer.append(ClasspathUtilCore.getFilename(base));
						else
							buffer.append(elements[i].getValue());
					}
					bundle.setHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, buffer.toString());
				} catch (BundleException e) {
				}
			}
			bmodel.save();
		}
	}

	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		if (model.getPluginBase().getLibraries().length > 0)
			return true;

		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			if (desc.getExecutionEnvironments().length > 0)
				return true;
			if (desc.getExportPackages().length > 0)
				return true;
		}
		return false;
	}

	private void setProjectNatures(IProject project, IPluginModelBase model) throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (!desc.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
		if (needsJavaNature(project, model) && !desc.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
	}

	/**
	 * Gets the list of libraries from the model and returns an array of their expanded
	 * names.  Will add the default library name if no libraries are specified.
	 * @param model
	 * @return list of library names
	 */
	private String[] getLibraryNames(IPluginModelBase model) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		ArrayList list = new ArrayList();
		for (int i = 0; i < libraries.length; i++) {
			list.add(ClasspathUtilCore.expandLibraryName(libraries[i].getName()));
		}
		if (libraries.length == 0 && isJARd(model))
			list.add(DEFAULT_LIBRARY_NAME);
		return (String[]) list.toArray(new String[list.size()]);
	}

	/**
	 * Returns the standard source directory name for a library name.  
	 * Used to get the source root name for a library as well as the
	 * standard destination name.
	 * @param libraryName
	 * @return source dir name
	 */
	private String getSourceDirName(String libraryName) {
		int dot = libraryName.lastIndexOf('.');
		return (dot != -1) ? libraryName.substring(0, dot) + DEFAULT_SOURCE_DIR : libraryName;
	}

	/**
	 * Returns whether the install location of the plug-in is a jar file or a folder
	 * @param model
	 * @return true if the install location is a jar, false if it is a folder
	 */
	private boolean isJARd(IPluginModelBase model) {
		return new File(model.getInstallLocation()).isFile();
	}

	private static class ImportQuery {
		// If 0, ask the user in a dialog, if -1 users has chosen no to all, if +1 user has chosen yes to all
		private int fCreateDialog = 0;

		/**
		 * @return returns one of IDialogConstants.YES_ID, IDialogConstants.NO_ID, IDialogConstants.CANCEL_ID
		 */
		public int doQuery(final String message) {
			if (fCreateDialog > 0) {
				return IDialogConstants.YES_ID;
			}
			if (fCreateDialog < 0) {
				return IDialogConstants.NO_ID;
			}

			final int[] result = {IDialogConstants.CANCEL_ID};

			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), PDEUIMessages.ImportWizard_messages_title, null, message, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
					result[0] = dialog.open();
				}
			});

			switch (result[0]) {
				case 0 :
					return IDialogConstants.YES_ID;
				case 1 :
					fCreateDialog = +1;
					return IDialogConstants.YES_ID;
				case 2 :
					return IDialogConstants.NO_ID;
				case 3 :
					fCreateDialog = -1;
					return IDialogConstants.NO_ID;
				default :
					return IDialogConstants.CANCEL_ID;
			}
		}
	}

}
