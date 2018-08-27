/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.launching.launcher;

import org.eclipse.pde.internal.launching.IPDEConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class OSGiMigrationDelegate extends PDEMigrationDelegate {

	@Override
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		return super.isCandidate(candidate) || !candidate.getAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "").equals("3.3"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void migrate(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		if (!wc.getAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "").equals("3.3")) { //$NON-NLS-1$ //$NON-NLS-2$
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
			StringBuilder vmArgs = new StringBuilder(wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "")); //$NON-NLS-1$
			if (vmArgs.indexOf("-Declipse.ignoreApp") == -1) { //$NON-NLS-1$
				if (vmArgs.length() > 0)
					vmArgs.append(" "); //$NON-NLS-1$
				vmArgs.append("-Declipse.ignoreApp=true"); //$NON-NLS-1$
			}
			if (vmArgs.indexOf("-Dosgi.noShutdown") == -1) { //$NON-NLS-1$
				vmArgs.append(" -Dosgi.noShutdown=true"); //$NON-NLS-1$
			}
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs.toString());
		}
		super.migrate(wc);
	}

}
