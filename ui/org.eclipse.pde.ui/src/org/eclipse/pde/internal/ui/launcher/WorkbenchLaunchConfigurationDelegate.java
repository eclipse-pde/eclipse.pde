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

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
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
	private static final String KEY_NO_STARTUP =
		"WorkbenchLauncherConfigurationDelegate.noStartup";
	private static final String KEY_PROBLEMS_DELETING =
		"WorkbenchLauncherConfigurationDelegate.problemsDeleting";
	private static final String KEY_TITLE =
		"WorkbenchLauncherConfigurationDelegate.title";
	private static final String KEY_DELETE_WORKSPACE =
		"WorkbenchLauncherConfigurationDelegate.confirmDeleteWorkspace";

	/*
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		try {
			monitor.beginTask("", 3);
			
			IVMInstall launcher = createLauncher(configuration, monitor);
			monitor.worked(1);
			
			VMRunnerConfiguration runnerConfig = createVMRunner(configuration);
			if (runnerConfig == null) {
				monitor.setCanceled(true);
				return;
			} 
			monitor.worked(1);
					
			String targetWorkspace =
				configuration.getAttribute(LOCATION + "0", LauncherUtils.getTempWorkspace());
			File workspaceFile = new Path(targetWorkspace).toFile();
			if (configuration.getAttribute(DOCLEAR, false) && workspaceFile.exists()) {
				if (!configuration.getAttribute(ASKCLEAR, true) || confirmDeleteWorkspace(workspaceFile)) {
					try {
						deleteContent(workspaceFile);
					} catch (IOException e) {
						showWarningDialog(PDEPlugin.getResourceString(KEY_PROBLEMS_DELETING));
					}
				}
			}
			// create a default source locator if required, and migrate configuration
			setDefaultSourceLocator(configuration, launch);
			PDEPlugin.getDefault().getLaunchesListener().manage(launch);
			launcher.getVMRunner(mode).run(runnerConfig, launch, monitor);		
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	
	private void setDefaultSourceLocator(ILaunchConfiguration configuration, ILaunch launch) throws CoreException {
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
	}

	private VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration)
		throws CoreException {
		String[] classpath = LauncherUtils.constructClasspath();
		if (classpath == null) {
			String message = PDEPlugin.getResourceString(KEY_NO_STARTUP);
			throw new CoreException(createErrorStatus(message));
		}

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath);
		runnerConfig.setVMArguments(getVMArguments(configuration));
		runnerConfig.setProgramArguments(getProgramArguments(configuration));
		return runnerConfig;
	}
	
	private String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		//35296
		programArgs.add(PDECore.ARG_PDELAUNCH);	
		
		String appName = configuration.getAttribute(APPLICATION, (String)null);
		if (appName != null && appName.length() > 0) {
			programArgs.add("-application");
			programArgs.add(appName);
		}
		


		String targetWorkspace = configuration.getAttribute(LOCATION + "0", LauncherUtils.getTempWorkspace());
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		
		boolean useDefault = true;
			useDefault = configuration.getAttribute(USECUSTOM, true);
		if (configuration.getAttribute(USEFEATURES, false)) {
			validateFeatures();
			IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
			File installDir = installPath.removeLastSegments(1).toFile();
			programArgs.add("-install");
			programArgs.add("file:" + installDir.getPath() + File.separator);
			programArgs.add("-update");
		} else {
			IPluginModelBase[] plugins =
				LauncherUtils.validatePlugins(
					LauncherUtils.getWorkspacePluginsToRun(configuration, useDefault),
					getExternalPluginsToRun(configuration, useDefault));
			if (plugins == null) 
				return null;
				
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
		
		if (LauncherUtils.isBootInSource()) {
			String bootPath = LauncherUtils.getBootPath();
			if (bootPath != null) {
				programArgs.add("-boot");
				programArgs.add("file:" + bootPath);
			}
		}

		programArgs.add("-dev");
		String devEntry =
			LauncherUtils.getBuildOutputFolders(
				LauncherUtils.getWorkspacePluginsToRun(configuration, useDefault));
		programArgs.add(configuration.getAttribute(CLASSPATH_ENTRIES, devEntry));

		if (configuration.getAttribute(SHOW_SPLASH, true)) {
			programArgs.add("-showsplash");
			programArgs.add(computeShowsplashArgument());
		}
		if (configuration.getAttribute(TRACING, false)) {
			programArgs.add("-debug");
			programArgs.add(getTracingFileArgument(configuration));
		}
		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, ""));
		while (tokenizer.hasMoreTokens()) {
			programArgs.add(tokenizer.nextToken());
		}
		
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	private String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(configuration.getAttribute(VMARGS,""),"").getVMArgumentsArray();
	}
	
 	private IVMInstall createLauncher(
		ILaunchConfiguration configuration,
		IProgressMonitor monitor)
		throws CoreException {
		String vm = configuration.getAttribute(VMINSTALL, (String) null);
		IVMInstall launcher = LauncherUtils.getVMInstall(vm);

		if (launcher == null) 
			throw new CoreException(
				createErrorStatus(PDEPlugin.getFormattedMessage(KEY_NO_JRE, vm)));
		
		if (!launcher.getInstallLocation().exists()) 
			throw new CoreException(
				createErrorStatus(PDEPlugin.getResourceString(KEY_JRE_PATH_NOT_FOUND)));
		
		return launcher;
	}
		
	private void validateFeatures()
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
			throw new CoreException(
				createErrorStatus(
					PDEPlugin.getResourceString(KEY_BAD_FEATURE_SETUP)));
		} else {
			// Ensure important files are present
			ensureProductFilesExist(getProductPath());
		}
	}
	
	private IPath getInstallPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation();
	}
	
	private IPath getProductPath() {
		return getInstallPath().removeLastSegments(1);
	}


	private IPluginModelBase[] getExternalPluginsToRun(
		ILaunchConfiguration config,
		boolean useDefault)
		throws CoreException {

		if (useDefault)
			return PDECore.getDefault().getExternalModelManager().getAllEnabledModels();

		ArrayList exList = new ArrayList();
		TreeSet selectedExModels = LauncherUtils.parseSelectedExtIds(config);
		IPluginModelBase[] exmodels =
			PDECore.getDefault().getExternalModelManager().getAllModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && selectedExModels.contains(id))
				exList.add(exmodels[i]);
		}
		return (IPluginModelBase[])exList.toArray(new IPluginModelBase[exList.size()]);
	}


	
	private String computeShowsplashArgument() {
		IPath eclipseHome = ExternalModelManager.getEclipseHome(null);
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



	private String getPrimaryFeatureId() {
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

	private void ensureProductFilesExist(IPath productArea) {
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

	private void copyFile(IPath eclipsePath, String name, File target) {
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
