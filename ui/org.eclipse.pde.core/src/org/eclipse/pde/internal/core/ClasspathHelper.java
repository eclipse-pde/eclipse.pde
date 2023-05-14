/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 577541 - Clean up ClasspathHelper and TargetWeaver
 *     Hannes Wellmann - Bug 577543 - Only weave dev.properties for secondary launches if plug-in is from Running-Platform
 *     Hannes Wellmann - Bug 577118 - Handle multiple Plug-in versions in launching facility
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;

public class ClasspathHelper {

	private ClasspathHelper() { // static use only
	}

	private static final String DOT = "."; //$NON-NLS-1$
	private static final String FRAGMENT_ANNOTATION = "@fragment@"; //$NON-NLS-1$
	private static final String DEV_CLASSPATH_ENTRY_SEPARATOR = ","; //$NON-NLS-1$
	private static final String DEV_CLASSPATH_VERSION_SEPARATOR = ";"; //$NON-NLS-1$

	public static String getDevEntriesProperties(String fileName, boolean checkExcluded) throws CoreException {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		Map<String, List<IPluginModelBase>> bundleModels = Arrays.stream(models)
				.filter(o -> o.toString() != null) //toString() used as key
				.collect(Collectors.groupingBy(m -> m.getPluginBase().getId()));

		Properties properties = getDevEntriesProperties(bundleModels, checkExcluded);
		return writeDevEntries(fileName, properties);
	}

	public static String getDevEntriesProperties(String fileName, Map<String, List<IPluginModelBase>> map)
			throws CoreException {
		Properties properties = getDevEntriesProperties(map, true);
		return writeDevEntries(fileName, properties);
	}

	public static String writeDevEntries(String fileName, Properties properties) throws CoreException {
		File file = new File(fileName);
		if (!file.exists()) {
			File directory = file.getParentFile();
			if (directory != null && (!directory.exists() || directory.isFile())) {
				directory.mkdirs();
			}
		}
		try (FileOutputStream stream = new FileOutputStream(fileName)) {
			properties.store(stream, ""); //$NON-NLS-1$
			return new URL("file:" + fileName).toString(); //$NON-NLS-1$
		} catch (IOException e) {
			PDECore.logException(e);
			throw new CoreException(Status.error("Failed to create dev.properties file", e)); //$NON-NLS-1$
		}
	}

	public static Properties getDevEntriesProperties(Map<String, List<IPluginModelBase>> bundlesMap,
			boolean checkExcluded) {

		Set<IPluginModelBase> launchedPlugins = bundlesMap.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Map<IPluginModelBase, String> modelEntries = new LinkedHashMap<>();
		// account for cascading workspaces
		TargetWeaver.weaveRunningPlatformDevProperties(modelEntries, launchedPlugins);

		for (List<IPluginModelBase> models : bundlesMap.values()) {
			for (IPluginModelBase model : models) {
				if (model.getUnderlyingResource() != null) {
					String entry = formatEntry(getDevPaths(model, checkExcluded, launchedPlugins));
					if (!entry.isEmpty()) {
						// overwrite entry, if plug-in from primary Eclipse is
						// also imported into workspace of secondary eclipse
						modelEntries.put(model, entry);
					}
				}
			}
			// Check if there is an entry of a workspace-model or
			// a target model woven from a primary-workspace plugin with same id
			if (models.stream().anyMatch(modelEntries::containsKey)) {
				for (IPluginModelBase model : models) {
					// in case of multiple models with same id add empty entries
					// for target-bundles to ensure the non-version entry is not
					// used to falsely extend their class-path
					modelEntries.putIfAbsent(model, ""); //$NON-NLS-1$
				}
			}
		}

		Properties properties = new Properties();
		modelEntries.forEach((m, cp) -> addDevClasspath(m.getPluginBase(), properties, cp, false));
		properties.put("@ignoredot@", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		return properties;
	}

	private static String formatEntry(Collection<IPath> paths) {
		return paths.stream().map(IPath::toString).collect(Collectors.joining(DEV_CLASSPATH_ENTRY_SEPARATOR));
	}

	public static void addDevClasspath(IPluginBase model, Properties devProperties, String devCP, boolean append) {
		// add entries with & without version to be backward-compatible with
		// 'old' Equinox, that doesn't consider versions, too.
		String id = model.getId();
		if (!devCP.isEmpty()) {
			addDevCPEntry(id, devCP, devProperties, append);
		}
		addDevCPEntry(id + DEV_CLASSPATH_VERSION_SEPARATOR + model.getVersion(), devCP, devProperties, append);
	}

	private static void addDevCPEntry(String id, String devCP, Properties devProperties, boolean append) {
		if (append) {
			devProperties.merge(id, devCP, (vOld, vNew) -> vOld + DEV_CLASSPATH_ENTRY_SEPARATOR + vNew);
		} else {
			devProperties.put(id, devCP);
		}
	}

	public static String getDevClasspath(Properties devProperties, String id, String version) {
		Object cp = devProperties.get(id + ClasspathHelper.DEV_CLASSPATH_VERSION_SEPARATOR + version);
		return (String) (cp != null ? cp : devProperties.get(id)); // prefer version-entry
	}

	// creates a map whose key is a Path to the source directory/jar and the value is a Path output directory or jar.
	private static Map<IPath, List<IPath>> getClasspathMap(IProject project, boolean checkExcluded,
			boolean absolutePaths) throws JavaModelException {
		Set<Path> excluded = getFoldersToExclude(project, checkExcluded);
		IJavaProject jProject = JavaCore.create(project);
		Map<IPath, List<IPath>> map = new LinkedHashMap<>();
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			// most of the paths we get will be project relative, so we need to make the paths relative
			// we will have problems adding an "absolute" path that is workspace relative
			IPath output = null;
			IPath source = null;
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				source = entry.getPath();
				output = entry.getOutputLocation();
				if (output == null) {
					output = jProject.getOutputLocation();
				}
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				source = entry.getPath();
				output = entry.getPath();
				if (source.segmentCount() == 1) {
					source = new Path(DOT);
				}
			}
			if (output != null && !excluded.contains(output)) {
				IResource file = project.findMember(output.removeFirstSegments(1));
				// make the path either relative or absolute
				if (file != null) {
					boolean isLinked = file.isLinked(IResource.CHECK_ANCESTORS);
					if (isLinked || absolutePaths) {
						IPath location = file.getLocation();
						if (location != null) {
							output = location.makeAbsolute();
						} else {
							PDECore.log(Status.error(NLS.bind(PDECoreMessages.ClasspathHelper_BadFileLocation, file.getFullPath())));
							continue;
						}
					} else {
						output = output.makeRelative();
					}
					map.computeIfAbsent(source, s -> new ArrayList<>()).add(output);
				}
			}
		}

		// Add additional entries from contributed bundle classpath resolvers
		IBundleClasspathResolver[] resolvers = PDECore.getDefault().getClasspathContainerResolverManager().getBundleClasspathResolvers(project);
		for (IBundleClasspathResolver resolver : resolvers) {
			Map<IPath, Collection<IPath>> resolved = resolver.getAdditionalClasspathEntries(jProject);
			resolved.forEach((ceSource, value) -> { // merge into map
				List<IPath> mapValue = map.computeIfAbsent(ceSource, s -> new ArrayList<>());
				mapValue.addAll(value);
			});
		}

		return map;
	}

	// find the corresponding paths for a library name.  Searches for source folders first, but includes any libraries on the buildpath with the same name
	private static List<IPath> findLibrary(String libName, IProject project, Map<IPath, List<IPath>> classpathMap, IBuild build) {
		List<IPath> paths = new ArrayList<>();
		IBuildEntry entry = (build != null) ? build.getEntry(IBuildEntry.JAR_PREFIX + libName) : null;
		if (entry != null) {
			String[] resources = entry.getTokens();
			for (String resource : resources) {
				IResource res = project.findMember(resource);
				if (res != null) {
					List<IPath> list = classpathMap.getOrDefault(res.getFullPath(), Collections.emptyList());
					paths.addAll(list);
				}
			}
		}

		// search for a library that exists in jar form on the buildpath
		IPath path = null;
		if (libName.equals(DOT)) {
			path = new Path(DOT);
		} else {
			IResource res = project.findMember(libName);
			if (res != null) {
				path = res.getFullPath();
			} else {
				path = new Path(libName);
			}
		}

		List<IPath> list = classpathMap.getOrDefault(path, Collections.emptyList());
		paths.addAll(list);
		return paths;
	}

	private static Set<IPath> getDevPaths(IPluginModelBase model, boolean checkExcluded, Set<IPluginModelBase> plugins) {
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				Map<IPath, List<IPath>> classpathMap = getClasspathMap(project, checkExcluded, false);
				IBuild build = getBuild(project);
				Set<IPath> result = new LinkedHashSet<>();
				// if it is a custom build, act like there is no build.properties (add everything)
				if (build != null && build.getEntry("custom") == null) { //$NON-NLS-1$
					IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
					if (libraries.length == 0) {
						List<IPath> paths = findLibrary(DOT, project, classpathMap, build);
						if (paths.isEmpty() && !classpathMap.isEmpty()) {
							// No mapping for default library, if there are source folders just add their corresponding output folders to the build path.
							// This likely indicates an error in the build.properties, but to be friendly we should add the output folders so running/debugging
							// works (see bug 237025)
							paths = new ArrayList<>();
							classpathMap.values().forEach(paths::addAll);
						}
						addPaths(paths, project, result);
					} else {
						for (int i = 0; i < libraries.length; i++) {
							List<IPath> paths = findLibrary(libraries[i].getName(), project, classpathMap, build);
							if (paths.isEmpty() && !libraries[i].getName().equals(DOT)) {
								paths = findLibraryFromFragments(libraries[i].getName(), model, checkExcluded, plugins);
							}
							addPaths(paths, project, result);
						}
					}
					return result;
				}
				// if no build.properties, add all output folders
				classpathMap.values().forEach(l -> addPaths(l, project, result));
				return result;
			}
		} catch (CoreException e) {
		}
		return Collections.emptySet();
	}

	private static void addPaths(List<IPath> paths, IProject project, Set<IPath> result) {
		for (IPath path : paths) {
			IPath resultPath = resolvePath(project, path);
			if (resultPath != null) {
				result.add(resultPath);
			}
		}
	}

	// looks for fragments for a plug-in.  Then searches the fragments for a specific library.  Will return paths which are absolute (required by runtime)
	private static List<IPath> findLibraryFromFragments(String libName, IPluginModelBase model, boolean checkExcluded, Set<IPluginModelBase> plugins) {
		IFragmentModel[] frags = PDEManager.findFragmentsFor(model);
		for (int i = 0; i < frags.length; i++) {
			if (!plugins.contains(frags[i])) {
				continue;
			}
			// look in project first
			if (frags[i].getUnderlyingResource() != null) {
				try {
					IProject project = frags[i].getUnderlyingResource().getProject();
					Map<IPath, List<IPath>> classpathMap = getClasspathMap(project, checkExcluded, true);
					IBuild build = getBuild(project);
					List<IPath> paths = findLibrary(libName, project, classpathMap, build);
					if (!paths.isEmpty()) {
						return postfixFragmentAnnotation(paths);
					}

				} catch (JavaModelException e) {
				}
				// if external plugin, look in child directories for library
			} else {
				File file = new File(frags[i].getInstallLocation());
				if (file.isDirectory()) {
					file = new File(file, libName);
					if (file.exists()) {
						// Postfix fragment annotation for fragment path (fix bug 294211)
						return List.of(new Path(file.getPath() + FRAGMENT_ANNOTATION));
					}
				}
			}
		}
		return Collections.emptyList();
	}

	private static IBuild getBuild(IProject project) {
		IFile file = PDEProject.getBuildProperties(project);
		IPath location = file.getLocation();
		boolean existsOnFileSystem = location != null && location.toFile().exists();
		return existsOnFileSystem ? new WorkspaceBuildModel(file).getBuild() : null;
	}

	/*
	 * Postfixes the fragment annotation for the paths that we know come
	 * from fragments.  This is needed to fix bug 294211.
	 */
	private static List<IPath> postfixFragmentAnnotation(List<IPath> paths) {
		return paths.stream().map(p -> new Path(p + FRAGMENT_ANNOTATION)).collect(Collectors.toList());
	}

	private static IPath resolvePath(IProject project, IPath path) {
		if (path.isAbsolute()) {
			return path;
		} else if (path.segmentCount() > 0 && path.segment(0).equals(project.getName())) {
			IContainer bundleRoot = PDEProject.getBundleRoot(project);
			IPath rootPath = bundleRoot.getFullPath();
			// make path relative to bundle root
			path = path.makeRelativeTo(rootPath);
			if (path.segmentCount() == 0) {
				return new Path(DOT);
			}
			if (bundleRoot.findMember(path) != null) {
				return path;
			}
		}
		return null;
	}

	private static final Pattern BIN_EXCLUDES_SEPARATOR = Pattern.compile(","); //$NON-NLS-1$

	private static Set<Path> getFoldersToExclude(IProject project, boolean checkExcluded) {
		if (checkExcluded) {
			IEclipsePreferences pref = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
			if (pref != null) {
				String binExcludes = pref.get(ICoreConstants.SELFHOSTING_BIN_EXCLUDES, ""); //$NON-NLS-1$
				if (!binExcludes.isBlank()) {
					Stream<String> elements = BIN_EXCLUDES_SEPARATOR.splitAsStream(binExcludes);
					return elements.map(String::trim).map(Path::new).collect(Collectors.toUnmodifiableSet());
				}
			}
		}
		return Collections.emptySet();
	}

}
