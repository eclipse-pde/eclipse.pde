/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
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
	private static final String KEY_BAD_FEATURE_SETUP =
		"WorkbenchLauncherConfigurationDelegate.badFeatureSetup";
	private static final String KEY_NO_BOOT =
		"WorkbenchLauncherConfigurationDelegate.noBoot";
	private static final String KEY_NO_STARTUP =
		"WorkbenchLauncherConfigurationDelegate.noStartup";
	private static final String KEY_BROKEN_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.brokenPlugins";
	private static final String KEY_PROBLEMS_DELETING =
		"WorkbenchLauncherConfigurationDelegate.problemsDeleting";
	private static final String KEY_TITLE =
		"WorkbenchLauncherConfigurationDelegate.title";
	private static final String KEY_DELETE_WORKSPACE =
		"WorkbenchLauncherConfigurationDelegate.confirmDeleteWorkspace";
	private static final String KEY_DUPLICATES =
		"WorkbenchLauncherConfigurationDelegate.duplicates";
	private static final String KEY_DUPLICATE_PLUGINS =
		"WorkbenchLauncherConfigurationDelegate.duplicatePlugins";

	private static String bootPath=null;
	private static boolean bootInSource=false;

	/*
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask("",3);
		String appName = configuration.getAttribute(APPLICATION, (String) null);
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", (String) null);

		IVMInstall launcher = createLauncher(configuration, monitor);
		monitor.worked(1);
		
		VMRunnerConfiguration runnerConfig =
			createWorkspaceRunnerConfiguration(
				configuration,
				targetWorkspace,
				appName,
				monitor);
		if (monitor.isCanceled())
			return;
			
		monitor.worked(1);
				
		File workspaceFile = new Path(targetWorkspace).toFile();
		if (configuration.getAttribute(DOCLEAR, false) && workspaceFile.exists()) {
			boolean askClear = configuration.getAttribute(ASKCLEAR, true);
			if (!askClear || confirmDeleteWorkspace(workspaceFile)) {
				try {
					deleteContent(workspaceFile);
				} catch (IOException e) {
					String message = PDEPlugin.getResourceString(KEY_PROBLEMS_DELETING);
					showWarningDialog(message);
				}
			}
		}
		
		PDEPlugin.getDefault().getLaunchesListener().manage(launch);
		launcher.getVMRunner(mode).run(runnerConfig, launch, monitor);
		
		// create a default source locator if required, and migrate configuration
		String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String)null);
		if (id == null) {
			IPersistableSourceLocator locator = DebugPlugin.getDefault().getLaunchManager().newSourceLocator(JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			ILaunchConfigurationWorkingCopy wc = null;
			if (configuration.isWorkingCopy()) {
				wc = (ILaunchConfigurationWorkingCopy)configuration;
			} else {
				wc = configuration.getWorkingCopy();
			}
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.pde.ui.workbenchClasspathProvider");
			locator.initializeDefaults(wc);
			wc.doSave();
			launch.setSourceLocator(locator);
		}
		
		monitor.worked(1);
	}

	/**
	 * Create a runner configuration with basic program arguments:<br>
	 *  -application -dev -configuration -data -os -ws -arch -nl.<br>
	 * Plugins used for the configuration are all workspace plug-ins and all
	 * enabled external plug-ins.<br>In the case of duplicates, a workspace
	 * plug- in masks an external one. <br>The values for the -os, -ws, -arch, -
	 * nl are the values set in the Target Environment preference page.
	 * 
	 * @param targetWorkspace - used for the -data argument.  Must not be null
	 * or empty.
	 * @param appName - used for the -application argument.  Can be null or
	 * empty.
	 * @param monitor - progress monitor.  If null, a new instance of
	 * NullProgressMonitor is used.
	 * 
	 */
	public static VMRunnerConfiguration createWorkspaceRunnerConfiguration(
		String targetWorkspace,
		String appName,
		IProgressMonitor monitor)
		throws CoreException {
		return createWorkspaceRunnerConfiguration(
			null,
			targetWorkspace,
			appName,
			monitor);
	}

	protected static VMRunnerConfiguration createWorkspaceRunnerConfiguration(
		ILaunchConfiguration configuration,
		String targetWorkspace,
		String appName,
		IProgressMonitor monitor)
		throws CoreException {
		
		if (monitor == null)
			monitor = new NullProgressMonitor();
			
		String[] classpath = constructClasspath();
		if (classpath == null) {
			String message = PDEPlugin.getResourceString(KEY_NO_STARTUP);
			monitor.setCanceled(true);
			throw new CoreException(createErrorStatus(message));
		}
			

		boolean useFeatures = false;
		boolean useDefault = true;
		if (configuration != null) {
			useFeatures = configuration.getAttribute(USEFEATURES, false);
			useDefault = configuration.getAttribute(USECUSTOM, true);
		}

		IPluginModelBase[] plugins = null;
		if (useFeatures) {
			validateFeatures(monitor);
		} else {
			bootPath = null;
			bootInSource = false;
			plugins =
				validatePlugins(
					getWorkspacePluginsToRun(configuration, useDefault),
					getExternalPluginsToRun(configuration, useDefault),
					monitor);
		}
		
		if (monitor.isCanceled())
			return null;
		
		ArrayList programArgs = new ArrayList();
		
		if (appName != null && appName.length() > 0) {
			programArgs.add("-application");
			programArgs.add(appName);
		}

		//35296
		programArgs.add(PDECore.ARG_PDELAUNCH);	
			
		if (bootPath!=null && bootInSource) {
			programArgs.add("-boot");
			programArgs.add("file:"+bootPath);
		}
		programArgs.add("-dev");
		programArgs.add(
			getBuildOutputFolders(getWorkspacePluginsToRun(configuration, useDefault)));
		if (useFeatures) {
			IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
			File installDir = installPath.removeLastSegments(1).toFile();
			programArgs.add("-install");
			programArgs.add("file:" + installDir.getPath() + File.separator);
			programArgs.add("-update");
		} else {
			programArgs.add("-configuration");
			String primaryFeatureId = getPrimaryFeatureId();
			File configFile =
				TargetPlatform.createPlatformConfiguration(
					plugins,
					new Path(targetWorkspace),
					primaryFeatureId);
			programArgs.add("file:" + configFile.getPath());
			if (primaryFeatureId != null) {
				programArgs.add("-feature");
				programArgs.add(primaryFeatureId);
			}
		}
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		if (configuration != null) {
			if (configuration.getAttribute(SHOW_SPLASH, true)) {
				programArgs.add("-showsplash");
				programArgs.add(
					computeShowsplashArgument(new SubProgressMonitor(monitor, 1)));
			}
			if (configuration.getAttribute(TRACING, false)) {
				programArgs.add("-debug");
				programArgs.add(getTracingFileArgument(configuration));
			}
			StringTokenizer tokenizer = new StringTokenizer(configuration.getAttribute(PROGARGS,""), " ");
			while (tokenizer.hasMoreTokens()) {
				programArgs.add(tokenizer.nextToken());
			}
		} else {
			programArgs.add("-os");
			programArgs.add(TargetPlatform.getOS());
			programArgs.add("-ws");
			programArgs.add(TargetPlatform.getWS());
			programArgs.add("-arch");
			programArgs.add(TargetPlatform.getOSArch());
			programArgs.add("-nl");
			programArgs.add(TargetPlatform.getNL());
		}
		
		String[] vmArgs = new String[0];
		if (configuration != null) {
			vmArgs = new ExecutionArguments(configuration.getAttribute(VMARGS,""),"").getVMArgumentsArray();
		}
		
		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration(
				"org.eclipse.core.launcher.Main",
				classpath);
		runnerConfig.setVMArguments(vmArgs);
		runnerConfig.setProgramArguments((String[])programArgs.toArray(new String[programArgs.size()]));

		return runnerConfig;
	}
	
	private IVMInstall createLauncher(
		ILaunchConfiguration configuration,
		IProgressMonitor monitor)
		throws CoreException {
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
				createErrorStatus(PDEPlugin.getResourceString(KEY_JRE_PATH_NOT_FOUND)));
		}
		return launcher;

	}
	
	
	private static boolean ignoreValidationErrors(final MultiStatus status) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEPlugin.getResourceString(KEY_TITLE);
				result[0] =
					MessageDialog.openConfirm(
						PDEPlugin.getActiveWorkbenchShell(),
						title,
						status.getMessage());
			}
		});

		return result[0];
	}
	
	private static void validateFeatures(IProgressMonitor monitor)
		throws CoreException {
		IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
		String lastSegment = installPath.lastSegment();
		boolean badStructure = false;
		if (lastSegment.equalsIgnoreCase("plugins") == false) {
			badStructure = true;
		}
		IPath featuresPath =
			installPath.removeLastSegments(1).append("features");
		if (featuresPath.toFile().exists() == false) {
			badStructure = true;
		}
		if (badStructure) {
			monitor.setCanceled(true);
			throw new CoreException(
				createErrorStatus(
					PDEPlugin.getResourceString(KEY_BAD_FEATURE_SETUP)));
		} else {
			// Ensure important files are present
			ensureProductFilesExist(getProductPath());
		}
	}
	
	private static IPath getInstallPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation();
	}
	
	private static IPath getProductPath() {
		return getInstallPath().removeLastSegments(1);
	}

	protected static IPluginModelBase[] getWorkspacePluginsToRun(
		ILaunchConfiguration config,
		boolean useDefault)
		throws CoreException {
			
		IPluginModelBase[] wsmodels =
				PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		if (useDefault)
			return wsmodels;
			
		ArrayList result = new ArrayList();
		TreeSet deselectedWSPlugins =
			AdvancedLauncherTab.parseDeselectedWSIds(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();
			if (id != null && !deselectedWSPlugins.contains(id))
				result.add(wsmodels[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}

	private static IPluginModelBase[] getExternalPluginsToRun(
		ILaunchConfiguration config,
		boolean useDefault)
		throws CoreException {

		if (useDefault)
			return PDECore.getDefault().getExternalModelManager().getAllEnabledModels();

		ArrayList exList = new ArrayList();
		TreeSet selectedExModels = AdvancedLauncherTab.parseSelectedExtIds(config);
		IPluginModelBase[] exmodels =
			PDECore.getDefault().getExternalModelManager().getAllModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && selectedExModels.contains(id))
				exList.add(exmodels[i]);
		}
		return (IPluginModelBase[])exList.toArray(new IPluginModelBase[exList.size()]);
	}

	private static IPluginModelBase[] validatePlugins(
		IPluginModelBase[] wsmodels,
		IPluginModelBase[] exmodels,
		IProgressMonitor monitor)
		throws CoreException {

		IPluginModelBase bootModel = null;
		ArrayList result = new ArrayList();
		ArrayList statusEntries = new ArrayList();

		for (int i = 0; i < wsmodels.length; i++) {
			IStatus status = validateModel(wsmodels[i]);
			if (status == null) {
				String id = wsmodels[i].getPluginBase().getId();
				if (id != null) {
					result.add(wsmodels[i]);
					if (id.equals("org.eclipse.core.boot"))
						bootModel = wsmodels[i];
				}
			} else {
				statusEntries.add(status);
			}
		}

		Vector duplicates = new Vector();
		for (int i = 0; i < exmodels.length; i++) {
			IStatus status = validateModel(exmodels[i]);
			if (status == null) {
				boolean duplicate = false;
				String id = exmodels[i].getPluginBase().getId();
				if (id == null)
					continue;
				for (int j = 0; j < wsmodels.length; j++) {
					if (wsmodels[j].getPluginBase().getId() == null)
						continue;
					if (isDuplicate(wsmodels[j], exmodels[i])) {
						duplicates.add(id);
						duplicate = true;
						break;
					}
				}
				if (!duplicate) {
					result.add(exmodels[i]);
					if (id.equals("org.eclipse.core.boot"))
						bootModel = exmodels[i];
				}
			} else {
				statusEntries.add(status);
			}
		}

		// Look for boot path.  Cancel launch, if not found.
		bootPath = getBootPath(bootModel);
		if (bootPath == null) {
			monitor.setCanceled(true);
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(KEY_TITLE),
				PDEPlugin.getResourceString(KEY_NO_BOOT));
			return null;
		}

		// alert user if there are duplicate plug-ins.
		if (duplicates.size() > 0 && !continueRunning(duplicates)) {
			monitor.setCanceled(true);
			return null;
		}

		// alert user if any plug-ins are not loaded correctly.
		if (statusEntries.size() > 0) {
			IStatus[] children =
				(IStatus[]) statusEntries.toArray(new IStatus[statusEntries.size()]);
			String message = PDEPlugin.getResourceString(KEY_BROKEN_PLUGINS);
			final MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					IStatus.OK,
					children,
					message,
					null);
			if (!ignoreValidationErrors(multiStatus)) {
				monitor.setCanceled(true);
				return null;
			}
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}

	private static IStatus validateModel(IPluginModelBase model) {
		Status status = null;
		if (!model.isLoaded()) {
			String message = model.getInstallLocation();
			if (model.getUnderlyingResource() != null)
				message =
					model.getUnderlyingResource().getProject().getName();
			status =
				new Status(
					IStatus.WARNING,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					message,
					null);
		}
		return status;
	}
	
	private static boolean isDuplicate(
		IPluginModelBase wsmodel,
		IPluginModelBase exmodel) {
		if (!wsmodel.isLoaded() || !exmodel.isLoaded())
			return false;
		return wsmodel.getPluginBase().getId().equalsIgnoreCase(
			exmodel.getPluginBase().getId());
	}




	private static String getBuildOutputFolders(IPluginModelBase[] wsmodels) {
		HashSet set = new HashSet();
		set.add(new Path("bin"));
		for (int i = 0; i < wsmodels.length; i++) {
			IPluginModelBase model = wsmodels[i];
			IProject project = model.getUnderlyingResource().getProject();
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					set.add(
						JavaCore.create(project).getOutputLocation().removeFirstSegments(
							1));
				}
			} catch (JavaModelException e) {
			} catch (CoreException e) {
			}
		}
		StringBuffer result = new StringBuffer();
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			result.append(iter.next().toString());
			if (iter.hasNext())
				result.append(",");
		}
		return result.toString();
	}

	private static String computeShowsplashArgument(IProgressMonitor monitor) {
		IPath eclipseHome = ExternalModelManager.getEclipseHome(monitor);
		IPath fullPath = eclipseHome.append("eclipse");
		return fullPath.toOSString() + " -showsplash 600";
	}

	private static String getTracingFileArgument(ILaunchConfiguration config) {
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

	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	private static IStatus createErrorStatus(String message) {
		return new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.OK,
			message,
			null);
	}

	private static boolean continueRunning(final Vector duplicates) {
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
	private static String[] constructClasspath()
		throws CoreException {

		File startupJar =
			ExternalModelManager.getEclipseHome(null).append("startup.jar").toFile();

		if (startupJar.exists())
			return new String[] { startupJar.getAbsolutePath()};
			
		return null;
	}

	private static String getBootPath(IPluginModelBase bootModel) {
		if (bootModel == null)
			return null;
		try {
			IResource resource = bootModel.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					resource = project.findMember("boot.jar");
					if (resource != null)
						return "file:" + resource.getLocation().toOSString();
					IPath path = JavaCore.create(project).getOutputLocation();
					if (path != null) {
						bootInSource=true;
						IPath sourceBootPath = project.getParent().getLocation().append(path);
						return sourceBootPath.addTrailingSeparator().toOSString();
					}
				}
			} else {
				File binDir = new File(bootModel.getInstallLocation(), "bin/");
				if (binDir.exists())
					return binDir.getAbsolutePath();

				File bootJar =
					new File(bootModel.getInstallLocation(), "boot.jar");
				if (bootJar.exists())
					return "file:" + bootJar.getAbsolutePath();

			}
		} catch (CoreException e) {
		}

		return null;
	}

	private static String getPrimaryFeatureId() {
		IPath eclipsePath = ExternalModelManager.getEclipseHome(null);
		File iniFile = new File(eclipsePath.toFile(), "install.ini");
		if (iniFile.exists() == false)
			return null;
		Properties pini = new Properties();
		try {
			FileInputStream fis = new FileInputStream(iniFile);
			pini.load(fis);
			fis.close();
			return pini.getProperty("feature.default.id");
		} catch (IOException e) {
			return null;
		}
	}

	private static void ensureProductFilesExist(IPath productArea) {
		File productDir = productArea.toFile();
		File marker = new File(productDir, ".eclipseproduct");
		File ini = new File(productDir, "install.ini");
		if (marker.exists() && ini.exists()) return;
		IPath eclipsePath = ExternalModelManager.getEclipseHome(null);
		if (!marker.exists()) 
			copyFile(eclipsePath, ".eclipseproduct", marker);
		if (!ini.exists())
			copyFile(eclipsePath, "install.ini", ini);
	}

	private static void copyFile(IPath eclipsePath, String name, File target) {
		File source = new File(eclipsePath.toFile(), name);
		if (source.exists() == false)
			return;
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(target);
			byte[] buf = new byte[1024];
			long currentLen = 0;
			int len = is.read(buf);
			while (len != -1) {
				currentLen += len;
				os.write(buf, 0, len);
				len = is.read(buf);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (is != null)
					is.close();
				if (os != null)
					os.close();
			} catch (IOException e) {
			}
		}
	}
}
