/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 195433
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.*;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.launching.EquinoxLaunchConfiguration;
import org.eclipse.pde.launching.IPDELauncherConstants;

public class VMHelper {

	/**
	 * Returns the id of an execution environment that is valid for the minimum BREE supplied by every bundle
	 * that will be launched in the given launch configuration or <code>null</code> if no valid execution
	 * environment could be found.
	 * 
	 * @param configuration the launch configuration to test the bundle's BREEs of
	 * @return string id of a valid execution environment or <code>null</code>
	 * @throws CoreException if there is a problem reading the bundles from the launch configuration
	 */
	public static String getDefaultEEName(ILaunchConfiguration configuration) throws CoreException {
		// List of all valid EEs, removed if they don't match
		List validEEs = new LinkedList(); // Use a list to keep order
		validEEs.addAll(Arrays.asList(JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments()));

		// Iterate through all launch models 
		boolean isOSGiLaunch = configuration instanceof EquinoxLaunchConfiguration; // TODO Test this
		IPluginModelBase[] plugins = BundleLauncherHelper.getMergedBundles(configuration, isOSGiLaunch);
		for (int i = 0; i < plugins.length; i++) {
			if (validEEs.isEmpty()) {
				break; // No valid EEs left, short circuit
			}
			if (plugins[i].isFragmentModel()) {
				continue; // The default EE shouldn't depend on fragments
			}
			BundleDescription desc = plugins[i].getBundleDescription();
			if (desc != null) {

				// List of all the BREEs that a valid environment must match
				String[] bundleEnvs = desc.getExecutionEnvironments();
				if (bundleEnvs.length > 0) {

					// Iterate through all remaining valid EEs, removing any that don't match
					for (Iterator iterator = validEEs.iterator(); iterator.hasNext();) {
						IExecutionEnvironment currentEE = (IExecutionEnvironment) iterator.next();
						boolean isValid = false;
						// To be valid, an EE must match at least one BREE
						for (int j = 0; j < bundleEnvs.length; j++) {
							if (isValid) {
								break; // sub environment was valid
							}
							if (bundleEnvs[j].equals(currentEE.getId())) {
								isValid = true;
								break; // No need to check subEnvironments at all
							}
							IExecutionEnvironment[] currentSubEE = currentEE.getSubEnvironments();
							for (int k = 0; k < currentSubEE.length; k++) {
								if (bundleEnvs[j].equals(currentSubEE[k].getId())) {
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
			return ((IExecutionEnvironment) validEEs.iterator().next()).getId();
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
	 * @return a vm install from {@link JavaRuntime}
	 * @throws CoreException if a vm install could not be found for the settings in the configuration
	 */
	public static IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
		String jre = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);

		// Launch configuration has a JRE or EE set, throw exception if associated vm not found
		if (jre != null) {
			IPath jrePath = Path.fromPortableString(jre);
			IVMInstall vm = JavaRuntime.getVMInstall(jrePath);
			if (vm == null) {
				String id = JavaRuntime.getExecutionEnvironmentId(jrePath);
				if (id == null) {
					String name = JavaRuntime.getVMInstallName(jrePath);
					throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, name)));
				}
				throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.VMHelper_cannotFindExecEnv, id)));
			}
			return vm;
		}

		// Check if legacy attribute is set, throw exception if associated vm not found
		String vmInstallAttribute = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
		if (vmInstallAttribute != null) {
			IVMInstall vm = getVMInstall(vmInstallAttribute);
			if (vm == null) {
				throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, vmInstallAttribute)));
			}
			return vm;
		}

		// Find a default EE
		String eeId = VMHelper.getDefaultEEName(configuration);
		if (eeId != null) {
			IExecutionEnvironment ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(eeId);
			String vmName = VMUtil.getVMInstallName(ee);
			if (ee != null) {
				IVMInstall vm = getVMInstall(vmName);
				if (vm != null) {
					return vm;
				}
			}
		}

		// Find a default JRE
		String defaultVMName = VMHelper.getDefaultVMInstallName(configuration);
		IVMInstall vm = getVMInstall(defaultVMName);
		if (vm != null) {
			return vm;
		}

		// No valid vm available, throw exception
		throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, defaultVMName)));

	}

	public static IVMInstall getVMInstall(String name) {
		if (name != null) {
			IVMInstall[] installs = VMUtil.getAllVMInstances();
			for (int i = 0; i < installs.length; i++) {
				if (installs[i].getName().equals(name))
					return installs[i];
			}
		}
		return JavaRuntime.getDefaultVMInstall();
	}

	public static IVMInstall createLauncher(ILaunchConfiguration configuration) throws CoreException {
		IVMInstall launcher = getVMInstall(configuration);
		if (!launcher.getInstallLocation().exists())
			throw new CoreException(LauncherUtils.createErrorStatus(PDEMessages.WorkbenchLauncherConfigurationDelegate_jrePathNotFound));
		return launcher;
	}

}
