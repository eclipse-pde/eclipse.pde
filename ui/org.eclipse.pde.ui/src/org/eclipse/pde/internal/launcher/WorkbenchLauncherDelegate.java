/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.pde.internal.TracingOptionsManager;
import org.eclipse.swt.SWT;

import org.eclipse.core.resources.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.IExecutableExtension;

public class WorkbenchLauncherDelegate
	implements ILauncherDelegate, IExecutableExtension {

	private boolean showWizard = false;

	public WorkbenchLauncherDelegate() {
	}

	/*
	 * @see ILauncherDelegate#launch(IStructuredSelection, String, ILauncher)
	 */
	public boolean launch(
		Object[] elements,
		final String mode,
		final ILauncher launcher) {
		if (showWizard) {
			WorkbenchLauncherWizard wizard = new WorkbenchLauncherWizard();
			wizard.init(launcher, mode, null);
			WizardDialog dialog =
				new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.open();
			// all errors are already reported to the user -> return true to not show the
			// generic launch error dialog
			return true;
		} else {
			return WorkbenchLauncherWizard.runHeadless(launcher, mode, null);
		}
	}

	/**
	 * Launches eclipse.
	 */
	public void doLaunch(
		ILauncher launcher,
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
		monitor.beginTask("Starting Eclipse Workbench...", 2);
		try {
			IWorkspace workspace = PDEPlugin.getWorkspace();

			File propertiesFile = TargetPlatform.createPropertiesFile(plugins);
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
				return;
			}

			VMRunnerConfiguration config =
				new VMRunnerConfiguration("EclipseRuntimeLauncher", classpath);
			config.setVMArguments(vmArgs);
			config.setProgramArguments(fullProgArgs);

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
				return;
			}
			VMRunnerResult result = runner.run(config);
			monitor.worked(1);
			if (result != null) {
				ISourceLocator sourceLocator = constructSourceLocator(plugins);
				ILaunch launch =
					new Launch(
						launcher,
						mode,
						workspace.getRoot(),
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
		;
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
	
	/*
	 * @see ILauncherDelegate#getLaunchMemento
	 */
	public String getLaunchMemento(Object element) {
		return PDEPlugin.getPluginId();
	}

	/*
	 * @see ILauncherDelegate#getLaunchObject
	 */
	public Object getLaunchObject(String memento) {
		// workspace root is the place holder of all launches
		return PDEPlugin.getWorkspace().getRoot();
	}

	public void setInitializationData(
		IConfigurationElement config,
		String propertyName,
		Object data)
		throws CoreException {
		String mode = data != null ? data.toString() : "";
		if (mode.equals("withWizard"))
			showWizard = true;
	}
}