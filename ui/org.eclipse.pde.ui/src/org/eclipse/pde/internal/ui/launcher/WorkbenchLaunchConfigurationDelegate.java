/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
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
import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.core.*;

public class WorkbenchLaunchConfigurationDelegate
	implements ILaunchConfigurationDelegate, ILauncherSettings {
	private static final String KEY_NO_JRE = "WorkbenchLauncherConfigurationDelegate.noJRE";
	private static final String KEY_STARTING = "WorkbenchLauncherConfigurationDelegate.starting";
	private static final String KEY_NO_BOOT = "WorkbenchLauncherConfigurationDelegate.noBoot";
	private static final String KEY_PROBLEMS_DELETING = "WorkbenchLauncherConfigurationDelegate.problemsDeleting";
	private static final String KEY_TITLE = "WorkbenchLauncherConfigurationDelegate.title";
	private static final String KEY_SLIMLAUNCHER = "WorkbenchLauncherConfigurationDelegate.slimlauncher";
	private static final String KEY_DELETE_WORKSPACE = "WorkbenchLauncherConfigurationDelegate.confirmDeleteWorkspace";

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
		final String appName = configuration.getAttribute(APPLICATION, (String) null);
		final String data = configuration.getAttribute(LOCATION + "0", (String) null);
		final boolean tracing = configuration.getAttribute(TRACING, false);
		final boolean clearWorkspace = configuration.getAttribute(DOCLEAR, false);
		final IPluginModelBase[] plugins = getPluginsFromConfiguration(configuration);

		String vmInstallName = configuration.getAttribute(VMINSTALL, (String) null);
		IVMInstall[] vmInstallations = BasicLauncherTab.getAllVMInstances();
		IVMInstall launcher = null;
		
		if (monitor==null)
			monitor = new NullProgressMonitor();

		if (vmInstallName != null) {
			for (int i = 0; i < vmInstallations.length; i++) {
				if (vmInstallName.equals(vmInstallations[i].getName())) {
					launcher = vmInstallations[i];
					break;
				}
			}
		} else if (vmInstallations.length>0)
			launcher = vmInstallations[0];
		if (launcher == null) {
			String message = PDEPlugin.getFormattedMessage(KEY_NO_JRE, vmInstallName);
			monitor.setCanceled(true);
			throw new CoreException(createErrorStatus(message));
		}
		IVMRunner runner = launcher.getVMRunner(mode);
		ExecutionArguments args = new ExecutionArguments(vmArgs, progArgs);
		IPath path = new Path(data);
		boolean success =
			doLaunch(
				launch,
				configuration,
				mode,
				runner,
				path,
				clearWorkspace,
				args,
				plugins,
				appName,
				tracing,
				monitor);
	}

	private IPluginModelBase[] getPluginsFromConfiguration(ILaunchConfiguration config)
		throws CoreException {
		boolean useDefault = config.getAttribute(USECUSTOM, true);
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
		AdvancedLauncherTab.ExternalStates exstates =
			new AdvancedLauncherTab.ExternalStates();
		if (exstring != null) {
			exstates.parseStates(exstring);
		}

		IPluginModelBase[] models = AdvancedLauncherTab.getWorkspacePlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (useDefault || !deselectedWSPlugins.contains(model.getPluginBase().getId()))
				res.add(model);
		}
		models = AdvancedLauncherTab.getExternalPlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (useDefault) {
				if (model.isEnabled())
					res.add(model);
			} else {
				AdvancedLauncherTab.ExternalState es =
					exstates.getState(model.getPluginBase().getId());
				if (es != null) {
					if(es.state)
						res.add(model);
				} else if (model.isEnabled())
					res.add(model);
			}
		}
		IPluginModelBase[] plugins =
			(IPluginModelBase[]) res.toArray(new IPluginModelBase[res.size()]);
		return plugins;
	}

	private boolean doLaunch(
		ILaunch launch,
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

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(PDEPlugin.getResourceString(KEY_STARTING), 2);
		try {
			File propertiesFile =
				TargetPlatform.createPropertiesFile(plugins, targetWorkbenchLocation);
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
				fullProgArgs[7] = getTracingFileArgument(config);
			}
			System.arraycopy(progArgs, 0, fullProgArgs, exCount, progArgs.length);

			String[] classpath = constructClasspath(plugins);
			if (classpath == null) {
				String message = PDEPlugin.getResourceString(KEY_NO_BOOT);
				monitor.setCanceled(true);
				throw new CoreException(createErrorStatus(message));
			}

			VMRunnerConfiguration runnerConfig =
				new VMRunnerConfiguration("EclipseRuntimeLauncher", classpath);
			runnerConfig.setVMArguments(vmArgs);
			runnerConfig.setProgramArguments(fullProgArgs);

			if (clearWorkspace && targetWorkbenchLocation.toFile().exists()) {
				if (confirmDeleteWorkspace()) {
				try {
					deleteContent(targetWorkbenchLocation.toFile());
				} catch (IOException e) {
					String message = PDEPlugin.getResourceString(KEY_PROBLEMS_DELETING);
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

	private String getTracingFileArgument(ILaunchConfiguration config) {
		TracingOptionsManager mng = PDECore.getDefault().getTracingOptionsManager();
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
	
	private boolean  confirmDeleteWorkspace() {
		Display display = getDisplay();
		final boolean [] result = new boolean[1];
		display.syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				String message = PDEPlugin.getResourceString(KEY_DELETE_WORKSPACE);
				result[0] = MessageDialog.openQuestion(PDEPlugin.getActiveWorkbenchShell(), title,message);
			}
		});
		return result[0];
	}

	private void showWarningDialog(final String message) {
		Display display = getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
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
				String[] bootClassPath = JavaRuntime.computeDefaultRuntimeClassPath(jproject);
				if (bootClassPath != null) {
					String[] resClassPath = new String[bootClassPath.length + 1];
					resClassPath[0] = slimLauncher.getPath();
					System.arraycopy(bootClassPath, 0, resClassPath, 1, bootClassPath.length);
					return resClassPath;
				}
			} else {
				// outside - locate boot.jar
				String installLocation = model.getInstallLocation();
				if (installLocation.startsWith("file:"))
					installLocation = installLocation.substring(5);
				File bootJar = new File(installLocation, "boot.jar");
				if (bootJar.exists()) {
					return new String[] { slimLauncher.getPath(), bootJar.getPath()};
				}
				// Check PDE case (third instance) - it may be in the bin
				File binDir = new File(installLocation, "bin/");
				if (binDir.exists()) {
					return new String[] { slimLauncher.getPath(), binDir.getPath()};
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
					new File(new URL("file:" + plugins[i].getInstallLocation()).getFile());
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
			(IJavaProject[]) javaProjects.toArray(new IJavaProject[javaProjects.size()]);
		return new JavaUISourceLocator(projs, false);
	}
}