/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.exports;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

public class BuildUtilities {

	public static String getBootClasspath() {
		return getBootClasspath(JavaRuntime.getDefaultVMInstall());
	}

	public static String getBootClasspath(String environmentID) {
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment environment = manager.getEnvironment(environmentID);
		IVMInstall vm = null;
		if (environment != null) {
			vm = environment.getDefaultVM();
			if (vm == null) {
				IVMInstall[] installs = environment.getCompatibleVMs();
				// take the first strictly compatible vm if there is no default
				for (IVMInstall install : installs) {
					if (environment.isStrictlyCompatible(install)) {
						vm = install;
						break;
					}
				}
				// use the first vm failing that
				if (vm == null && installs.length > 0) {
					vm = installs[0];
				}
			}
		}
		if (vm == null) {
			vm = JavaRuntime.getDefaultVMInstall();
		}
		return getBootClasspath(vm);
	}

	public static String getBootClasspath(IVMInstall install) {
		StringBuilder buffer = new StringBuilder();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(install);
		for (int i = 0; i < locations.length; i++) {
			buffer.append(locations[i].getSystemLibraryPath().toOSString());
			if (i < locations.length - 1) {
				buffer.append(";"); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

}
