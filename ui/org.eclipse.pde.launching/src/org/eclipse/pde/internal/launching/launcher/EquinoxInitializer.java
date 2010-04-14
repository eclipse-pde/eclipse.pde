/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.*;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.OSGiLaunchConfigurationInitializer;

public class EquinoxInitializer extends OSGiLaunchConfigurationInitializer {

	private Map fStartLevels;

	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		super.initialize(configuration);
		initializeProgramArguments(configuration);
		initializeVMArguments(configuration);
		initializeTracing(configuration);
	}

	private void initializeProgramArguments(ILaunchConfigurationWorkingCopy configuration) {
		StringBuffer buffer = new StringBuffer(LaunchArgumentsHelper.getInitialProgramArguments());
		if (buffer.length() > 0) {
			// Note that -console applies to the same indexof as -consoleLog
			if (buffer.indexOf("-console ") == -1 && !buffer.toString().endsWith("-console")) { //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(" -console"); //$NON-NLS-1$
			}
		} else {
			buffer.append("-console"); //$NON-NLS-1$
		}
		configuration.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, buffer.toString());
	}

	private void initializeVMArguments(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		StringBuffer vmArgs = new StringBuffer(preferences.getString(ICoreConstants.VM_ARGS));
		if (vmArgs.indexOf("-Declipse.ignoreApp") == -1) { //$NON-NLS-1$
			if (vmArgs.length() > 0)
				vmArgs.append(" "); //$NON-NLS-1$
			vmArgs.append("-Declipse.ignoreApp=true"); //$NON-NLS-1$
		}
		if (vmArgs.indexOf("-Dosgi.noShutdown") == -1) { //$NON-NLS-1$
			vmArgs.append(" -Dosgi.noShutdown=true"); //$NON-NLS-1$
		}
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs.toString());
	}

	private void initializeTracing(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
	}

	protected void initializeBundleState(ILaunchConfigurationWorkingCopy configuration) {
		initializeBundleState();
		super.initializeBundleState(configuration);
	}

	protected String getAutoStart(String bundleID) {
		if (fStartLevels.containsKey(bundleID)) {
			String value = fStartLevels.get(bundleID).toString();
			return value.substring(value.indexOf(":") + 1); //$NON-NLS-1$
		}
		return super.getAutoStart(bundleID);
	}

	protected String getStartLevel(String bundleID) {
		if (fStartLevels.containsKey(bundleID)) {
			String value = fStartLevels.get(bundleID).toString();
			return value.substring(0, value.indexOf(":")); //$NON-NLS-1$
		}
		return super.getStartLevel(bundleID);
	}

	private void initializeBundleState() {
		if (fStartLevels == null)
			fStartLevels = new HashMap();
		Properties props = TargetPlatformHelper.getConfigIniProperties();
		if (props != null) {
			String value = (String) props.get("osgi.bundles"); //$NON-NLS-1$
			if (value != null) {
				StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					String tokenValue = tokenizer.nextToken();
					int index = tokenValue.indexOf("@"); //$NON-NLS-1$
					if (index > 0) {
						String bundle = tokenValue.substring(0, index).trim();
						fStartLevels.put(bundle, getStartValue(tokenValue.substring(index)));
					}
				}
			}
		}
	}

	private String getStartValue(String value) {
		StringBuffer buffer = new StringBuffer(value);
		StringBuffer result = new StringBuffer(":"); //$NON-NLS-1$

		int index = value.indexOf("start"); //$NON-NLS-1$
		result.append(Boolean.toString(index != -1));

		if (index != -1)
			buffer.delete(index, index + 5);

		int colon = value.indexOf(':');
		if (colon != -1)
			buffer.deleteCharAt(colon);

		// delete the first char '@'
		buffer.deleteCharAt(0);

		try {
			result.insert(0, Integer.parseInt(buffer.toString().trim()));
		} catch (NumberFormatException e) {
			result.insert(0, DEFAULT);
		}
		return result.toString();
	}

}
