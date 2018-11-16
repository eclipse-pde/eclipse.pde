/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.launching;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.internal.launching.launcher.*;

/**
 * An abstract launch delegate for PDE-based launch configurations
 * <p>
 * Clients may subclass this class.
 * </p>
 * <p>
 * This class originally existed in 3.2 as
 * <code>org.eclipse.pde.ui.launcher.AbstractPDELaunchConfiguration</code>.
 * </p>
 * @since 3.6
 */
public abstract class AbstractPDELaunchConfiguration extends LaunchConfigurationDelegate {

	protected File fConfigDir = null;

	/**
	 * This field will control the addition of argument --add-modules=ALL-SYSTEM in the VM arguments
	 * during PDE launch. This VM argument is required from Java9 onwards for launching non-modular system
	 * @deprecated This field was wrongly added and is no longer used.
	 * @since 3.8
	 * @noreference This field is not intended to be referenced by clients.
	 */
	@Deprecated
	public static boolean shouldVMAddModuleSystem = false;

	private static final String PDE_LAUNCH_SHOW_COMMAND = "pde.launch.showcommandline"; //$NON-NLS-1$

	@Override
	protected boolean isLaunchProblem(IMarker problemMarker) throws CoreException {
		return super.isLaunchProblem(problemMarker) && (problemMarker.getType().equals(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER) || problemMarker.getType().equals(PDEMarkerFactory.MARKER_ID));
	}

	@Override
	public String showCommandLine(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String commandLine = ""; //$NON-NLS-1$
		launch.setAttribute(PDE_LAUNCH_SHOW_COMMAND, "true"); //$NON-NLS-1$
		try {
			fConfigDir = null;
			SubMonitor subMonitor = SubMonitor.convert(monitor, 4);
			try {
				preLaunchCheck(configuration, launch, subMonitor.split(2));
			} catch (CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					subMonitor.setCanceled(true);
					return commandLine;
				}
				throw e;
			}

			VMRunnerConfiguration runnerConfig = new VMRunnerConfiguration(getMainClass(), getClasspath(configuration));
			IVMInstall launcher = VMHelper.createLauncher(configuration);
			boolean isModular = JavaRuntime.isModularJava(launcher);
			runnerConfig.setVMArguments(updateVMArgumentWithAddModuleSystem(getVMArguments(configuration), isModular));
			runnerConfig.setProgramArguments(getProgramArguments(configuration));
			runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
			runnerConfig.setEnvironment(getEnvironment(configuration));
			runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

			subMonitor.worked(1);

			setDefaultSourceLocator(configuration);
			manageLaunch(launch);
			IVMRunner runner = getVMRunner(configuration, mode);
			if (runner != null)
				commandLine = runner.showCommandLine(runnerConfig, launch, subMonitor);
			else
				subMonitor.setCanceled(true);

		} catch (final CoreException e) {
			throw e;
		}
		return commandLine;
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			fConfigDir = null;
			SubMonitor subMonitor = SubMonitor.convert(monitor, 4);
			try {
				preLaunchCheck(configuration, launch, subMonitor.split(2));
			} catch (CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					subMonitor.setCanceled(true);
					return;
				}
				throw e;
			}

			VMRunnerConfiguration runnerConfig = new VMRunnerConfiguration(getMainClass(), getClasspath(configuration));
			IVMInstall launcher = VMHelper.createLauncher(configuration);
			boolean isModular = JavaRuntime.isModularJava(launcher);
			runnerConfig.setVMArguments(updateVMArgumentWithAddModuleSystem(getVMArguments(configuration), isModular));
			runnerConfig.setProgramArguments(getProgramArguments(configuration));
			runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
			runnerConfig.setEnvironment(getEnvironment(configuration));
			runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

			subMonitor.worked(1);

			setDefaultSourceLocator(configuration);
			manageLaunch(launch);
			IVMRunner runner = getVMRunner(configuration, mode);
			if (runner != null)
				runner.run(runnerConfig, launch, subMonitor.split(1));
			else
				subMonitor.setCanceled(true);

		} catch (final CoreException e) {
			throw e;
		}
	}

	private String[] updateVMArgumentWithAddModuleSystem(String[] args, boolean isModular) {
		String modAllSystem= "--add-modules=ALL-SYSTEM"; //$NON-NLS-1$
		if (isModular && !argumentContainsModuleSystem(args, modAllSystem)) {
			args = Arrays.copyOf(args, args.length + 1);
			args[args.length - 1] = modAllSystem;
		}
		if (!isModular) {
			ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(args));
			arrayList.remove(modAllSystem);
			arrayList.trimToSize();
			args = arrayList.toArray(new String[arrayList.size()]);
		}
		return args;
	}

	private boolean argumentContainsModuleSystem(String[] args, String modAllSystem) {
		for (String string : args) {
			if (string.equals(modAllSystem))
				return true;
		}
		return false;
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
		IVMInstall launcher = VMHelper.createLauncher(configuration);
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
			String message = PDEMessages.WorkbenchLauncherConfigurationDelegate_noStartup;
			throw new CoreException(LauncherUtils.createErrorStatus(message));
		}
		return classpath;
	}

	/**
	 * Returns an array of environment variables to be used when
	 * launching the given configuration or <code>null</code> if unspecified.
	 *
	 * @param configuration launch configuration
	 * @return the environment variables to be used when launching or <code>null</code>
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
	public Map<String, Object> getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
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
		String[] vmArgs = new ExecutionArguments(LaunchArgumentsHelper.getUserVMArguments(configuration), "").getVMArgumentsArray(); //$NON-NLS-1$
		return vmArgs;
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
	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		ArrayList<String> programArgs = new ArrayList<>();

		// add tracing, if turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false) && !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			programArgs.add(LaunchArgumentsHelper.getTracingFileArgument(configuration, getConfigDir(configuration).toString() + IPath.SEPARATOR + ICoreConstants.OPTIONS_FILENAME));
		}

		// add the program args specified by the user
		String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
		ArrayList<String> userDefined = new ArrayList<>();
		for (String userArg : userArgs) {
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (userArg.equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			userDefined.add(userArg);
		}

		if (!configuration.getAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, false)) {

			if (!userDefined.contains("-os")) { //$NON-NLS-1$
				programArgs.add("-os"); //$NON-NLS-1$
				programArgs.add(TargetPlatform.getOS());
			}
			if (!userDefined.contains("-ws")) { //$NON-NLS-1$
				programArgs.add("-ws"); //$NON-NLS-1$
				programArgs.add(TargetPlatform.getWS());
			}
			if (!userDefined.contains("-arch")) { //$NON-NLS-1$
				programArgs.add("-arch"); //$NON-NLS-1$
				programArgs.add(TargetPlatform.getOSArch());
			}
		}

		if (!userDefined.isEmpty()) {
			programArgs.addAll(userDefined);
		}

		return programArgs.toArray(new String[programArgs.size()]);
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
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String attribute = launch.getAttribute(PDE_LAUNCH_SHOW_COMMAND);
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
			LauncherUtils.setLastLaunchMode(launch.getLaunchMode());
			clear(configuration, subMonitor.split(1));
		}
		launch.setAttribute(PDE_LAUNCH_SHOW_COMMAND, "false"); //$NON-NLS-1$
		launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigDir(configuration).toString());
		synchronizeManifests(configuration, subMonitor.split(1));
	}

	/**
	 * Returns the configuration area specified by the given launch
	 * configuration.
	 *
	 * @param configuration
	 *            launch configuration
	 * @return the directory path specified by the given launch
	 *         configuration
	 */
	protected File getConfigDir(ILaunchConfiguration configuration) {
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
	 * Returns the fully-qualified name of the class to launch.
	 *
	 * @return the fully-qualified name of the class to launch.  Must not return <code>null</code>.
	 * @since 3.3
	 */
	public String getMainClass() {
		if (TargetPlatformHelper.getTargetVersion() >= 3.3)
			return "org.eclipse.equinox.launcher.Main"; //$NON-NLS-1$
		return "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
	}

	/**
	 * Adds a listener to the launch to be notified at interesting launch lifecycle
	 * events such as when the launch terminates.
	 *
	 * @param launch
	 * 			the launch
	 *
	 * @since 3.3
	 */
	protected void manageLaunch(ILaunch launch) {
		PDELaunchingPlugin.getDefault().getLaunchListener().manage(launch);
	}

	/**
	 * Checks for old-style plugin.xml files that have become stale since the last launch.
	 * For any stale plugin.xml files found, the corresponding MANIFEST.MF is deleted
	 * from the runtime configuration area so that it gets regenerated upon startup.
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			a progress monitor
	 *
	 * @since 3.3
	 */
	protected void synchronizeManifests(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigDir(configuration));
		monitor.done();
	}

	/**
	 * Checks if the Automated Management of Dependencies option is turned on.
	 * If so, it makes aure all manifests are updated with the correct dependencies.
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			a progress monitor
	 *
	 * @since 3.3
	 */
	protected void validateProjectDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		LauncherUtils.validateProjectDependencies(configuration, monitor);
	}

	/**
	 * By default, this method does nothing.  Clients should override, if appropriate.
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			the progress monitor
	 * @throws CoreException
	 * 			if unable to retrieve launch attribute values or the clear operation was cancelled
	 * @since 3.3
	 */
	protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
	}

	/**
	 * Validates inter-bundle dependencies automatically prior to launching
	 * if that option is turned on.
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @param monitor
	 * 			a progress monitor
	 * @since 3.3
	 */
	protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		EclipsePluginValidationOperation op = new EclipsePluginValidationOperation(configuration);
		LaunchPluginValidator.runValidationOperation(op, monitor);
	}

	/**
	 * Updates the field shouldVMAddModuleSystem.
	 * 
	 * @since 3.8
	 * @deprecated This method was wrongly added and is no longer used. It is a no-op now.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Deprecated
	public static void updatePDELaunchConfigModuleSystem(boolean java9) {
	}

}
