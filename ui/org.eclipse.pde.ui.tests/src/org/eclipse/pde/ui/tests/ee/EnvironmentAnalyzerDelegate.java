/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.ee;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.*;
import org.eclipse.jdt.launching.environments.*;

/**
 * Test analyzer recognizes 1.3 JREs and higher as compatible
 */
public class EnvironmentAnalyzerDelegate implements IExecutionEnvironmentAnalyzerDelegate {

	/**
	 * Environment identifier
	 */
	public static final String EE_NO_SOUND = "J2SE-1.3-NO-SOUND";

	@Override
	public CompatibleEnvironment[] analyze(IVMInstall vm, IProgressMonitor monitor) throws CoreException {
		if (!(vm instanceof IVMInstall2))
			return new CompatibleEnvironment[0];
		ArrayList<CompatibleEnvironment> result = new ArrayList<>();
		IVMInstall2 vm2 = (IVMInstall2) vm;
		String javaVersion = vm2.getJavaVersion();
		if (javaVersion != null) {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(EE_NO_SOUND);
			List<String> allVersions = JavaCore.getAllVersions();
			int length = allVersions.size();
			for (int i = length - 1; i >= 3; i--) {
				String element = allVersions.get(i);
				if (javaVersion.startsWith(element)) {
					result.add(new CompatibleEnvironment(env, false));
				}
			}
		}
		return result.toArray(new CompatibleEnvironment[result.size()]);

	}

}
