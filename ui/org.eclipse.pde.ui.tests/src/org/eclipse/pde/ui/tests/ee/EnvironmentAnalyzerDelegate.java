/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.environments.IExecutionEnvironmentAnalyzerDelegate#analyze(org.eclipse.jdt.launching.IVMInstall, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public CompatibleEnvironment[] analyze(IVMInstall vm, IProgressMonitor monitor) throws CoreException {
		if (!(vm instanceof IVMInstall2))
			return new CompatibleEnvironment[0];
		ArrayList result = new ArrayList();
		IVMInstall2 vm2 = (IVMInstall2) vm;
		String javaVersion = vm2.getJavaVersion();
		if (javaVersion != null) {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(EE_NO_SOUND);
			String[] compatible = new String[]{"1.7", "1.6", "1.5", "1.4"};
			for (int i = 0; i < compatible.length; i++) {
				if (javaVersion.startsWith(compatible[i])) {
					result.add(new CompatibleEnvironment(env, false));
				}
			}
			if (javaVersion.startsWith("1.3")) {
				result.add(new CompatibleEnvironment(env, true));
			}
		}
		return (CompatibleEnvironment[])result.toArray(new CompatibleEnvironment[result.size()]);

	}

}
