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
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.core.*;

public class WorkbenchLaunchConfigurationDelegate
	implements ILaunchConfigurationDelegate, ILauncherSettings {
	private static final String KEY_BAD_FEATURE_SETUP =
		"WorkbenchLauncherConfigurationDelegate.badFeatureSetup";
	private static final String KEY_NO_STARTUP =
		"WorkbenchLauncherConfigurationDelegate.noStartup";

	private File configFile = null;
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
			
			launch.setAttribute(
				ILauncherSettings.CONFIG_LOCATION,
				(configFile == null) ? null : configFile.getParent());
				
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
		String[] classpath = LauncherUtils.constructClasspath();
		if (classpath == null) {
			String message = PDEPlugin.getResourceString(KEY_NO_STARTUP);
			throw new CoreException(LauncherUtils.createErrorStatus(message));
		}
		String[] programArgs = getProgramArguments(configuration);
		if (programArgs == null)
			return null;

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath);
		runnerConfig.setVMArguments(getVMArguments(configuration));
		runnerConfig.setProgramArguments(programArgs);
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
		
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString());
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		
		boolean useDefault = configuration.getAttribute(USECUSTOM, true);
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
			configFile =
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

		if (configuration.getAttribute(TRACING, false)) {
			programArgs.add("-debug");
			programArgs.add(getTracingFileArgument(configuration));
		}

		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, ""));
		while (tokenizer.hasMoreTokens()) {
			programArgs.add(tokenizer.nextToken());
		}
		
		if (configuration.getAttribute(SHOW_SPLASH, true)) {
			boolean showSplash = true;
			int index = programArgs.indexOf("-application");
			if (index != -1 && index <= programArgs.size() - 2) {
				if (!programArgs.get(index + 1).equals("org.eclipse.ui.workbench")) {
					showSplash = false;
				}
			}
			if (showSplash) {
				programArgs.add("-showsplash");
				programArgs.add(computeShowsplashArgument());
			}
		}
		
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	private String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(configuration.getAttribute(VMARGS,""),"").getVMArgumentsArray();
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
				LauncherUtils.createErrorStatus(
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
