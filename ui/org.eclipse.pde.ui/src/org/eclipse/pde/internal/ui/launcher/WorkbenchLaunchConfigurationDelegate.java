/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class WorkbenchLaunchConfigurationDelegate extends LaunchConfigurationDelegate 
			implements ILauncherSettings {
	protected File fConfigDir = null;
	
	/*
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		final IProgressMonitor monitor)
		throws CoreException {
		try {
			fConfigDir = null;
			monitor.beginTask("", 5); //$NON-NLS-1$
			
			String workspace = configuration.getAttribute(LOCATION + "0", LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
			File file = new File(workspace, ".metadata/.lock"); //$NON-NLS-1$
			if (file.exists() && file.isFile()) {
				monitor.setCanceled(true);
				LauncherUtils.getDisplay().syncExec(new Runnable() {
					public void run() {
						MessageDialog dialog = new MessageDialog(
								LauncherUtils.getDisplay().getActiveShell(), 
								PDEUIMessages.JUnitLaunchConfiguration_cantLock, 
								null,
								PDEUIMessages.JUnitLaunchConfiguration_cantLockMessage, 
								MessageDialog.ERROR, 
								new String[]{IDialogConstants.OK_LABEL}, 
								0);
						dialog.open();
					}
				});
				return;
			}
			
			// Clear workspace and prompt, if necessary
			if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
				monitor.setCanceled(true);
				return;
			}

			// clear config area, if necessary
			if (configuration.getAttribute(CONFIG_CLEAR_AREA, false))
				LauncherUtils.clearConfigArea(getConfigDir(configuration), new SubProgressMonitor(monitor, 1));
			launch.setAttribute(ILauncherSettings.CONFIG_LOCATION, getConfigDir(configuration).toString());
			
			
			// create launcher
			IVMInstall launcher = LauncherUtils.createLauncher(configuration);
			monitor.worked(1);
			
			// load the arguments on the launcher
			VMRunnerConfiguration runnerConfig = createVMRunner(configuration);
			if (runnerConfig == null) {
				monitor.setCanceled(true);
				return;
			} 
			monitor.worked(1);
						
			LauncherUtils.setDefaultSourceLocator(configuration, launch);
			LauncherUtils.synchronizeManifests(configuration, getConfigDir(configuration));
			PDEPlugin.getDefault().getLaunchListener().manage(launch);
			launcher.getVMRunner(mode).run(runnerConfig, launch, monitor);		
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}

	protected VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration)
		throws CoreException {
		String[] classpath = LauncherUtils.constructClasspath(configuration);
		if (classpath == null) {
			String message = PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noStartup;
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
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath); //$NON-NLS-1$
		runnerConfig.setVMArguments(getVMArguments(configuration));
		runnerConfig.setProgramArguments(programArgs);
		runnerConfig.setEnvironment(envp);
		runnerConfig.setVMSpecificAttributesMap(LauncherUtils.getVMSpecificAttributes(configuration));
		return runnerConfig;
	}
	
	protected String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(USE_PRODUCT, false)) {
			programArgs.add("-product"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(PRODUCT, "")); //$NON-NLS-1$
		} else {
			// specify the application to launch
			programArgs.add("-application"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(APPLICATION, LauncherUtils.getDefaultApplicationName()));
		}
		
		// specify the workspace location for the runtime workbench
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		programArgs.add("-data"); //$NON-NLS-1$
		programArgs.add(targetWorkspace);
		
		boolean isOSGI = PDECore.getDefault().getModelManager().isOSGiRuntime();
		boolean showSplash = true;
		if (configuration.getAttribute(USEFEATURES, false)) {
			validateFeatures();
			IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
			programArgs.add("-install"); //$NON-NLS-1$
			programArgs.add("file:" + installPath.removeLastSegments(1).addTrailingSeparator().toString()); //$NON-NLS-1$
			if (isOSGI && !configuration.getAttribute(CONFIG_USE_DEFAULT_AREA, true)) {
				programArgs.add("-configuration"); //$NON-NLS-1$
				programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
			}
			programArgs.add("-update"); //$NON-NLS-1$
            // add the output folder names
            programArgs.add("-dev"); //$NON-NLS-1$
            if (PDECore.getDefault().getModelManager().isOSGiRuntime())
                programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", true)); //$NON-NLS-1$
            else
                programArgs.add(ClasspathHelper.getDevEntries(true));
            
		} else {
			TreeMap pluginMap = LauncherUtils.getPluginsToRun(configuration);
			if (pluginMap == null) 
				return null;
				
			String brandingPlugin = LauncherUtils.getBrandingPluginID(configuration);
			if (isOSGI) {
				Properties prop = LauncherUtils.createConfigIniFile(configuration,
						brandingPlugin, pluginMap, getConfigDir(configuration));
				showSplash = prop.containsKey("osgi.splashPath") || prop.containsKey("splashLocation"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			TargetPlatform.createPlatformConfigurationArea(
					pluginMap,
					getConfigDir(configuration),
					brandingPlugin);
			programArgs.add("-configuration"); //$NON-NLS-1$
			if (isOSGI)
				programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
			else
				programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).append("platform.cfg").toString()); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (!isOSGI) {
				if (brandingPlugin != null) {
					programArgs.add("-feature"); //$NON-NLS-1$
					programArgs.add(brandingPlugin);					
				}
				IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get("org.eclipse.core.boot"); //$NON-NLS-1$
				String bootPath = LauncherUtils.getBootPath(bootModel);
				if (bootPath != null && !bootPath.endsWith(".jar")) { //$NON-NLS-1$
					programArgs.add("-boot"); //$NON-NLS-1$
					programArgs.add("file:" + bootPath); //$NON-NLS-1$
				}
			}
            // add the output folder names
            programArgs.add("-dev"); //$NON-NLS-1$
            if (PDECore.getDefault().getModelManager().isOSGiRuntime())
                programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", pluginMap)); //$NON-NLS-1$
            else
                programArgs.add(ClasspathHelper.getDevEntries(true));            
		}
		
		// necessary for PDE to know how to load plugins when target platform = host platform
		// see PluginPathFinder.getPluginPaths()
		programArgs.add("-pdelaunch"); //$NON-NLS-1$

		// add tracing, if turned on
		if (configuration.getAttribute(TRACING, false)
				&& !TRACING_NONE.equals(configuration.getAttribute(TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			programArgs.add(
				LauncherUtils.getTracingFileArgument(
					configuration,
					getConfigDir(configuration).toString() + Path.SEPARATOR + ".options")); //$NON-NLS-1$
		}

		// add the program args specified by the user
		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, "")); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (token.equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			programArgs.add(token);
		}

		if (!programArgs.contains("-nosplash") && showSplash) { //$NON-NLS-1$
			if (PDECore.getDefault().getTargetVersion().equals(ICoreConstants.TARGET31)) {
				programArgs.add(0, "-launcher");  //$NON-NLS-1$
				IPath path = ExternalModelManager.getEclipseHome().append("eclipse"); //$NON-NLS-1$
				programArgs.add(1, path.toOSString()); //This could be the branded launcher if we want (also this does not bring much)
				programArgs.add(2, "-name"); //$NON-NLS-1$
				programArgs.add(3, "Eclipse");	//This should be the name of the product //$NON-NLS-1$
				programArgs.add(4, "-showsplash"); //$NON-NLS-1$
				programArgs.add(5, "600"); //$NON-NLS-1$
			} else {
				programArgs.add(0, "-showsplash"); //$NON-NLS-1$
				programArgs.add(1, computeShowsplashArgument());
			}
		}
		if (!programArgs.contains("-os")) { //$NON-NLS-1$
			programArgs.add("-os"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getOS());
		}
		if (!programArgs.contains("-ws")) { //$NON-NLS-1$
			programArgs.add("-ws"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getWS());
		}
		if (!programArgs.contains("-arch")) { //$NON-NLS-1$
			programArgs.add("-arch"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getOSArch());
		}
		return (String[])programArgs.toArray(new String[programArgs.size()]);
	}
	
	protected String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(configuration.getAttribute(VMARGS,""),"").getVMArgumentsArray(); //$NON-NLS-1$ //$NON-NLS-2$
	}
			
	protected void validateFeatures() throws CoreException {
		IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
		String lastSegment = installPath.lastSegment();
		boolean badStructure = lastSegment == null;
		if (!badStructure) {
			IPath featuresPath = installPath.removeLastSegments(1).append("features"); //$NON-NLS-1$
			badStructure = !lastSegment.equalsIgnoreCase("plugins") //$NON-NLS-1$
					|| !featuresPath.toFile().exists();
		}
		if (badStructure) {
			throw new CoreException(LauncherUtils.createErrorStatus(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_badFeatureSetup));
		}
		// Ensure important files are present
		ensureProductFilesExist(getProductPath());		
	}
	
	protected IPath getInstallPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation();
	}
	
	protected IPath getProductPath() {
		return getInstallPath().removeLastSegments(1);
	}

	protected String computeShowsplashArgument() {
		IPath eclipseHome = ExternalModelManager.getEclipseHome();
		IPath fullPath = eclipseHome.append("eclipse"); //$NON-NLS-1$
		return fullPath.toOSString() + " -showsplash 600"; //$NON-NLS-1$
	}

	protected void ensureProductFilesExist(IPath productArea) {
		File productDir = productArea.toFile();		
		File marker = new File(productDir, ".eclipseproduct"); //$NON-NLS-1$
		IPath eclipsePath = ExternalModelManager.getEclipseHome();
		if (!marker.exists()) 
			copyFile(eclipsePath, ".eclipseproduct", marker); //$NON-NLS-1$
		
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			File configDir = new File(productDir, "configuration"); //$NON-NLS-1$
			if (!configDir.exists())
				configDir.mkdirs();		
			File ini = new File(configDir, "config.ini");			 //$NON-NLS-1$
			if (!ini.exists())
				copyFile(eclipsePath.append("configuration"), "config.ini", ini); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			File ini = new File(productDir, "install.ini"); //$NON-NLS-1$
			if (!ini.exists()) 
				copyFile(eclipsePath, "install.ini", ini);		 //$NON-NLS-1$
		}
	}

	protected void copyFile(IPath eclipsePath, String name, File target) {
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
		return computeBuildOrder(LauncherUtils.getAffectedProjects(configuration));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getProjectsForProblemSearch(
			ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return LauncherUtils.getAffectedProjects(configuration);
	}
	
	protected File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null) {
			try {
				if (config.getAttribute(USEFEATURES, false) && config.getAttribute(CONFIG_USE_DEFAULT_AREA, true)) {
					String root = getProductPath().toString();
					if (PDECore.getDefault().getModelManager().isOSGiRuntime())
						root += "/configuration"; //$NON-NLS-1$
					fConfigDir = new File(root);
				} else {
					fConfigDir = LauncherUtils.createConfigArea(config);
				}
			} catch (CoreException e) {
				fConfigDir = LauncherUtils.createConfigArea(config);
			}
		}
		if (!fConfigDir.exists())
			fConfigDir.mkdirs();
		return fConfigDir;
	}
}
