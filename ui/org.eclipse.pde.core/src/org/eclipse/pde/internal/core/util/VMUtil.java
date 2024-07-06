/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 *     Chris Aniszczyk <zx@code9.com> - Bug 195433
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.util.NLS;

public class VMUtil {

	public static IVMInstall[] getAllVMInstances() {
		ArrayList<IVMInstall> res = new ArrayList<>();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			IVMInstall[] installs = type.getVMInstalls();
			Collections.addAll(res, installs);
		}
		return res.toArray(new IVMInstall[res.size()]);
	}

	public static String[] getVMInstallNames() {
		IVMInstall[] installs = getAllVMInstances();
		String[] names = new String[installs.length];
		for (int i = 0; i < installs.length; i++) {
			names[i] = installs[i].getName();
		}
		return names;
	}

	/**
	 * Returns the name of the default VM Install from Java Runtime.
	 * Will return an empty string if no default VM has been set.
	 * @return name of the default vm install, possibly an empty string
	 */
	public static String getDefaultVMInstallName() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			return install.getName();
		}
		return ""; //$NON-NLS-1$
	}

	public static String getDefaultVMInstallLocation() {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			return install.getInstallLocation().getAbsolutePath();
		}
		return null;
	}

	public static IVMInstall getVMInstall(String name) {
		if (name != null) {
			IVMInstall[] installs = getAllVMInstances();
			for (IVMInstall install : installs) {
				if (install.getName().equals(name)) {
					return install;
				}
			}
		}
		return JavaRuntime.getDefaultVMInstall();
	}

	public static IExecutionEnvironment[] getExecutionEnvironments() {
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		return manager.getExecutionEnvironments();
	}

	public static IExecutionEnvironment getExecutionEnvironment(String id) {
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		return manager.getEnvironment(id);
	}

	public static String getVMInstallName(IExecutionEnvironment ee) throws CoreException {
		IPath containerPath = JavaRuntime.newJREContainerPath(ee);
		IVMInstall vmi = JavaRuntime.getVMInstall(containerPath);
		if (vmi == null) {
			throw new CoreException(Status.error(NLS.bind(UtilMessages.VMHelper_noJreForExecEnv, ee.getId())));
		}
		return vmi.getName();
	}

	private static final Map<String, Double> JAVA_VERSION_OF_EE = Arrays.stream(getExecutionEnvironments())
			.collect(Collectors.toMap(IExecutionEnvironment::getId, VMUtil::getJavaTargetVersion));

	public static final Comparator<String> ASCENDING_EE_JAVA_VERSION = Comparator
			.comparingDouble((String ee) -> JAVA_VERSION_OF_EE.getOrDefault(ee, 0.0))
			.thenComparing(Comparator.naturalOrder());

	public static double getJavaTargetVersion(IExecutionEnvironment ee) {
		return switch (ee.getId()) {
		// The EE's handled explicitly have unexpected java targets.
		// See https://github.com/eclipse-equinox/equinox/pull/655
		case "J2SE-1.2" -> 1.2; //$NON-NLS-1$
		case "J2SE-1.3" -> 1.3; //$NON-NLS-1$
		case "J2SE-1.4" -> 1.4; //$NON-NLS-1$
		default -> {
			Properties properties = ee.getProfileProperties();
			Object target = properties != null //
					? properties.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)
					: null;
			yield target instanceof String version ? Double.parseDouble(version) : 0.0;
		}
		};
	}

}
