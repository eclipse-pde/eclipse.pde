/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

import org.eclipse.pde.launching.IPDELauncherConstants;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.launching.PDEMessages;

public class VMHelper {

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

	public static IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
		String jre = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
		IVMInstall vm = null;
		if (jre == null) {
			String name = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
			if (name == null) {
				name = getDefaultVMInstallName(configuration);
			}
			vm = getVMInstall(name);
			if (vm == null) {
				throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, name)));
			}
		} else {
			IPath jrePath = Path.fromPortableString(jre);
			vm = JavaRuntime.getVMInstall(jrePath);
			if (vm == null) {
				String id = JavaRuntime.getExecutionEnvironmentId(jrePath);
				if (id == null) {
					String name = JavaRuntime.getVMInstallName(jrePath);
					throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.WorkbenchLauncherConfigurationDelegate_noJRE, name)));
				}
				throw new CoreException(LauncherUtils.createErrorStatus(NLS.bind(PDEMessages.VMHelper_cannotFindExecEnv, id)));
			}
		}
		return vm;
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
