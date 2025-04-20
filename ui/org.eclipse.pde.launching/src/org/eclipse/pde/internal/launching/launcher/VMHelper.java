/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     Les Jones <lesojones@gmail.com> - Bug 195433
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.launching.PDEMessages;

public class VMHelper {

	/**
	 * Returns the id of an execution environment that is both bound to a JRE and valid for the minimum
	 * BREE supplied by every bundle that will be launched in the given launch configuration or
	 * <code>null</code> if no valid/bound execution environment could be found.
	 *
	 * @param plugins the set of plug-ins to test for their BREEs
	 * @return string id of a valid execution environment with a bound JRE or <code>null</code>
	 * @throws CoreException if there is a problem reading the bundles from the launch configuration
	 */
	public static String getDefaultEEName(Set<IPluginModelBase> plugins) throws CoreException {
		// List of all valid EEs, removed if they don't match
		List<IExecutionEnvironment> validEEs = new LinkedList<>(); // Use a list to keep order
		validEEs.addAll(Arrays.asList(JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments()));

		// Find EEs that do not have a compatible JRE (are unbound)
		Set<String> unboundEEs = new HashSet<>();
		for (Iterator<IExecutionEnvironment> iterator = validEEs.iterator(); iterator.hasNext();) {
			IExecutionEnvironment current = iterator.next();
			if (current.getCompatibleVMs().length == 0) {
				iterator.remove();
				unboundEEs.add(current.getId());
			}
		}

		// Iterate through all launch models
		for (IPluginModelBase plugin : plugins) {
			if (validEEs.isEmpty()) {
				break; // No valid EEs left, short circuit
			}
			if (plugin.isFragmentModel()) {
				continue; // The default EE shouldn't depend on fragments
			}
			BundleDescription desc = plugin.getBundleDescription();
			if (desc != null) {

				// List of all the BREEs that a valid environment must match
				String[] bundleEnvs = desc.getExecutionEnvironments();
				if (bundleEnvs.length > 0) {

					// See if the BREE matches an unbound EE, if so skip this plug-in as we cannot launch it
					boolean isUnbound = false;
					for (String bundleEnv : bundleEnvs) {
						if (unboundEEs.contains(bundleEnv)) {
							isUnbound = true;
							break;
						}
					}
					if (isUnbound) {
						continue; // Use another bundle to determine best EE
					}

					// Iterate through all remaining valid EEs, removing any that don't match
					for (Iterator<IExecutionEnvironment> iterator = validEEs.iterator(); iterator.hasNext();) {
						IExecutionEnvironment currentEE = iterator.next();
						boolean isValid = false;
						// To be valid, an EE must match at least one BREE
						for (String bundleEnv : bundleEnvs) {
							if (isValid) {
								break; // sub environment was valid
							}
							if (bundleEnv.equals(currentEE.getId())) {
								isValid = true;
								break; // No need to check subEnvironments at all
							}
							IExecutionEnvironment[] currentSubEE = currentEE.getSubEnvironments();
							for (IExecutionEnvironment element : currentSubEE) {
								if (bundleEnv.equals(element.getId())) {
									isValid = true;
									break; // No need to check other subEnvironments
								}
							}
						}
						// The current EE doesn't support this bundle, remove it
						if (!isValid) {
							iterator.remove();
						}
					}
				}
			}
		}

		// JavaRuntime appears to return the EEs from smallest to largest, so taking the first valid EE is a good selection
		// To improve this we could check if any valid EE has another valid EE as a subEnvironment
		if (!validEEs.isEmpty()) {
			return validEEs.iterator().next().getId();
		}
		return null;
	}

	/**
	 * Get the default VMInstall name using the available info in the config,
	 * using the JavaProject if available.
	 *
	 * @param configuration
	 *            Launch configuration to check
	 * @return name of the VMInstall
	 * @throws CoreException
	 *             thrown if there's a problem getting the VM name
	 */
	public static String getDefaultVMInstallName(ILaunchConfiguration configuration) throws CoreException {
		IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
		IVMInstall vmInstall = null;
		if (javaProject != null) {
			vmInstall = JavaRuntime.getVMInstall(javaProject);
		}

		if (vmInstall != null) {
			return vmInstall.getName();
		}

		return VMUtil.getDefaultVMInstallName();
	}

	/**
	 * Returns the vm install to launch this configuration with based on launch configuration settings or throws
	 * a CoreException if no valid vm install is found.  If the launch configuration has no JRE attributes set,
	 * a default setting will be used (but not saved in the launch configuration).
	 *
	 * @param configuration the configuration to get a vm install for
	 * @param plugins all plugins included in the launch
	 * @return a vm install from {@link JavaRuntime}
	 * @throws CoreException if a vm install could not be found for the settings in the configuration
	 */
	public static IVMInstall getVMInstall(ILaunchConfiguration configuration, Set<IPluginModelBase> plugins) throws CoreException {
		String jre = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);

		// Launch configuration has a JRE or EE set, throw exception if associated vm not found
		if (jre != null) {
			IPath jrePath = IPath.fromPortableString(jre);
			IVMInstall vm = JavaRuntime.getVMInstall(jrePath);
			if (vm == null) {
				String id = JavaRuntime.getExecutionEnvironmentId(jrePath);
				if (id == null) {
					String name = JavaRuntime.getVMInstallName(jrePath);
					throw new CoreException(Status.error(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, name)));
				}
				throw new CoreException(Status.error(NLS.bind(PDEMessages.VMHelper_cannotFindExecEnv, id)));
			}
			return vm;
		}

		// Find a default EE
		String eeId = VMHelper.getDefaultEEName(plugins);
		if (eeId != null) {
			IExecutionEnvironment ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(eeId);
			String vmName = VMUtil.getVMInstallName(ee);
			if (ee != null) {
				IVMInstall vm = VMUtil.getVMInstall(vmName);
				if (vm != null) {
					return vm;
				}
			}
		}

		// Find a default JRE
		String defaultVMName = VMHelper.getDefaultVMInstallName(configuration);
		IVMInstall vm = VMUtil.getVMInstall(defaultVMName);
		if (vm != null) {
			return vm;
		}

		// No valid vm available, throw exception
		throw new CoreException(Status.error(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, defaultVMName)));

	}

	public static IVMInstall createLauncher(ILaunchConfiguration configuration, Set<IPluginModelBase> plugins) throws CoreException {
		IVMInstall launcher = getVMInstall(configuration, plugins);
		if (!launcher.getInstallLocation().exists()) {
			throw new CoreException(Status.error(PDEMessages.WorkbenchLauncherConfigurationDelegate_jrePathNotFound));
		}
		return launcher;
	}

	/**
	 * Returns a JRE runtime classpath entry
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @return a JRE runtime classpath entry
	 * @throws CoreException
	 * 			if the JRE associated with the launch configuration cannot be found
	 * 			or if unable to retrieve the launch configuration attributes
	 */
	public static IRuntimeClasspathEntry getJREEntry(ILaunchConfiguration configuration) throws CoreException {
		Set<IPluginModelBase> plugins = BundleLauncherHelper.getMergedBundleMap(configuration, false).keySet();
		IVMInstall jre = createLauncher(configuration, plugins);
		IPath containerPath = IPath.fromOSString(JavaRuntime.JRE_CONTAINER);
		containerPath = containerPath.append(jre.getVMInstallType().getId());
		containerPath = containerPath.append(jre.getName());
		return JavaRuntime.newRuntimeContainerClasspathEntry(containerPath, IRuntimeClasspathEntry.BOOTSTRAP_CLASSES);
	}

	public static void addNewArgument(List<String> arguments, String key, String value) {
		if (arguments.stream().noneMatch(a -> a.startsWith(key + "="))) { //$NON-NLS-1$
			arguments.add(key + "=" + value); //$NON-NLS-1$
		}
	}

}
