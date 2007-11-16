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
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class PDEMigrationDelegate implements ILaunchConfigurationMigrationDelegate {
	
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		return !candidate.getAttribute(IPDEUIConstants.APPEND_ARGS_EXPLICITLY, false) ||
			candidate.hasAttribute(IPDELauncherConstants.VMINSTALL);
	}

	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		migrate(wc);
		wc.doSave();
	}

	public void migrate(ILaunchConfigurationWorkingCopy candidate) throws CoreException {
		if (!candidate.getAttribute(IPDEUIConstants.APPEND_ARGS_EXPLICITLY, false)) {
			candidate.setAttribute(IPDEUIConstants.APPEND_ARGS_EXPLICITLY, true);
			String args = candidate.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
					""); //$NON-NLS-1$
			StringBuffer buffer = new StringBuffer(LaunchArgumentsHelper.getInitialProgramArguments());
			if (args.length() > 0) {
				buffer.append(" "); //$NON-NLS-1$
				buffer.append(args);
			}
			candidate.setAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
					buffer.toString());
		}
		if (candidate.hasAttribute(IPDELauncherConstants.VMINSTALL)) {
			String name = candidate.getAttribute(IPDELauncherConstants.VMINSTALL, (String)null);
			if (name != null) {
				IVMInstall vm = VMHelper.getVMInstall(name);
				if (vm != null) {
					IPath path = JavaRuntime.newJREContainerPath(vm);
					candidate.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, path.toPortableString());
				}				
			}
			candidate.removeAttribute(IPDELauncherConstants.VMINSTALL);
		}
	}

}
