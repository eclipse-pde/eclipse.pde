/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import org.eclipse.debug.core.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.swt.widgets.*;
import org.eclipse.jdt.launching.*;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.core.resources.*;
import org.eclipse.debug.core.model.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import java.net.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.TargetPlatformManager;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.TracingOptionsManager;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

public class WorkbenchLaunchConfigurationDelegate
	implements ILaunchConfigurationDelegate, ILauncherSettings {

	/*
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public ILaunch launch(ILaunchConfiguration configuration, String mode)
		throws CoreException {
		return verifyAndLaunch(configuration, mode, true);
	}

	/*
	 * @see ILaunchConfigurationDelegate#verify(ILaunchConfiguration, String)
	 */
	public void verify(ILaunchConfiguration configuration, String mode)
		throws CoreException {
		verifyAndLaunch(configuration, mode, false);
	}

	private ILaunch verifyAndLaunch(
		final ILaunchConfiguration configuration,
		final String mode,
		boolean performLaunch)
		throws CoreException {
		final String vmArgs = configuration.getAttribute(VMARGS, "");
		final String progArgs = configuration.getAttribute(PROGARGS, "");
		final String appName = configuration.getAttribute(APPLICATION, (String) null);
		final String data = configuration.getAttribute(LOCATION + "0", (String) null);
		final boolean tracing = configuration.getAttribute(TRACING, false);
		final boolean clearWorkspace = false;
		final IPluginModelBase[] plugins = getPluginsFromConfiguration(configuration);

		String vmInstallName = configuration.getAttribute(VMINSTALL, (String) null);
		IVMInstall[] vmInstallations = WorkbenchLauncherBasicTab.getAllVMInstances();
		IVMInstall launcher = null;

		if (vmInstallName != null) {
			for (int i = 0; i < vmInstallations.length; i++) {
				if (vmInstallName.equals(vmInstallations[i].getName())) {
					launcher = vmInstallations[i];
					break;
				}
			}
		} else
			launcher = vmInstallations[0];
		final IVMRunner runner = launcher.getVMRunner(mode);
		final ExecutionArguments args = new ExecutionArguments(vmArgs, progArgs);

		if (!performLaunch)
			return null;

		ProgressMonitorDialog dialog =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());

		final ILaunch[] launch = new ILaunch[1];

		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						launch[0] =
							doLaunch(
								configuration,
								mode,
								runner,
								new Path(data),
								clearWorkspace,
								args,
								plugins,
								appName,
								tracing,
								monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (InterruptedException e) {
			return null;
		}
		catch (InvocationTargetException e) {
			String title = "Launch Eclipse Workbench";
			String message = "Launch failed. See log for details.";
			PDEPlugin.logException(e, title, message);
			return null; // exception handled
		}
		return launch[0];
	}

/*
 * @see ILaunchConfigurationDelegate#initializeDefaults(ILaunchConfigurationWorkingCopy, Object)
 */
public void initializeDefaults(
	ILaunchConfigurationWorkingCopy config,
	Object object) {
	int jreSelectionIndex = 0;
	String vmArgs = "";
	String progArgs = "";
	String appName = "org.eclipse.ui.workbench";
	String[] workspaceSelectionItems = new String[0];
	boolean doClear = false;
	boolean tracing = false;
	IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
	String defaultWorkspace = WorkbenchLauncherBasicTab.getDefaultWorkspace(pstore);

	config.setAttribute(VMARGS, vmArgs);
	config.setAttribute(PROGARGS, progArgs);
	config.setAttribute(APPLICATION, appName);
	config.setAttribute(TRACING, tracing);
	config.setAttribute(LOCATION + "0", defaultWorkspace);

	config.setAttribute(USECUSTOM, false);
}

private IPluginModelBase[] getPluginsFromConfiguration(ILaunchConfiguration config)
	throws CoreException {
	boolean useDefault = !config.getAttribute(USECUSTOM, false);
	ArrayList res = new ArrayList();

	ArrayList deselectedWSPlugins = new ArrayList();

	String wstring = config.getAttribute(WSPROJECT, (String) null);
	String exstring = config.getAttribute(EXTPLUGINS, (String) null);

	if (wstring != null) {
		StringTokenizer tok = new StringTokenizer(wstring, File.pathSeparator);
		while (tok.hasMoreTokens()) {
			deselectedWSPlugins.add(tok.nextToken());
		}
	}
	WorkbenchLauncherAdvancedTab.ExternalStates exstates =
		new WorkbenchLauncherAdvancedTab.ExternalStates();
	if (exstring != null) {
		exstates.parseStates(exstring);
	}

	IPluginModelBase[] models = WorkbenchLauncherAdvancedTab.getWorkspacePlugins();
	for (int i = 0; i < models.length; i++) {
		IPluginModelBase model = models[i];
		if (useDefault || !deselectedWSPlugins.contains(model.getPluginBase().getId()))
			res.add(model);
	}
	models = WorkbenchLauncherAdvancedTab.getExternalPlugins();
	for (int i = 0; i < models.length; i++) {
		IPluginModelBase model = models[i];
		if (useDefault) {
			if (model.isEnabled())
				res.add(model);
		} else {
			WorkbenchLauncherAdvancedTab.ExternalState es =
				exstates.getState(model.getPluginBase().getId());
			if (es != null && es.state) {
				res.add(model);
			} else if (model.isEnabled())
				res.add(model);
		}
	}
	IPluginModelBase[] plugins =
		(IPluginModelBase[]) res.toArray(new IPluginModelBase[res.size()]);
	return plugins;
}

private ILaunch doLaunch(
	ILaunchConfiguration config,
	String mode,
	IVMRunner runner,
	IPath targetWorkbenchLocation,
	boolean clearWorkspace,
	ExecutionArguments args,
	IPluginModelBase[] plugins,
	String appname,
	boolean tracing,
	IProgressMonitor monitor)
	throws CoreException {

	ILaunch launch = null;
	if (monitor == null) {
		monitor = new NullProgressMonitor();
	}
	monitor.beginTask("Starting Eclipse Workbench...", 2);
	try {
		IWorkspace workspace = PDEPlugin.getWorkspace();

		File propertiesFile = TargetPlatformManager.createPropertiesFile(plugins);
		String[] vmArgs = args.getVMArgumentsArray();
		String[] progArgs = args.getProgramArgumentsArray();

		int exCount = tracing ? 8 : 6;
		String[] fullProgArgs = new String[progArgs.length + exCount];
		fullProgArgs[0] = appname;
		fullProgArgs[1] = propertiesFile.getPath();
		fullProgArgs[2] = "-dev";
		fullProgArgs[3] = "bin";
		fullProgArgs[4] = "-data";
		fullProgArgs[5] = targetWorkbenchLocation.toOSString();
		if (tracing) {
			fullProgArgs[6] = "-debug";
			fullProgArgs[7] = getTracingFileArgument();
		}
		System.arraycopy(progArgs, 0, fullProgArgs, exCount, progArgs.length);

		String[] classpath = constructClasspath(plugins);
		if (classpath == null) {
			String message =
				"Launching failed.\nPlugin 'org.eclipse.core.boot' is missing or does not contain 'boot.jar'\n(If in workspace, check that boot.jar is on its classpath)";
			showErrorDialog(message, null);
			return null;
		}

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("SlimLauncher", classpath);
		runnerConfig.setVMArguments(vmArgs);
		runnerConfig.setProgramArguments(fullProgArgs);

		if (clearWorkspace && targetWorkbenchLocation.toFile().exists()) {
			try {
				deleteContent(targetWorkbenchLocation.toFile());
			} catch (IOException e) {
				String message =
					"Problems while deleting files in workspace. Launch will continue";
				showWarningDialog(message);
			}
		}
		monitor.worked(1);
		if (monitor.isCanceled()) {
			return null;
		}
		VMRunnerResult result = runner.run(runnerConfig);
		monitor.worked(1);
		if (result != null) {
			ISourceLocator sourceLocator = constructSourceLocator(plugins);
			launch =
				new Launch(
					config,
					mode,
					sourceLocator,
					result.getProcesses(),
					result.getDebugTarget());
			registerLaunch(launch);
		} else {
			String message = "Launch was not successful.";
			showErrorDialog(message, null);
		}
	} finally {
		monitor.done();
	}
	return launch;
}

private String getTracingFileArgument() {
	TracingOptionsManager mng = PDEPlugin.getDefault().getTracingOptionsManager();
	mng.ensureTracingFileExists();
	String optionsFileName = mng.getTracingFileName();
	String tracingArg;
	if (SWT.getPlatform().equals("motif"))
		tracingArg = "file:" + optionsFileName;
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

private void registerLaunch(final ILaunch launch) {
	Display display = getDisplay();
	display.syncExec(new Runnable() {
		public void run() {
			DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
		}
	});
	PDEPlugin.getDefault().registerLaunch(launch);
}

private void showErrorDialog(final String message, final IStatus status) {
	Display display = getDisplay();
	display.syncExec(new Runnable() {
		public void run() {
			String title = "Eclipse Workbench Launcher";
			if (status == null) {
				MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), title, message);
			} else {
				ErrorDialog.openError(
					PDEPlugin.getActiveWorkbenchShell(),
					title,
					message,
					status);
			}
		}
	});
}

private void showWarningDialog(final String message) {
	Display display = getDisplay();
	display.syncExec(new Runnable() {
		public void run() {
			String title = "Eclipse Workbench Launcher";
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), title, message);
		}
	});
}

/**
 * Constructs a classpath with the slimlauncher and the boot plugin (org.eclipse.core.boot)
 * If the boot project is in the workspace, the classpath used in the workspace is used.
 */
private String[] constructClasspath(IPluginModelBase[] plugins)
	throws CoreException {
	File slimLauncher =
		PDEPlugin.getFileInPlugin(new Path("launcher/slimlauncher.jar"));
	if (slimLauncher == null || !slimLauncher.exists()) {
		PDEPlugin.logErrorMessage(
			"PluginLauncherDelegate: slimlauncher.jar not existing");
		return null;
	}
	IPluginModelBase model = findModel("org.eclipse.core.boot", plugins);
	if (model != null) {
		try {
			File pluginDir =
				new File(new URL("file:" + model.getInstallLocation()).getFile());
			IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();

			IContainer bootProject =
				root.getContainerForLocation(new Path(pluginDir.getPath()));
			if (bootProject instanceof IProject) {
				// if we find the boot project in the workspace use its class path. This allows
				// to develop the boot project itselve
				String[] bootClassPath =
					JavaRuntime.computeDefaultRuntimeClassPath(
						JavaCore.create((IProject) bootProject));
				if (bootClassPath != null) {
					String[] resClassPath = new String[bootClassPath.length + 1];
					resClassPath[0] = slimLauncher.getPath();
					System.arraycopy(bootClassPath, 0, resClassPath, 1, bootClassPath.length);
					return resClassPath;
				}
			}
			// use boot.jar next to the boot plugins plugin.xml
			File bootJar = new File(pluginDir, "boot.jar");
			if (bootJar.exists()) {
				return new String[] { slimLauncher.getPath(), bootJar.getPath()};
			}
		} catch (IOException e) {
			throw new CoreException(
				new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", e));
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
				new File(new URL("file:" + plugins[i].getInstallLocation()).getFile());
			IContainer project =
				root.getContainerForLocation(new Path(pluginDir.getPath()));
			if (project instanceof IProject) {
				javaProjects.add(JavaCore.create((IProject) project));
			}
		} catch (MalformedURLException e) {
			PDEPlugin.log(e);
		}
	}
	IJavaProject[] projs =
		(IJavaProject[]) javaProjects.toArray(new IJavaProject[javaProjects.size()]);
	return new ProjectSourceLocator(projs, false);
}
}