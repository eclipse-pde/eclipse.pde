/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
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
import org.eclipse.pde.internal.ui.wizards.plugin.ClasspathComputer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.BundleException;

/**
 * Imports one or more plugins into the workspace.  There are three different
 * ways to import a plugin: as binary, as binary with linked source,
 * and as source. 
 */
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

	private boolean fLaunchedConfigurations = false;
	private ArrayList fAffectedPlugins;

	public interface IImportQuery {
		public static final int CANCEL = 0;
		public static final int NO = 1;
		public static final int YES = 2;

		int doQuery(String message);
	}

	/**
	 * Constructor
	 * @param models models of plugins to import
	 * @param importType one of three types specified by constants, binary, binary with links, source
	 * @param replaceQuery defines what to do if the project already exists in the workspace
	 * @param executionQuery defines what to do if the project requires an unsupported execution environment
	 */
	public PluginImportOperation(IPluginModelBase[] models, int importType, IImportQuery replaceQuery, IImportQuery executionQuery) {
		fModels = models;
		fImportType = importType;
		fReplaceQuery = replaceQuery;
		fExecutionQuery = executionQuery;
		fAffectedPlugins = new ArrayList(models.length);
	}

	/**
	 * Constructor
	 * @param models models of plugins to import
	 * @param importType one of three types specified by constants, binary, binary with links, source
	 * @param replaceQuery defines what to do if the project already exists in the workspace
	 * @param executionQuery defines what to do if the project requires an unsupported execution environment
	 * @param forceAutobuild whether to force a build after the import
	 */
	public PluginImportOperation(IPluginModelBase[] models, int importType, IImportQuery replaceQuery, IImportQuery executionQuery, boolean forceAutobuild) {
		this(models, importType, replaceQuery, executionQuery);
		fForceAutobuild = forceAutobuild;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_creating, fModels.length + 1);
		try {
			MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(), IStatus.OK, PDEUIMessages.ImportWizard_operation_multiProblem, null);

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
			if (!fAffectedPlugins.isEmpty()) {
				final Display display = Display.getDefault();
				display.syncExec(new Runnable() {
					public void run() {
						PluginImportFinishDialog dialog = new PluginImportFinishDialog(display.getActiveShell());
						dialog.setTitle(PDEUIMessages.PluginImportInfoDialog_title);
						dialog.setMessage(PDEUIMessages.PluginImportInfoDialog_message);
						dialog.setInput(fAffectedPlugins);
						dialog.open();
					}

				});
			}
		}
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
	 * Sets the raw classpath of projects that need to be updated
	 * @param monitor
	 * @throws JavaModelException if a classpath could not be set
	 */
	private void setClasspaths(IProgressMonitor monitor) throws JavaModelException {
		monitor.beginTask("", fProjectClasspaths.size()); //$NON-NLS-1$
		Enumeration keys = fProjectClasspaths.keys();
		while (keys.hasMoreElements()) {
			IProject project = (IProject) keys.nextElement();
			IClasspathEntry[] classpath = (IClasspathEntry[]) fProjectClasspaths.get(project);
			monitor.subTask(project.getName());
			JavaCore.create(project).setRawClasspath(classpath, new SubProgressMonitor(monitor, 1));
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
	private void importPlugin(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
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

			IProject project = findProject(id);

			if (project.exists() || new File(project.getParent().getLocation().toFile(), project.getName()).exists()) {
				if (!queryReplace(project))
					return;
				if (RepositoryProvider.isShared(project))
					RepositoryProvider.unmap(project);
				if (!project.exists())
					project.create(new SubProgressMonitor(monitor, 1));
				if (!safeDeleteCheck(project, monitor)) {
					fAffectedPlugins.add(model);
					return;
				}
				//bug 212755
				try {
					project.delete(true, true, monitor);
				} catch (CoreException e) {
					fAffectedPlugins.add(model);
					return;
				}
			}

			project.create(monitor);
			if (!project.isOpen())
				project.open(monitor);
			monitor.worked(1);

			switch (fImportType) {
				case IMPORT_BINARY :
					importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					break;
				case IMPORT_BINARY_WITH_LINKS :
					if (id.startsWith("org.eclipse.swt") && !isJARd(model)) { //$NON-NLS-1$
						importAsBinary(project, model, true, monitor);
					} else {
						importAsBinaryWithLinks(project, model, new SubProgressMonitor(monitor, 4));
					}
					break;
				case IMPORT_WITH_SOURCE :
					if (isExempt(model)) {
						importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					} else {
						importAsSource(project, model, new SubProgressMonitor(monitor, 4));
					}
			}

			setProjectDescription(project, model);

			if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
				fProjectClasspaths.put(project, ClasspathComputer.getClasspath(project, model, true, false));
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			monitor.done();
		}
	}

	// Returns the name of any projects the currently exist with same id and version.  Otherwise it returns a default naming convention
	protected String getProjectName(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		String version = model.getPluginBase().getVersion();
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			IPluginModelBase[] existingModels = entry.getWorkspaceModels();
			for (int i = 0; i < existingModels.length; i++) {
				String existingVersion = existingModels[i].getPluginBase().getVersion();
				if (version.equals(existingVersion)) {
					IResource res = existingModels[i].getUnderlyingResource();
					if (res != null)
						return res.getProject().getName();
				}
			}
		}
		return id + "_" + version; //$NON-NLS-1$
	}

	// returns true if it is safe to delete the project.  It is not safe to delete if
	// one of its libraries is locked by a running launch configuration.
	private boolean safeDeleteCheck(IProject project, IProgressMonitor monitor) {
		if (!fLaunchedConfigurations)
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
	 * Imports the contents of the plugin and adds links to the source location(s).
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void importAsBinaryWithLinks(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		if (isJARd(model)) {
			extractJARdPlugin(project, model, monitor);
		} else {
			File installLocation = new File(model.getInstallLocation());
			File[] items = installLocation.listFiles();
			if (items != null) {
				monitor.beginTask(PDEUIMessages.PluginImportOperation_linking, items.length + 1);
				for (int i = 0; i < items.length; i++) {
					File sourceFile = items[i];
					String name = sourceFile.getName();
					if (sourceFile.isDirectory()) {
						project.getFolder(name).createLink(new Path(sourceFile.getPath()), IResource.NONE, new SubProgressMonitor(monitor, 1));
					} else {
						if (!name.equals(".project")) { //$NON-NLS-1$ 
							project.getFile(name).createLink(new Path(sourceFile.getPath()), IResource.NONE, new SubProgressMonitor(monitor, 1));
						} else {
							// if the binary project with links has a .project file, copy it instead of linking (allows us to edit it)
							ArrayList filesToImport = new ArrayList(1);
							filesToImport.add(sourceFile);
							importContent(installLocation, project.getFullPath(), FileSystemStructureProvider.INSTANCE, filesToImport, new SubProgressMonitor(monitor, 1));
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

	/**
	 * Imports the contents of the plugin and imports source files as binary files that will not be compiled.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param markAsBinary whether to mark the project as a binary project
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void importAsBinary(IProject project, IPluginModelBase model, boolean markAsBinary, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 4); //$NON-NLS-1$
		if (isJARd(model)) {
			extractJARdPlugin(project, model, new SubProgressMonitor(monitor, 3));
		} else {
			importContent(new File(model.getInstallLocation()), project.getFullPath(), FileSystemStructureProvider.INSTANCE, null, new SubProgressMonitor(monitor, 1));
			importSourceArchives(project, model, new SubProgressMonitor(monitor, 1));

			// make sure all libraries have been imported
			// if any are missing, check in fragments		
			IFragment[] fragments = getFragmentsFor(model);
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();

			IProgressMonitor fragmentMonitor = new SubProgressMonitor(monitor, 1);
			fragmentMonitor.beginTask("", libraries.length); //$NON-NLS-1$
			for (int i = 0; i < libraries.length; i++) {
				String libraryName = libraries[i].getName();
				if (ClasspathUtilCore.containsVariables(libraryName) && !project.exists(new Path(ClasspathUtilCore.expandLibraryName(libraryName)))) {
					for (int j = 0; j < fragments.length; j++) {
						importJarFromFragment(project, fragments[j], libraryName);
						importSourceFromFragment(project, fragments[j], libraryName, new SubProgressMonitor(monitor, 1));
					}
				} else {
					monitor.worked(1);
				}
			}
		}
		if (markAsBinary) {
			project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);
			importAdditionalResources(project, model, new SubProgressMonitor(monitor, 1));
		} else {
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
		monitor.beginTask("", 4); //$NON-NLS-1$
		importAsBinary(project, model, false, new SubProgressMonitor(monitor, 2));
		List list = importAdditionalResources(project, model, new SubProgressMonitor(monitor, 1));
		WorkspaceBuildModel buildModel = new WorkspaceBuildModel(project.getFile("build.properties")); //$NON-NLS-1$
		if (!isJARd(model) || containsCode(new File(model.getInstallLocation()))) {
			String[] libraries = getLibraryNames(model, false);
			if (libraries.length == 0)
				libraries = new String[] {"."}; //$NON-NLS-1$
			for (int i = 0; i < libraries.length; i++) {
				if (ClasspathUtilCore.containsVariables(libraries[i]))
					continue;
				String name = ClasspathUtilCore.expandLibraryName(libraries[i]);
				IPath libraryPath = (name.equals(".") && isJARd(model)) //$NON-NLS-1$
				? new Path(new File(model.getInstallLocation()).getName())
						: new Path(name);
				IResource jarFile = project.findMember(libraryPath);
				if (jarFile != null) {
					String srcName = ClasspathUtilCore.getSourceZipName(libraryPath.lastSegment());
					IResource srcZip = jarFile.getProject().findMember(srcName);
					if (srcZip == null) {
						int extIndex = srcName.lastIndexOf('.');
						if (extIndex != -1) {
							srcZip = jarFile.getProject().findMember(srcName.substring(0, extIndex));
						}
					}
					// srcZip == null if plug-in has embedded source
					// if it jarred, all necessary files already in src folder
					if (srcZip == null && libraries[i].equals(".") && !isJARd(model)) //$NON-NLS-1$
						// if src does not exist (and returns null), then must not be plug-in with embedded source
						srcZip = jarFile.getProject().findMember("src"); //$NON-NLS-1$
					if (srcZip != null) {
						String jarName = libraries[i].equals(".") ? "" : libraryPath.removeFileExtension().lastSegment(); //$NON-NLS-1$ //$NON-NLS-2$
						String folder = addBuildEntry(buildModel, "source." + libraries[i], "src" + (jarName.length() == 0 ? "/" : "-" + jarName + "/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						IFolder dest = jarFile.getProject().getFolder(folder);

						if (srcZip instanceof IFolder) {
							// if the source (srcZip) equals the destination folder (dest), then we don't want to delete/copy since every
							// is already where it needs to be.  This happens when importing source bundles in folder format declaring source with ext. point. (bug 214542)
							if (!srcZip.equals(dest)) {
								if (dest.exists()) {
									dest.delete(true, null);
								}
								((IFolder) srcZip).move(dest.getFullPath(), true, new SubProgressMonitor(monitor, 1));
							}
						} else if (srcZip instanceof IFile) {
							if (!dest.exists()) {
								dest.create(true, true, null);
							}
							extractZipFile(srcZip.getLocation().toFile(), dest.getFullPath(), new SubProgressMonitor(monitor, 1));
							srcZip.delete(true, null);
						} else
							monitor.worked(1);

						if (jarFile instanceof IFile) {
							if (isJARd(model)) {
								extractJavaResources(jarFile.getLocation().toFile(), dest, new SubProgressMonitor(monitor, 1));
							} else {
								extractResources(jarFile.getLocation().toFile(), dest, new SubProgressMonitor(monitor, 1));
							}
							jarFile.delete(true, null);
						} else {
							moveBinaryContents((IContainer) jarFile, dest, new SubProgressMonitor(monitor, 1));
						}
					}
				} else if (name.equals(".") && project.getFolder("src").exists()) { //$NON-NLS-1$ //$NON-NLS-2$
					addBuildEntry(buildModel, "source..", "src/"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		configureBinIncludes(buildModel, model, project);
		if (list.size() > 0)
			configureSrcIncludes(buildModel, list);
		buildModel.save();
	}

	/**
	 * Moves the binary files from the source container to the folder destination.
	 * Moves any file that isn't a .class file
	 * @param srcFolder container to move from
	 * @param dest folder to move to
	 * @param monitor progress monitor
	 */
	private void moveBinaryContents(IContainer srcFolder, IFolder dest, IProgressMonitor monitor) {
		try {
			// get all the folders for which we want to search
			IResource[] children = dest.members();
			ArrayList validFolders = new ArrayList();
			for (int i = 0; i < children.length; i++)
				if (children[i] instanceof IFolder) {
					String folderName = children[i].getName();
					IResource folder = srcFolder.findMember(folderName);
					if (folder != null && folder instanceof IFolder)
						validFolders.add(folder);
				}

			monitor.beginTask(new String(), validFolders.size());

			ListIterator li = validFolders.listIterator();
			while (li.hasNext()) {
				IFolder folder = (IFolder) li.next();
				int pathSegments = folder.getProjectRelativePath().segmentCount() - 1;
				Stack stack = new Stack();
				IResource[] resources = folder.members();
				for (int i = 0; i < resources.length; i++)
					stack.push(resources[i]);

				while (!stack.isEmpty()) {
					IResource res = (IResource) stack.pop();
					if (res instanceof IFile) {
						if (!res.getName().endsWith(".class")) { //$NON-NLS-1$
							String pathName = res.getProjectRelativePath().removeFirstSegments(pathSegments).toString();
							IFile destFile = dest.getFile(pathName);
							if (!destFile.getParent().exists()) {
								CoreUtility.createFolder((IFolder) destFile.getParent());
							}
							// file might exist if previous project was deleted without removing underlying resources
							if (destFile.exists())
								destFile.delete(true, null);
							res.move(destFile.getFullPath(), true, null);
						}
					} else {
						resources = ((IFolder) res).members();
						for (int i = 0; i < resources.length; i++)
							stack.push(resources[i]);
					}
				}
				folder.delete(true, null);
				monitor.worked(1);
			}
		} catch (CoreException e) {
		}
	}

	/**
	 * Searches source locations for files to import to the new project, will ignore
	 * src.zip.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @return list of imported files
	 * @throws CoreException if there is a problem completing the import
	 */
	private List importAdditionalResources(IProject project, IPluginModelBase model, SubProgressMonitor monitor) throws CoreException {
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		File location = manager.findSourcePlugin(model.getPluginBase());
		if (location != null) {
			ArrayList list = new ArrayList();
			if (location.isDirectory()) {
				Object root = location;
				File[] children = location.listFiles();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						String name = children[i].getName();
						if (!project.exists(new Path(name)) && !"src.zip".equals(name)) { //$NON-NLS-1$
							list.add(children[i]);
						}
					}
					importContent(root, project.getFullPath(), FileSystemStructureProvider.INSTANCE, list, monitor);
					ArrayList srcEntryList = new ArrayList(list.size());
					for (ListIterator iterator = list.listIterator(); iterator.hasNext();) {
						File current = (File) iterator.next();
						String entry = current.getName();
						if (current.isDirectory()) {
							entry += "/"; //$NON-NLS-1$
						}
						srcEntryList.add(entry);
					}
					return srcEntryList;
				}
			} else if (location.isFile()) {
				ZipFile zipFile = null;
				try {
					zipFile = new ZipFile(location);
					ZipFileStructureProvider zipProvider = new ZipFileStructureProvider(zipFile);
					Object root = zipProvider.getRoot();
					collectAdditionalResources(zipProvider, root, list, project);
					importContent(root, project.getFullPath(), zipProvider, list, monitor);
					ArrayList srcEntryList = new ArrayList(list.size());
					for (Iterator iterator = list.iterator(); iterator.hasNext();) {
						ZipEntry current = (ZipEntry) iterator.next();
						String entry = current.getName();
						srcEntryList.add(entry);
					}
					return srcEntryList;
				} catch (IOException e) {
					IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
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
		}
		return new ArrayList(0);
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
				String[] tokens = getTopLevelResources(location);
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

	private void configureSrcIncludes(WorkspaceBuildModel buildModel, List list) throws CoreException {
		IBuildEntry entry = buildModel.getBuild(true).getEntry("src.includes"); //$NON-NLS-1$
		if (entry == null) {
			entry = buildModel.getFactory().createEntry("src.includes"); //$NON-NLS-1$
			for (int i = 0; i < list.size(); i++) {
				entry.addToken(list.get(i).toString());
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

	/**
	 * Creates links in the project to the source locations for the various libraries.
	 * If the source for all libraries is in a single bundle, one link is created
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void linkSourceArchives(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource, libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		if (manager.hasBundleManifestLocation(model.getPluginBase())) {
			IPath srcPath = manager.findSourcePath(model.getPluginBase(), null);
			if (srcPath != null) {
				// Source for all libraries is in the same bundle, just create one link to the source bundle
				IPath path = new Path(project.getName() + "src.zip"); //$NON-NLS-1$
				IFile srcFile = project.getFile(path.lastSegment());
				if (!srcFile.exists()) {
					srcFile.createLink(srcPath, IResource.NONE, new SubProgressMonitor(monitor, 1));
				}
			}
			monitor.worked(libraries.length);
		} else {
			for (int i = 0; i < libraries.length; i++) {
				String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
				IPath path = new Path(zipName);
				if (project.findMember(path) == null) {
					IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
					if (srcPath != null) {
						if ("src.zip".equals(zipName) && isJARd(model)) { //$NON-NLS-1$
							path = new Path(ClasspathUtilCore.getSourceZipName(new File(model.getInstallLocation()).getName()));
						}
						IFile zipFile = project.getFile(path.lastSegment());
						if (!zipFile.exists()) {
							zipFile.createLink(srcPath, IResource.NONE, new SubProgressMonitor(monitor, 1));
						}
					}
				}
				monitor.worked(1);
			}
		}
		monitor.done();
	}

	private void importSourceArchives(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource, libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();

		Set roots = null;
		if (manager.hasBundleManifestLocation(model.getPluginBase()))
			roots = manager.findSourceRoots(model.getPluginBase());

		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				// if we are importing the source through a sourceBundle header...
				if (roots != null) {
					IPath sourceLocation = manager.findSourcePath(model.getPluginBase(), null);
					String currentRoot = ".".equals(libraries[i]) ? "." : path.removeFileExtension().toString(); //$NON-NLS-1$ //$NON-NLS-2$
					if (roots.contains(currentRoot)) {
						if (".".equals(currentRoot)) { //$NON-NLS-1$
							// Save to a special folder name based on the install location
							IPath sourceName = getDefaultSourceNameForProject(model);
							sourceName = sourceName.removeFileExtension();
							IFolder dest = project.getFolder(sourceName);
							if (!dest.exists()) {
								dest.create(true, true, null);
							}

							// List all of the other source roots so they are not included when importing source from the root, ".", of the jar
							Set allBundleRoots = manager.findAllSourceRootsInSourceLocation(model.getPluginBase());
							List rootsToExclude = new ArrayList(allBundleRoots.size() - 1);
							for (Iterator iterator2 = allBundleRoots.iterator(); iterator2.hasNext();) {
								String rootString = (String) iterator2.next();
								if (!".".equals(rootString)) { //$NON-NLS-1$
									rootsToExclude.add(new Path(rootString));
								}
							}

							// Extract folders containing java source
							extractJavaSource(new File(sourceLocation.toOSString()), rootsToExclude, dest, monitor);
						} else {
							// Extract the specific library from it's folder
							extractResourcesFromFolder(new File(sourceLocation.toOSString()), new Path(currentRoot), project, monitor);
						}
					}
				} else {
					IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
					if (srcPath != null) {
						if ("src.zip".equals(zipName) && isJARd(model)) { //$NON-NLS-1$
							path = getDefaultSourceNameForProject(model);
						}
						importArchive(project, new File(srcPath.toOSString()), path);
					}
				}
			}
			monitor.worked(1);
		}
		monitor.done();
	}

	/**
	 * Creates a path representing a zip file that is named based on the plugin install location.
	 * Used to replace src.zip with a more unique and meaningful name.
	 * @param model model that the src.zip containg source for
	 * @return a new path describing the zip file
	 */
	private IPath getDefaultSourceNameForProject(IPluginModelBase model) {
		return new Path(ClasspathUtilCore.getSourceZipName(new File(model.getInstallLocation()).getName()));
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
		return (String[]) list.toArray(new String[list.size()]);
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
					project.getFile(file.getName()).createLink(new Path(file.getAbsolutePath()), IResource.NONE, null);
				} else {
					importArchive(project, file, new Path(file.getName()));
				}
				if (!hasEmbeddedSource(provider)) {
					if (fImportType == IMPORT_BINARY_WITH_LINKS) {
						linkSourceArchives(project, model, new SubProgressMonitor(monitor, 1));
					} else {
						importSourceArchives(project, model, new SubProgressMonitor(monitor, 1));
					}
				}
			}
			if (fImportType != IMPORT_WITH_SOURCE) {
				modifyBundleClasspathHeader(project, model);
			} else {
				removeSignedHeaders(project);
			}
			setPermissions(model, project);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, e.getMessage(), e);
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

	private void removeSignedHeaders(IProject project) {
		IFile file = project.getFile(JarFile.MANIFEST_NAME);
		if (!file.exists())
			return;
		WorkspaceBundleModel model = new WorkspaceBundleModel(file);
		model.save();
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

	private boolean queryReplace(IProject project) throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(NLS.bind(PDEUIMessages.ImportWizard_messages_exists, project.getName()))) {
			case IImportQuery.CANCEL :
				throw new OperationCanceledException();
			case IImportQuery.NO :
				return false;
		}
		return true;
	}

	private boolean queryExecutionEnvironment(String message) throws OperationCanceledException {
		switch (fExecutionQuery.doQuery(message)) {
			case IImportQuery.CANCEL :
				throw new OperationCanceledException();
			case IImportQuery.NO :
				return false;
		}
		return true;
	}

	private void setProjectDescription(IProject project, IPluginModelBase model) throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (!desc.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
		if (needsJavaNature(project, model) && !desc.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
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
				|| "org.eclipse.osgi.services".equals(id) //$NON-NLS-1$
				|| "org.eclipse.core.runtime.compatibility.registry".equals(id)) //$NON-NLS-1$
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
				IFragment fragment = ((IFragmentModel) model).getFragment();
				if ("org.eclipse.swt".equals(fragment.getPluginId())) { //$NON-NLS-1$
					IResource[] children = project.members();
					for (int i = 0; i < children.length; i++) {
						if (children[i] instanceof IFile && isInterestingResource(children[i].getName())) {
							Runtime.getRuntime().exec(new String[] {"chmod", "755", children[i].getLocation().toOSString()}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
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
				HostSpecification spec = fModels[i].getBundleDescription().getHost();
				BundleDescription host = spec == null ? null : (BundleDescription) spec.getSupplier();
				if (model.getBundleDescription().equals(host)) {
					result.add(((IFragmentModel) fModels[i]).getFragment());
				}
			}
		}
		return (IFragment[]) result.toArray(new IFragment[result.size()]);
	}

	private void importJarFromFragment(IProject project, IFragment fragment, String name) throws CoreException {
		IPath jarPath = new Path(ClasspathUtilCore.expandLibraryName(name));
		File jar = new File(fragment.getModel().getInstallLocation(), jarPath.toString());
		if (jar.exists()) {
			importArchive(project, jar, jarPath);
		}
	}

	/**
	 * Imports the source for a library from a fragment.
	 * @param project destination project of the import
	 * @param fragment fragment to import the library from
	 * @param libraryName name of the library to import, 
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void importSourceFromFragment(IProject project, IFragment fragment, String libraryName, IProgressMonitor monitor) throws CoreException {
		try {
			IPath jarPath = new Path(ClasspathUtilCore.expandLibraryName(libraryName));
			String zipName = ClasspathUtilCore.getSourceZipName(jarPath.toString());
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
				IPath srcPath = manager.findSourcePath(fragment, path);
				if (srcPath != null) {
					if (manager.hasBundleManifestLocation(fragment)) {
						// Extract the specific library from it's folder
						extractResourcesFromFolder(new File(srcPath.toOSString()), path.removeFileExtension(), project, monitor);
					} else {
						importArchive(project, new File(srcPath.toOSString()), path);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	protected void collectAdditionalResources(ZipFileStructureProvider provider, Object element, ArrayList collected, IProject project) {
		collectAdditionalResources(provider, element, collected);
		ListIterator li = collected.listIterator();
		while (li.hasNext()) {
			ZipEntry ze = (ZipEntry) li.next();
			String name = ze.getName();
			// only import the entries that don't already exist
			if (project.findMember(name) != null) {
				li.remove();
			}
		}
	}

	protected void collectNonJavaResources(ZipFileStructureProvider provider, Object element, ArrayList collected) {
		super.collectNonJavaResources(provider, element, collected);
		if (fImportType != IMPORT_WITH_SOURCE)
			return;
		// filter the resources we get back to include only relevant resource files
		ListIterator li = collected.listIterator();
		while (li.hasNext()) {
			ZipEntry ze = (ZipEntry) li.next();
			String name = ze.getName();
			// filter out signature files - bug 175756
			if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				li.remove();
		}
	}

	public void setLaunchedConfiguration(boolean launchedConfiguration) {
		fLaunchedConfigurations = launchedConfiguration;
	}

}
