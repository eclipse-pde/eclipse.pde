/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.team.core.RepositoryProvider;

public class ClasspathComputer {

	private static Map<String, Integer> fSeverityTable = null;
	private static final int SEVERITY_ERROR = 3;
	private static final int SEVERITY_WARNING = 2;
	private static final int SEVERITY_IGNORE = 1;

	public static void setClasspath(IProject project, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = getClasspath(project, model, null, false, true);
		JavaCore.create(project).setRawClasspath(entries, null);
	}

	public static IClasspathEntry[] getClasspath(IProject project, IPluginModelBase model, Map<?, ?> sourceLibraryMap, boolean clear, boolean overrideCompliance) throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		ArrayList<IClasspathEntry> result = new ArrayList<>();
		IBuild build = getBuild(project);

		// add JRE and set compliance options
		String ee = getExecutionEnvironment(model.getBundleDescription());
		result.add(createEntryUsingPreviousEntry(javaProject, ee, PDECore.JRE_CONTAINER_PATH));
		setComplianceOptions(JavaCore.create(project), ee, overrideCompliance);

		// add pde container
		result.add(createEntryUsingPreviousEntry(javaProject, ee, PDECore.REQUIRED_PLUGINS_CONTAINER_PATH));

		// add own libraries/source
		addSourceAndLibraries(project, model, build, clear, sourceLibraryMap, result);

		IClasspathEntry[] entries = result.toArray(new IClasspathEntry[result.size()]);
		IJavaModelStatus validation = JavaConventions.validateClasspath(javaProject, entries, javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		return result.toArray(new IClasspathEntry[result.size()]);
	}

	private static void addSourceAndLibraries(IProject project, IPluginModelBase model, IBuild build, boolean clear, Map<?, ?> sourceLibraryMap, ArrayList<IClasspathEntry> result) throws CoreException {
		String testPluginPattern = PDECore.getDefault().getPreferencesManager().getString(ICoreConstants.TEST_PLUGIN_PATTERN);
		boolean isTestPlugin = testPluginPattern != null && testPluginPattern.length() > 0
				&& Pattern.compile(testPluginPattern).matcher(project.getName()).find();
		HashSet<IPath> paths = new HashSet<>();

		// keep existing source folders
		if (!clear) {
			IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
			for (IClasspathEntry entry : entries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
						|| entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					if (paths.add(entry.getPath())) {
						result.add(updateTestAttribute(isTestPlugin, entry));
					}
				}
			}
		}

		IClasspathAttribute[] attrs = getClasspathAttributes(project, model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (IPluginLibrary library : libraries) {
			IBuildEntry buildEntry = build == null ? null : build.getEntry("source." + library.getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				addSourceFolder(buildEntry, project, paths, result, isTestPlugin);
			} else {
				IPath sourceAttachment = sourceLibraryMap != null ? (IPath) sourceLibraryMap.get(library.getName()) : null;
				if (library.getName().equals(".")) { //$NON-NLS-1$
					addJARdPlugin(project, ClasspathUtilCore.getFilename(model), sourceAttachment, attrs, result);
				} else {
					addLibraryEntry(project, library, sourceAttachment, attrs, result);
				}
			}
		}
		if (libraries.length == 0) {
			if (build != null) {
				IBuildEntry buildEntry = build.getEntry("source.."); //$NON-NLS-1$
				if (buildEntry != null) {
					addSourceFolder(buildEntry, project, paths, result, isTestPlugin);
				}
			} else if (ClasspathUtilCore.hasBundleStructure(model)) {
				IPath sourceAttachment = sourceLibraryMap != null ? (IPath) sourceLibraryMap.get(".") : null; //$NON-NLS-1$
				addJARdPlugin(project, ClasspathUtilCore.getFilename(model), sourceAttachment, attrs, result);
			}
		}
	}

	private static IClasspathEntry updateTestAttribute(boolean isTestPlugin, IClasspathEntry entry) {
		if (isTestPlugin == entry.isTest() || entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
			return entry;
		}
		IClasspathAttribute[] classpathAttributes = Arrays.stream(entry.getExtraAttributes())
				.filter(e -> !e.getName().equals(IClasspathAttribute.TEST)).toArray(IClasspathAttribute[]::new);
		if (isTestPlugin) {
			int length = classpathAttributes.length;
			System.arraycopy(classpathAttributes, 0, classpathAttributes = new IClasspathAttribute[length + 1], 0,
					length);
			classpathAttributes[length] = JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true"); //$NON-NLS-1$
		}
		return JavaCore.newSourceEntry(entry.getPath(), entry.getInclusionPatterns(),
				entry.getExclusionPatterns(), entry.getOutputLocation(), classpathAttributes);

	}

	private static IClasspathAttribute[] getClasspathAttributes(IProject project, IPluginModelBase model) {
		IClasspathAttribute[] attributes = new IClasspathAttribute[0];
		if (!RepositoryProvider.isShared(project)) {
			JavadocLocationManager manager = PDECore.getDefault().getJavadocLocationManager();
			String javadoc = manager.getJavadocLocation(model);
			if (javadoc != null) {
				attributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadoc)};
			}
		}
		return attributes;
	}

	private static void addSourceFolder(IBuildEntry buildEntry, IProject project, HashSet<IPath> paths,
			ArrayList<IClasspathEntry> result, boolean isTestPlugin) throws CoreException {
		String[] folders = buildEntry.getTokens();
		for (String folder : folders) {
			IPath path = project.getFullPath().append(folder);
			if (paths.add(path)) {
				if (project.findMember(folder) == null) {
					CoreUtility.createFolder(project.getFolder(folder));
				} else {
					IPackageFragmentRoot root = JavaCore.create(project).getPackageFragmentRoot(path.toString());
					if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
						result.add(root.getRawClasspathEntry());
						continue;
					}
				}
				if (isTestPlugin) {
					result.add(JavaCore.newSourceEntry(path, null, null, null, new IClasspathAttribute[] {
							JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") })); //$NON-NLS-1$
				} else {
					result.add(JavaCore.newSourceEntry(path));
				}
			}
		}
	}

	protected static IBuild getBuild(IProject project) throws CoreException {
		IFile buildFile = PDEProject.getBuildProperties(project);
		IBuildModel buildModel = null;
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}

	private static void addLibraryEntry(IProject project, IPluginLibrary library, IPath sourceAttachment, IClasspathAttribute[] attrs, ArrayList<IClasspathEntry> result) throws JavaModelException {
		String name = ClasspathUtilCore.expandLibraryName(library.getName());
		IResource jarFile = project.findMember(name);
		if (jarFile == null) {
			return;
		}

		IPackageFragmentRoot root = JavaCore.create(project).getPackageFragmentRoot(jarFile);
		if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
			IClasspathEntry oldEntry = root.getRawClasspathEntry();
			// If we have the same binary root but new or different source, we should recreate the entry
			if ((sourceAttachment == null && oldEntry.getSourceAttachmentPath() != null) || (sourceAttachment != null && sourceAttachment.equals(oldEntry.getSourceAttachmentPath()))) {
				if (!result.contains(oldEntry)) {
					result.add(oldEntry);
					return;
				}
			}
		}

		IClasspathEntry entry = createClasspathEntry(project, jarFile, name, sourceAttachment, attrs, library.isExported());
		if (!result.contains(entry)) {
			result.add(entry);
		}
	}

	private static void addJARdPlugin(IProject project, String filename, IPath sourceAttachment, IClasspathAttribute[] attrs, ArrayList<IClasspathEntry> result) {
		String name = ClasspathUtilCore.expandLibraryName(filename);
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IClasspathEntry entry = createClasspathEntry(project, jarFile, filename, sourceAttachment, attrs, true);
			if (!result.contains(entry)) {
				result.add(entry);
			}
		}
	}

	private static IClasspathEntry createClasspathEntry(IProject project, IResource library, String fileName, IPath sourceAttachment, IClasspathAttribute[] attrs, boolean isExported) {
		IResource resource = sourceAttachment != null ? project.findMember(sourceAttachment) : project.findMember(ClasspathUtilCore.getSourceZipName(fileName));
		return JavaCore.newLibraryEntry(library.getFullPath(), resource == null ? null : resource.getFullPath(), null, new IAccessRule[0], attrs, isExported);
	}

	private static String getExecutionEnvironment(BundleDescription bundleDescription) {
		if (bundleDescription != null) {
			String[] envs = bundleDescription.getExecutionEnvironments();
			if (envs.length > 0) {
				return envs[0];
			}
		}
		return null;
	}

	/**
	 * Sets compiler compliance options on the given project to match the default compliance settings
	 * for the specified execution environment. Overrides any existing settings.
	 *
	 * @param project project to set compiler compliance options for
	 * @param eeId execution environment identifier or <code>null</code>
	 */
	public static void setComplianceOptions(IJavaProject project, String eeId) {
		setComplianceOptions(project, eeId, true);
	}

	/**
	 * Sets compiler compliance options on the given project to match the default compliance settings
	 * for the specified execution environment. Only sets options that do not already have an explicit
	 * setting based on the given override flag.
	 * <p>
	 * If the specified execution environment is <code>null</code> and override is <code>true</code>,
	 * all compliance options are removed from the options map before applying to the project.
	 * </p>
	 * @param project project to set compiler compliance options for
	 * @param eeId execution environment identifier, or <code>null</code>
	 * @param overrideExisting whether to override a setting if already present
	 */
	public static void setComplianceOptions(IJavaProject project, String eeId, boolean overrideExisting) {
		Map<String, String> projectMap = project.getOptions(false);
		IExecutionEnvironment ee = null;
		Map<?, ?> options = null;
		if (eeId != null) {
			ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(eeId);
			if (ee != null) {
				options = ee.getComplianceOptions();
			}
		}
		if (options == null) {
			if (overrideExisting && !projectMap.isEmpty()) {
				projectMap.remove(JavaCore.COMPILER_COMPLIANCE);
				projectMap.remove(JavaCore.COMPILER_SOURCE);
				projectMap.remove(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
				projectMap.remove(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER);
				projectMap.remove(JavaCore.COMPILER_PB_ENUM_IDENTIFIER);
			} else {
				return;
			}
		} else {
			String compliance = (String) options.get(JavaCore.COMPILER_COMPLIANCE);
			Iterator<?> iterator = options.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
				String option = (String) entry.getKey();
				String value = (String) entry.getValue();
				if (JavaCore.VERSION_1_3.equals(compliance) || JavaCore.VERSION_1_4.equals(compliance)) {
					if (JavaCore.COMPILER_PB_ASSERT_IDENTIFIER.equals(option) || JavaCore.COMPILER_PB_ENUM_IDENTIFIER.equals(option)) {
						// for 1.3 & 1.4 projects, only override the existing setting if the default setting
						// is a greater severity than the existing setting
						setMinimumCompliance(projectMap, option, value, overrideExisting);
					} else {
						setCompliance(projectMap, option, value, overrideExisting);
					}
				} else {
					setCompliance(projectMap, option, value, overrideExisting);
				}
			}
		}

		project.setOptions(projectMap);

	}

	/**
	 * Puts the key/value pair into the map if the map can be overridden or the map doesn't
	 * already contain the key. If the value is <code>null</code>, the existing value remains.
	 *
	 * @param map map to put the value in
	 * @param key key for the value
	 * @param value value to put in the map or <code>null</code>
	 * @param override whether existing map entries should be replaced with the value
	 */
	private static void setCompliance(Map<String, String> map, String key, String value, boolean override) {
		if (value != null && (override || !map.containsKey(key))) {
			map.put(key, value);
		}
	}

	/**
	 * Checks if the current value stored in the map is less severe than the given minimum value. If
	 * the minimum value is higher, the map will be updated with the minimum. If the minimum value
	 * is <code>null</code>, the existing value remains.
	 *
	 * @param map the map to check the value in
	 * @param key the key to get the current value out of the map
	 * @param minimumValue the minimum value allowed or <code>null</code>
	 * @param override whether an existing value in the map should be replaced
	 */
	private static void setMinimumCompliance(Map<String, String> map, String key, String minimumValue, boolean override) {
		if (minimumValue != null && (override || !map.containsKey(key))) {
			if (fSeverityTable == null) {
				fSeverityTable = new HashMap<>(3);
				fSeverityTable.put(JavaCore.IGNORE, Integer.valueOf(SEVERITY_IGNORE));
				fSeverityTable.put(JavaCore.WARNING, Integer.valueOf(SEVERITY_WARNING));
				fSeverityTable.put(JavaCore.ERROR, Integer.valueOf(SEVERITY_ERROR));
			}
			String currentValue = map.get(key);
			int current = currentValue != null && fSeverityTable.containsKey(currentValue) ? fSeverityTable.get(currentValue).intValue() : 0;
			int minimum = fSeverityTable.containsKey(minimumValue) ? fSeverityTable.get(minimumValue).intValue() : 0;
			if (current < minimum) {
				map.put(key, minimumValue);
			}
		}
	}

	/**
	 * Returns a new classpath container entry for the given execution environment.  If the given java project
	 * has an existing JRE/EE classpath entry, the access rules, extra attributes and isExported settings of
	 * the existing entry will be added to the new execution entry.
	 *
	 * @param javaProject project to check for existing classpath entries
	 * @param ee id of the execution environment to create an entry for
	 * @param path id of the container to create an entry for
	 *
	 * @return new classpath container entry
	 * @throws CoreException if there is a problem accessing the classpath entries of the project
	 */
	public static IClasspathEntry createEntryUsingPreviousEntry(IJavaProject javaProject, String ee, IPath path) throws CoreException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if (path.isPrefixOf(entry.getPath()) && path.equals(PDECore.JRE_CONTAINER_PATH)) {
				return JavaCore.newContainerEntry(getEEPath(ee), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
			}
			if (entry.getPath().equals(path)) {
				return JavaCore.newContainerEntry(path, entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported());
			}
		}

		if (path.equals(PDECore.JRE_CONTAINER_PATH)) {
			return createJREEntry(ee);
		}

		return JavaCore.newContainerEntry(path);
	}

	/**
	 * Returns a classpath container entry for the given execution environment.
	 * @param ee id of the execution environment or <code>null</code>
	 * @return classpath container entry
	 */
	public static IClasspathEntry createJREEntry(String ee) {
		return JavaCore.newContainerEntry(getEEPath(ee));
	}

	/**
	 * Returns the JRE container path for the execution environment with the given id.
	 * @param ee execution environment id or <code>null</code>
	 * @return JRE container path for the execution environment
	 */
	private static IPath getEEPath(String ee) {
		IPath path = null;
		if (ee != null) {
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment env = manager.getEnvironment(ee);
			if (env != null) {
				path = JavaRuntime.newJREContainerPath(env);
			}
		}
		if (path == null) {
			path = JavaRuntime.newDefaultJREContainerPath();
		}
		return path;
	}

	/**
	 * @return a new classpath container entry for a required plugin container
	 */
	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH);
	}

}
