/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.ee;

import java.util.ArrayList;
import java.util.List;
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
			String[] compatible = new String[] { "10", "9", "1.8", "1.7", "1.6", "1.5", "1.4" };
			for (String element : compatible) {
				if (javaVersion.startsWith(element)) {
					result.add(new CompatibleEnvironment(env, false));
				}
			}
			if (javaVersion.startsWith("1.3")) {
				result.add(new CompatibleEnvironment(env, true));
			}

			// for > java 10
			if (result.isEmpty()) {
				List<String> allVersions = JavaCore.getAllVersions();
				for (int i = allVersions.size() - 1; i >= 0; i--) {
					String string = allVersions.get(i);
					int parseInt = -1;
					try {
						parseInt = Integer.parseInt(string);
					} catch (NumberFormatException e) {
					}
					if (parseInt > 10) {
						if (javaVersion.startsWith(string)) {
							result.add(new CompatibleEnvironment(env, false));
							break;
						}
					}
				}
			}

		}
		return result.toArray(new CompatibleEnvironment[result.size()]);

	}

}
