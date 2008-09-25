/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsProcessor;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.*;

public class LauncherUtils {

	private static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
	private static final String FILE_NAME = "dep-timestamp.properties"; //$NON-NLS-1$
	private static Properties fLastRun;

	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	public final static Shell getActiveShell() {
		IWorkbenchWindow window = PDEPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PDEPlugin.getDefault().getWorkbench().getWorkbenchWindows();
			if (windows.length > 0)
				return windows[0].getShell();
		} else
			return window.getShell();
		return getDisplay().getActiveShell();
	}

	public static boolean clearWorkspace(ILaunchConfiguration configuration, String workspace, IProgressMonitor monitor) throws CoreException {

		// If the workspace is not defined, there is no workspace to clear
		// What will happen is that the workspace chooser dialog will be 
		// brought up because no -data parameter will be specified on the 
		// launch
		if (workspace.length() == 0) {
			monitor.done();
			return true;
		}

		// Check if the workspace is already in use, if so, open a message and stop the launch before clearing
		boolean isLocked = false;
		try {
			BundleContext context = PDECore.getDefault().getBundleContext();
			ServiceReference[] references = context.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
			if (references.length > 0) {
				Object service = context.getService(references[0]);
				if (service instanceof Location) {
					URL workspaceURL = new Path(workspace).toFile().toURI().toURL();
					Location targetLocation = ((Location) service).createLocation(null, workspaceURL, false);
					targetLocation.setURL(targetLocation.getDefault(), false);
					isLocked = targetLocation.isLocked();
				}
			}
		} catch (InvalidSyntaxException e) {
			PDECore.log(e);
			isLocked = false;
		} catch (MalformedURLException e) {
			PDECore.log(e);
			isLocked = false;
		} catch (IOException e) {
			PDECore.log(e);
			isLocked = false;
		}

		if (isLocked) {
			generateErrorDialog(PDEUIMessages.LauncherUtils_workspaceLocked, NLS.bind(PDEUIMessages.LauncherUtils_cannotLaunchApplication, workspace));
			monitor.done();
			return false;
		}

		File workspaceFile = new Path(workspace).toFile().getAbsoluteFile();
		if (configuration.getAttribute(IPDELauncherConstants.DOCLEAR, false) && workspaceFile.exists()) {
			if (configuration.getAttribute(IPDELauncherConstants.ASKCLEAR, true)) {
				int result;
				if (configuration.getAttribute(IPDEUIConstants.DOCLEARLOG, false)) {
					result = generateDialog(PDEUIMessages.LauncherUtils_clearLogFile);
				} else {
					result = generateDialog(NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace, workspaceFile.getPath()));
				}

				if (result == 2 /*Cancel Button*/|| result == -1 /*Dialog close button*/) {
					monitor.done();
					return false;
				} else if (result == 0) {
					if (configuration.getAttribute(IPDEUIConstants.DOCLEARLOG, false)) {
						LauncherUtils.clearWorkspaceLog(workspace);
					} else {
						CoreUtility.deleteContent(workspaceFile);
					}
				}
			}
		}

		monitor.done();
		return true;
	}

	public static boolean generateConfigIni() {
		String message = PDEUIMessages.LauncherUtils_generateConfigIni;
		return generateDialog(message) == 0;
	}

	/**
	 * Creates a message dialog using a syncExec in case we are launching in the background.
	 * Dialog will be a question dialog with Yes, No and Cancel buttons.
	 * @param message Message to use in the dialog
	 * @return int representing the button clicked (-1 or 2 for cancel, 0 for yes, 1 for no).
	 */
	private static int generateDialog(final String message) {
		final int[] result = new int[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEUIMessages.LauncherUtils_title;
				MessageDialog dialog = new MessageDialog(getActiveShell(), title, null, message, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				result[0] = dialog.open();
			}
		});
		return result[0];
	}

	private static void generateErrorDialog(final String title, final String message) {
		getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog dialog = new MessageDialog(getActiveShell(), title, null, message, MessageDialog.ERROR, new String[] {IDialogConstants.OK_LABEL}, 0);
				dialog.open();
			}
		});
	}

	public static void validateProjectDependencies(ILaunchConfiguration launch, final IProgressMonitor monitor) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (!store.getBoolean(IPreferenceConstants.PROP_AUTO_MANAGE))
			return;

		String timeStamp;
		boolean useDefault, autoAdd;
		try {
			timeStamp = launch.getAttribute(TIMESTAMP, "0"); //$NON-NLS-1$
			autoAdd = launch.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			useDefault = launch.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			useDefault |= launch.getAttribute(IPDELauncherConstants.USEFEATURES, false);
			final ArrayList projects = new ArrayList();
			if (useDefault)
				handleUseDefault(timeStamp, projects);
			else if (autoAdd)
				handleDeselectedPlugins(launch, timeStamp, projects);
			else
				handleSelectedPlugins(launch, timeStamp, projects);

			if (!projects.isEmpty())
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						OrganizeManifestsProcessor processor = new OrganizeManifestsProcessor(projects);
						initializeProcessor(processor);
						try {
							Change change = processor.createChange(monitor);
							change.perform(monitor);
							// update table for each project with current time stamp
							Properties table = getLastRun();
							String ts = Long.toString(System.currentTimeMillis());
							Iterator it = projects.iterator();
							while (it.hasNext())
								table.put(((IProject) it.next()).getName(), ts);
						} catch (OperationCanceledException e) {
						} catch (CoreException e) {
						}
					}
				});

			ILaunchConfigurationWorkingCopy wc = null;
			if (launch.isWorkingCopy())
				wc = (ILaunchConfigurationWorkingCopy) launch;
			else
				wc = launch.getWorkingCopy();
			wc.setAttribute(TIMESTAMP, Long.toString(System.currentTimeMillis()));
			wc.doSave();
		} catch (CoreException e) {
		}
	}

	private static void initializeProcessor(OrganizeManifestsProcessor processor) {
		processor.setAddMissing(false);
		processor.setRemoveUnresolved(false);
		processor.setModifyDep(false);
		processor.setRemoveLazy(false);
		processor.setRemoveUselessFiles(false);
		processor.setAddDependencies(true);
		processor.setCalculateUses(false);
		processor.setMarkInternal(false);
		processor.setPrefixIconNL(false);
		processor.setUnusedDependencies(false);
		processor.setUnusedKeys(false);
	}

	private static String getTimeStamp(IProject project) {
		IJavaProject jp = JavaCore.create(project);
		try {
			long timeStamp = 0;
			IClasspathEntry[] entries = jp.getResolvedClasspath(true);
			for (int i = 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					File file;
					IPath location = entries[i].getOutputLocation();
					if (location == null)
						location = jp.getOutputLocation();
					IResource res = project.getWorkspace().getRoot().findMember(location);
					IPath path = res == null ? null : res.getLocation();
					if (path == null)
						continue;
					file = path.toFile();
					Stack files = new Stack();
					files.push(file);
					while (!files.isEmpty()) {
						file = (File) files.pop();
						if (file.isDirectory()) {
							File[] children = file.listFiles();
							if (children != null) {
								for (int j = 0; j < children.length; j++)
									files.push(children[j]);
							}
						} else if (file.getName().endsWith(".class") && timeStamp < file.lastModified()) //$NON-NLS-1$
							timeStamp = file.lastModified();
					}
				}
			}
			String[] otherFiles = {ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR, ICoreConstants.BUILD_FILENAME_DESCRIPTOR};
			for (int i = 0; i < otherFiles.length; i++) {
				IResource file = project.getFile(otherFiles[i]);
				if (file != null) {
					long fileTimeStamp = file.getRawLocation().toFile().lastModified();
					if (timeStamp < fileTimeStamp)
						timeStamp = fileTimeStamp;
				}
			}
			return Long.toString(timeStamp);
		} catch (JavaModelException e) {
		}
		return "0"; //$NON-NLS-1$
	}

	private static void handleUseDefault(String launcherTimeStamp, ArrayList projects) {
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			if (!WorkspaceModelManager.isPluginProject(projs[i]))
				continue;
			String timestamp = getTimeStamp(projs[i]);
			if (timestamp.compareTo(launcherTimeStamp) > 0 && shouldAdd(projs[i], launcherTimeStamp, timestamp))
				projects.add(projs[i]);
		}
	}

	private static void handleSelectedPlugins(ILaunchConfiguration config, String timeStamp, ArrayList projects) throws CoreException {
		Set selectedPlugins = LaunchPluginValidator.parsePlugins(config, IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS);
		Iterator it = selectedPlugins.iterator();
		while (it.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) it.next();
			IResource res = model.getUnderlyingResource();
			if (res != null) {
				IProject project = res.getProject();
				String projTimeStamp = getTimeStamp(project);
				if (projTimeStamp.compareTo(timeStamp) > 0 && shouldAdd(project, timeStamp, projTimeStamp))
					projects.add(project);
			}
		}
	}

	private static void handleDeselectedPlugins(ILaunchConfiguration config, String launcherTimeStamp, ArrayList projects) throws CoreException {
		Set deSelectedPlugins = LaunchPluginValidator.parsePlugins(config, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			if (!WorkspaceModelManager.isPluginProject(projs[i]))
				continue;
			IPluginModelBase base = PluginRegistry.findModel(projs[i]);
			if (base == null || base != null && deSelectedPlugins.contains(base))
				continue;
			String timestamp = getTimeStamp(projs[i]);
			if (timestamp.compareTo(launcherTimeStamp) > 0 && shouldAdd(projs[i], launcherTimeStamp, timestamp))
				projects.add(projs[i]);
		}
	}

	public static final void shutdown() {
		if (fLastRun == null)
			return;
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File(getDirectory(), FILE_NAME));
			fLastRun.store(stream, "Cached timestamps"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}
		}
	}

	private static File getDirectory() {
		IPath path = PDECore.getDefault().getStateLocation().append(".cache"); //$NON-NLS-1$
		File directory = new File(path.toOSString());
		if (!directory.exists() || !directory.isDirectory())
			directory.mkdirs();
		return directory;
	}

	private static Properties getLastRun() {
		if (fLastRun == null) {
			fLastRun = new Properties();
			FileInputStream fis = null;
			try {
				File file = new File(getDirectory(), FILE_NAME);
				if (file.exists()) {
					fis = new FileInputStream(file);
					fLastRun.load(fis);
					fis.close();
				}
			} catch (IOException e) {
				PDECore.logException(e);
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e1) {
				}
			}
		}
		return fLastRun;
	}

	private static boolean shouldAdd(IProject proj, String launcherTS, String fileSystemTS) {
		String projTS = (String) getLastRun().get(proj.getName());
		if (projTS == null)
			return true;
		return ((projTS.compareTo(launcherTS) < 0) || (projTS.compareTo(fileSystemTS) < 0));
	}

	public static boolean requiresUI(ILaunchConfiguration configuration) {
		try {
			String projectID = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			if (projectID.length() > 0) {
				IResource project = PDEPlugin.getWorkspace().getRoot().findMember(projectID);
				if (project instanceof IProject) {
					IPluginModelBase model = PluginRegistry.findModel((IProject) project);
					if (model != null) {
						Set plugins = DependencyManager.getSelfAndDependencies(model);
						return plugins.contains("org.eclipse.swt"); //$NON-NLS-1$
					}
				}
			}
		} catch (CoreException e) {
		}
		return true;
	}

	public static boolean clearWorkspaceLog(String workspace) {
		File logFile = new File(workspace, ".metadata" + File.separator + ".log"); //$NON-NLS-1$ //$NON-NLS-2$
		if (logFile != null && logFile.exists()) {
			return logFile.delete();
		}
		return true;
	}

	public static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	}

}
