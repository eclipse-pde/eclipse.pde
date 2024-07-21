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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.util.NLS;

public class VMUtil {

	public static Stream<IVMInstall> getAllVMInstances() {
		return Arrays.stream(JavaRuntime.getVMInstallTypes()).flatMap(type -> Arrays.stream(type.getVMInstalls()));
	}

	public static String[] getVMInstallNames() {
		return getAllVMInstances().map(IVMInstall::getName).toArray(String[]::new);
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
			Optional<IVMInstall> install = getAllVMInstances().filter(i -> i.getName().equals(name)).findFirst();
			if (install.isPresent()) {
				return install.get();
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
		Properties properties = ee.getProfileProperties();
		Object target = properties != null //
				? properties.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)
				: null;
		return target instanceof String version ? Double.parseDouble(version) : 0.0;
	}

}
