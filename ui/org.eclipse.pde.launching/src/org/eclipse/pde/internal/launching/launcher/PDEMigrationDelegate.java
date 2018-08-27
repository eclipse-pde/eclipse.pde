/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.launching.IPDEConstants;

public class PDEMigrationDelegate implements ILaunchConfigurationMigrationDelegate {

	@Override
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		return !candidate.getAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, false) || candidate.hasAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH);
	}

	@Override
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		migrate(wc);
		wc.doSave();
	}

	public void migrate(ILaunchConfigurationWorkingCopy candidate) throws CoreException {
		if (!candidate.getAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, false)) {
			candidate.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
			String args = candidate.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""); //$NON-NLS-1$
			StringBuilder buffer = new StringBuilder(LaunchArgumentsHelper.getInitialProgramArguments());
			if (args.length() > 0) {
				buffer.append(" "); //$NON-NLS-1$
				buffer.append(args);
			}
			candidate.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, buffer.toString());
		}
		if (candidate.hasAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH)) {
			String name = candidate.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
			if (name != null) {
				IVMInstall vm = VMHelper.getVMInstall(name);
				if (vm != null) {
					IPath path = JavaRuntime.newJREContainerPath(vm);
					candidate.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, path.toPortableString());
				}
			}
			candidate.removeAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH);
		}
	}

}
