/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.OSGiLaunchConfigurationInitializer;

public class EquinoxInitializer extends OSGiLaunchConfigurationInitializer {
	
	private Map fStartLevels;

	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		super.initialize(configuration);
		initializeProgramArguments(configuration);
		initializeVMArguments(configuration);
		initializeTracing(configuration);
	}

	private void initializeProgramArguments(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-console"); //$NON-NLS-1$
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String progArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (progArgs.indexOf("-console") == -1) //$NON-NLS-1$
			progArgs = "-console " + progArgs; //$NON-NLS-1$
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, progArgs); //$NON-NLS-1$
	}
	
	private void initializeVMArguments(ILaunchConfigurationWorkingCopy configuration) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);	
	}
	
	private void initializeTracing(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, 
								   IPDELauncherConstants.TRACING_NONE);	
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
		Properties props = TargetPlatform.getConfigIniProperties();
		if (props != null) {
			String value = (String)props.get("osgi.bundles"); //$NON-NLS-1$
			if (value != null) {
				StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					String tokenValue = tokenizer.nextToken();
					int index = tokenValue.indexOf("@"); //$NON-NLS-1$
					if (index > 0) {
						String bundle = tokenValue.substring(0,index).trim();
						fStartLevels.put(bundle, getStartValue(tokenValue.substring(index)));
					}
				}
			}
		} 
	}
		
	private String getStartValue(String value) {
		StringBuffer buffer = new StringBuffer(value);		
		StringBuffer result = new StringBuffer(":");  //$NON-NLS-1$
		
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
