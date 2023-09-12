/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.BundleException;

/**
 * Imports one or more plug-ins into the workspace.  There are three different
 * ways to import a plugin: as binary, as binary with linked source,
 * and as source.
 */
@SuppressWarnings("restriction")
// The IBundleImporter API is currently provisional
public class PluginImportOperation extends WorkspaceJob {

	public static final int IMPORT_BINARY = 1;
	public static final int IMPORT_BINARY_WITH_LINKS = 2;
	public static final int IMPORT_WITH_SOURCE = 3;
	public static final int IMPORT_FROM_REPOSITORY = 4;

	private static final String DEFAULT_SOURCE_DIR = "src"; //$NON-NLS-1$
	private static final String DEFAULT_LIBRARY_NAME = "."; //$NON-NLS-1$

	private static final int MAX_RETRY = 5;
	private static final int RETRY_DELAY = 1000;

	private final IPluginModelBase[] fModels;
	private final int fImportType;
	private final Hashtable<IProject, IClasspathEntry[]> fProjectClasspaths = new Hashtable<>();

	/**
	 * Maps project ids to a List of IWorkingSets, the map is filled when determining what projects to delete
	 */
	private final Map<String, List<IWorkingSet>> fProjectWorkingSets = new HashMap<>();
	private final boolean fForceAutobuild;

	// used when importing from a repository
	private Map<IBundleImporter, ScmUrlImportDescription[]> fImportDescriptions;

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
	 * @param forceAutobuild whether to force a build after the import
	 */
	public PluginImportOperation(IPluginModelBase[] models, int importType, boolean forceAutobuild) {
		super(PDEUIMessages.ImportWizard_title);
		fModels = models;
		fImportType = importType;
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

	/**
	 * This custom message box class is used for warning the user about the projects that did not get imported.
	 * see bug 337730
	 *
	 * This class should get removed when Bug 346078 is fixed.
	 *
	 */
	private class NotImportedProjectsWarningDialog extends MessageDialog {

		/**
		 * The list of the projects that did not get imported.
		 */
		private final List<String> fNamesOfNotImportedProjects;

		/**
		 * Creates a warning message dialog. The message area will contain the scrollable
		 * text box that will show the list of the projects supplied.
		 *
		 * @param warningMessage
		 * 				the warning message to be shown on the dialog.
		 * @param namesOfNotImportedProjects
		 * 				the list of the project names that did not get imported.
		 */
		public NotImportedProjectsWarningDialog(String warningMessage, List<String> namesOfNotImportedProjects) {
			super(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), PDEUIMessages.ImportWizard_title, null, warningMessage, MessageDialog.WARNING, new String[] {IDialogConstants.OK_LABEL}, 0);
			fNamesOfNotImportedProjects = namesOfNotImportedProjects;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			composite.setLayout(layout);

			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			Text projectText = new Text(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY | SWT.BORDER);
			projectText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = convertWidthInCharsToPixels(60);
			gd.heightHint = convertHeightInCharsToPixels(10);
			projectText.setLayoutData(gd);

			StringBuilder projectListBuffer = new StringBuilder();
			for (String project : fNamesOfNotImportedProjects) {
				projectListBuffer.append(project).append('\n');
			}
			projectText.setText(projectListBuffer.toString());
			return composite;

		}
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		int ticks = (fImportType == IMPORT_FROM_REPOSITORY) ? fImportDescriptions.size() + 2 : fModels.length + 1;
		SubMonitor subMonitor = SubMonitor.convert(monitor, ticks);
		MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(), IStatus.OK,
				PDEUIMessages.ImportWizard_operation_multiProblem, null);

		deleteConflictingProjects(multiStatus, subMonitor.split(2));
		if (subMonitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		if (fImportType == IMPORT_FROM_REPOSITORY) {
			final List<String> namesOfNotImportedProjects = new ArrayList<>();
			Iterator<Entry<IBundleImporter, ScmUrlImportDescription[]>> iterator = fImportDescriptions.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Entry<IBundleImporter, ScmUrlImportDescription[]> entry = iterator.next();
				IBundleImporter importer = entry.getKey();
				ScmUrlImportDescription[] descriptions = entry.getValue();
				if (descriptions.length == 0)
					continue;
				IProject[] importedProjects = importer.performImport(descriptions, subMonitor.split(1));
				if (importedProjects != null && importedProjects.length == descriptions.length)
					continue;

				ArrayList<String> namesOfImportedProjects = new ArrayList<>(importedProjects.length);
				for (IProject importedProject : importedProjects) {
					namesOfImportedProjects.add(importedProject.getName());
				}
				for (ScmUrlImportDescription description : descriptions) {
					String projectName = description.getProject();
					if (!namesOfImportedProjects.contains(projectName)) {
						namesOfNotImportedProjects.add(projectName);
					}
				}
			}
			if (!namesOfNotImportedProjects.isEmpty()) {
				UIJob job = UIJob.create(PDEUIMessages.PluginImportOperation_WarningDialogJob, m -> {
					String dialogMessage = namesOfNotImportedProjects.size() == 1
							? PDEUIMessages.PluginImportOperation_WarningDialogMessageSingular
							: PDEUIMessages.PluginImportOperation_WarningDialogMessagePlural;
					NotImportedProjectsWarningDialog dialog = new NotImportedProjectsWarningDialog(dialogMessage,
							namesOfNotImportedProjects);
					dialog.open();
				});
				try {
					job.schedule();
					job.join();
				} catch (InterruptedException e1) {
				}
			}
		} else {
			for (IPluginModelBase model : fModels) {
				subMonitor.setTaskName(NLS.bind(PDEUIMessages.PluginImportOperation_Importing_plugin,
						model.getPluginBase().getId()));
				try {
					importPlugin(model, fImportType, subMonitor.split(1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (subMonitor.isCanceled()) {
					try {
						setClasspaths(subMonitor.split(1));
					} catch (JavaModelException e) {
						/* Do nothing as we are already cancelled */
					}
					return Status.CANCEL_STATUS;
				}
			}
			subMonitor.setTaskName(PDEUIMessages.PluginImportOperation_Set_up_classpaths);
			try {
				setClasspaths(subMonitor.split(1));
			} catch (JavaModelException e) {
				multiStatus.merge(e.getStatus());
			}
		}
		if (!ResourcesPlugin.getWorkspace().isAutoBuilding() && fForceAutobuild)
			runBuildJob();
		return multiStatus;
	}

	/**
	 * If there are existing projects in the workspace with the same symbolic name, open a dialog
	 * asking the user if they would like to delete those projects.  The projects are deleted before
	 * the import continues so the individual imports can do a simple search for an allowed plug-in name.
	 *
	 * @param status the multi-status used to report problems
	 * @param monitor progress monitor, must not be <code>null</code>
	 */
	private void deleteConflictingProjects(MultiStatus status, IProgressMonitor monitor) {
		monitor.beginTask("", 5); //$NON-NLS-1$
		IPluginModelBase[] workspacePlugins = PluginRegistry.getWorkspaceModels();
		HashMap<String, ArrayList<IPluginModelBase>> workspacePluginMap = new HashMap<>();
		for (IPluginModelBase plugin : workspacePlugins) {
			if (plugin.getBundleDescription() != null) {
				String symbolicName = plugin.getBundleDescription().getSymbolicName();
				ArrayList<IPluginModelBase> pluginsWithSameSymbolicName = workspacePluginMap.get(symbolicName);
				if (pluginsWithSameSymbolicName == null) {
					pluginsWithSameSymbolicName = new ArrayList<>();
					workspacePluginMap.put(symbolicName, pluginsWithSameSymbolicName);
				}
				pluginsWithSameSymbolicName.add(plugin);
			}
		}
		monitor.worked(1);

		final ArrayList<IPluginModelBase> conflictingPlugins = new ArrayList<>();
		for (IPluginModelBase model : fModels) {
			if (model.getBundleDescription() == null) {
				continue;
			}
			String symbolicName = model.getBundleDescription().getSymbolicName();
			ArrayList<IPluginModelBase> plugins = workspacePluginMap.get(symbolicName);
			if (plugins == null || plugins.isEmpty()) {
				continue;
			}
			if (!conflictingPlugins.containsAll(plugins)) {
				conflictingPlugins.addAll(plugins);
			}
		}
		monitor.worked(1);

		final ArrayList<Object> overwriteProjectList = new ArrayList<>();
		if (!conflictingPlugins.isEmpty()) {

			UIJob job = UIJob.create(PDEUIMessages.PluginImportOperation_OverwritePluginProjects, m -> {
				Shell shell = Display.getCurrent().getActiveShell();
				SelectionDialog dialog = new OverwriteProjectsSelectionDialog(shell, conflictingPlugins);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.OK) {
					overwriteProjectList.addAll(Arrays.asList(dialog.getResult()));
					return Status.OK_STATUS;
				}
				return Status.CANCEL_STATUS;
			});

			try {
				job.schedule();
				job.join();
			} catch (InterruptedException e1) {
			}

			if (job.getResult() == null || !job.getResult().isOK()) {
				monitor.setCanceled(true);
				return;
			}
			monitor.worked(1);

			// collect working set information
			IWorkingSetManager wsManager = PlatformUI.getWorkbench().getWorkingSetManager();
			IWorkingSet[] sets = wsManager.getAllWorkingSets();
			for (IWorkingSet set : sets) {
				IAdaptable[] contents = set.getElements();
				for (IAdaptable content : contents) {
					IResource resource = content.getAdapter(IResource.class);
					if (resource instanceof IProject) {
						// TODO For now just list everything in the map
						String id = ((IProject) resource).getName();
						List<IWorkingSet> workingSets = fProjectWorkingSets.get(id);
						if (workingSets == null) {
							workingSets = new ArrayList<>();
							fProjectWorkingSets.put(id, workingSets);
						}
						workingSets.add(set);
					}
				}
			}

			// delete the selected projects
			for (int i = 0; i < overwriteProjectList.size(); i++) {
				IPluginModelBase plugin = (IPluginModelBase) overwriteProjectList.get(i);
				monitor.setTaskName(
						NLS.bind(PDEUIMessages.PluginImportOperation_Importing_plugin, plugin.getPluginBase().getId()));
				IProject project = plugin.getUnderlyingResource().getProject();
				try {
					if (RepositoryProvider.isShared(project))
						RepositoryProvider.unmap(project);
					if (!safeDeleteCheck(project, monitor)) {
						status.add(Status.error(NLS.bind(PDEUIMessages.PluginImportOperation_could_not_delete_project,
								project.getName())));
					}
					boolean deleteContent = project.getWorkspace().getRoot().getLocation()
							.equals(project.getLocation().removeLastSegments(1));
					deleteProject(project, deleteContent, monitor);

				} catch (CoreException ex) {
					status.add(Status.error(
							NLS.bind(PDEUIMessages.PluginImportOperation_could_not_delete_project, project.getName()),
							ex));
				}
			}
			monitor.worked(2);
		} else {
			monitor.worked(3);
		}
	}

	/**
	 * deletes a project. Retries if deletion failed (e.g. because the indexer
	 * still locks the file).
	 *
	 * @param project
	 *            the project to delete
	 * @param deleteContent
	 *            a flag controlling how whether content is aggressively deleted
	 * @param monitor
	 *            progress monitor, must not be <code>null</code>
	 * @throws CoreException
	 *             if operation failed
	 */
	private void deleteProject(IProject project, boolean deleteContent, IProgressMonitor monitor) throws CoreException {
		for (int i = 0; i < MAX_RETRY; i++) {
			try {
				project.delete(deleteContent, true, monitor);
				i = MAX_RETRY;
			} catch (CoreException e) {
				if (i == MAX_RETRY - 1) {
					throw e;
				}
				try {
					Thread.sleep(RETRY_DELAY);
				} catch (InterruptedException e1) {
				}
			}
		}
	}

	/**
	 * Sets the raw classpath of projects that need to be updated
	 * @param monitor
	 * @throws JavaModelException if a classpath could not be set
	 */
	private void setClasspaths(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, fProjectClasspaths.size());
		Enumeration<IProject> keys = fProjectClasspaths.keys();
		while (keys.hasMoreElements()) {
			IProject project = keys.nextElement();
			IClasspathEntry[] classpath = fProjectClasspaths.get(project);
			subMonitor.subTask(project.getName());
			JavaCore.create(project).setRawClasspath(classpath, subMonitor.split(1));
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
		SubMonitor subMonitor = SubMonitor.convert(monitor, 5);

		// Create the project or ask to overwrite if project exists
		IProject project = createProject(model, subMonitor.split(1));
		if (project == null) {
			return;
		}

		// Target Weaving: if we are importing plug-ins in the runtime workbench
		// from the host workbench, import everything as-is and return
		// Target weaving will also break things when importing from a
		// non-default target because the dev.properties changes the libraries
		// to 'bin/' see bug 294005
		if (Platform.inDevelopmentMode()) {
			File location = new File(model.getInstallLocation());
			if (location.isDirectory()) {
				File classpathFile = new File(location, ".classpath"); //$NON-NLS-1$
				File projectFile = new File(location, ".project"); //$NON-NLS-1$
				if (classpathFile.exists() && classpathFile.isFile() && projectFile.exists() && projectFile.isFile()) {
					PluginImportHelper.importContent(location, project.getFullPath(),
							FileSystemStructureProvider.INSTANCE, null, subMonitor.split(4));
					return;
				}
			}
		}

		// Perform the import
		Map<String, IPath> sourceMap = null;
		if (importType == IMPORT_BINARY || (importType == IMPORT_WITH_SOURCE && !canFindSource(model))) {
			sourceMap = importAsBinary(project, model, subMonitor.split(4));
		} else if (importType == IMPORT_BINARY_WITH_LINKS) {
			sourceMap = importAsBinaryWithLinks(project, model, subMonitor.split(4));
		} else if (importType == IMPORT_WITH_SOURCE) {
			importAsSource(project, model, subMonitor.split(4));
		}

		setProjectNatures(project, model);

		if(importType == IMPORT_WITH_SOURCE) {
			// if the project has .api_description, then API nature must be
			// added
			if (project.findMember(".api_description") != null) { //$NON-NLS-1$
				CoreUtility.addNatureToProject(project, "org.eclipse.pde.api.tools.apiAnalysisNature", null); //$NON-NLS-1$
			}
		}

		// Set the classpath
		if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
			fProjectClasspaths.put(project, ClasspathComputer.getClasspath(project, model, sourceMap, true, false));
	}

	/**
	 * Imports the contents of the plugin and imports source files as binary files that will not be compiled.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @return a mapping of libraries to source locations to use in the classpath
	 * @throws CoreException if there is a problem completing the import
	 */
	private Map<String, IPath> importAsBinary(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 4);

		// Import the plug-in content
		File srcFile = new File(model.getInstallLocation());
		if (isJARd(model)) {
			PluginImportHelper.copyArchive(srcFile, project.getFile(srcFile.getName()), subMonitor.split(1));
		} else {
			PluginImportHelper.importContent(new File(model.getInstallLocation()), project.getFullPath(),
					FileSystemStructureProvider.INSTANCE, null, subMonitor.split(1));
		}

		// Import source from known source locations
		Map<String, IPath> sourceMap = importSourceArchives(project, model, IMPORT_BINARY, subMonitor.split(1));

		// Import additional source files such as schema files for easy access,
		// see bug 139161
		importAdditionalSourceFiles(project, model, subMonitor.split(1));

		// Extract the required bundle files and modify the imported manifest to
		// have the correct classpath
		importRequiredPluginFiles(project, model, subMonitor.split(1));
		modifyBundleClasspathHeader(project, model);

		// Mark the project as binary
		RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);

		return sourceMap;
	}

	/**
	 * Creates links to remote plugin and source locations and sets up the project
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @return mapping of library name to path to source library (relative to project)
	 * @throws CoreException if there is a problem completing the import
	 */
	private Map<String, IPath> importAsBinaryWithLinks(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 4);

		// Link the plug-in content
		File srcFile = new File(model.getInstallLocation());
		if (srcFile.isFile()) {
			IFile dstFile = project.getFile(IPath.fromOSString(srcFile.getName()));
			dstFile.createLink(srcFile.toURI(), IResource.NONE, subMonitor.split(1));
		} else {
			IFolder dstFile = project.getFolder(IPath.fromOSString(srcFile.getName()));
			dstFile.createLink(srcFile.toURI(), IResource.NONE, subMonitor.split(1));
		}

		// Link source from known source locations
		Map<String, IPath> sourceMap = importSourceArchives(project, model, IMPORT_BINARY_WITH_LINKS,
				subMonitor.split(1));

		// Import additional source files such as schema files for easy access,
		// see bug 139161
		importAdditionalSourceFiles(project, model, subMonitor.split(1));

		// Extract the required bundle files and modify the imported manifest to
		// have the correct classpath
		importRequiredPluginFiles(project, model, subMonitor.split(1));
		modifyBundleClasspathHeader(project, model);

		// Mark the project as binary
		RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
		project.setPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY, PDECore.BINARY_PROJECT_VALUE);

		return sourceMap;
	}

	/**
	 * Imports the contents of the plugin and imports source files to source folders that will be compiled.
	 * @param project destination project of the import
	 * @param model model representing the plugin to import
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem completing the import
	 */
	private void importAsSource(IProject project, IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 4);

			// Extract the source, track build entries and package locations
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(PDEProject.getBuildProperties(project));
			Map<IPath, IPath> packageLocations = new HashMap<>(); // maps package path to a src folder
			boolean sourceFound = extractSourceFolders(project, model, buildModel, packageLocations,
					subMonitor.split(1));
			// If no source was found previously, check if there was a source folder (src) inside the binary plug-in
			if (!sourceFound) {
				sourceFound = handleInternalSource(model, buildModel, packageLocations);
			}

			// Extract additional non-java files from the source bundles
			importAdditionalSourceFiles(project, model, subMonitor.split(1));

			// Extract the binary plug-in (for non-class files)
			// Use the package locations map to put files that belong in the package directory structure into the proper source directory
			if (isJARd(model)) {
				SubMonitor iterationMonitor = subMonitor.split(1);
				try (ZipFile zip = new ZipFile(new File(model.getInstallLocation()))) {
					ZipFileStructureProvider provider = new ZipFileStructureProvider(zip);
					Map<IPath, List<Object>> collected = new HashMap<>();
					PluginImportHelper.collectBinaryFiles(provider, provider.getRoot(), packageLocations, collected);
					iterationMonitor.setWorkRemaining(collected.size());
					for (Entry<IPath, List<Object>> entry : collected.entrySet()) {
						IPath currentDestination = entry.getKey();
						IPath destination = project.getFullPath();
						destination = destination.append(currentDestination);
						PluginImportHelper.importContent(provider.getRoot(), destination, provider,
								entry.getValue(), iterationMonitor.split(1));

					}
				}
			} else {
				SubMonitor iterationMonitor = subMonitor.split(1);
				Map<IPath, List<Object>> collected = new HashMap<>();
				File srcFile = new File(model.getInstallLocation());
				PluginImportHelper.collectBinaryFiles(FileSystemStructureProvider.INSTANCE, srcFile, packageLocations, collected);
				iterationMonitor.setWorkRemaining(collected.size());
				for (Entry<IPath, List<Object>> entry : collected.entrySet()) {
					IPath currentDestination = entry.getKey();
					IPath destination = project.getFullPath();
					destination = destination.append(currentDestination);
					PluginImportHelper.importContent(srcFile, destination, FileSystemStructureProvider.INSTANCE,
							entry.getValue(), iterationMonitor.split(1));

				}
			}

			// If the source bundle had a pre-configured build.properties file, do not overwrite with the generated file (Bug 391801)
			if (!PDEProject.getBuildProperties(project).exists()) {
				configureBinIncludes(buildModel, model, project);
				buildModel.save();
			}
			subMonitor.worked(1);

		} catch (IOException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
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
		monitor.beginTask("", 2); //$NON-NLS-1$
		IProject project = findProject(model.getPluginBase().getId());
		if (project.exists() || new File(project.getParent().getLocation().toFile(), project.getName()).exists()) {

			project = PDEPlugin.getWorkspace().getRoot().getProject(model.getPluginBase().getId());
			if (project.exists()) {
				File installLocation = new File(model.getInstallLocation());
				String projectName = installLocation.getName();
				int jarIndex = projectName.toLowerCase().lastIndexOf(".jar"); //$NON-NLS-1$
				if (jarIndex >= 0) {
					projectName = projectName.substring(0, jarIndex);
				}
				project = PDEPlugin.getWorkspace().getRoot().getProject(projectName);
				int index = 0;
				while (project.exists() == true) {
					index++;
					project = PDEPlugin.getWorkspace().getRoot().getProject(projectName + '_' + index);
				}
			}
		}

		project.create(monitor);
		if (!project.isOpen())
			project.open(monitor);

		// If we know that a previous project of the same name belonged to one
		// or more working sets, add the new project to them
		List<IWorkingSet> workingSets = fProjectWorkingSets.get(project.getName());
		if (workingSets != null) {
			for (IWorkingSet ws : workingSets) {
				IAdaptable newElement = project;
				IAdaptable[] projectAdaptables = ws.adaptElements(new IAdaptable[] { project });
				if (projectAdaptables.length > 0) {
					newElement = projectAdaptables[0];
				}
				IAdaptable[] currentElements = ws.getElements();
				IAdaptable[] newElements = new IAdaptable[currentElements.length + 1];
				System.arraycopy(currentElements, 0, newElements, 0, currentElements.length);
				newElements[currentElements.length] = newElement;
				ws.setElements(newElements);
			}
		}

		monitor.worked(1);

		return project;
	}

	private IProject findProject(String id) {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null && resource.exists()) {
				return resource.getProject();
			}
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
			for (IPluginLibrary library : libraries) {
				IResource res = project.findMember(library.getName());
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
		// Check the manager(s) for source
		if (getSourceManager(model) != null) {
			return true;
		}

		// Check for source inside the binary plug-in
		ZipFile zip = null;
		try {
			IImportStructureProvider provider;
			Object root;
			if (isJARd(model)) {
				zip = new ZipFile(new File(model.getInstallLocation()));
				provider = new ZipFileStructureProvider(zip);
				root = ((ZipFileStructureProvider) provider).getRoot();
			} else {
				provider = FileSystemStructureProvider.INSTANCE;
				root = new File(model.getInstallLocation());
			}
			List<?> children = provider.getChildren(root);
			for (Object child : children) {
				String label = provider.getLabel(child);
				if (label.equals(DEFAULT_SOURCE_DIR)) {
					return true;
				}
			}
		} catch (IOException e) {
			// Do nothing, any other problems will be caught during binary import
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {
				}
			}
		}

		return false;
	}

	/**
	 * Should be used inside this class rather than PDEPlugin.getSourceLocationManager as it checks if the alternate
	 * source manager has source for the given plug-in.  The alternate source manager is set if we are importing from
	 * a location other than the active target platform.  In that case we want to use source from the import location
	 * if available.  If this method returns <code>null</code> no seperate source feature/bundle could be found.  There
	 * may still be source stored internally in the plug-in.
	 *
	 * @return the most relevant source manager than contains source for the plug-in or <code>null</code> if no separate source could be found
	 */
	private SourceLocationManager getSourceManager(IPluginModelBase model) {
		// Check the alternate source manager first
		if (fAlternateSource != null) {
			// Check for a source bundle
			if (fAlternateSource.hasBundleManifestLocation(model.getPluginBase())) {
				return fAlternateSource;
			}

			// Check for an old style source plug-in
			String[] libraries = getLibraryNames(model);
			for (String library : libraries) {
				String zipName = ClasspathUtilCore.getSourceZipName(library);
				IPath srcPath = fAlternateSource.findSourcePath(model.getPluginBase(), IPath.fromOSString(zipName));
				if (srcPath != null) {
					return fAlternateSource;
				}
			}
		}

		// Check the pde default source manager second
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();

		// Check for a source bundle
		if (manager.hasBundleManifestLocation(model.getPluginBase())) {
			return manager;
		}

		// Check for an old style source plug-in
		String[] libraries = getLibraryNames(model);
		for (String library : libraries) {
			String zipName = ClasspathUtilCore.getSourceZipName(library);
			IPath srcPath = manager.findSourcePath(model.getPluginBase(), IPath.fromOSString(zipName));
			if (srcPath != null) {
				return manager;
			}
		}

		return null;
	}

	/**
	 * Starts a job that will build the workspace
	 */
	private void runBuildJob() {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) {
			@Override
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_AUTO_BUILD == family;
			}

			@Override
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
	private Map<String, IPath> importSourceArchives(IProject project, IPluginModelBase model, int mode, IProgressMonitor monitor) throws CoreException {
		String[] libraries = getLibraryNames(model);
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDEUIMessages.ImportWizard_operation_importingSource,
				libraries.length);

		Map<String, IPath> sourceMap = new HashMap<>(libraries.length);
		SourceLocationManager manager = getSourceManager(model);
		if (manager != null) {
			for (int i = 0; i < libraries.length; i++) {
				subMonitor.setWorkRemaining(libraries.length - i);
				String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
				IPluginBase pluginBase = model.getPluginBase();
				// check default locations
				IPath srcPath = manager.findSourcePath(pluginBase, IPath.fromOSString(zipName));
				if (srcPath != null) {
					zipName = srcPath.lastSegment();
					IPath dstPath = IPath.fromOSString(zipName);
					sourceMap.put(libraries[i], dstPath);
					if (project.findMember(dstPath) == null) {
						if (mode == IMPORT_BINARY) {
							PluginImportHelper.copyArchive(new File(srcPath.toOSString()), project.getFile(dstPath),
									subMonitor.split(1));
						} else if (mode == IMPORT_BINARY_WITH_LINKS) {
							IFile dstFile = project.getFile(dstPath);
							dstFile.createLink(srcPath, IResource.NONE, subMonitor.split(1));
						}
					}
				}
			}
		}
		return sourceMap;
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
	 * @return whether a source location was found
	 * @throws CoreException if there is a problem extracting the source or creating a build entry
	 */
	private boolean extractSourceFolders(IProject project, IPluginModelBase model, WorkspaceBuildModel buildModel, Map<IPath, IPath> packageLocations, IProgressMonitor monitor) throws CoreException {
		String[] libraries = getLibraryNames(model);
		SubMonitor subMonitor = SubMonitor.convert(monitor, PDEUIMessages.ImportWizard_operation_importingSource,
				libraries.length);

		SourceLocationManager manager = getSourceManager(model);
		if (manager != null) {

			// Check if we have new style individual source bundles
			if (manager.hasBundleManifestLocation(model.getPluginBase())) {
				File srcFile = manager.findSourcePlugin(model.getPluginBase());
				Set<String> sourceRoots = manager.findSourceRoots(model.getPluginBase());
				for (String library : libraries) {
					if (library.equals(DEFAULT_LIBRARY_NAME)) {
						// Need to pull out any java source that is not in
						// another source root
						IResource destination = project.getFolder(DEFAULT_SOURCE_DIR);
						if (!destination.exists()) {
							List<IPath> excludeFolders = new ArrayList<>(sourceRoots.size());
							for (String root : sourceRoots) {
								if (!root.equals(DEFAULT_LIBRARY_NAME)) {
									excludeFolders.add(IPath.fromOSString(root));
								}
							}
							Set<IPath> collectedPackages = new HashSet<>();
							PluginImportHelper.extractJavaSourceFromArchive(srcFile, excludeFolders,
									destination.getFullPath(), collectedPackages, subMonitor.split(1));
							addBuildEntry(buildModel, "source." + DEFAULT_LIBRARY_NAME, DEFAULT_SOURCE_DIR + "/"); //$NON-NLS-1$ //$NON-NLS-2$
							addPackageEntries(collectedPackages, IPath.fromOSString(DEFAULT_SOURCE_DIR), packageLocations);

						}
					} else if (sourceRoots.contains(getSourceDirName(library))) {
						IPath sourceDir = IPath.fromOSString(getSourceDirName(library));
						if (!project.getFolder(sourceDir).exists()) {
							Set<IPath> collectedPackages = new HashSet<>();
							PluginImportHelper.extractFolderFromArchive(srcFile, sourceDir, project.getFullPath(),
									collectedPackages, subMonitor.split(1));
							addBuildEntry(buildModel, "source." + library, sourceDir.toString()); //$NON-NLS-1$
							addPackageEntries(collectedPackages, sourceDir, packageLocations);
						}
					}
				}
				return true;
			}

			// Old style, zips in folders, determine the source zip
			// name/location and extract it to the project
			boolean sourceFound = false;
			for (String library : libraries) {
				String zipName = ClasspathUtilCore.getSourceZipName(library);
				IPath srcPath = manager.findSourcePath(model.getPluginBase(), IPath.fromOSString(zipName));
				if (srcPath != null) {
					sourceFound = true;
					IPath dstPath = IPath.fromOSString(getSourceDirName(library));
					IResource destination = project.getFolder(dstPath);
					if (!destination.exists()) {
						Set<IPath> collectedPackages = new HashSet<>();
						PluginImportHelper.extractArchive(new File(srcPath.toOSString()), destination.getFullPath(),
								collectedPackages, subMonitor.split(1));
						addBuildEntry(buildModel, "source." + library, dstPath.toString()); //$NON-NLS-1$
						addPackageEntries(collectedPackages, dstPath, packageLocations);
					}
				}
			}
			return sourceFound;
		}
		return false;
	}

	/**
	 * Looks inside the binary plug-in to see if source was packaged inside of a 'src' directory.  If found, the build model and
	 * package locations are updated with the appropriate information.  This method does not actually import the source as
	 * that is handled when the binary plug-in is extracted.
	 *
	 * @param model plug-in model being imported
	 * @param buildModel build model to update if source is found
	 * @param packageLocations package location map (package path to destination) to update if source is found
	 * @return true if source was found inside the binary plug-in, false otherwise
	 *
	 * @throws CoreException
	 * @throws ZipException
	 * @throws IOException
	 */
	private boolean handleInternalSource(IPluginModelBase model, WorkspaceBuildModel buildModel, Map<IPath, IPath> packageLocations) throws CoreException, ZipException, IOException {
		IImportStructureProvider provider;
		Object root;
		IPath prefixPath;
		IPath defaultSourcePath = IPath.fromOSString(DEFAULT_SOURCE_DIR);
		ZipFile zip = null;
		try {
			if (isJARd(model)) {
				zip = new ZipFile(new File(model.getInstallLocation()));
				provider = new ZipFileStructureProvider(zip);
				root = ((ZipFileStructureProvider) provider).getRoot();
				prefixPath = defaultSourcePath;
			} else {
				provider = FileSystemStructureProvider.INSTANCE;
				File rootFile = new File(model.getInstallLocation());
				root = rootFile;
				prefixPath = IPath.fromOSString(rootFile.getPath()).append(defaultSourcePath);
			}

			ArrayList<Object> collected = new ArrayList<>();
			PluginImportHelper.collectResourcesFromFolder(provider, root, defaultSourcePath, collected);
			if (!collected.isEmpty()) {
				Set<IPath> packages = new HashSet<>();
				PluginImportHelper.collectJavaPackages(provider, collected, prefixPath, packages);
				addPackageEntries(packages, defaultSourcePath, packageLocations);
				addBuildEntry(buildModel, "source." + DEFAULT_LIBRARY_NAME, DEFAULT_SOURCE_DIR + "/"); //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
			return false;
		} finally {
			if (zip != null) {
				zip.close();
			}
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
	private void addPackageEntries(Set<IPath> packages, IPath destination, Map<IPath, IPath> packageLocations) {
		for (IPath currentPackage : packages) {
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
	private void importAdditionalSourceFiles(IProject project, IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		SourceLocationManager manager = getSourceManager(model);
		if (manager != null) {
			File sourceLocation = manager.findSourcePlugin(model.getPluginBase());
			if (sourceLocation != null) {
				if (sourceLocation.isFile()) {
					ZipFile zip = null;
					try {
						zip = new ZipFile(sourceLocation);
						ZipFileStructureProvider provider = new ZipFileStructureProvider(zip);
						ArrayList<Object> collected = new ArrayList<>();
						PluginImportHelper.collectNonJavaNonBuildFiles(provider, provider.getRoot(), collected);
						PluginImportHelper.importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
					} catch (IOException e) {
						throw new CoreException(Status.error(e.getMessage(), e));
					} finally {
						if (zip != null) {
							try {
								zip.close();
							} catch (IOException e) {
							}
						}
					}
				} else {
					ArrayList<Object> collected = new ArrayList<>();
					PluginImportHelper.collectNonJavaNonBuildFiles(FileSystemStructureProvider.INSTANCE, sourceLocation, collected);
					PluginImportHelper.importContent(sourceLocation, project.getFullPath(), FileSystemStructureProvider.INSTANCE, collected, monitor);
				}
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
			ZipFile zip = null;
			try {
				zip = new ZipFile(new File(model.getInstallLocation()));
				ZipFileStructureProvider provider = new ZipFileStructureProvider(zip);
				ArrayList<Object> collected = new ArrayList<>();
				PluginImportHelper.collectRequiredBundleFiles(provider, provider.getRoot(), collected);
				PluginImportHelper.importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
			} catch (IOException e) {
				throw new CoreException(Status.error(e.getMessage(), e));
			} finally {
				if (zip != null) {
					try {
						zip.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			ArrayList<Object> collected = new ArrayList<>();
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
		HashMap<String, String> libraryDirs = getSourceDirectories(build);
		if (entry == null) {
			entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
			File location = new File(model.getInstallLocation());
			if (location.isDirectory()) {
				File[] files = location.listFiles();
				for (File file : files) {
					String token = file.getName();
					if ((project.findMember(token) == null) && (build.getEntry(IBuildEntry.JAR_PREFIX + token) == null))
						continue;
					if (file.isDirectory()) {
						token = token + "/"; //$NON-NLS-1$
						if (libraryDirs.containsKey(token))
							token = libraryDirs.get(token).toString();
					}
					entry.addToken(token);
				}
			} else {
				String[] tokens = PluginImportHelper.getTopLevelResources(location);
				for (String token : tokens) {
					IResource res = project.findMember(token);
					if ((res == null) && (build.getEntry(IBuildEntry.JAR_PREFIX + token) == null))
						continue;
					if ((res instanceof IFolder) && (libraryDirs.containsKey(token)))
						continue;
					entry.addToken(token);
				}
			}
			buildModel.getBuild().add(entry);
		}
	}

	private HashMap<String, String> getSourceDirectories(IBuild build) {
		HashMap<String, String> set = new HashMap<>();
		IBuildEntry[] entries = build.getBuildEntries();
		for (IBuildEntry entry : entries) {
			String name = entry.getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
				name = name.substring(7);
				String[] tokens = entry.getTokens();
				for (String token : tokens) {
					set.put(token, name);
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
		IFile file = PDEProject.getManifest(project);
		if (file.exists()) {
			WorkspaceBundleModel bmodel = new WorkspaceBundleModel(file);
			IBundle bundle = bmodel.getBundle();
			String classpath = bundle.getHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH);
			if (classpath == null) {
				bundle.setHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, ClasspathUtilCore.getFilename(base));
			} else {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, classpath);
					StringBuilder buffer = new StringBuilder();
					for (ManifestElement element : elements) {
						if (buffer.length() > 0) {
							buffer.append(","); //$NON-NLS-1$
							buffer.append(System.lineSeparator());
							buffer.append(" "); //$NON-NLS-1$
						}
						if (element.getValue().equals(".")) //$NON-NLS-1$
							buffer.append(ClasspathUtilCore.getFilename(base));
						else
							buffer.append(element.getValue());
					}
					bundle.setHeader(org.osgi.framework.Constants.BUNDLE_CLASSPATH, buffer.toString());
				} catch (BundleException e) {
				}
			}
			bmodel.save();
		}
	}

	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		// If there are class libraries we need a java nature
		if (model.getPluginBase().getLibraries().length > 0)
			return true;

		// If the plug-in exports packages or requires an execution environment it has java code
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			if (desc.getExecutionEnvironments().length > 0)
				return true;
			if (desc.getExportPackages().length > 0)
				return true;
		}

		// If the build.properties file has a default source folder we need a java nature
		IFile buildProperties = PDEProject.getBuildProperties(project);
		if (buildProperties.exists()) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildProperties);
			buildModel.load();
			IBuild build = buildModel.getBuild();
			if (build != null) {
				IBuildEntry buildEntry = build.getEntry("source.."); //$NON-NLS-1$
				if (buildEntry != null) {
					return true;
				}
			}
		}

		return false;
	}

	private void setProjectNatures(IProject project, IPluginModelBase model) throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (!desc.hasNature(PDE.PLUGIN_NATURE)) {
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
		}
		if (!desc.hasNature(JavaCore.NATURE_ID) && needsJavaNature(project, model)) {
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
		}

	}

	/**
	 * Gets the list of libraries from the model and returns an array of their expanded
	 * names.  Will add the default library name if no libraries are specified.
	 *
	 * <p>If run in dev mode (target workbench), and the plug-in is in the host workspace
	 * the library names will be replaced with 'bin/'.  See bug 294005.</p>
	 *
	 * @param model
	 * @return list of library names
	 */
	private String[] getLibraryNames(IPluginModelBase model) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		ArrayList<String> list = new ArrayList<>();
		for (IPluginLibrary library : libraries) {
			list.add(ClasspathUtilCore.expandLibraryName(library.getName()));
		}
		if (libraries.length == 0 && isJARd(model))
			list.add(DEFAULT_LIBRARY_NAME);
		return list.toArray(new String[list.size()]);
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

	/**
	 * Sets the import descriptions to use when importing from a repository.
	 *
	 * @param descriptions map of {@link IBundleImporter} to arrays of {@link ScmUrlImportDescription}.
	 */
	public void setImportDescriptions(Map<IBundleImporter, ScmUrlImportDescription[]> descriptions) {
		fImportDescriptions = descriptions;
	}

}
