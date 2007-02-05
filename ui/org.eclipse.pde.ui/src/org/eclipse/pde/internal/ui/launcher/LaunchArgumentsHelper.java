/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.TracingOptionsManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
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
		return new ExecutionArguments("", args).getProgramArgumentsArray(); //$NON-NLS-1$
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
		return args == null ? "" : getSubstitutedString(args); //$NON-NLS-1$
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

		// hack on the args from eclipse.ini here
		File installDirectory = new File(Platform.getInstallLocation().getURL().getFile());
		File eclipseIniFile = new File(installDirectory, "eclipse.ini"); //$NON-NLS-1$
		BufferedReader in = null;
		StringBuffer buffer = new StringBuffer(args == null ? "" : args); //$NON-NLS-1$
		if(eclipseIniFile.exists()) {
			try {
				in = new BufferedReader(new FileReader(eclipseIniFile));
				String str;
				boolean vmargs = false;
				while ((str = in.readLine()) != null) {
					if(vmargs) {
						buffer.append(" "+str); //$NON-NLS-1$
					}
					// start concat'ng if we have vmargs
					if(vmargs == false && str.equals("-vmargs")) //$NON-NLS-1$
						vmargs = true;
				}
			} catch (IOException e) {
				PDEPlugin.log(e);
			} finally {
				if(in != null)
					try {
						in.close();
					} catch (IOException e) {
						PDEPlugin.log(e);
					}
			}
		}
		return getSubstitutedString(buffer.toString());
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
			ModelEntry entry = PluginRegistry.findEntry("org.eclipse.jdt.debug"); //$NON-NLS-1$
			if (entry != null) {
				IPluginModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					File file = new File(models[i].getInstallLocation());
					if (!file.isFile())
						file = new File(file, "jdi.jar"); //$NON-NLS-1$
					if (file.exists()) {
						map.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, new String[] {file.getAbsolutePath()});
						break;
					}
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
		String jarPath = TargetPlatformHelper.usesEquinoxStartup() ? getEquinoxStartupPath() : getStartupJarPath();
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

	private static String getEquinoxStartupPath() throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.equinox.launcher"); //$NON-NLS-1$
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource == null) 
				return model.getInstallLocation();
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IClasspathEntry[] entries = jProject.getRawClasspath();
				for (int i = 0; i < entries.length; i++) {
					int kind = entries[i].getEntryKind();
					if (kind == IClasspathEntry.CPE_SOURCE || kind == IClasspathEntry.CPE_LIBRARY) {
						IPackageFragmentRoot[] roots = jProject.findPackageFragmentRoots(entries[i]);
						for (int j = 0; j < roots.length; j++) {
							if (roots[i].getPackageFragment("org.eclipse.equinox.launcher").exists()) { //$NON-NLS-1$
								IPath path;
								// if source folder, find the output folder
								if (kind == IClasspathEntry.CPE_SOURCE) {
									path = entries[i].getOutputLocation();
									if (path == null)
										path = jProject.getOutputLocation();
									// else if is a library jar, then get the location of the jar itself
								} else 
									path = entries[i].getPath();
								path = path.removeFirstSegments(1);
								return project.getLocation().append(path).toOSString();
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static String getStartupJarPath() throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.platform"); //$NON-NLS-1$
		if (model != null && model.getUnderlyingResource() != null) {
			IProject project = model.getUnderlyingResource().getProject();
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
			new Path(TargetPlatform.getLocation()).append("startup.jar").toFile(); //$NON-NLS-1$

		// if something goes wrong with the preferences, fall back on the startup.jar 
		// in the running eclipse.  
		if (!startupJar.exists())
			startupJar = new Path(TargetPlatform.getDefaultLocation()).append("startup.jar").toFile(); //$NON-NLS-1$

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
