/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.TracingOptionsManager;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class LaunchArgumentsHelper {

	public static String getWorkspaceLocation(ILaunchConfiguration configuration) 
				throws CoreException {
		String location = configuration.getAttribute(IPDELauncherConstants.LOCATION, (String)null);
		if (location == null) {
			// backward compatibility
			location = configuration.getAttribute(IPDELauncherConstants.LOCATION + "0", (String)null);  //$NON-NLS-1$
			if (location != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute(IPDELauncherConstants.LOCATION + "0", (String)null); //$NON-NLS-1$
				wc.setAttribute(IPDELauncherConstants.LOCATION, location);
				wc.doSave();
			}
		}
		return getSubstitutedString(location);
	}

	public static String[] getUserProgramArgumentArray(ILaunchConfiguration configuration) throws CoreException {
		String args = getUserProgramArguments(configuration);
		return new ExecutionArguments("", getSubstitutedString(args)).getProgramArgumentsArray(); //$NON-NLS-1$
	}
	
	public static String getUserProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
				(String)null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("progargs", (String)null); //$NON-NLS-1$
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("progargs", (String)null); //$NON-NLS-1$
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
				wc.doSave();			
			}
		}
		return args == null ? "" : args; //$NON-NLS-1$
	}
	
	public static String[] getUserVMArgumentArray(ILaunchConfiguration configuration) throws CoreException {
		String args = getUserVMArguments(configuration);
		return new ExecutionArguments(getSubstitutedString(args), "").getVMArgumentsArray(); //$NON-NLS-1$
	}
	
	public static String getUserVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, 
				(String)null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("vmargs", (String)null); //$NON-NLS-1$
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("vmargs", (String)null); //$NON-NLS-1$
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, args);
				wc.doSave();			
			}
		}
		return args == null ? "" : args; //$NON-NLS-1$
	}
	
	public static File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		String working;
		try {
			working = configuration.getAttribute(
						IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, 
						new File(".").getCanonicalPath()); //$NON-NLS-1$
		} catch (IOException e) {
			working = "${workspace_loc}/../"; //$NON-NLS-1$
		}
		File dir = new File(getSubstitutedString(working));
		if (!dir.exists())
			dir.mkdirs();
		return dir;			
	}
	
	public static Map getVMSpecificAttributesMap(ILaunchConfiguration config) throws CoreException {
		Map map = new HashMap(2);
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, (String)null); 
		map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);
		if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry("org.eclipse.jdt.debug"); //$NON-NLS-1$
			if (entry != null) {
				IPluginModelBase model = entry.getExternalModel();
				if (model != null) {
                    File file = new File(model.getInstallLocation());
                    if (!file.isFile())
                        file = new File(file, "jdi.jar"); //$NON-NLS-1$
					if (file.exists())
						map.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, new String[] {file.getAbsolutePath()});
				}
			}
		}
		return map;
	}
	
	public static String getTracingFileArgument(
			ILaunchConfiguration config,
			String optionsFileName)
			throws CoreException {
		try {
			TracingOptionsManager mng = PDECore.getDefault().getTracingOptionsManager();
			Map options =
				config.getAttribute(IPDELauncherConstants.TRACING_OPTIONS, (Map) null);
			String selected = config.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String)null);
			if (selected == null) {
				mng.save(optionsFileName, options);
			} else if (!selected.equals(IPDELauncherConstants.TRACING_NONE)) {
				HashSet result = new HashSet();
				StringTokenizer tokenizer = new StringTokenizer(selected, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					result.add(tokenizer.nextToken());
				}
				mng.save(optionsFileName, options, result);
			}
		} catch (CoreException e) {
			return ""; //$NON-NLS-1$
		}
		return optionsFileName;
	}
	
	public static String[] constructClasspath(ILaunchConfiguration configuration) throws CoreException {
		String jarPath = getStartupJarPath();
		if (jarPath == null)
			return null;
		
		ArrayList entries = new ArrayList();
		entries.add(jarPath);
		
		String bootstrap = configuration.getAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
		StringTokenizer tok = new StringTokenizer(getSubstitutedString(bootstrap), ","); //$NON-NLS-1$
		while (tok.hasMoreTokens())
			entries.add(tok.nextToken().trim());
		return (String[])entries.toArray(new String[entries.size()]);
	}
		
	private static String getStartupJarPath() throws CoreException {
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.platform"); //$NON-NLS-1$
		if (plugin != null && plugin.getModel().getUnderlyingResource() != null) {
			IProject project = plugin.getModel().getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE &&
							roots[i].getPackageFragment("org.eclipse.core.launcher").exists()){ //$NON-NLS-1$
						IPath path = jProject.getOutputLocation().removeFirstSegments(1);
						return project.getLocation().append(path).toOSString();
					}
				}
			}
			if (project.getFile("startup.jar").exists()) //$NON-NLS-1$
				return project.getFile("startup.jar").getLocation().toOSString(); //$NON-NLS-1$
		}
		File startupJar =
			ExternalModelManager.getEclipseHome().append("startup.jar").toFile(); //$NON-NLS-1$
		
		// if something goes wrong with the preferences, fall back on the startup.jar 
		// in the running eclipse.  
		if (!startupJar.exists())
			startupJar = new Path(ExternalModelManager.computeDefaultPlatformPath()).append("startup.jar").toFile(); //$NON-NLS-1$
		
		return startupJar.exists() ? startupJar.getAbsolutePath() : null;
	}

		
	private static String getSubstitutedString(String text) throws CoreException {
		if (text == null)
			return ""; //$NON-NLS-1$
		IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		return mgr.performStringSubstitution(text);
	}
	
	public static String getDefaultWorkspaceLocation(String uniqueName) {
		return "${workspace_loc}/../runtime-" + uniqueName.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static String getDefaultJUnitWorkspaceLocation() {
		return "${workspace_loc}/../junit-workspace"; //$NON-NLS-1$
	}
	
	public static String getDefaultJUnitConfigurationLocation() {
		return "${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/pde-junit"; //$NON-NLS-1$
	}
	

}
