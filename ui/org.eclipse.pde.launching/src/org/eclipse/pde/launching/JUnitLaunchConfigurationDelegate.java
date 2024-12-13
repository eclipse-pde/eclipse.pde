/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 572520 - Run As > JUnit Plugin Test fails if the test is in a source-folder marked as 'includes test sources'
 *******************************************************************************/
package org.eclipse.pde.launching;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.launching.launcher.RequirementHelper;
import org.eclipse.pde.internal.launching.launcher.VMHelper;

/**
 * A launch delegate for launching JUnit Plug-in tests.
 * <p>
 * This class originally existed in 3.3 as
 * <code>org.eclipse.pde.ui.launcher.JUnitLaunchConfigurationDelegate</code>.
 * </p>
 * @since 3.6
 */
public class JUnitLaunchConfigurationDelegate extends org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate {

	static {
		RequirementHelper.registerLaunchTypeRequirements("org.eclipse.pde.ui.JunitLaunchConfig", lc -> { //$NON-NLS-1$
			// Junit launch configs can have the core test application set in either the 'app to test' or the 'application' attribute
			String application = lc.getAttribute(IPDELauncherConstants.APP_TO_TEST, (String) null);
			if (application == null) {
				application = lc.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
			}
			if (application == null) {
				application = TargetPlatform.getDefaultApplication();
			}
			if (!IPDEConstants.CORE_TEST_APPLICATION.equals(application)) {
				return RequirementHelper.getApplicationRequirements(application);
			}
			return Collections.emptyList();
		});
	}

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
	private Map<String, List<IPluginModelBase>> fAllBundles;

	// key is a model, value is startLevel:autoStart
	private Map<IPluginModelBase, String> fModels;
	private String launchMode;

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

	private IPluginBase getTestPlugin(ILaunchConfiguration configuration) throws CoreException {
		IJavaProject javaProject = getJavaProject(configuration);
		IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
		if (model == null) {
			abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_notaplugin, javaProject.getProject().getName()), null, IStatus.OK);
		}
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IPluginBase hostModel = getFragmentHostModel(fragment.getPluginId(), fragment.getPluginVersion(), fragment.getRule());
			if (hostModel == null) {
				abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, fragment.getPluginId()), null, IStatus.OK);
			}
			model = hostModel.getPluginModel();
		}
		return model.getPluginBase();
	}

	private IPluginBase getFragmentHostModel(String hostId, String hostVersion, int hostVersionMatchRule) {
		// return host plug-in model with matching version from bundles selected for launch 
		List<IPluginModelBase> hosts = fAllBundles.getOrDefault(hostId, Collections.emptyList());
		Stream<IPluginBase> hostPlugins = hosts.stream().map(IPluginModelBase::getPluginBase);
		return hostPlugins.filter(h -> VersionUtil.compare(h.getVersion(), hostVersion, hostVersionMatchRule)) //
				.max(Comparator.comparing(IPluginBase::getVersion)).orElse(null);
	}

	@Override
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, code, message, exception));
	}

	@Override
	public String getModuleCLIOptions(ILaunchConfiguration configuration) throws CoreException {
		// The JVM options should be specified in target platform, see getVMArguments()
		return ""; //$NON-NLS-1$ 
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
		IPluginBase testPlugin = getTestPlugin(configuration);
		LaunchConfigurationHelper.createConfigIniFile(configuration, productID, fAllBundles, null, fModels, getConfigurationDirectory(configuration));
		TargetPlatformHelper.checkPluginPropertiesConsistency(fAllBundles, getConfigurationDirectory(configuration));

		programArgs.add("-configuration"); //$NON-NLS-1$
		programArgs.add("file:" + IPath.fromOSString(getConfigurationDirectory(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		// Specify the output folder names
		programArgs.add("-dev"); //$NON-NLS-1$

		IJavaProject javaProject = getJavaProject(configuration);
		Properties devProperties = ClasspathHelper.getDevEntriesProperties(fAllBundles, true);
		if (javaProject != null) {
			// source-folders of type "test" are omitted in the previous search so the need to be added here as they are part of the test but not part of the build.properties
			Arrays.stream(javaProject.getRawClasspath())//
					.filter(entry -> entry.getEntryKind() == IClasspathEntry.CPE_SOURCE)//
					.filter(IClasspathEntry::isTest)//
					.filter(entry -> entry.getOutputLocation() != null).forEach(entry -> {
						IPath relativePath = entry.getOutputLocation().removeFirstSegments(1).makeRelative();
						ClasspathHelper.addDevClasspath(testPlugin, devProperties, relativePath.toString(), true);
					});
		}
		programArgs.add(ClasspathHelper.writeDevEntries(getConfigurationDirectory(configuration).toString() + "/dev.properties", devProperties)); //$NON-NLS-1$

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false) && !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			Path path = getConfigurationDirectory(configuration).toPath().resolve(ICoreConstants.OPTIONS_FILENAME);
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
		programArgs.add(testPlugin.getId());

		IVMInstall launcher = VMHelper.createLauncher(configuration);
		boolean isModular = JavaRuntime.isModularJava(launcher);
		if (isModular) {
			VMHelper.addNewArgument(vmArguments, "--add-modules", "ALL-SYSTEM"); //$NON-NLS-1$//$NON-NLS-2$
		}
		//  if element is a test class annotated with @RunWith(JUnitPlatform.class, we add this in program arguments
		@SuppressWarnings("restriction")
		String attrRunWithJunitPlatformAnnotation = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.ATTR_RUN_WITH_JUNIT_PLATFORM_ANNOTATION;
		if (configuration.getAttribute(attrRunWithJunitPlatformAnnotation, false)) {
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

		// launch the UI test application
		if (application == null)
			application = IPDEConstants.UI_TEST_APPLICATION;
		return application;
	}

	private IPluginModelBase findRequiredPluginInTargetOrHost(String id) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model == null || !model.getBundleDescription().isResolved()) {
			// prefer bundle from host over unresolved bundle from target
			model = PDECore.getDefault().findPluginInHost(id);
		}
		if (model == null) {
			abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id), null, IStatus.OK);
		}
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
		vmArgs = concatArg(vmArgs, "-Declipse.pde.launch=true"); //$NON-NLS-1$
		// For p2 target, add "-Declipse.p2.data.area=@config.dir/p2" unless already specified by user
		if (fAllBundles.containsKey("org.eclipse.equinox.p2.core")) { //$NON-NLS-1$
			if (!vmArgs.contains("-Declipse.p2.data.area=")) { //$NON-NLS-1$
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
		fAllBundles = fModels.keySet().stream().collect(Collectors.groupingBy(m -> m.getPluginBase().getId(), LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));
		launchMode = launch.getLaunchMode();

		// implicitly add the plug-ins required for JUnit testing if necessary
		addRequiredJunitRuntimePlugins(configuration);

		String attribute = launch.getAttribute(PDE_JUNIT_SHOW_COMMAND);
		boolean isShowCommand = false;
		if (attribute != null) {
			isShowCommand = attribute.equals("true"); //$NON-NLS-1$
		}
		boolean autoValidate = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		SubMonitor subMonitor = SubMonitor.convert(monitor, autoValidate ? 3 : 4);
		if (isShowCommand == false) {
			if (autoValidate) {
				validatePluginDependencies(configuration, subMonitor.split(1));
			}
			validateProjectDependencies(configuration, subMonitor.split(1));
			clear(configuration, subMonitor.split(1));
		}
		launch.setAttribute(PDE_JUNIT_SHOW_COMMAND, "false"); //$NON-NLS-1$
		launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigurationDirectory(configuration).toString());
		synchronizeManifests(configuration, subMonitor.split(1));
	}

	private void addRequiredJunitRuntimePlugins(ILaunchConfiguration configuration) throws CoreException {
		Set<String> requiredPlugins = new LinkedHashSet<>(getRequiredJunitRuntimePlugins(configuration));

		if (fAllBundles.containsKey("junit-platform-runner") || fAllBundles.containsKey("org.junit.platform.runner")) { //$NON-NLS-1$ //$NON-NLS-2$
			// add launcher and jupiter.engine to support @RunWith(JUnitPlatform.class)
			requiredPlugins.add("junit-platform-launcher"); //$NON-NLS-1$
			requiredPlugins.add("junit-jupiter-engine"); //$NON-NLS-1$
		}

		Set<BundleDescription> addedRequirements = new HashSet<>();
		addAbsentRequirements(requiredPlugins, addedRequirements);

		Set<BundleDescription> requirementsOfRequirements = DependencyManager.findRequirementsClosure(addedRequirements);
		Set<String> rorIds = requirementsOfRequirements.stream().map(BundleDescription::getSymbolicName).collect(Collectors.toSet());
		addAbsentRequirements(rorIds, null);
	}

	private void addAbsentRequirements(Collection<String> requirements, Set<BundleDescription> addedRequirements) throws CoreException {
		for (String id : requirements) {
			List<IPluginModelBase> models = fAllBundles.computeIfAbsent(id, k -> new ArrayList<>());
			if (models.stream().noneMatch(m -> m.getBundleDescription().isResolved())) {
				IPluginModelBase model = findRequiredPluginInTargetOrHost(id);
				models.add(model);
				BundleLauncherHelper.addDefaultStartingBundle(fModels, model);
				if (addedRequirements != null) {
					addedRequirements.add(model.getBundleDescription());
				}
			}
		}
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @param configuration non null config
	 * @return required plugins
	 */
	@SuppressWarnings("restriction")
	public static Collection<String> getRequiredJunitRuntimePlugins(ILaunchConfiguration configuration) {
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			return Collections.emptyList();
		}
		List<String> plugins = new ArrayList<>();
		plugins.add("org.eclipse.pde.junit.runtime"); //$NON-NLS-1$

		if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add("org.eclipse.jdt.junit4.runtime"); //$NON-NLS-1$
		} else if (org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKind.getId())) {
			plugins.add("org.eclipse.jdt.junit5.runtime"); //$NON-NLS-1$
		}
		return plugins;
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
		LauncherUtils.clearWorkspace(configuration, fWorkspaceLocation, launchMode, subMon.split(25));

		subMon.setWorkRemaining(25);

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false)) {
			CoreUtility.deleteContent(getConfigurationDirectory(configuration), subMon.split(25));
		}

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
		EclipsePluginValidationOperation op = new EclipsePluginValidationOperation(configuration, fModels.keySet(), launchMode);
		LaunchPluginValidator.runValidationOperation(op, monitor);
	}
}
