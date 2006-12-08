/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.launcher.TestSearchResult;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.launcher.VMHelper;

/**
 * A launch delegate for launching JUnit Plug-in tests.
 * <p>
 * <b>
 * Note: This class may still undergo significant changes in 3.3 before it stabilizes
 * </b>
 * </p>
 * @since 3.3
 */
public class JUnitLaunchConfigurationDelegate extends JUnitBaseLaunchConfiguration  {

	public static final String CORE_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$
	public static final String UI_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication"; //$NON-NLS-1$
	public static final String LEGACY_UI_APPLICATION = "org.eclipse.pde.junit.runtime.legacytestapplication"; //$NON-NLS-1$
	
	public static String[] REQUIRED_PLUGINS = {"org.junit", "org.eclipse.jdt.junit.runtime", "org.eclipse.pde.junit.runtime"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	protected File fConfigDir = null;
	private Map fPluginMap;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
				throws CoreException {
		try {
			fConfigDir = null;
			monitor.beginTask("", 4); //$NON-NLS-1$
			TestSearchResult testSearchResult = findTestTypes(configuration, new SubProgressMonitor(monitor, 1));
			IType[] testTypes = testSearchResult.getTypes();
			
			// Get the list of plug-ins to run
			fPluginMap = LaunchPluginValidator.getPluginsToRun(configuration);
			
			// implicitly add the plug-ins required for JUnit testing if necessary
			for (int i = 0; i < REQUIRED_PLUGINS.length; i++) {
				String id = REQUIRED_PLUGINS[i];
				if (!fPluginMap.containsKey(id)) {
					fPluginMap.put(id, findPlugin(id));
				}
			}
			
			try {
				preLaunchCheck(configuration, launch, new SubProgressMonitor(monitor, 2));
			} catch (CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					monitor.setCanceled(true);
					return;
				}
				throw e;
			}
			
			int port = SocketUtil.findFreePort();
			launch.setAttribute(PORT_ATTR, Integer.toString(port));
			launch.setAttribute(TESTTYPE_ATTR, testTypes[0].getHandleIdentifier());
			VMRunnerConfiguration runnerConfig = createVMRunner(configuration, testSearchResult, port, mode);
			monitor.worked(1);
			
			setDefaultSourceLocator(launch, configuration);
			manageLaunch(launch);
			IVMRunner runner = getVMRunner(configuration, mode);
			if (runner != null)
				runner.run(runnerConfig, launch, monitor);
			else
				monitor.setCanceled(true);
			monitor.done();
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMRunner(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall launcher = VMHelper.createLauncher(configuration);
		return launcher.getVMRunner(mode);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration#createVMRunner(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.jdt.internal.junit.launcher.TestSearchResult, int, java.lang.String)
	 */
	protected VMRunnerConfiguration createVMRunner(
		ILaunchConfiguration configuration,
		TestSearchResult testTypes,
		int port,
		String runMode)
		throws CoreException {

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", getClasspath(configuration)); //$NON-NLS-1$
		runnerConfig.setVMArguments(new ExecutionArguments(getVMArguments(configuration), "").getVMArgumentsArray()); //$NON-NLS-1$
		runnerConfig.setProgramArguments(getProgramArgumentsArray(configuration, testTypes, port, runMode));
		runnerConfig.setEnvironment(getEnvironment(configuration));
		runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
		runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));
		return runnerConfig;
	}

	private String getTestPluginId(ILaunchConfiguration configuration)
		throws CoreException {
		IJavaProject javaProject = getJavaProject(configuration);
		IPluginModelBase model =
			PDECore.getDefault().getModelManager().findModel(javaProject.getProject());
		if (model == null)
			throw new CoreException(
				new Status(
					IStatus.ERROR,
					IPDEUIConstants.PLUGIN_ID,
					IStatus.ERROR,
					PDEUIMessages.JUnitLaunchConfiguration_error_notaplugin, 
					null));
		if (model instanceof IFragmentModel)
			return ((IFragmentModel)model).getFragment().getPluginId();

		return model.getPluginBase().getId();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration#abort(java.lang.String, java.lang.Throwable, int)
	 */
	protected void abort(String message, Throwable exception, int code)
		throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, code, message, exception));
	}
	
	/**
	 * Returns the program arguments to launch with.
	 * This list is a combination of arguments computed by PDE based on attributes
	 * specified in the given launch configuration, followed by the program arguments
	 * that the entered directly into the launch configuration.
	 *  
	 * @param configuration
	 *            launch configuration
	 * @return the program arguments necessary for launching
	 * 
	 * @exception CoreException
	 *                if unable to retrieve the attribute or create the
	 *                necessary configuration files            
	 */
	private String[] getProgramArgumentsArray(
		ILaunchConfiguration configuration,
		TestSearchResult testSearchResult,
		int port,
		String runMode)
		throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		programArgs.addAll(getBasicArguments(configuration, port, runMode, testSearchResult));
		
		// Specify the JUnit Plug-in test application to launch
		programArgs.add("-application"); //$NON-NLS-1$
		String application = null;
		try {
			application = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
		} catch (CoreException e) {
		}
		if (application == null)
			application = TargetPlatform.usesNewApplicationModel() ? UI_APPLICATION : LEGACY_UI_APPLICATION;
		programArgs.add(application);
		
		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			programArgs.add("-product"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(IPDELauncherConstants.PRODUCT, "")); //$NON-NLS-1$
		} else {
			// Specify the application to test
			String testApplication = configuration.getAttribute(IPDELauncherConstants.APP_TO_TEST, (String)null);
			if (testApplication != null && testApplication.length() > 0) {
				programArgs.add("-testApplication"); //$NON-NLS-1$
				programArgs.add(testApplication);
			}
		}
		
		// Specify the location of the runtime workbench
		String targetWorkspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		if (targetWorkspace.length() > 0) {
			programArgs.add("-data"); //$NON-NLS-1$
			programArgs.add(targetWorkspace);
		}
		
		// Create the platform configuration for the runtime workbench
		String productID = LaunchConfigurationHelper.getProductID(configuration);
		LaunchConfigurationHelper.createConfigIniFile(configuration,
				productID, fPluginMap, getConfigurationDirectory(configuration));
		TargetPlatform.createPlatformConfigurationArea(
				fPluginMap,
				getConfigurationDirectory(configuration),
				LaunchConfigurationHelper.getContributingPlugin(productID));
		
		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigurationDirectory(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
		
		// Specify the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$
		programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigurationDirectory(configuration).toString() + "/dev.properties", fPluginMap)); //$NON-NLS-1$
		
		// necessary for PDE to know how to load plugins when target platform = host platform
		// see PluginPathFinder.getPluginPaths()
		if (fPluginMap.containsKey(PDECore.getPluginId()))
			programArgs.add("-pdelaunch"); //$NON-NLS-1$	

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false)
				&& !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(
						IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			String path = getConfigurationDirectory(configuration).getPath() + IPath.SEPARATOR + ".options"; //$NON-NLS-1$
			programArgs.add(LaunchArgumentsHelper.getTracingFileArgument(configuration, path));
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
			
		programArgs.add("-testpluginname"); //$NON-NLS-1$
		programArgs.add(getTestPluginId(configuration));
		
		programArgs.add("-loaderpluginname"); //$NON-NLS-1$
		programArgs.add(testSearchResult.getTestKind().getLoaderPluginId());
		
		String testFailureNames = configuration.getAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, ""); //$NON-NLS-1$
		if (testFailureNames.length() > 0) {
			programArgs.add("-testfailures"); //$NON-NLS-1$
			programArgs.add(testFailureNames);			
		}

		// a testname was specified just run the single test
		IType[] testTypes = testSearchResult.getTypes();
		String testName =
			configuration.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, ""); //$NON-NLS-1$
		if (testName.length() > 0) {
			programArgs.add("-test"); //$NON-NLS-1$
			programArgs.add(testTypes[0].getFullyQualifiedName() + ":" + testName); //$NON-NLS-1$
		} else {
			programArgs.add("-classnames"); //$NON-NLS-1$
			for (int i = 0; i < testTypes.length; i++)
			programArgs.add(testTypes[i].getFullyQualifiedName());
		}
		return (String[]) programArgs.toArray(new String[programArgs.size()]);
	}
		
	private IPluginModelBase findPlugin(String id) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(id);
		if (model == null)
			model = PDECore.getDefault().findPluginInHost(id);
		if (model == null)
			abort(
				NLS.bind(PDEUIMessages.JUnitLaunchConfiguration_error_missingPlugin, id),
				null,
				IStatus.OK);
		return model;
	}
		
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getProgramArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return LaunchArgumentsHelper.getUserProgramArguments(configuration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getVMArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return LaunchArgumentsHelper.getUserVMArguments(configuration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getEnvironment(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getClasspath(org.eclipse.debug.core.ILaunchConfiguration)
	 */	
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			abort(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
		}
		return classpath;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getWorkingDirectory(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getWorkingDirectory(configuration);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMSpecificAttributesMap(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#setDefaultSourceLocator(org.eclipse.debug.core.ILaunch, org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		String id = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				(String) null);
		if (!PDESourcePathProvider.ID.equals(id)) {
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
							PDESourcePathProvider.ID); 
			wc.doSave();
		}
	}
	
	/**
	 * Returns the location of the configuration area
	 * 
	 * @param configuration
	 * 				the launch configuration
	 * @return a directory where the configuration area is located
	 */
	protected File getConfigurationDirectory(ILaunchConfiguration configuration) {
		if (fConfigDir == null)
			fConfigDir = LaunchConfigurationHelper.getConfigurationArea(configuration);	
		return fConfigDir;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getBuildOrder(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration,
			String mode) throws CoreException {
		return computeBuildOrder(LaunchPluginValidator.getAffectedProjects(configuration));
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	protected IProject[] getProjectsForProblemSearch(
			ILaunchConfiguration configuration, String mode)
			throws CoreException {
		return LaunchPluginValidator.getAffectedProjects(configuration);
	}
	
	/**
	 * Adds a listener to the launch to be notified at interesting launch lifecycle
	 * events such as when the launch terminates.
	 * 
	 * @param launch
	 * 			the launch 			
	 */
	protected void manageLaunch(ILaunch launch) {
		PDEPlugin.getDefault().getLaunchListener().manage(launch);		
	}
	
	/**
 	 * Does sanity checking before launching.  The criteria whether the launch should 
 	 * proceed or not is specific to the launch configuration type.
 	 * 
 	 * @param configuration launch configuration
 	 * @param launch the launch object to contribute processes and debug targets to
 	 * @param monitor a progress monitor
 	 * 
 	 * @throws CoreException exception thrown if launch fails or canceled or if unable to retrieve attributes
 	 * from the launch configuration
 	 * 				
 	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) 
			throws CoreException {
		boolean autoValidate = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		monitor.beginTask("", autoValidate ? 3 : 4); //$NON-NLS-1$
		if (autoValidate)
			validatePluginDependencies(configuration, new SubProgressMonitor(monitor, 1));
		validateProjectDependencies(configuration, new SubProgressMonitor(monitor, 1));
		clear(configuration, new SubProgressMonitor(monitor, 1));
		launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigurationDirectory(configuration).toString());		
		synchronizeManifests(configuration, new SubProgressMonitor(monitor, 1));		
	}
	/**
	 * Checks for old-style plugin.xml files that have become stale since the last launch.
	 * For any stale plugin.xml files found, the corresponding MANIFEST.MF is deleted 
	 * from the runtime configuration area so that it gets regenerated upon startup.
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			the progress monitor
	 */
	protected void synchronizeManifests(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigurationDirectory(configuration));
		monitor.done();
	}

	/**
	 * Clears the workspace prior to launching if the workspace exists and the option to 
	 * clear it is turned on.  Also clears the configuration area if that option is chosen.
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			the progress monitor
	 * @throws CoreException
	 * 			if unable to retrieve launch attribute values
	 * @since 3.3
	 */
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		String workspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		// Clear workspace and prompt, if necessary
		if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
			monitor.setCanceled(true);
			return;
		}

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigurationDirectory(configuration));	
	}

	/**
	 * Checks if the Automated Management of Dependencies option is turned on.
	 * If so, it makes aure all manifests are updated with the correct dependencies.
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			a progress monitor
	 */
	protected void validateProjectDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		LauncherUtils.validateProjectDependencies(configuration, monitor);
	}
	
	/**
	 * Validates inter-bundle dependencies automatically prior to launching
	 * if that option is turned on.
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			a progress monitor
	 */
	protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(fPluginMap);
		IPluginModelBase[] models = (IPluginModelBase[])fPluginMap.values().toArray(new IPluginModelBase[fPluginMap.size()]);
		LaunchPluginValidator.validatePluginDependencies(models, monitor);
	}
}
