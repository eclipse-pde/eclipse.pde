package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.eclipse.core.boot.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.build.*;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration implements ILauncherSettings {

	private static final String KEY_NO_STARTUP = "WorkbenchLauncherConfigurationDelegate.noStartup"; //$NON-NLS-1$
		
	public static final String CORE_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$
	public static final String UI_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication"; //$NON-NLS-1$
	public static final String LEGACY_UI_APPLICATION = "org.eclipse.pde.junit.runtime.legacyUItestapplication"; //$NON-NLS-1$
	
	private static IPluginModelBase[] registryPlugins;
	private File fConfigDir = null;

	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		try {
			fConfigDir = null;
			monitor.beginTask("", 6); //$NON-NLS-1$
			IJavaProject javaProject = getJavaProject(configuration);
			if ((javaProject == null) || !javaProject.exists()) {
				abort(PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.invalidproject"), null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
			}
			IType[] testTypes = getTestTypes(configuration, javaProject, new SubProgressMonitor(monitor, 1));
			if (testTypes.length == 0) {
				abort(PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.notests"), null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
			}
			monitor.worked(1);
			
			String workspace = configuration.getAttribute(LOCATION + "0", getDefaultWorkspace(configuration)); //$NON-NLS-1$
			if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
				monitor.setCanceled(true);
				return;
			}

			if (configuration.getAttribute(CONFIG_CLEAR, false))
				LauncherUtils.clearConfigArea(getConfigDir(configuration), new SubProgressMonitor(monitor, 1));
			launch.setAttribute(ILauncherSettings.CONFIG_LOCATION, getConfigDir(configuration).toString());
			
			IVMInstall launcher = LauncherUtils.createLauncher(configuration);
			monitor.worked(1);

			int port = SocketUtil.findFreePort();
			VMRunnerConfiguration runnerConfig =
				createVMRunner(configuration, testTypes, port, mode);
			if (runnerConfig == null) {
				monitor.setCanceled(true);
				return;
			} 
			monitor.worked(1);
			
			setDefaultSourceLocator(launch, configuration);
			launch.setAttribute(PORT_ATTR, Integer.toString(port));
			launch.setAttribute(TESTTYPE_ATTR, testTypes[0].getHandleIdentifier());
			PDEPlugin.getDefault().getLaunchesListener().manage(launch);
			launcher.getVMRunner(mode).run(runnerConfig, launch, monitor);
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	
	/*
	 * @see JUnitBaseLauncherDelegate#configureVM(IType[], int, String)
	 */
	protected VMRunnerConfiguration createVMRunner(
		ILaunchConfiguration configuration,
		IType[] testTypes,
		int port,
		String runMode)
		throws CoreException {
		String[] classpath = LauncherUtils.constructClasspath(configuration);
		if (classpath == null) {
			abort(PDEPlugin.getResourceString(KEY_NO_STARTUP), null, IStatus.OK);
		}

		// Program arguments
		String[] programArgs =
			computeProgramArguments(configuration, testTypes, port, runMode);
		if (programArgs == null)
			return null;

		// Environment variables
		String[] envp =
			DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath); //$NON-NLS-1$
		runnerConfig.setVMArguments(computeVMArguments(configuration));
		runnerConfig.setProgramArguments(programArgs);
		runnerConfig.setEnvironment(envp);

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
					PDEPlugin.PLUGIN_ID,
					IStatus.ERROR,
					PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.notaplugin"), //$NON-NLS-1$
					null));
		return model.getPluginBase().getId();
	}
	
	protected void abort(String message, Throwable exception, int code)
		throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.PLUGIN_ID, code, message, exception));
	}
	
	private String[] computeProgramArguments(
		ILaunchConfiguration configuration,
		IType[] testTypes,
		int port,
		String runMode)
		throws CoreException {
		ArrayList programArgs = new ArrayList();
		
		// Get the list of plug-ins to run
		TreeMap pluginMap = LauncherUtils.getPluginsToRun(configuration);
		if (pluginMap == null)
			return null;		
		addRequiredPlugins(pluginMap);
		
		programArgs.add("-version"); //$NON-NLS-1$
		programArgs.add("3"); //$NON-NLS-1$
		
		// Specify the application to launch based on the list of plug-ins to run.
		programArgs.add("-application"); //$NON-NLS-1$
		programArgs.add(getApplicationName(pluginMap, configuration));
		
		// Specify the application to test
		String testApplication = configuration.getAttribute(APP_TO_TEST, (String)null);
		if (testApplication != null && testApplication.length() > 0) {
			programArgs.add("-testApplication"); //$NON-NLS-1$
			programArgs.add(testApplication);
		}
		
		// Specify the location of the runtime workbench
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", getDefaultWorkspace(configuration)); //$NON-NLS-1$
		programArgs.add("-data"); //$NON-NLS-1$
		programArgs.add(targetWorkspace);
		
		// Create the platform configuration for the runtime workbench
		String primaryFeatureId = LauncherUtils.getPrimaryFeatureId();
		
		TargetPlatform.createPlatformConfigurationArea(
			pluginMap,
			getConfigDir(configuration),
			primaryFeatureId,
			LauncherUtils.getAutoStartPlugins(configuration));
		programArgs.add("-configuration"); //$NON-NLS-1$
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$
		else
			programArgs.add("file:" + new Path(getConfigDir(configuration).getPath()).append("platform.cfg").toString()); //$NON-NLS-1$ //$NON-NLS-2$

		
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			if (primaryFeatureId != null) {
				programArgs.add("-feature"); //$NON-NLS-1$
				programArgs.add(primaryFeatureId);
			}
			// Pre-OSGi platforms need the location of org.eclipse.core.boot specified
			IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get("org.eclipse.core.boot"); //$NON-NLS-1$
			String bootPath = LauncherUtils.getBootPath(bootModel);
			if (bootPath != null && !bootPath.endsWith(".jar")) { //$NON-NLS-1$
				programArgs.add("-boot"); //$NON-NLS-1$
				programArgs.add("file:" + bootPath); //$NON-NLS-1$
			}
		}

		// Specify the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigDir(configuration).toString() + "/dev.properties", true)); //$NON-NLS-1$
		else
			programArgs.add(ClasspathHelper.getDevEntries(true));

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(TRACING, false)
				&& !TRACING_NONE.equals(configuration.getAttribute(
						TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			String path = getConfigDir(configuration).getPath() + Path.SEPARATOR + ".options"; //$NON-NLS-1$
			programArgs.add(LauncherUtils.getTracingFileArgument(configuration, path));
		}
		
		// Add the program args entered by the user
		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, "")); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			programArgs.add(tokenizer.nextToken());
		}
			
		if (keepAlive(configuration) && runMode.equals(ILaunchManager.DEBUG_MODE))
			programArgs.add("-keepalive"); //$NON-NLS-1$
		programArgs.add("-port"); //$NON-NLS-1$
		programArgs.add(Integer.toString(port));
		programArgs.add("-testpluginname"); //$NON-NLS-1$
		programArgs.add(getTestPluginId(configuration));

		// a testname was specified just run the single test
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
	
	private IPluginModelBase[] addRequiredPlugins(TreeMap pluginMap)
		throws CoreException {
		if (!pluginMap.containsKey("org.eclipse.pde.junit.runtime")) { //$NON-NLS-1$
			pluginMap.put(
				"org.eclipse.pde.junit.runtime", //$NON-NLS-1$
				findPlugin("org.eclipse.pde.junit.runtime")); //$NON-NLS-1$
		}
		if (!pluginMap.containsKey("org.eclipse.jdt.junit.runtime")) { //$NON-NLS-1$
			pluginMap.put(
				"org.eclipse.jdt.junit.runtime", //$NON-NLS-1$
				findPlugin("org.eclipse.jdt.junit.runtime")); //$NON-NLS-1$
		}
		if (!pluginMap.containsKey("org.junit")) { //$NON-NLS-1$
			pluginMap.put("org.junit", findPlugin("org.junit")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return (IPluginModelBase[]) pluginMap.values().toArray(
			new IPluginModelBase[pluginMap.size()]);
	}
	
	private IPluginModelBase findPlugin(String id) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(id);
		if (model != null)
			return model;

		if (registryPlugins == null) {
			String[] pluginPaths =
				PluginPathFinder.getPluginPaths(BootLoader.getInstallURL().getFile());
			registryPlugins = TargetPlatformRegistryLoader.loadModels(pluginPaths, false, new NullProgressMonitor());
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		abort(
			PDEPlugin.getFormattedMessage(
				"JUnitLaunchConfiguration.error.missingPlugin", //$NON-NLS-1$
				id),
			null,
			IStatus.OK);
		return null;
	}
	
	private String[] computeVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(getVMArguments(configuration),"").getVMArgumentsArray();		 //$NON-NLS-1$
	}
	
	public String getProgramArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return configuration.getAttribute(ILauncherSettings.PROGARGS, ""); //$NON-NLS-1$
	}
	
	public String getVMArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return configuration.getAttribute(ILauncherSettings.VMARGS, ""); //$NON-NLS-1$
	}

	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
		LauncherUtils.setDefaultSourceLocator(configuration, launch);
	}
	
	private String getDefaultWorkspace(ILaunchConfiguration config) throws CoreException {
		if (config.getAttribute(APPLICATION, UI_APPLICATION).equals(UI_APPLICATION))
			return LauncherUtils.getDefaultPath().append("junit-workbench-workspace").toOSString(); //$NON-NLS-1$
		return LauncherUtils.getDefaultPath().append("junit-core-workspace").toOSString();				 //$NON-NLS-1$
	}
	
	private String getApplicationName(TreeMap pluginMap, ILaunchConfiguration configuration) {
		try {
			String application = configuration.getAttribute(APPLICATION, (String)null);
			if (CORE_APPLICATION.equals(application) || !requiresUI(configuration))
				return CORE_APPLICATION;
		} catch (CoreException e) {
		}
				
		IPluginModelBase model = (IPluginModelBase)pluginMap.get("org.eclipse.ui"); //$NON-NLS-1$
		if (model != null) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				String point = extensions[i].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) { //$NON-NLS-1$
					if ("workbench".equals(extensions[i].getId())){ //$NON-NLS-1$
						return LEGACY_UI_APPLICATION;
					}
				}
			}
		}
		return UI_APPLICATION;
	}
	
	public static String getPluginID(ILaunchConfiguration configuration) {
		try {
			String projectID = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			if (projectID.length() > 0) {
				IResource project = PDEPlugin.getWorkspace().getRoot().findMember(projectID);
				if (project != null && project instanceof IProject) {
					IPluginModelBase model = PDECore.getDefault().getModelManager().findModel((IProject)project);
					if (model != null) {
						return model.getPluginBase().getId();
					}
				}
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	public static boolean requiresUI(ILaunchConfiguration configuration) {
		String id = getPluginID(configuration);
		if (id != null) {
			IPluginModelBase[] models = getPluginAndPrereqs(id);
			int i = 0;
			for (; i < models.length; i++) {
				if ("org.eclipse.swt".equals(models[i].getPluginBase().getId())) //$NON-NLS-1$
					return true;
			}
			return false;
		}
		return true;
	}
	
	public static IPluginModelBase[] getPluginAndPrereqs(String id) {
		TreeMap map = new TreeMap();
		addPluginAndPrereqs(id, map);
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			addPluginAndPrereqs("org.eclipse.core.boot", map); //$NON-NLS-1$
			addPluginAndPrereqs("org.eclipse.core.runtime", map); //$NON-NLS-1$
		}
		
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	private static void addPluginAndPrereqs(String id, TreeMap map) {
		if (map.containsKey(id))
			return;
		
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
		if (entry == null)
			return;
		
		IPluginModelBase model = entry.getActiveModel();
		
		map.put(id, model);
		
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			addPluginAndPrereqs(imports[i].getId(), map);
		}
		
		if (model instanceof IFragmentModel) {
			addPluginAndPrereqs(((IFragmentModel) model).getFragment().getPluginId(), map);
		} else {
			IFragment[] fragments = PDECore.getDefault().findFragmentsFor(id, model.getPluginBase().getVersion());
			for (int i = 0; i < fragments.length; i++) {
				addPluginAndPrereqs(fragments[i].getId(), map);
			}
		}
	}

	
	private File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null) {
			fConfigDir = LauncherUtils.createConfigArea(config.getName());
		}
		return fConfigDir;
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


}