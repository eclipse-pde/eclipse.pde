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
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration implements ILauncherSettings {

	private static final String KEY_NO_STARTUP = "WorkbenchLauncherConfigurationDelegate.noStartup";
		
	public static final String CORE_APPLICATION = "org.eclipse.pde.junit.runtime.coretestapplication";
	public static final String UI_APPLICATION = "org.eclipse.pde.junit.runtime.uitestapplication";
	public static final String LEGACY_UI_APPLICATION = "org.eclipse.pde.junit.runtime.legacyUItestapplication";
	
	private static IPluginModelBase[] registryPlugins;
	private File fConfigDir = null;

	public void launch(
		ILaunchConfiguration configuration,
		String mode,
		ILaunch launch,
		IProgressMonitor monitor)
		throws CoreException {
		try {
			monitor.beginTask("", 4);
			IJavaProject javaProject = getJavaProject(configuration);
			if ((javaProject == null) || !javaProject.exists()) {
				abort(PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.invalidproject"), null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
			}
			IType[] testTypes = getTestTypes(configuration, javaProject, new SubProgressMonitor(monitor, 1));
			if (testTypes.length == 0) {
				abort(PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.notests"), null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
			}
			monitor.worked(1);
			
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
			
			launch.setAttribute(ILauncherSettings.CONFIG_LOCATION,fConfigDir.getPath());
			
			String workspace = configuration.getAttribute(LOCATION + "0", getDefaultWorkspace(configuration));
			LauncherUtils.clearWorkspace(configuration,workspace);

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
		String[] classpath = LauncherUtils.constructClasspath();
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
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath);
		runnerConfig.setVMArguments(computeVMArguments(configuration));
		runnerConfig.setProgramArguments(programArgs);
		runnerConfig.setEnvironment(envp);

		return runnerConfig;
	}

	protected String getTestPluginId(ILaunchConfiguration configuration)
		throws CoreException {
		IJavaProject javaProject = getJavaProject(configuration);
		IWorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		IPluginModelBase model =
			(IPluginModelBase) manager.getWorkspaceModel(javaProject.getProject());
		if (model == null)
			throw new CoreException(
				new Status(
					IStatus.ERROR,
					PDEPlugin.PLUGIN_ID,
					IStatus.ERROR,
					PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.notaplugin"),
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
		programArgs.add("-application");
		programArgs.add(getApplicationName(pluginMap, configuration));
		
		// Specify the application to test
		String testApplication = configuration.getAttribute(APP_TO_TEST, (String)null);
		if (testApplication != null && testApplication.length() > 0) {
			programArgs.add("-testApplication");
			programArgs.add(testApplication);
		}
		
		// Specify the location of the runtime workbench
		String targetWorkspace =
			configuration.getAttribute(LOCATION + "0", getDefaultWorkspace(configuration));
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		
		// Create the platform configuration for the runtime workbench
		String primaryFeatureId = LauncherUtils.getPrimaryFeatureId();
		
		fConfigDir =
			TargetPlatform.createPlatformConfigurationArea(
				pluginMap,
				new Path(targetWorkspace),
				primaryFeatureId,
				LauncherUtils.getAutoStartPlugins(configuration));
		programArgs.add("-configuration");
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			programArgs.add("file:" + fConfigDir.getPath() + "/");
		else
			programArgs.add("file:" + fConfigDir.getPath() + "/platform.cfg");

		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			if (primaryFeatureId != null) {
				programArgs.add("-feature");
				programArgs.add(primaryFeatureId);
			}
			// Pre-OSGi platforms need the location of org.eclipse.core.boot specified
			IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get("org.eclipse.core.boot");
			String bootPath = LauncherUtils.getBootPath(bootModel);
			if (bootPath != null && !bootPath.endsWith(".jar")) {
				programArgs.add("-boot");
				programArgs.add("file:" + bootPath);
			}
		}

		// Specify the output folder names
		programArgs.add("-dev");
		String devEntry = LauncherUtils.getBuildOutputFolders();
		programArgs.add(configuration.getAttribute(CLASSPATH_ENTRIES, devEntry));

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(TRACING, false)
				&& !TRACING_NONE.equals(configuration.getAttribute(
						TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug");
			String path = fConfigDir.getPath() + Path.SEPARATOR + ".options";
			programArgs.add(LauncherUtils.getTracingFileArgument(configuration, path));
		}
		
		// Add the program args entered by the user
		StringTokenizer tokenizer =
			new StringTokenizer(configuration.getAttribute(PROGARGS, ""));
		while (tokenizer.hasMoreTokens()) {
			programArgs.add(tokenizer.nextToken());
		}
			
		if (keepAlive(configuration) && runMode.equals(ILaunchManager.DEBUG_MODE))
			programArgs.add("-keepalive");
		programArgs.add("-consolelog");
		programArgs.add("-port");
		programArgs.add(Integer.toString(port));
		programArgs.add("-testpluginname");
		programArgs.add(getTestPluginId(configuration));

		// a testname was specified just run the single test
		String testName =
			configuration.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, "");
		if (testName.length() > 0) {
			programArgs.add("-test"); //$NON-NLS-1$
			programArgs.add(testTypes[0].getFullyQualifiedName() + ":" + testName);
		} else {
			programArgs.add("-classnames");
			for (int i = 0; i < testTypes.length; i++)
			programArgs.add(testTypes[i].getFullyQualifiedName());
		}
		return (String[]) programArgs.toArray(new String[programArgs.size()]);
	}
	
	private IPluginModelBase[] addRequiredPlugins(TreeMap pluginMap)
		throws CoreException {
		if (!pluginMap.containsKey("org.eclipse.pde.junit.runtime")) {
			pluginMap.put(
				"org.eclipse.pde.junit.runtime",
				findPlugin("org.eclipse.pde.junit.runtime"));
		}
		if (!pluginMap.containsKey("org.eclipse.jdt.junit.runtime")) {
			pluginMap.put(
				"org.eclipse.jdt.junit.runtime",
				findPlugin("org.eclipse.jdt.junit.runtime"));
		}
		if (!pluginMap.containsKey("org.junit")) {
			pluginMap.put("org.junit", findPlugin("org.junit"));
		}
		return (IPluginModelBase[]) pluginMap.values().toArray(
			new IPluginModelBase[pluginMap.size()]);
	}
	
	private IPluginModelBase findPlugin(String id) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findPlugin(id, null, 0);
		if (model != null)
			return model;

		if (registryPlugins == null) {
			String[] pluginPaths =
				PluginPathFinder.getPluginPaths(BootLoader.getInstallURL().getFile());
			Vector models = new Vector();
			RegistryLoader.loadFromDirectories(
				models,
				new Vector(),
				pluginPaths,
				false,
				false,
				new NullProgressMonitor());
			registryPlugins =
				(IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		abort(
			PDEPlugin.getFormattedMessage(
				"JUnitLaunchConfiguration.error.missingPlugin",
				id),
			null,
			IStatus.OK);
		return null;
	}
	
	private String[] computeVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return new ExecutionArguments(getVMArguments(configuration),"").getVMArgumentsArray();		
	}
	
	public String getProgramArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return configuration.getAttribute(ILauncherSettings.PROGARGS, "");
	}
	
	public String getVMArguments(ILaunchConfiguration configuration)
		throws CoreException {
		return configuration.getAttribute(ILauncherSettings.VMARGS, "");
	}

	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
		LauncherUtils.setDefaultSourceLocator(configuration, launch);
	}
	
	private String getDefaultWorkspace(ILaunchConfiguration config) throws CoreException {
		if (config.getAttribute(APPLICATION, UI_APPLICATION).equals(UI_APPLICATION))
			return LauncherUtils.getDefaultPath().append("junit-workbench-workspace").toOSString();
		return LauncherUtils.getDefaultPath().append("junit-core-workspace").toOSString();				
	}
	
	private String getApplicationName(TreeMap pluginMap, ILaunchConfiguration configuration) {
		try {
			String application = configuration.getAttribute(APPLICATION, (String)null);
			if (CORE_APPLICATION.equals(application) || !requiresUI(configuration))
				return CORE_APPLICATION;
		} catch (CoreException e) {
		}
				
		IPluginModelBase model = (IPluginModelBase)pluginMap.get("org.eclipse.ui");
		if (model != null) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				String point = extensions[i].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) {
					if ("workbench".equals(extensions[i].getId())){
						return LEGACY_UI_APPLICATION;
					}
				}
			}
		}
		return UI_APPLICATION;
	}
	
	public static String getPluginID(ILaunchConfiguration configuration) {
		try {
			String projectID = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			if (projectID.length() > 0) {
				IResource project = PDEPlugin.getWorkspace().getRoot().findMember(projectID);
				if (project != null && project instanceof IProject) {
					IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel((IProject)project);
					if (model != null && model instanceof IPluginModelBase) {
						return ((IPluginModelBase)model).getPluginBase().getId();
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
			IPluginModelBase[] models = LauncherUtils.getPluginAndPrereqs(id);
			int i = 0;
			for (; i < models.length; i++) {
				if ("org.eclipse.swt".equals(models[i].getPluginBase().getId()))
					return true;
			}
			return false;
		}
		return true;
	}
}