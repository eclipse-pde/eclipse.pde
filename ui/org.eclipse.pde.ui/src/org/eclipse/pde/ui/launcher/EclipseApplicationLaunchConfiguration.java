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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;

/**
 * A launch delegate for launching Eclipse applications
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.2
 */
public class EclipseApplicationLaunchConfiguration extends AbstractPDELaunchConfiguration {
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
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
			Map pluginMap = LaunchPluginValidator.getPluginsToRun(configuration);
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
		
		String[] args = super.getProgramArguments(configuration);
		for (int i = 0; i < args.length; i++) {
			programArgs.add(args[i]);
		}
		
		if (!programArgs.contains("-nosplash") && showSplash) { //$NON-NLS-1$
			if (TargetPlatform.getTargetVersion() >= 3.1) {
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
			throw new CoreException(VMHelper.createErrorStatus(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_badFeatureSetup));
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#getConfigDir(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected File getConfigDir(ILaunchConfiguration config) {
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

	/**
	 * Prompts and clears the workspace area and/or the configuration area, if appropriate
	 * 
	 * @see org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) 
		throws CoreException {		
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
	}
}
