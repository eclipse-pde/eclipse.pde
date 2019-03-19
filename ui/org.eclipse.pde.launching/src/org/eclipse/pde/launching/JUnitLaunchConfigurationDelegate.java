/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     David Saff <saff@mit.edu> - bug 102632
 *     Ketan Padegaonkar <KetanPadegaonkar@gmail.com> - bug 250340
 *******************************************************************************/
package org.eclipse.pde.launching;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.internal.launching.launcher.*;
import org.osgi.framework.Version;

/**
 * A launch delegate for launching JUnit Plug-in tests.
 * <p>
 * This class originally existed in 3.3 as
 * <code>org.eclipse.pde.ui.launcher.JUnitLaunchConfigurationDelegate</code>.
 * </p>
 * @since 3.6
 */
public class JUnitLaunchConfigurationDelegate extends org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate {

	/**
	 * To avoid duplicating variable substitution (and duplicate prompts)
	 * this variable will store the substituted workspace location.
	 */
	private String fWorkspaceLocation;

	/**
	 * Caches the configuration directory when a launch is started
	 */
	protected File fConfigDir = null;

	// used to generate the dev classpath entries
	// key is bundle ID, value is a model
	private Map<String, IPluginModelBase> fAllBundles;

	// key is a model, value is startLevel:autoStart
	private Map<IPluginModelBase, String> fModels;

	private static final String PDE_JUNIT_SHOW_COMMAND = "pde.junit.showcommandline"; //$NON-NLS-1$

	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall launcher = VMHelper.createLauncher(configuration);
		return launcher.getVMRunner(mode);
	}

	@Override
	public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		if (TargetPlatformHelper.getTargetVersion() >= 3.3)
			return "org.eclipse.equinox.launcher.Main"; //$NON-NLS-1$
		return "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
	}

	private String getTestPluginId(ILaunchConfiguration configuration) throws CoreException {
		IJavaProject javaProject = getJavaProject(configuration);
		IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
		if (model == null)
			abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_notaplugin, javaProject.getProject().getName()), null, IStatus.OK);
		if (model instanceof IFragmentModel)
			return ((IFragmentModel) model).getFragment().getPluginId();

		return model.getPluginBase().getId();
	}

	@Override
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, code, message, exception));
	}

	@Override
	protected void collectExecutionArguments(ILaunchConfiguration configuration, List<String> vmArguments, List<String> programArgs) throws CoreException {
		super.collectExecutionArguments(configuration, vmArguments, programArgs);

		// Specify the JUnit Plug-in test application to launch
		programArgs.add("-application"); //$NON-NLS-1$
		String application = getApplication(configuration);

		programArgs.add(application);

		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			programArgs.add("-product"); //$NON-NLS-1$
			programArgs.add(configuration.getAttribute(IPDELauncherConstants.PRODUCT, "")); //$NON-NLS-1$
		} else {
			// Specify the application to test
			String defaultApplication = TargetPlatform.getDefaultApplication();
			if (IPDEConstants.CORE_TEST_APPLICATION.equals(application)) {
				// If we are launching the core test application we don't need a test app
				defaultApplication = null;
			} else if (IPDEConstants.NON_UI_THREAD_APPLICATION.equals(application)) {
				// When running in a non-UI thread, run the core test app to avoid opening the workbench
				defaultApplication = IPDEConstants.CORE_TEST_APPLICATION;
			}

			String testApplication = configuration.getAttribute(IPDELauncherConstants.APP_TO_TEST, defaultApplication);
			if (testApplication != null) {
				programArgs.add("-testApplication"); //$NON-NLS-1$
				programArgs.add(testApplication);
			}
		}

		// Specify the location of the runtime workbench
		if (fWorkspaceLocation == null) {
			fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		}
		if (fWorkspaceLocation.length() > 0) {
			programArgs.add("-data"); //$NON-NLS-1$
			programArgs.add(fWorkspaceLocation);
		}

		// Create the platform configuration for the runtime workbench
		String productID = LaunchConfigurationHelper.getProductID(configuration);
		LaunchConfigurationHelper.createConfigIniFile(configuration, productID, fAllBundles, fModels, getConfigurationDirectory(configuration));
		TargetPlatformHelper.checkPluginPropertiesConsistency(fAllBundles, getConfigurationDirectory(configuration));

		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + new Path(getConfigurationDirectory(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		// Specify the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$
		programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigurationDirectory(configuration).toString() + "/dev.properties", fAllBundles)); //$NON-NLS-1$

		// necessary for PDE to know how to load plugins when target platform = host platform
		// see PluginPathFinder.getPluginPaths()
		IPluginModelBase base = findPlugin(PDECore.PLUGIN_ID);
		if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) < 0) //$NON-NLS-1$
			programArgs.add("-pdelaunch"); //$NON-NLS-1$

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false) && !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			String path = getConfigurationDirectory(configuration).getPath() + IPath.SEPARATOR + ICoreConstants.OPTIONS_FILENAME;
			programArgs.add(LaunchArgumentsHelper.getTracingFileArgument(configuration, path));
		}

		// add the program args specified by the user
		String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
		for (String userArg : userArgs) {
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (userArg.equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			programArgs.add(userArg);
		}

		if (!configuration.getAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, false)) {
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
		}

		programArgs.add("-testpluginname"); //$NON-NLS-1$
		programArgs.add(getTestPluginId(configuration));
		IVMInstall launcher = VMHelper.createLauncher(configuration);
		boolean isModular = JavaRuntime.isModularJava(launcher);
		if (isModular) {
			String modAllSystem = "--add-modules=ALL-SYSTEM"; //$NON-NLS-1$
			if (!vmArguments.contains(modAllSystem))
				vmArguments.add(modAllSystem);
		}
		//  if element is a test class annotated with @RunWith(JUnitPlatform.class, we add this in program arguments
		if (configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_RUN_WITH_JUNIT_PLATFORM_ANNOTATION, false)) {
			programArgs.add("-runasjunit5"); //$NON-NLS-1$
		}
	}

	@Override
	public String showCommandLine(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		launch.setAttribute(PDE_JUNIT_SHOW_COMMAND, "true"); //$NON-NLS-1$
		return super.showCommandLine(configuration, mode, launch, monitor);
	}
	
	/**
	 * Returns the application to launch plug-in tests with
	 *
	 * @since 3.5
	 *
	 * @param configuration The launch configuration in which the application is specified.
	 * @return the application
	 */
	protected String getApplication(ILaunchConfiguration configuration) {
		String application = null;

		boolean shouldRunInUIThread = true;
		try {
			shouldRunInUIThread = configuration.getAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, true);
		} catch (CoreException e) {
		}

		if (!shouldRunInUIThread) {
			return IPDEConstants.NON_UI_THREAD_APPLICATION;
		}

		try {
			// if application is set, it must be a headless app.
			application = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
		} catch (CoreException e) {
		}

		// if application is not set, we should launch the default UI test app
		// Check to see if we should launch the legacy UI app
		if (application == null) {
			IPluginModelBase model = fAllBundles.get("org.eclipse.pde.junit.runtime"); //$NON-NLS-1$
			BundleDescription desc = model != null ? model.getBundleDescription() : null;
			if (desc != null) {
				Version version = desc.getVersion();
				int major = version.getMajor();
				// launch legacy UI app only if we are launching a target that does
				// not use the new application model and we are launching with a
				// org.eclipse.pde.junit.runtime whose version is >= 3.3
				if (major >= 3 && version.getMinor() >= 3 && !TargetPlatformHelper.usesNewApplicationModel()) {
					application = IPDEConstants.LEGACY_UI_TEST_APPLICATION;
				}
			}
		}

		// launch the UI test application
		if (application == null)
			application = IPDEConstants.UI_TEST_APPLICATION;
		return application;
	}

	private IPluginModelBase findPlugin(String id) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model == null)
			model = PDECore.getDefault().findPluginInHost(id);
		if (model == null)
			abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id), null, IStatus.OK);
		return model;
	}

	@Override
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getUserProgramArguments(configuration);
	}

	@Override
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String vmArgs = LaunchArgumentsHelper.getUserVMArguments(configuration);

		// necessary for PDE to know how to load plugins when target platform = host platform
		IPluginModelBase base = fAllBundles.get(PDECore.PLUGIN_ID);
		if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) >= 0) { //$NON-NLS-1$
			vmArgs = concatArg(vmArgs, "-Declipse.pde.launch=true"); //$NON-NLS-1$
		}
		// For p2 target, add "-Declipse.p2.data.area=@config.dir/p2" unless already specified by user
		if (fAllBundles.containsKey("org.eclipse.equinox.p2.core")) { //$NON-NLS-1$
			if (vmArgs.indexOf("-Declipse.p2.data.area=") < 0) { //$NON-NLS-1$
				vmArgs = concatArg(vmArgs, "-Declipse.p2.data.area=@config.dir" + File.separator + "p2"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return vmArgs;
	}

	/**
	 * Returns the result of concatenating the given argument to the
	 * specified vmArgs.
	 *
	 * @param vmArgs existing VM arguments
	 * @param arg argument to concatenate
	 * @return result of concatenation
	 */
	private String concatArg(String vmArgs, String arg) {
		if (vmArgs.length() > 0 && !vmArgs.endsWith(" ")) //$NON-NLS-1$
			vmArgs = vmArgs.concat(" "); //$NON-NLS-1$
		return vmArgs.concat(arg);
	}

	@Override
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
	}

	@Deprecated
	@Override
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			abort(PDEMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
		}
		return classpath;
	}

	@Override
	public String[][] getClasspathAndModulepath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			abort(PDEMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
		}
		String[][] cpmp = super.getClasspathAndModulepath(configuration);
		cpmp[0] = classpath;
		return cpmp;
	}

	@Override
	public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getWorkingDirectory(configuration);
	}

	@Override
	public Map<String, Object> getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
	}

	@Override
	protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
		if (!PDESourcePathProvider.ID.equals(id)) {
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
			wc.doSave();
		}

		manageLaunch(launch);
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

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return computeBuildOrder(LaunchPluginValidator.getAffectedProjects(configuration));
	}

	@Override
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
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
		PDELaunchingPlugin.getDefault().getLaunchListener().manage(launch);
	}

	@Override
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		fWorkspaceLocation = null;
		fConfigDir = null;
		fModels = BundleLauncherHelper.getMergedBundleMap(configuration, false);
		fAllBundles = new LinkedHashMap<>(fModels.size());
		Iterator<IPluginModelBase> iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = iter.next();
			fAllBundles.put(model.getPluginBase().getId(), model);
		}

		// implicitly add the plug-ins required for JUnit testing if necessary
		String[] requiredPlugins = getRequiredPlugins(configuration);
		for (String requiredPlugin : requiredPlugins) {
			String id = requiredPlugin;
			if (!fAllBundles.containsKey(id)) {
				IPluginModelBase model = findPlugin(id);
				fAllBundles.put(id, model);
				fModels.put(model, "default:default"); //$NON-NLS-1$
			}
		}
		String attribute = launch.getAttribute(PDE_JUNIT_SHOW_COMMAND);
		boolean isShowCommand = false;
		if (attribute != null) {
			isShowCommand = attribute.equals("true"); //$NON-NLS-1$
		}
		boolean autoValidate = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		SubMonitor subMonitor = SubMonitor.convert(monitor, autoValidate ? 3 : 4);
		if (isShowCommand == false) {
			if (autoValidate)
				validatePluginDependencies(configuration, subMonitor.split(1));
			validateProjectDependencies(configuration, subMonitor.split(1));
			LauncherUtils.setLastLaunchMode(launch.getLaunchMode());
			clear(configuration, subMonitor.split(1));
		}
		launch.setAttribute(PDE_JUNIT_SHOW_COMMAND, "false"); //$NON-NLS-1$
		launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigurationDirectory(configuration).toString());
		synchronizeManifests(configuration, subMonitor.split(1));
	}

	private String[] getRequiredPlugins(ILaunchConfiguration configuration) {
		// if we are using JUnit4, we need to include the junit4 specific bundles
		ITestKind testKind = JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId()))
			return new String[] {"org.junit", "org.eclipse.jdt.junit.runtime", "org.eclipse.pde.junit.runtime", "org.eclipse.jdt.junit4.runtime"}; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return new String[] {"org.junit", "org.eclipse.jdt.junit.runtime", "org.eclipse.pde.junit.runtime"}; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
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
		if (fWorkspaceLocation == null) {
			fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 50);

		// Clear workspace and prompt, if necessary
		if (!LauncherUtils.clearWorkspace(configuration, fWorkspaceLocation, subMon.split(25))) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		subMon.setWorkRemaining(25);
		if (subMon.isCanceled()) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigurationDirectory(configuration), subMon.split(25));

		subMon.done();
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
		EclipsePluginValidationOperation op = new EclipsePluginValidationOperation(configuration);
		LaunchPluginValidator.runValidationOperation(op, monitor);
	}
}
