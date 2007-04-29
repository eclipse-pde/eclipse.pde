/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.ui.IPDEUIConstants;

public class OSGiMigrationDelegate implements ILaunchConfigurationMigrationDelegate {

	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		return !candidate.getAttribute(IPDEUIConstants.LAUNCHER_PDE_VERSION, "").equals("3.3"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		wc.setAttribute(IPDEUIConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
		adjustVMArguments(wc);
		wc.doSave();
	}
	
	private void adjustVMArguments(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		StringBuffer vmArgs = new StringBuffer(wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "")); //$NON-NLS-1$
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

}
