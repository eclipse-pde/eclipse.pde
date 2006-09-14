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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
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
import org.eclipse.pde.internal.ui.launcher.PluginValidationDialog;
import org.eclipse.pde.internal.ui.launcher.PluginValidationOperation;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.swt.widgets.Display;
import org.eclipse.update.configurator.ConfiguratorUtils;


public class JUnitLaunchConfigurationDelegate extends JUnitBaseLaunchConfiguration  {

	public static final String CORE_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$
	public static final String UI_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication"; //$NON-NLS-1$
	
	protected static IPluginModelBase[] registryPlugins;
	protected File fConfigDir = null;

	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		try {
			fConfigDir = null;
			monitor.beginTask("", 7); //$NON-NLS-1$
			TestSearchResult testSearchResult = findTestTypes(configuration, monitor);
			IType[] testTypes = testSearchResult.getTypes();
			monitor.worked(1);
			
			validateProjectDependencies(configuration, new SubProgressMonitor(monitor, 1));
			
			promptToClear(configuration, new SubProgressMonitor(monitor, 1));
			launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigDir(configuration).toString());
			
			int port = SocketUtil.findFreePort();
			VMRunnerConfiguration runnerConfig = createVMRunner(configuration, testSearchResult, port, mode);
			if (runnerConfig == null) {
				monitor.setCanceled(true);
				return;
			}
			monitor.worked(1);
			
			setDefaultSourceLocator(launch, configuration);
			synchronizeManifests(configuration);
			launch.setAttribute(PORT_ATTR, Integer.toString(port));
			launch.setAttribute(TESTTYPE_ATTR, testTypes[0].getHandleIdentifier());
			manageLaunch(launch);
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
	
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall launcher = VMHelper.createLauncher(configuration);
		return launcher.getVMRunner(mode);
	}
	/*
	 * @see JUnitBaseLauncherDelegate#configureVM(IType[], int, String)
	 */
	protected VMRunnerConfiguration createVMRunner(
		ILaunchConfiguration configuration,
		TestSearchResult testTypes,
		int port,
		String runMode)
		throws CoreException {

		// Program arguments
		String[] programArgs = getProgramArgumentsArray(configuration, testTypes, port, runMode);
		if (programArgs == null)
			return null;

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", getClasspath(configuration)); //$NON-NLS-1$
		runnerConfig.setVMArguments(getVMArgumentsArray(configuration));
		runnerConfig.setProgramArguments(programArgs);
		runnerConfig.setEnvironment(getEnvironment(configuration));
		runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
		runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));
		return runnerConfig;
	}

	protected String getTestPluginId(ILaunchConfiguration configuration)
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
	
	protected void abort(String message, Throwable exception, int code)
		throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, code, message, exception));
	}
	
	public String[] getProgramArgumentsArray(
		ILaunchConfiguration configuration,
		TestSearchResult testSearchResult,
		int port,
		String runMode)
		throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		// Get the list of plug-ins to run
		Map pluginMap = LaunchPluginValidator.getPluginsToRun(configuration);
		
		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false)) {
			IPluginModelBase[] models = (IPluginModelBase[])pluginMap.values().toArray(new IPluginModelBase[pluginMap.size()]);
			final PluginValidationOperation op = new PluginValidationOperation(models);
			try {
				op.run(new NullProgressMonitor());
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			} finally {
				if (op.hasErrors()) {
					final int[] result = new int[1];
					final Display display = LauncherUtils.getDisplay();
					display.syncExec(new Runnable() {
						public void run() {
							result[0] = new PluginValidationDialog(display.getActiveShell(), op).open();
					}});
					if (result[0] == IDialogConstants.CANCEL_ID) {
						return null;
					}
				}
			}
		}

		addRequiredPlugins(pluginMap, testSearchResult);
		
		programArgs.addAll(getBasicArguments(configuration, port, runMode, testSearchResult));
		
		// Specify the application to launch based on the list of plug-ins to run.
		programArgs.add("-application"); //$NON-NLS-1$
		programArgs.add(getApplicationName(pluginMap, configuration));
		
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
				productID, pluginMap, getConfigDir(configuration));
		TargetPlatform.createPlatformConfigurationArea(
				pluginMap,
				getConfigDir(configuration),
				LaunchConfigurationHelper.getContributingPlugin(productID));
		
		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
		
		// Specify the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$
		programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", pluginMap)); //$NON-NLS-1$
		
		// necessary for PDE to know how to load plugins when target platform = host platform
		// see PluginPathFinder.getPluginPaths()
		if (pluginMap.containsKey(PDECore.getPluginId()))
			programArgs.add("-pdelaunch"); //$NON-NLS-1$	

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false)
				&& !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(
						IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			String path = getConfigDir(configuration).getPath() + IPath.SEPARATOR + ".options"; //$NON-NLS-1$
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
		
	protected IPluginModelBase[] addRequiredPlugins(Map pluginMap, TestSearchResult result)
		throws CoreException {
		addRequiredPlugin(pluginMap, "org.eclipse.pde.junit.runtime"); //$NON-NLS-1$
		addRequiredPlugin(pluginMap, "org.eclipse.jdt.junit.runtime"); //$NON-NLS-1$
		addRequiredPlugin(pluginMap, "org.junit"); //$NON-NLS-1$
		addRequiredPlugin(pluginMap, result.getTestKind().getLoaderPluginId());
		return (IPluginModelBase[]) pluginMap.values().toArray(
			new IPluginModelBase[pluginMap.size()]);
	}
	
	private void addRequiredPlugin(Map pluginMap, final String pluginId) throws CoreException {
		if (!pluginMap.containsKey(pluginId)) { //$NON-NLS-1$
			pluginMap.put(pluginId, findPlugin(pluginId)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected IPluginModelBase findPlugin(String id) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(id);
		if (model != null)
			return model;

		if (registryPlugins == null) {
			URL[] pluginPaths = ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
			PDEState state = new PDEState(pluginPaths, false, new NullProgressMonitor());
			registryPlugins = state.getTargetModels();
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		abort(
			NLS.bind(PDEUIMessages.JUnitLaunchConfiguration_error_missingPlugin, id),
			null,
			IStatus.OK);
		return null;
	}
	
	public String[] getVMArgumentsArray(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getUserVMArgumentArray(configuration);
	}
	
	public String getProgramArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return LaunchArgumentsHelper.getUserProgramArguments(configuration);
	}
	
	public String getVMArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return LaunchArgumentsHelper.getUserVMArguments(configuration);
	}
	
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
	}
	
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			abort(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
		}
		return classpath;
	}
	
	public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getWorkingDirectory(configuration);
	}
	
	public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
	}

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
	
	protected String getDefaultWorkspace(ILaunchConfiguration config) throws CoreException {
		if (config.getAttribute(IPDELauncherConstants.APPLICATION, UI_APPLICATION).equals(UI_APPLICATION))
			return "${workspace_loc}/../junit-workbench-workspace"; //$NON-NLS-1$
		return "${workspace_loc}/../junit-core-workspace"; //$NON-NLS-1$
	}
	
	protected String getApplicationName(Map pluginMap, ILaunchConfiguration configuration) {
		try {
			String application = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
			if (CORE_APPLICATION.equals(application)) 
				return CORE_APPLICATION;				
		} catch (CoreException e) {
		}
		return UI_APPLICATION;
	}
		
	protected File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null)
			fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
	
		return fConfigDir;
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
	
	public void manageLaunch(ILaunch launch) {
		PDEPlugin.getDefault().getLaunchListener().manage(launch);		
	}
	
	public void synchronizeManifests(ILaunchConfiguration configuration) {
		LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigDir(configuration));
	}

	protected void promptToClear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		String workspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		// Clear workspace and prompt, if necessary
		if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
			monitor.setCanceled(true);
			return;
		}

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigDir(configuration));	
	}

	protected void validateProjectDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		LauncherUtils.validateProjectDependencies(configuration, monitor);
	}


}
