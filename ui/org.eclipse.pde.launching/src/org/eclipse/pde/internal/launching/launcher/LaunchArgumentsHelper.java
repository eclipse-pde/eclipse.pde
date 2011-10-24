/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Bundle;

public class LaunchArgumentsHelper {

	/**
	 * Returns the location that will be used as the workspace when launching.  Will
	 * replace variables, so this method should only be called
	 * when variable substitution (may prompt the user) is appropriate.
	 * @param configuration the launch configuration to get the workspace value for
	 * @return workspace location path as a string
	 * @throws CoreException if there is a problem with the configuration
	 */
	public static String getWorkspaceLocation(ILaunchConfiguration configuration) throws CoreException {
		String location = configuration.getAttribute(IPDELauncherConstants.LOCATION, (String) null);
		if (location == null) {
			// backward compatibility
			location = configuration.getAttribute(IPDELauncherConstants.LOCATION + "0", (String) null); //$NON-NLS-1$
			if (location != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute(IPDELauncherConstants.LOCATION + "0", (String) null); //$NON-NLS-1$
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
		String args = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("progargs", (String) null); //$NON-NLS-1$
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("progargs", (String) null); //$NON-NLS-1$
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
				wc.doSave();
			}
		}
		return args == null ? "" : getSubstitutedString(args); //$NON-NLS-1$
	}

	public static String getUserVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("vmargs", (String) null); //$NON-NLS-1$
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("vmargs", (String) null); //$NON-NLS-1$
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, args);
				wc.doSave();
			}
		}
		return args == null ? "" : getSubstitutedString(args); //$NON-NLS-1$
	}

	/**
	 * Fetches the VM Arguments from the current Target Platform
	 *  
	 * @return	VM Arguments from the current Target Platform or empty string if none found
	 */
	public static String getInitialVMArguments() {

		try {
			ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (service != null) {
				ITargetHandle target = service.getWorkspaceTargetHandle();
				if (target != null) {
					String result = target.getTargetDefinition().getVMArguments();
					result = result != null ? result : ""; //$NON-NLS-1$
					return result;
				}
			}
		} catch (CoreException e) {
		}

		// TODO: Generally, once the new preference target platform preference page is in use,
		// this code path will not be used. Once we decide to remove support for old targets/preferences
		// this code can be removed.
		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		StringBuffer result = new StringBuffer(preferences.getString(ICoreConstants.VM_ARGS));

		if (preferences.getBoolean(ICoreConstants.VM_LAUNCHER_INI)) {
			// hack on the arguments from eclipse.ini
			result.append(TargetPlatformHelper.getIniVMArgs());
		}
		return result.toString();
	}

	public static String getInitialProgramArguments() {
		StringBuffer buffer = new StringBuffer("-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog"); //$NON-NLS-1$

		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		String programArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (programArgs.length() > 0) {
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(programArgs);
		}
		return buffer.toString();
	}

	public static File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		String working;
		try {
			working = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, new File(".").getCanonicalPath()); //$NON-NLS-1$
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
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, (String) null);
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

	public static String getTracingFileArgument(ILaunchConfiguration config, String optionsFileName) throws CoreException {
		try {
			TracingOptionsManager mng = PDECore.getDefault().getTracingOptionsManager();
			Map options = config.getAttribute(IPDELauncherConstants.TRACING_OPTIONS, (Map) null);
			String selected = config.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
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
		double targetVersion = TargetPlatformHelper.getTargetVersion();
		String jarPath = targetVersion >= 3.3 ? getEquinoxStartupPath(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER) : getStartupJarPath();
		if (jarPath == null && targetVersion < 3.3)
			jarPath = getEquinoxStartupPath("org.eclipse.core.launcher"); //$NON-NLS-1$

		if (jarPath == null)
			return null;

		ArrayList entries = new ArrayList();
		entries.add(jarPath);

		String bootstrap = configuration.getAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
		StringTokenizer tok = new StringTokenizer(getSubstitutedString(bootstrap), ","); //$NON-NLS-1$
		while (tok.hasMoreTokens())
			entries.add(tok.nextToken().trim());
		return (String[]) entries.toArray(new String[entries.size()]);
	}

	private static String getEquinoxStartupPath(String packageName) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			// found in the target
			if (resource == null)
				return model.getInstallLocation();

			// find it in the workspace
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IClasspathEntry[] entries = jProject.getRawClasspath();
				for (int i = 0; i < entries.length; i++) {
					int kind = entries[i].getEntryKind();
					if (kind == IClasspathEntry.CPE_SOURCE || kind == IClasspathEntry.CPE_LIBRARY) {
						IPackageFragmentRoot[] roots = jProject.findPackageFragmentRoots(entries[i]);
						for (int j = 0; j < roots.length; j++) {
							if (roots[j].getPackageFragment(packageName).exists()) {
								// if source folder, find the output folder
								if (kind == IClasspathEntry.CPE_SOURCE) {
									IPath path = entries[i].getOutputLocation();
									if (path == null)
										path = jProject.getOutputLocation();
									path = path.removeFirstSegments(1);
									return project.getLocation().append(path).toOSString();
								}
								// else if is a library jar, then get the location of the jar itself
								IResource jar = roots[j].getResource();
								if (jar != null) {
									return jar.getLocation().toOSString();
								}
							}
						}
					}
				}
			}
		}
		Bundle bundle = Platform.getBundle(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER);
		if (bundle != null) {
			try {
				URL url = FileLocator.resolve(bundle.getEntry("/")); //$NON-NLS-1$
				url = FileLocator.toFileURL(url);
				String path = url.getFile();
				if (path.startsWith("file:")) //$NON-NLS-1$
					path = path.substring(5);
				path = new File(path).getAbsolutePath();
				if (path.endsWith("!")) //$NON-NLS-1$
					path = path.substring(0, path.length() - 1);
				return path;
			} catch (IOException e) {
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
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE && roots[i].getPackageFragment("org.eclipse.core.launcher").exists()) { //$NON-NLS-1$
						IPath path = jProject.getOutputLocation().removeFirstSegments(1);
						return project.getLocation().append(path).toOSString();
					}
				}
			}
			if (project.getFile("startup.jar").exists()) //$NON-NLS-1$
				return project.getFile("startup.jar").getLocation().toOSString(); //$NON-NLS-1$
		}
		File startupJar = new Path(TargetPlatform.getLocation()).append("startup.jar").toFile(); //$NON-NLS-1$

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
		return getDefaultWorkspaceLocation(uniqueName, false);
	}

	public static String getDefaultWorkspaceLocation(String uniqueName, boolean isJUnit) {
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		String location = launchingStore.getString(isJUnit ? ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION : ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION);
		if (launchingStore.getBoolean(isJUnit ? ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER : ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER)) {
			return location + uniqueName.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return location;
	}

	public static boolean getDefaultJUnitWorkspaceIsContainer() {
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		return launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
	}

	public static String getDefaultJUnitConfigurationLocation() {
		return "${workspace_loc}/.metadata/.plugins/org.eclipse.pde.core/pde-junit"; //$NON-NLS-1$
	}

}
