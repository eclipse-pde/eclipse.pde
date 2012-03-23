/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.*;

public class LauncherUtils {
	public static final int WORKSPACE_LOCKED = 2000;
	public static final int CLEAR_LOG = 2001;
	public static final int DELETE_WORKSPACE = 2002;
	public static final int GENERATE_CONFIG_INI = 2003;
	public static final int ORGANIZE_MANIFESTS = 2004;
	public static final int SELECT_WORKSPACE_FIELD = 2005;

	private static final String TIMESTAMP = "timestamp"; //$NON-NLS-1$
	private static final String FILE_NAME = "dep-timestamp.properties"; //$NON-NLS-1$
	private static Properties fLastRun;
	/**
	 * Stores the last known launch mode so status handlers can open the correct launch configuration dialog
	 * @see LauncherUtils#setLastLaunchMode(String)
	 */
	private static String fLastLaunchMode;

	public static boolean clearWorkspace(ILaunchConfiguration configuration, String workspace, IProgressMonitor monitor) throws CoreException {

		// If the workspace is not defined, there is no workspace to clear
		// What will happen is that the workspace chooser dialog will be 
		// brought up because no -data parameter will be specified on the 
		// launch
		if (workspace.length() == 0) {
			if (monitor != null) {
				monitor.done();
			}
			return true;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 100);

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
					targetLocation.set(targetLocation.getDefault(), false);
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

		subMon.setWorkRemaining(90);
		if (subMon.isCanceled()) {
			return false;
		}

		if (isLocked) {
			Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, WORKSPACE_LOCKED, null, null);
			IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
			if (statusHandler != null)
				statusHandler.handleStatus(status, new Object[] {workspace, configuration, fLastLaunchMode});
			subMon.done();
			if (monitor != null) {
				monitor.done();
			}
			return false;
		}

		File workspaceFile = new Path(workspace).toFile().getAbsoluteFile();
		if (configuration.getAttribute(IPDELauncherConstants.DOCLEAR, false) && workspaceFile.exists()) {
			if (configuration.getAttribute(IPDELauncherConstants.ASKCLEAR, true)) {
				int result = 0;
				if (configuration.getAttribute(IPDEConstants.DOCLEARLOG, false)) {
					Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, CLEAR_LOG, null, null);
					IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
					if (statusHandler != null)
						result = ((Integer) statusHandler.handleStatus(status, null)).intValue();
				} else {
					Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, DELETE_WORKSPACE, null, null);
					IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
					if (statusHandler != null)
						result = ((Integer) statusHandler.handleStatus(status, workspaceFile.getPath())).intValue();
				}

				if (result == 2 /*Cancel Button*/|| result == -1 /*Dialog close button*/) {
					subMon.done();
					if (monitor != null) {
						monitor.done();
					}
					return false;
				} else if (result == 0) {
					if (configuration.getAttribute(IPDEConstants.DOCLEARLOG, false)) {
						LauncherUtils.clearWorkspaceLog(workspace);
					} else {
						CoreUtility.deleteContent(workspaceFile, subMon.newChild(90));
					}
				}
			} else if (configuration.getAttribute(IPDEConstants.DOCLEARLOG, false)) {
				LauncherUtils.clearWorkspaceLog(workspace);
			} else {
				CoreUtility.deleteContent(workspaceFile, subMon.newChild(90));
			}
		}

		if (subMon.isCanceled()) {
			return false;
		}

		subMon.done();
		if (monitor != null) {
			monitor.done();
		}

		return true;
	}

	public static boolean generateConfigIni() throws CoreException {
		Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, GENERATE_CONFIG_INI, null, null);
		IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
		return statusHandler == null ? true : ((Boolean) statusHandler.handleStatus(status, null)).booleanValue();
	}

	public static void validateProjectDependencies(ILaunchConfiguration launch, final IProgressMonitor monitor) {
		PDEPreferencesManager store = PDELaunchingPlugin.getDefault().getPreferenceManager();
		if (!store.getBoolean(org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants.PROP_AUTO_MANAGE))
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

			// If the set of projects being launched has changed, offer to organize the manifests
			if (!projects.isEmpty()) {
				Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, ORGANIZE_MANIFESTS, null, null);
				IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
				if (statusHandler != null)
					statusHandler.handleStatus(status, new Object[] {projects, monitor, getLastRun()});

				// Store the timestamp so we can avoid repeatedly organizing the same manifest files
				ILaunchConfigurationWorkingCopy wc = null;
				if (launch.isWorkingCopy())
					wc = (ILaunchConfigurationWorkingCopy) launch;
				else
					wc = launch.getWorkingCopy();
				wc.setAttribute(TIMESTAMP, Long.toString(System.currentTimeMillis()));
				wc.doSave();
			}

		} catch (CoreException e) {
		}
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
			IFile[] otherFiles = new IFile[] {PDEProject.getManifest(project), PDEProject.getBuildProperties(project)};
			for (int i = 0; i < otherFiles.length; i++) {
				IFile file = otherFiles[i];
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
		Map selectedPlugins = BundleLauncherHelper.getWorkspaceBundleMap(config, null, IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS);
		Iterator it = selectedPlugins.keySet().iterator();
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
		Map deSelectedPlugins = BundleLauncherHelper.getWorkspaceBundleMap(config, null, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
		IProject[] projs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projs.length; i++) {
			if (!WorkspaceModelManager.isPluginProject(projs[i]))
				continue;
			IPluginModelBase base = PluginRegistry.findModel(projs[i]);
			if (base == null || deSelectedPlugins.containsKey(base))
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
				IResource project = PDELaunchingPlugin.getWorkspace().getRoot().findMember(projectID);
				if (project instanceof IProject) {
					IPluginModelBase model = PluginRegistry.findModel((IProject) project);
					if (model != null) {
						Set plugins = DependencyManager.getSelfAndDependencies(model, null);
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
		if (logFile.exists()) {
			return logFile.delete();
		}
		return true;
	}

	public static IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PDELaunchingPlugin.getPluginId(), IStatus.OK, message, null);
	}

	/**
	 * Updates the stores launch mode.  This should be called on any PDE Eclipse launch.  The launch mode
	 * is passed to the status handler so it can open the correct launch configuration dialog
	 * 
	 * @param launchMode last known launch mode, see {@link ILaunch#getLaunchMode()}
	 */
	public static void setLastLaunchMode(String launchMode) {
		fLastLaunchMode = launchMode;
	}
}
