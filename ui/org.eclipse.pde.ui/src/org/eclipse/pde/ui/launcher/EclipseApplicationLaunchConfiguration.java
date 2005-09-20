/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
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
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.ui.launcher.LaunchVMHelper;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;

/**
 * A launch delegate for launching Eclipse applications
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.2
 */
public class EclipseApplicationLaunchConfiguration extends LaunchConfigurationDelegate {
	private File fConfigDir = null;
	
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
			monitor.beginTask("", 4); //$NON-NLS-1$
			
			String workspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
			// Clear workspace and prompt, if necessary
			if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
				monitor.setCanceled(true);
				return;
			}

			// clear config area, if necessary
			if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
				CoreUtility.deleteContent(getConfigDir(configuration));
			launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigDir(configuration).toString());
				
			monitor.worked(1);
			
			// Program arguments
			String[] programArgs = getProgramArguments(configuration);
			if (programArgs == null) {
				monitor.setCanceled(true);
				return;
			}
	
			VMRunnerConfiguration runnerConfig = new VMRunnerConfiguration(
														"org.eclipse.core.launcher.Main",  //$NON-NLS-1$
														getClasspath(configuration)); 
			runnerConfig.setVMArguments(getVMArguments(configuration));
			runnerConfig.setProgramArguments(programArgs);
			runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
			runnerConfig.setEnvironment(getEnvironment(configuration));
			runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

			monitor.worked(1);
					
			setDefaultSourceLocator(configuration);
			LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigDir(configuration));
			PDEPlugin.getDefault().getLaunchListener().manage(launch);
			IVMRunner runner = getVMRunner(configuration, mode);
			if (runner != null)
				runner.run(runnerConfig, launch, monitor);
			else
				monitor.setCanceled(true);
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	
	/**
	 * Returns the VM runner for the given launch mode to use when launching the
	 * given configuration.
	 *  
	 * @param configuration launch configuration
	 * @param mode launch node
	 * @return VM runner to use when launching the given configuration in the given mode
	 * @throws CoreException if a VM runner cannot be determined
	 */
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall launcher = LaunchVMHelper.createLauncher(configuration);
		return launcher.getVMRunner(mode);
	}
	
	/**
	 * Assigns a default source locator to the given launch if a source locator
	 * has not yet been assigned to it, and the associated launch configuration
	 * does not specify a source locator.
	 * 
	 * @param configuration
	 *            configuration being launched
	 * @exception CoreException
	 *                if unable to set the source locator
	 */
	protected void setDefaultSourceLocator(ILaunchConfiguration configuration) throws CoreException {
		LauncherUtils.setDefaultSourceLocator(configuration);		
	}
	
	/**
	 * Returns the entries that should appear on boot classpath.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the location of startup.jar and 
	 * 		the bootstrap classpath specified by the given launch configuration
	 *        
	 * @exception CoreException
	 *                if unable to find startup.jar
	 */
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			String message = PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noStartup;
			throw new CoreException(LaunchVMHelper.createErrorStatus(message));
		}
		return classpath;
	}
	
	/** 
	 * Returns an array of environment variables to be used when
	 * launching the given configuration or <code>null</code> if unspecified.
	 * 
	 * @param configuration launch configuration
	 * @throws CoreException if unable to access associated attribute or if
	 * unable to resolve a variable in an environment variable's value
	 */	
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
	}
	
	/**
	 * Returns the working directory path specified by the given launch
	 * configuration, or <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the working directory path specified by the given launch
	 *         configuration, or <code>null</code> if none
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getWorkingDirectory(configuration);
	}
	
	/**
	 * Returns the Map of VM-specific attributes specified by the given launch
	 * configuration, or <code>null</code> if none.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the <code>Map</code> of VM-specific attributes
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
	}
	
	/**
	 * Returns the VM arguments specified by the given launch configuration, as
	 * an array of strings. 
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the VM arguments specified by the given launch configuration,
	 *         possibly an empty array
	 * @exception CoreException
	 *                if unable to retrieve the attribute
	 */
	public String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getUserVMArgumentArray(configuration);
	}

	/**
	 * Returns the program arguments to launch the Eclipse application with.
	 * This list is a combination of arguments computed by PDE based on attributes
	 * specified in the given launch configuration, followed by the program arguments
	 * that the entered directly into the launch configuration.
	 * 
	 * This computation may require user interaction (i.e an answer to a question), etc.
	 * If the answer is to not proceed, then this method returns null.
	 * 
	 * @param configuration
	 *            launch configuration
	 * @return the program arguments necessar for launching
	 * 				 or <code>null</null>
	 * @exception CoreException
	 *                if unable to retrieve the attribute or if self-hosting could not
	 *                proceed due to a bad setup, missing plug-ins, inability to create the
	 *                necessary configuration files.
	 *              
	 */
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			programArgs.add("-product"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(IPDELauncherConstants.PRODUCT, "")); //$NON-NLS-1$
		} else {
			// specify the application to launch
			programArgs.add("-application"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(IPDELauncherConstants.APPLICATION, LaunchConfigurationHelper.getDefaultApplicationName()));
		}
		
		// specify the workspace location for the runtime workbench
		String targetWorkspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		if (targetWorkspace.length() > 0) {
			programArgs.add("-data"); //$NON-NLS-1$
			programArgs.add(targetWorkspace);
		}
		
		boolean isOSGI = PDECore.getDefault().getModelManager().isOSGiRuntime();
		boolean showSplash = true;
		if (configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false)) {
			validateFeatures();
			IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
			programArgs.add("-install"); //$NON-NLS-1$
			programArgs.add("file:" + installPath.removeLastSegments(1).addTrailingSeparator().toString()); //$NON-NLS-1$
			if (isOSGI && !configuration.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
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
    		// necessary for PDE to know how to load plugins when target platform = host platform
    		// see PluginPathFinder.getPluginPaths()
     		programArgs.add("-pdelaunch"); //$NON-NLS-1$           
		} else {
			TreeMap pluginMap = LaunchPluginValidator.getPluginsToRun(configuration);
			if (pluginMap == null) 
				return null;
				
			if (isOSGI) {
				String productID = LaunchConfigurationHelper.getProductID(configuration);
				Properties prop = LaunchConfigurationHelper.createConfigIniFile(configuration,
						productID, pluginMap, getConfigDir(configuration));
				showSplash = prop.containsKey("osgi.splashPath") || prop.containsKey("splashLocation"); //$NON-NLS-1$ //$NON-NLS-2$
				TargetPlatform.createPlatformConfigurationArea(
						pluginMap,
						getConfigDir(configuration),
						LaunchConfigurationHelper.getContributingPlugin(productID));
			} else {
				String primaryPlugin = LaunchConfigurationHelper.getPrimaryPlugin();
				TargetPlatform.createPlatformConfigurationArea(
						pluginMap,
						getConfigDir(configuration),
						primaryPlugin);
				if (primaryPlugin != null) {
					programArgs.add("-feature"); //$NON-NLS-1$
					programArgs.add(primaryPlugin);					
				}
				IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get("org.eclipse.core.boot"); //$NON-NLS-1$
				String bootPath = LaunchConfigurationHelper.getBootPath(bootModel);
				if (bootPath != null && !bootPath.endsWith(".jar")) { //$NON-NLS-1$
					programArgs.add("-boot"); //$NON-NLS-1$
					programArgs.add("file:" + bootPath); //$NON-NLS-1$
				}				
			}
			
			programArgs.add("-configuration"); //$NON-NLS-1$
			if (isOSGI)
				programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
			else
				programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).append("platform.cfg").toString()); //$NON-NLS-1$ //$NON-NLS-2$
			
            // add the output folder names
            programArgs.add("-dev"); //$NON-NLS-1$
            if (PDECore.getDefault().getModelManager().isOSGiRuntime())
                programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", pluginMap)); //$NON-NLS-1$
            else
                programArgs.add(ClasspathHelper.getDevEntries(true));            

    		// necessary for PDE to know how to load plugins when target platform = host platform
    		// see PluginPathFinder.getPluginPaths()
    		if (pluginMap.containsKey(PDECore.getPluginId()))
    			programArgs.add("-pdelaunch"); //$NON-NLS-1$	
		}
		

		// add tracing, if turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false)
				&& !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			programArgs.add(
					LaunchArgumentsHelper.getTracingFileArgument(
					configuration,
					getConfigDir(configuration).toString() + Path.SEPARATOR + ".options")); //$NON-NLS-1$
		}

		// add the program args specified by the user
		String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
		for (int i = 0; i < userArgs.length; i++) {
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (userArgs[i].equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			programArgs.add(userArgs[i]);
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
	
	private void validateFeatures() throws CoreException {
		IPath installPath = PDEPlugin.getWorkspace().getRoot().getLocation();
		String lastSegment = installPath.lastSegment();
		boolean badStructure = lastSegment == null;
		if (!badStructure) {
			IPath featuresPath = installPath.removeLastSegments(1).append("features"); //$NON-NLS-1$
			badStructure = !lastSegment.equalsIgnoreCase("plugins") //$NON-NLS-1$
					|| !featuresPath.toFile().exists();
		}
		if (badStructure) {
			throw new CoreException(LaunchVMHelper.createErrorStatus(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_badFeatureSetup));
		}
		// Ensure important files are present
		ensureProductFilesExist(getProductPath());		
	}
	
	private IPath getProductPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
	}

	private String computeShowsplashArgument() {
		IPath eclipseHome = ExternalModelManager.getEclipseHome();
		IPath fullPath = eclipseHome.append("eclipse"); //$NON-NLS-1$
		return fullPath.toOSString() + " -showsplash 600"; //$NON-NLS-1$
	}

	private void ensureProductFilesExist(IPath productArea) {
		File productDir = productArea.toFile();		
		File marker = new File(productDir, ".eclipseproduct"); //$NON-NLS-1$
		IPath eclipsePath = ExternalModelManager.getEclipseHome();
		if (!marker.exists()) 
			CoreUtility.copyFile(eclipsePath, ".eclipseproduct", marker); //$NON-NLS-1$
		
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			File configDir = new File(productDir, "configuration"); //$NON-NLS-1$
			if (!configDir.exists())
				configDir.mkdirs();		
			File ini = new File(configDir, "config.ini");			 //$NON-NLS-1$
			if (!ini.exists())
				CoreUtility.copyFile(eclipsePath.append("configuration"), "config.ini", ini); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			File ini = new File(productDir, "install.ini"); //$NON-NLS-1$
			if (!ini.exists()) 
				CoreUtility.copyFile(eclipsePath, "install.ini", ini);		 //$NON-NLS-1$
		}
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getBuildOrder(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration,
			String mode) throws CoreException {
		return computeBuildOrder(LaunchPluginValidator.getAffectedProjects(configuration));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getProjectsForProblemSearch(
			ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return LaunchPluginValidator.getAffectedProjects(configuration);
	}
	
	private File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null) {
			try {
				if (config.getAttribute(IPDELauncherConstants.USEFEATURES, false) 
						&& config.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
					String root = getProductPath().toString();
					if (PDECore.getDefault().getModelManager().isOSGiRuntime())
						root += "/configuration"; //$NON-NLS-1$
					fConfigDir = new File(root);
					if (!fConfigDir.exists())
						fConfigDir.mkdirs();
				} else {
					fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
				}
			} catch (CoreException e) {
				fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
			}
		}
		return fConfigDir;
	}
}
