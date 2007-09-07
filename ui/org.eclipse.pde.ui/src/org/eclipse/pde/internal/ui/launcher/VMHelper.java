/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class VMHelper {

	public static IVMInstall[] getAllVMInstances() {
		ArrayList res = new ArrayList();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (int i = 0; i < types.length; i++) {
			IVMInstall[] installs = types[i].getVMInstalls();
			for (int k = 0; k < installs.length; k++) {
				res.add(installs[k]);
			}
		}
		return (IVMInstall[]) res.toArray(new IVMInstall[res.size()]);
	}

	public static String[] getVMInstallNames() {
		IVMInstall[] installs = getAllVMInstances();
		String[] names = new String[installs.length];
		for (int i = 0; i < installs.length; i++) {
			names[i] = installs[i].getName();
		}
		return names;
	}

	public static String getDefaultVMInstallName() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null)
			return install.getName();
		return null;
	}

	public static String getDefaultVMInstallLocation() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null)
			return install.getInstallLocation().getAbsolutePath();
		return null;
	}
	
	public static IVMInstall getVMInstall(ILaunchConfiguration configuration) throws CoreException {
		String vm = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
		return getVMInstall(vm);
	}


	public static IVMInstall getVMInstall(String name) {
		if (name != null) {
			IVMInstall[] installs = getAllVMInstances();
			for (int i = 0; i < installs.length; i++) {
				if (installs[i].getName().equals(name))
					return installs[i];
			}
		}
		return JavaRuntime.getDefaultVMInstall();
	}

	public static IVMInstall createLauncher(
			ILaunchConfiguration configuration)
	throws CoreException {
		boolean vmSelected = configuration.getAttribute(IPDELauncherConstants.USE_VMINSTALL, true); //$NON-NLS-1$ //$NON-NLS-2$
		String vm;
		if (vmSelected)
			vm = configuration.getAttribute(IPDELauncherConstants.VMINSTALL, (String) null);
		else {
			String id = configuration.getAttribute(IPDELauncherConstants.EXECUTION_ENVIRONMENT, (String)null);
			if (id != null) {
				IExecutionEnvironment ee = getExecutionEnvironment(id);
				if (ee == null)
					throw new CoreException(
						LauncherUtils.createErrorStatus(NLS.bind(PDEUIMessages.VMHelper_cannotFindExecEnv, id)));
				vm = getVMInstallName(ee);
			}
			else
				vm = getDefaultVMInstallName();
		}
		IVMInstall launcher = getVMInstall(vm);
		if (launcher == null) 
			throw new CoreException(
					LauncherUtils.createErrorStatus(NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noJRE, vm)));

		if (!launcher.getInstallLocation().exists()) 
			throw new CoreException(
					LauncherUtils.createErrorStatus(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_jrePathNotFound));

		return launcher;
	}

	public static IExecutionEnvironment[] getExecutionEnvironments() {
		IExecutionEnvironmentsManager manager = 
			JavaRuntime.getExecutionEnvironmentsManager();
		return manager.getExecutionEnvironments();
	}

	public static IExecutionEnvironment getExecutionEnvironment(String id) {
		IExecutionEnvironmentsManager manager = 
			JavaRuntime.getExecutionEnvironmentsManager();
		return manager.getEnvironment(id);
	}

	public static String getVMInstallName(IExecutionEnvironment ee) throws CoreException {
		IVMInstall vmi = ee.getDefaultVM();
		if (vmi == null) {
			IVMInstall[] vmis = ee.getCompatibleVMs();
			for (int i = 0; i < vmis.length; i++) {
				if (ee.isStrictlyCompatible(vmis[i])) {
					vmi = vmis[i];
					break;
				}
				if (vmi == null)
					vmi = vmis[i];
			}
			if (vmi == null)
				throw new CoreException(
					LauncherUtils.createErrorStatus(NLS.bind(PDEUIMessages.VMHelper_noJreForExecEnv, ee.getId())));
		}
		return vmi.getName();
	}
}
