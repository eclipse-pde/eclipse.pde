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
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.core.*;

public class WorkbenchLaunchConfigurationDelegate extends LaunchConfigurationDelegate 
			implements ILauncherSettings {
	private static final String KEY_BAD_FEATURE_SETUP =
		"WorkbenchLauncherConfigurationDelegate.badFeatureSetup";
	private static final String KEY_NO_STARTUP =
		"WorkbenchLauncherConfigurationDelegate.noStartup";

	private File fConfigDir = null;
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
			
			IVMInstall launcher = LauncherUtils.createLauncher(configuration);
			monitor.worked(1);
			
			VMRunnerConfiguration runnerConfig = createVMRunner(configuration);
			if (runnerConfig == null) {
				monitor.setCanceled(true);
				return;
			} 
			monitor.worked(1);
			
			if (fConfigDir == null) {
				launch.setAttribute(ILauncherSettings.CONFIG_LOCATION, null);
			} else {
				launch.setAttribute(ILauncherSettings.CONFIG_LOCATION,
						fConfigDir.toString());
			}
				
			String workspace = configuration.getAttribute(LOCATION + "0", LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString());
			LauncherUtils.clearWorkspace(configuration, workspace);
				
			LauncherUtils.setDefaultSourceLocator(configuration, launch);
			PDEPlugin.getDefault().getLaunchesListener().manage(launch);
			launcher.getVMRunner(mode).run(runnerConfig, launch, monitor);		
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}

	private VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration)
		throws CoreException {
		String[] classpath = LauncherUtils.constructClasspath(configuration);
		if (classpath == null) {
			String message = PDEPlugin.getResourceString(KEY_NO_STARTUP);
			throw new CoreException(LauncherUtils.createErrorStatus(message));
		}
		
		// Program arguments
		String[] programArgs = getProgramArguments(configuration);
		if (programArgs == null)
			return null;

		// Environment variables
		String[] envp =
			DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath);
		runnerConfig.setVMArguments(getVMArguments(configuration));
		runnerConfig.setProgramArguments(programArgs);
		runnerConfig.setEnvironment(envp);
		return runnerConfig;
	}
	
	private String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		// specify the application to launch, only if it is not the default.
		String appName = configuration.getAttribute(APPLICATION, (String)null);
		if (appName != null && appName.length() > 0) {
			programArgs.add("-application");
			programArgs.add(appName);
		}
		
		// specify the workspace location for the runtime workbench
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString());
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		
		boolean isOSGI = PDECore.getDefault().getModelManager().isOSGiRuntime();
		if (configuration.getAttribute(USEFEATURES, false)) {
			validateFeatures();
			IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
			programArgs.add("-install");
			programArgs.add("file:" + installPath.removeLastSegments(1).addTrailingSeparator().toString());
			programArgs.add("-update");
		} else {
			TreeMap pluginMap = LauncherUtils.getPluginsToRun(configuration);
			if (pluginMap == null) 
				return null;
				
			String primaryFeatureId = LauncherUtils.getPrimaryFeatureId();
			fConfigDir =
				TargetPlatform.createPlatformConfigurationArea(
					pluginMap,
					configuration.getName(),
					primaryFeatureId,
					LauncherUtils.getAutoStartPlugins(configuration));
			programArgs.add("-configuration");
			if (isOSGI)
				programArgs.add("file:" + new Path(fConfigDir.getPath()).addTrailingSeparator().toString());
			else
				programArgs.add("file:" + new Path(fConfigDir.getPath()).append("platform.cfg").toString());
			if (configuration.getAttribute(CONFIG_CLEAR, true))
				LauncherUtils.clearConfigArea(fConfigDir);
			
			if (!isOSGI) {
				if (primaryFeatureId != null) {
					programArgs.add("-feature");
					programArgs.add(primaryFeatureId);					
				}
				IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get("org.eclipse.core.boot");
				String bootPath = LauncherUtils.getBootPath(bootModel);
				if (bootPath != null && !bootPath.endsWith(".jar")) {
					programArgs.add("-boot");
					programArgs.add("file:" + bootPath);
				}
			}
		}
		
		// add the output folder names
		programArgs.add("-dev");
		String devEntry = LauncherUtils.getBuildOutputFolders();
		programArgs.add(configuration.getAttribute(CLASSPATH_ENTRIES, devEntry));

		// add tracing, if turned on
		if (configuration.getAttribute(TRACING, false)
				&& !TRACING_NONE.equals(configuration.getAttribute(
					TRACING_CHECKED, (String) null))) {
			if (fConfigDir == null) {
				fConfigDir =
					TargetPlatform.createWorkingDirectory(configuration.getName());
			}
			String directoryName = fConfigDir.toString();
			programArgs.add("-debug");
			programArgs.add(
				LauncherUtils.getTracingFileArgument(
					configuration,
					directoryName + Path.SEPARATOR + ".options"));
		}

		// add the program args specified by the user
		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, ""));
		while (tokenizer.hasMoreTokens()) {
			programArgs.add(tokenizer.nextToken());
		}
		
		// show splash only if we are launching the default application
		boolean showSplash = true;
		int index = programArgs.indexOf("-application");
		if (index != -1 && index <= programArgs.size() - 2) {
			if (!programArgs.get(index + 1).equals(LauncherUtils.getDefaultApplicationName())) {
				showSplash = false;
			}
		}
		if (showSplash && !programArgs.contains("-nosplash")) {
			programArgs.add(0, "-showsplash");
			programArgs.add(1, computeShowsplashArgument());
		}
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	private String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(configuration.getAttribute(VMARGS,""),"").getVMArgumentsArray();
	}
			
	private void validateFeatures() throws CoreException {
		IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
		String lastSegment = installPath.lastSegment();
		boolean badStructure = lastSegment == null;
		if (!badStructure) {
			IPath featuresPath = installPath.removeLastSegments(1).append("features");
			badStructure = !lastSegment.equalsIgnoreCase("plugins")
					|| !featuresPath.toFile().exists();
		}
		if (badStructure) {
			throw new CoreException(LauncherUtils.createErrorStatus(PDEPlugin
					.getResourceString(KEY_BAD_FEATURE_SETUP)));
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

	private String computeShowsplashArgument() {
		IPath eclipseHome = ExternalModelManager.getEclipseHome();
		IPath fullPath = eclipseHome.append("eclipse");
		return fullPath.toOSString() + " -showsplash 600";
	}

	private void ensureProductFilesExist(IPath productArea) {
		File productDir = productArea.toFile();
		
		File marker = new File(productDir, ".eclipseproduct");
		IPath eclipsePath = ExternalModelManager.getEclipseHome();
		if (!marker.exists()) 
			copyFile(eclipsePath, ".eclipseproduct", marker);
		
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			fConfigDir = new File(productDir, "configuration");
			if (!fConfigDir.exists())
				fConfigDir.mkdirs();		
			File ini = new File(fConfigDir, "config.ini");			
			if (!ini.exists())
				copyFile(eclipsePath.append("configuration"), "config.ini", ini);
		} else {
			File ini = new File(productDir, "install.ini");
			fConfigDir = productDir;
			if (!ini.exists()) 
				copyFile(eclipsePath, "install.ini", ini);		
		}
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getBuildOrder(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration,
			String mode) throws CoreException {
		return super.getBuildOrder(configuration, mode);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getProjectsForProblemSearch(
			ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return super.getProjectsForProblemSearch(configuration, mode);
	}
}
