/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.core.*;

public class WorkbenchLaunchConfigurationDelegate
	implements ILaunchConfigurationDelegate, ILauncherSettings {
	private static final String KEY_NO_JRE =
		"WorkbenchLauncherConfigurationDelegate.noJRE";
	private static final String KEY_JRE_PATH_NOT_FOUND =
		"WorkbenchLauncherConfigurationDelegate.jrePathNotFound";
	private static final String KEY_STARTING =
		"WorkbenchLauncherConfigurationDelegate.starting";
	private static final String KEY_NO_BOOT =
		"WorkbenchLauncherConfigurationDelegate.noBoot";
	private static final String KEY_BROKEN_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.brokenPlugins";
	private static final String KEY_PROBLEMS_DELETING =
		"WorkbenchLauncherConfigurationDelegate.problemsDeleting";
	private static final String KEY_TITLE =
		"WorkbenchLauncherConfigurationDelegate.title";
	private static final String KEY_SLIMLAUNCHER =
		"WorkbenchLauncherConfigurationDelegate.slimlauncher";
	private static final String KEY_DELETE_WORKSPACE =
		"WorkbenchLauncherConfigurationDelegate.confirmDeleteWorkspace";
	private static final String KEY_DUPLICATES =
		"WorkbenchLauncherConfigurationDelegate.duplicates";
	private static final String KEY_DUPLICATE_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.duplicatePlugins";

	private Vector duplicates = new Vector();

	/*
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		final String vmArgs = configuration.getAttribute(VMARGS, "");
		final String progArgs = configuration.getAttribute(PROGARGS, "");
		final String appName =
			configuration.getAttribute(APPLICATION, (String) null);
		final String data =
			configuration.getAttribute(LOCATION + "0", (String) null);
		final boolean tracing = configuration.getAttribute(TRACING, false);
		final boolean clearWorkspace =
			configuration.getAttribute(DOCLEAR, false);
		final boolean showSplash =
			configuration.getAttribute(SHOW_SPLASH, true);
		final IPluginModelBase[] plugins =
			getPluginsFromConfiguration(configuration);

		if (duplicates.size() > 0) {
			if (!continueRunning()) {
				launch.terminate();
				return;
			}
		}

		String vmInstallName =
			configuration.getAttribute(
				VMINSTALL,
				BasicLauncherTab.getDefaultVMInstallName());
		IVMInstall[] vmInstallations = BasicLauncherTab.getAllVMInstances();
		IVMInstall launcher = null;

		if (monitor == null)
			monitor = new NullProgressMonitor();

		for (int i = 0; i < vmInstallations.length; i++) {
			if (vmInstallName.equals(vmInstallations[i].getName())) {
				launcher = vmInstallations[i];
				break;
			}
		}

		if (launcher == null) {
			monitor.setCanceled(true);
			throw new CoreException(
				createErrorStatus(
					PDEPlugin.getFormattedMessage(KEY_NO_JRE, vmInstallName)));
		}
		if (!launcher.getInstallLocation().exists()) {
			monitor.setCanceled(true);
			throw new CoreException(
				createErrorStatus(
					PDEPlugin.getResourceString(KEY_JRE_PATH_NOT_FOUND)));
		}

		validatePlugins(plugins, monitor);
		IVMRunner runner = launcher.getVMRunner(mode);
		ExecutionArguments args = new ExecutionArguments(vmArgs, progArgs);
		IPath path = new Path(data);
		
		doLaunch(
			launch,
			configuration,
			mode,
			runner,
			path,
			clearWorkspace,
			showSplash,
			args,
			plugins,
			appName,
			tracing,
			monitor);
	}

	private IPluginModelBase[] getPluginsFromConfiguration(ILaunchConfiguration config)
		throws CoreException {
		boolean useDefault = config.getAttribute(USECUSTOM, true);

		ArrayList deselectedWSPlugins = new ArrayList();

		String wstring = config.getAttribute(WSPROJECT, (String) null);
		String exstring = config.getAttribute(EXTPLUGINS, (String) null);

		if (wstring != null) {
			StringTokenizer tok =
				new StringTokenizer(wstring, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselectedWSPlugins.add(tok.nextToken());
			}
		}
		AdvancedLauncherTab.ExternalStates exstates =
			new AdvancedLauncherTab.ExternalStates();
		if (exstring != null) {
			exstates.parseStates(exstring);
		}

		ArrayList wsList = new ArrayList();

		IPluginModelBase[] wsmodels = AdvancedLauncherTab.getWorkspacePlugins();
		for (int i = 0; i < wsmodels.length; i++) {
			IPluginModelBase model = wsmodels[i];
			if (useDefault
				|| !deselectedWSPlugins.contains(model.getPluginBase().getId()))
				wsList.add(model);
		}
		IPluginModelBase[] exmodels = AdvancedLauncherTab.getExternalPlugins();
		ArrayList exList = new ArrayList();
		for (int i = 0; i < exmodels.length; i++) {
			IPluginModelBase model = exmodels[i];
			if (useDefault) {
				if (model.isEnabled())
					exList.add(model);
			} else {
				AdvancedLauncherTab.ExternalState es =
					exstates.getState(model.getPluginBase().getId());
				if (es != null) {
					if (es.state)
						exList.add(model);
				} else if (model.isEnabled())
					exList.add(model);
			}
		}
		ArrayList result = new ArrayList();
		mergeWithoutDuplicates(wsList, exList, result);
		IPluginModelBase[] plugins =
			(IPluginModelBase[]) result.toArray(
				new IPluginModelBase[result.size()]);
		return plugins;
	}

	private void mergeWithoutDuplicates(
		ArrayList wsmodels,
		ArrayList exmodels,
		ArrayList result) {

		for (int i = 0; i < wsmodels.size(); i++) {
			if (((IPluginModelBase) wsmodels.get(i)).getPluginBase().getId()
				!= null)
				result.add(wsmodels.get(i));
		}
		duplicates = new Vector();
		for (int i = 0; i < exmodels.size(); i++) {
			IPluginModelBase exmodel = (IPluginModelBase) exmodels.get(i);
			boolean duplicate = false;
			for (int j = 0; j < wsmodels.size(); j++) {
				IPluginModelBase wsmodel = (IPluginModelBase) wsmodels.get(j);
				if (wsmodel.getPluginBase().getId() != null
					&& exmodel.getPluginBase().getId() != null) {
					if (isDuplicate(wsmodel, exmodel)) {
						duplicates.add(
							exmodel.getPluginBase().getId()
								+ " ("
								+ exmodel.getPluginBase().getVersion()
								+ ")");
						duplicate = true;
						break;
					}
				}
			}
			if (!duplicate)
				result.add(exmodel);
		}
	}

	private boolean isDuplicate(
		IPluginModelBase wsmodel,
		IPluginModelBase exmodel) {
		if (!wsmodel.isLoaded() || !exmodel.isLoaded())
			return false;
		return wsmodel.getPluginBase().getId().equalsIgnoreCase(
			exmodel.getPluginBase().getId());
	}

	private void validatePlugins(
		IPluginModelBase[] plugins,
		IProgressMonitor monitor)
		throws CoreException {
		ArrayList entries = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			IPluginModelBase model = plugins[i];
			if (model.isLoaded() == false) {
				String message = model.getInstallLocation();
				if (model.getUnderlyingResource() != null)
					message =
						model.getUnderlyingResource().getProject().getName();
				Status status =
					new Status(
						IStatus.WARNING,
						PDEPlugin.getPluginId(),
						IStatus.OK,
						message,
						null);
				entries.add(status);
			}
		}
		if (entries.size() > 0) {
			IStatus[] children =
				(IStatus[]) entries.toArray(new IStatus[entries.size()]);
			String message = PDEPlugin.getResourceString(KEY_BROKEN_PLUGINS);
			final MultiStatus status =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					children,
					message,
					null);
			//monitor.setCanceled(true);
			//throw new CoreException(status);
			Display display = getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					String title = PDEPlugin.getResourceString(KEY_TITLE);
					ErrorDialog.openError(
						PDEPlugin.getActiveWorkbenchShell(),
						title,
						null,
						status);
				}
			});
		}
	}

	private boolean doLaunch(
		ILaunch launch,
		ILaunchConfiguration config,
		String mode,
		IVMRunner runner,
		IPath targetWorkspace,
		boolean clearWorkspace,
		boolean showSplash,
		ExecutionArguments args,
		IPluginModelBase[] plugins,
		String appname,
		boolean tracing,
		IProgressMonitor monitor)
		throws CoreException {

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(PDEPlugin.getResourceString(KEY_STARTING), 3);
		try {
			File propertiesFile =
				TargetPlatform.createPropertiesFile(plugins, targetWorkspace);
			String[] vmArgs = args.getVMArgumentsArray();
			String[] progArgs = args.getProgramArgumentsArray();

			int exCount = 8;
			if (tracing)
				exCount += 2;
			if (showSplash)
				exCount += 2;

			String[] fullProgArgs = new String[progArgs.length + exCount];
			int i = 0;
			fullProgArgs[i++] = "-application";
			fullProgArgs[i++] = appname;
			fullProgArgs[i++] = "-plugins";
			fullProgArgs[i++] = "file:" + propertiesFile.getPath();
			fullProgArgs[i++] = "-dev";
			fullProgArgs[i++] = getBuildOutputFolders();
			fullProgArgs[i++] = "-data";
			fullProgArgs[i++] = targetWorkspace.toOSString();
			if (showSplash) {
				fullProgArgs[i++] = "-showsplash";
				fullProgArgs[i++] =
					computeShowsplashArgument(
						new SubProgressMonitor(monitor, 1));
			}
			if (tracing) {
				fullProgArgs[i++] = "-debug";
				fullProgArgs[i++] = getTracingFileArgument(config);
			}
			System.arraycopy(
				progArgs,
				0,
				fullProgArgs,
				exCount,
				progArgs.length);

			String[] classpath = constructClasspath(plugins);
			if (classpath == null) {
				String message = PDEPlugin.getResourceString(KEY_NO_BOOT);
				monitor.setCanceled(true);
				throw new CoreException(createErrorStatus(message));
			}

			VMRunnerConfiguration runnerConfig =
				new VMRunnerConfiguration(
					"org.eclipse.core.launcher.Main",
					classpath);
			runnerConfig.setVMArguments(vmArgs);
			runnerConfig.setProgramArguments(fullProgArgs);

			File workspaceFile = targetWorkspace.toFile();
			if (clearWorkspace && workspaceFile.exists()) {
				if (confirmDeleteWorkspace(workspaceFile)) {
					try {
						deleteContent(workspaceFile);
					} catch (IOException e) {
						String message =
							PDEPlugin.getResourceString(KEY_PROBLEMS_DELETING);
						showWarningDialog(message);
					}
				}
			}
			monitor.worked(1);
			if (monitor.isCanceled()) {
				return false;
			}
			runner.run(runnerConfig, launch, monitor);
			monitor.worked(1);
			ISourceLocator sourceLocator = constructSourceLocator(plugins);
			launch.setSourceLocator(sourceLocator);
		} finally {
			monitor.done();
		}
		return true;
	}

	private String getBuildOutputFolders() {
		IPluginModelBase[] wsmodels = AdvancedLauncherTab.getWorkspacePlugins();
		HashSet set = new HashSet();
		set.add(new Path("bin"));
		for (int i = 0; i < wsmodels.length; i++) {
			IProject project = wsmodels[i].getUnderlyingResource().getProject();
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					set.add(
						JavaCore
							.create(project)
							.getOutputLocation()
							.removeFirstSegments(
							1));
				}
			} catch (JavaModelException e) {
			} catch (CoreException e) {
			}
		}
		StringBuffer result = new StringBuffer();
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			String folder = iter.next().toString();
			result.append(folder);
			if (iter.hasNext())
				result.append(",");
		}
		return result.toString();
	}

	private String computeShowsplashArgument(IProgressMonitor monitor) {
		IPath eclipseHome = ExternalModelManager.getEclipseHome(monitor);
		IPath fullPath = eclipseHome.append("eclipse");
		return fullPath.toOSString() + " -showsplash 600";
	}

	private String getTracingFileArgument(ILaunchConfiguration config) {
		TracingOptionsManager mng =
			PDECore.getDefault().getTracingOptionsManager();
		Map options;
		try {
			options =
				config.getAttribute(
					ILauncherSettings.TRACING_OPTIONS,
					mng.getTracingTemplateCopy());
		} catch (CoreException e) {
			return "";
		}
		mng.save(options);
		String optionsFileName = mng.getTracingFileName();
		String tracingArg;
		if (SWT.getPlatform().equals("motif"))
			tracingArg = "file:" + optionsFileName;
		// defect 17661
		else if (SWT.getPlatform().equals("gtk"))
			tracingArg = "file://localhost" + optionsFileName;
		else
			tracingArg = "\"file:" + optionsFileName + "\"";
		return tracingArg;
	}

	private void deleteContent(File curr) throws IOException {
		if (curr.isDirectory()) {
			File[] children = curr.listFiles();
			for (int i = 0; i < children.length; i++) {
				deleteContent(children[i]);
			}
		}
		curr.delete();
	}

	private Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	private IStatus createErrorStatus(String message) {
		return new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.OK,
			message,
			null);
	}

	private boolean continueRunning() {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				StringBuffer message =
					new StringBuffer(
						PDEPlugin.getFormattedMessage(
							KEY_DUPLICATES,
							new Integer(duplicates.size()).toString()));
				if (duplicates.size() <= 5) {
					String lineSeparator = System.getProperty("line.separator");
					message.append(
						lineSeparator
							+ lineSeparator
							+ PDEPlugin.getResourceString(KEY_DUPLICATE_PLUGINS)
							+ ":"
							+ lineSeparator);
					for (int i = 0; i < duplicates.size(); i++)
						message.append(duplicates.get(i) + lineSeparator);
				}
				result[0] =
					MessageDialog.openConfirm(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString(KEY_TITLE),
						message.toString());
			}
		});
		return result[0];
	}

	private boolean confirmDeleteWorkspace(final File workspaceFile) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				String message =
					PDEPlugin.getFormattedMessage(
						KEY_DELETE_WORKSPACE,
						workspaceFile.getPath());
				result[0] =
					MessageDialog.openQuestion(
						PDEPlugin.getActiveWorkbenchShell(),
						title,
						message);
			}
		});
		return result[0];
	}

	private void showWarningDialog(final String message) {
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				MessageDialog.openWarning(
					PDEPlugin.getActiveWorkbenchShell(),
					title,
					message);
			}
		});
	}

	/**
	 * Constructs a classpath with the slimlauncher and the boot plugin (org.eclipse.core.boot)
	 * If the boot project is in the workspace, the classpath used in the workspace is used.
	 */
	private String[] constructClasspath(IPluginModelBase[] plugins)
		throws CoreException {
		/*
		File slimLauncher =
			PDEPlugin.getFileInPlugin(new Path("launcher/slimlauncher.jar"));
		*/
		IPath eclipsePath = ExternalModelManager.getEclipseHome(null);
		File slimLauncher = eclipsePath.append("startup.jar").toFile();

		if (slimLauncher == null || !slimLauncher.exists()) {
			PDEPlugin.logErrorMessage(
				PDEPlugin.getResourceString(KEY_SLIMLAUNCHER));
			return null;
		}
		IPluginModelBase model = findModel("org.eclipse.core.boot", plugins);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				// in workspace - use the java project
				IProject project = resource.getProject();
				IJavaProject jproject = JavaCore.create(project);
				String[] bootClassPath =
					JavaRuntime.computeDefaultRuntimeClassPath(jproject);
				if (bootClassPath != null) {
					String[] resClassPath =
						new String[bootClassPath.length + 1];
					resClassPath[0] = slimLauncher.getPath();
					System.arraycopy(
						bootClassPath,
						0,
						resClassPath,
						1,
						bootClassPath.length);
					return resClassPath;
				}
			} else {
				// outside - locate boot.jar
				String installLocation = model.getInstallLocation();
				if (installLocation.startsWith("file:"))
					installLocation = installLocation.substring(5);
				File bootJar = new File(installLocation, "boot.jar");
				if (bootJar.exists()) {
					return new String[] {
						slimLauncher.getPath(),
						bootJar.getPath()};
				}
				// Check PDE case (third instance) - it may be in the bin
				File binDir = new File(installLocation, "bin/");
				if (binDir.exists()) {
					return new String[] {
						slimLauncher.getPath(),
						binDir.getPath()};
				}
			}
		}
		// failed to construct the class path: boot plugin not existing or boot.jar not found
		return null;
	}

	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			if (model.getPluginBase().getId().equals(id))
				return model;
		}
		return null;
	}

	/**
	 * Constructs a source locator containg all projects selected as plugins.
	 */
	private ISourceLocator constructSourceLocator(IPluginModelBase[] plugins)
		throws CoreException {
		ArrayList javaProjects = new ArrayList(plugins.length);
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		for (int i = 0; i < plugins.length; i++) {
			try {
				File pluginDir =
					new File(
						new URL("file:" + plugins[i].getInstallLocation())
							.getFile());
				IContainer container =
					root.getContainerForLocation(new Path(pluginDir.getPath()));
				if (container instanceof IProject) {
					IProject project = (IProject) container;
					if (WorkspaceModelManager.isJavaPluginProject(project))
						javaProjects.add(JavaCore.create(project));
				}
			} catch (MalformedURLException e) {
				PDEPlugin.log(e);
			}
		}
		IJavaProject[] projs =
			(IJavaProject[]) javaProjects.toArray(
				new IJavaProject[javaProjects.size()]);
		return new JavaUISourceLocator(projs, false);
	}
}