package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginRegistryModel;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.RegistryLoader;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration implements ILauncherSettings {

	private static final String KEY_NO_STARTUP =
		"WorkbenchLauncherConfigurationDelegate.noStartup";
		
	public static final String[] fgApplicationNames= new String[] {
			"org.eclipse.pde.junit.runtime.uitestapplication",
			"org.eclipse.pde.junit.runtime.coretestapplication"
	};
	public static final String fgDefaultApp= fgApplicationNames[0];
	
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
			IType[] testTypes = getTestTypes(configuration, javaProject, monitor);
			if (testTypes.length == 0) {
				abort(PDEPlugin.getResourceString("JUnitLaunchConfiguration.error.notests"), null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
			}
			monitor.worked(1);
			
			IVMInstall launcher = LauncherUtils.createLauncher(configuration);
			monitor.worked(1);

			int port = SocketUtil.findUnusedLocalPort("", 5000, 15000); //$NON-NLS-1$
			VMRunnerConfiguration runConfig =
				createVMRunner(configuration, testTypes, port, mode);
			monitor.worked(1);
			
			String workspace = configuration.getAttribute(LOCATION + "0", LauncherUtils.getJUnitTempWorkspace());
			LauncherUtils.clearWorkspace(configuration,workspace);
			setDefaultSourceLocator(launch, configuration);
			launch.setAttribute(PORT_ATTR, Integer.toString(port));
			launch.setAttribute(TESTTYPE_ATTR, testTypes[0].getHandleIdentifier());
			launcher.getVMRunner(mode).run(runConfig, launch, monitor);
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	/*
	 * @see JUnitBaseLauncherDelegate#configureVM(IType[], int, String)
	 */
	protected VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, IType[] testTypes, int port, String runMode) throws CoreException {
		String[] classpath = LauncherUtils.constructClasspath();
		if (classpath == null) {
			abort(PDEPlugin.getResourceString(KEY_NO_STARTUP), null, IStatus.OK);
		}

		VMRunnerConfiguration runnerConfig =
			new VMRunnerConfiguration("org.eclipse.core.launcher.Main", classpath);
		runnerConfig.setVMArguments(computeVMArguments(configuration));
		runnerConfig.setProgramArguments(computeProgramArguments(configuration, testTypes, port, runMode));
		
		return runnerConfig;
	}

	protected String getTestPluginId(ILaunchConfiguration configuration)
		throws CoreException {
		IJavaProject javaProject = getJavaProject(configuration);
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
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
		
		programArgs.add("-application");
		programArgs.add(configuration.getAttribute(APPLICATION, fgDefaultApp));

		String targetWorkspace = configuration.getAttribute(LOCATION + "0", LauncherUtils.getJUnitTempWorkspace());
		programArgs.add("-data");
		programArgs.add(targetWorkspace);
		
		boolean useDefault = configuration.getAttribute(USECUSTOM, true);
		IPluginModelBase[] plugins =
			LauncherUtils.validatePlugins(
				LauncherUtils.getWorkspacePluginsToRun(configuration, useDefault),
				getExternalPluginsToRun(configuration, useDefault));
		if (plugins == null)
			return null;
			
		plugins = addRequiredPlugins(plugins);

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
	
	/**
	 * @param plugins
	 */
	private IPluginModelBase[] addRequiredPlugins(IPluginModelBase[] plugins) throws CoreException {
		boolean pdeJunitMissing = true;
		boolean jdtJunitMissing = true;
		boolean junitMissing = true;
		for (int i = 0; i < plugins.length; i++) {
			if (!pdeJunitMissing && !jdtJunitMissing && !junitMissing)
				break;
			String id = plugins[i].getPluginBase().getId();
			if (id.equals("org.eclipse.pde.junit.runtime")) {
				pdeJunitMissing = false;
			} else if (id.equals("org.eclipse.jdt.junit.runtime")) {
				jdtJunitMissing = false;
			} else if (id.equals("org.junit")) {
				junitMissing = false;
			}
		}

		ArrayList extraPlugins = new ArrayList();
		if (pdeJunitMissing) {
			extraPlugins.add(findPlugin("org.eclipse.pde.junit.runtime"));
		}
		if (jdtJunitMissing) {
			extraPlugins.add(findPlugin("org.eclipse.jdt.junit.runtime"));
		} 
		if (junitMissing) {
			extraPlugins.add(findPlugin("org.junit"));
		}
		if (extraPlugins.size() > 0) {
			IPluginModelBase[] all =
				new IPluginModelBase[plugins.length + extraPlugins.size()];
			System.arraycopy(plugins, 0, all, 0, plugins.length);
			for (int i = 0; i < extraPlugins.size(); i++) {
				all[plugins.length + i] = (IPluginModelBase) extraPlugins.get(i);
			}
			return all;
		}
		return plugins;	
	}
	
	private IPluginModelBase findPlugin(String id) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findPlugin(id, null, 0);
		if (model != null)
			return model;
		PluginRegistryModel registryModel = (PluginRegistryModel) Platform.getPluginRegistry();
		PluginDescriptorModel plugin = registryModel.getPlugin(id);
		if (plugin == null)
			abort(
				PDEPlugin.getFormattedMessage("JUnitLaunchConfiguration.error.missingPlugin", id),
				null,
				IStatus.OK);
		return RegistryLoader.processPluginModel(plugin, false);
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


}