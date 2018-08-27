/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

import java.io.*;
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
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class LaunchArgumentsHelper {

	/**
	 * Returns the location that will be used as the workspace when launching or
	 * an empty string if the user has specified the <code>-data &#64none</code>
	 * argument for no workspace.  Will replace variables, so this method should
	 * only be called when variable substitution (may prompt the user) is appropriate.
	 *
	 * @param configuration the launch configuration to get the workspace value for
	 * @return workspace location path as a string or an empty if no workspace will be used
	 * @throws CoreException if there is a problem with the configuration
	 */
	public static String getWorkspaceLocation(ILaunchConfiguration configuration) throws CoreException {
		// Check if -data @none is specified
		String[] userArgs = getUserProgramArgumentArray(configuration);
		for (int i = 0; i < userArgs.length; i++) {
			if (userArgs[i].equals("-data") && (i + 1) < userArgs.length && userArgs[i + 1].equals("@none")) { //$NON-NLS-1$ //$NON-NLS-2$
				return ""; //$NON-NLS-1$
			}
		}

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
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service != null) {
				String result = service.getWorkspaceTargetDefinition().getVMArguments();
				result = result != null ? result : ""; //$NON-NLS-1$
				return result;
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
		return ""; //$NON-NLS-1$
	}

	public static String getInitialProgramArguments() {
		StringBuilder buffer = new StringBuilder("-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog"); //$NON-NLS-1$

		try {
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service != null) {
				String result = service.getWorkspaceTargetDefinition().getProgramArguments();
				if (result != null) {
					buffer.append(' ').append(result);
				}
			}
		} catch (CoreException e) {
			PDECore.log(e);
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

	public static Map<String, Object> getVMSpecificAttributesMap(ILaunchConfiguration config) throws CoreException {
		Map<String, Object> map = new HashMap<>(2);
		String javaCommand = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, (String) null);
		map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, javaCommand);
		// Add jdi.jar for macos when java version is less than 1.7
		if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
			ModelEntry entry = PluginRegistry.findEntry("org.eclipse.jdt.debug"); //$NON-NLS-1$
			if (entry != null) {
				IVMInstall vmInstall = VMHelper.getVMInstall(config);
				if (vmInstall instanceof AbstractVMInstall) {
					String javaVersion = ((AbstractVMInstall) vmInstall).getJavaVersion();
					Version osgiVersion = new Version(javaVersion);
					if (osgiVersion.getMajor() == 1 && osgiVersion.getMinor() < 7) {
						IPluginModelBase[] models = entry.getExternalModels();
						for (IPluginModelBase model : models) {
							File file = new File(model.getInstallLocation());
							if (!file.isFile())
								file = new File(file, "jdi.jar"); //$NON-NLS-1$
							if (file.exists()) {
								map.put(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, new String[] {file.getAbsolutePath()});
								break;
							}
						}
					}
				}
			}
		}
		return map;
	}

	public static String getTracingFileArgument(ILaunchConfiguration config, String optionsFileName) {
		try {
			TracingOptionsManager mng = PDECore.getDefault().getTracingOptionsManager();
			Map<String, String> options = config.getAttribute(IPDELauncherConstants.TRACING_OPTIONS, (Map<String, String>) null);
			String selected = config.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null);
			if (selected == null) {
				mng.save(optionsFileName, options);
			} else if (!selected.equals(IPDELauncherConstants.TRACING_NONE)) {
				HashSet<String> result = new HashSet<>();
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

		ArrayList<String> entries = new ArrayList<>();
		entries.add(jarPath);

		String bootstrap = configuration.getAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
		StringTokenizer tok = new StringTokenizer(getSubstitutedString(bootstrap), ","); //$NON-NLS-1$
		while (tok.hasMoreTokens())
			entries.add(tok.nextToken().trim());
		return entries.toArray(new String[entries.size()]);
	}

	/**
	 * Returns the path to the equinox launcher jar.  If the launcher is available
	 * in the workspace, the packageName will be used to determine the expected output
	 * location.
	 *
	 * @param packageName name of the launcher package, typically {@link IPDEBuildConstants#BUNDLE_EQUINOX_LAUNCHER}
	 * @return the path to the equinox launcher jar or <code>null</code>
	 * @throws CoreException
	 */
	private static String getEquinoxStartupPath(String packageName) throws CoreException {
		// See if PDE has the launcher in the workspace or target
		IPluginModelBase model = PluginRegistry.findModel(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource == null) {
				// Found in the target
				String installLocation = model.getInstallLocation();
				if (installLocation == null) {
					return null;
				}

				File bundleFile = new File(installLocation);
				if (!bundleFile.isDirectory()) {
					// The launcher bundle is usually jarred, just return the bundle root
					return installLocation;
				}

				// Unjarred bundle, search for the built jar at the root of the folder
				File[] files = bundleFile.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.indexOf(IPDEBuildConstants.BUNDLE_EQUINOX_LAUNCHER) >= 0;
					}
				});
				for (File file : files) {
					if (file.isFile()) {
						return file.getPath();
					}
				}

				// Source bundle from git://git.eclipse.org/gitroot/equinox/rt.equinox.framework.git
				File binFolder = new File(bundleFile, "bin"); //$NON-NLS-1$
				if (binFolder.isDirectory()) {
					return binFolder.getPath();
				}
				return null;
			}

			// Found in the workspace
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IClasspathEntry[] entries = jProject.getRawClasspath();
				for (IClasspathEntry entrie : entries) {
					int kind = entrie.getEntryKind();
					if (kind == IClasspathEntry.CPE_SOURCE || kind == IClasspathEntry.CPE_LIBRARY) {
						IPackageFragmentRoot[] roots = jProject.findPackageFragmentRoots(entrie);
						for (IPackageFragmentRoot root : roots) {
							if (root.getPackageFragment(packageName).exists()) {
								// if source folder, find the output folder
								if (kind == IClasspathEntry.CPE_SOURCE) {
									IPath path = entrie.getOutputLocation();
									if (path == null)
										path = jProject.getOutputLocation();
									path = path.removeFirstSegments(1);
									return project.getLocation().append(path).toOSString();
								}
								// else if is a library jar, then get the location of the jar itself
								IResource jar = root.getResource();
								if (jar != null) {
									return jar.getLocation().toOSString();
								}
							}
						}
					}
				}
			}
		}

		// No PDE model, see if the launcher bundle is installed
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
				for (IPackageFragmentRoot root : roots) {
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE && root.getPackageFragment("org.eclipse.core.launcher").exists()) { //$NON-NLS-1$
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
