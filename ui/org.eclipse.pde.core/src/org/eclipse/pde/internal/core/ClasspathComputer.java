/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
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
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.PDEClasspathContainerSaveHelper;
import org.eclipse.team.core.RepositoryProvider;

public class ClasspathComputer {

	private record ClasspathConfiguration(IPluginModelBase model, IJavaProject javaProject,
			IClasspathAttribute[] defaultAttrs, Map<IPath, IClasspathEntry> originalByPath,
			List<IClasspathEntry> reloaded) {
	}

	private static Map<String, Integer> fSeverityTable = null;
	private static final int SEVERITY_ERROR = 3;
	private static final int SEVERITY_WARNING = 2;
	private static final int SEVERITY_IGNORE = 1;

	/**
	 * Job used to update class path containers.
	 */
	private static final UpdateClasspathsJob fUpdateJob = new UpdateClasspathsJob();

	static final IResourceChangeListener CHANGE_LISTENER = new IResourceChangeListener() {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResource resource = event.getResource();
			if (resource instanceof IProject project) {
				if (PDECore.DEBUG_STATE) {
					System.out.println(String.format("Project %s was deleted.", project.getName())); //$NON-NLS-1$
				}
				getStateFile(project).delete();
			}
		}
	};

	public static void setClasspath(IProject project, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = getClasspath(project, model, null, false, true);
		JavaCore.create(project).setRawClasspath(entries, null);
	}

	public static IClasspathEntry[] getClasspath(IProject project, IPluginModelBase model, Map<String, IPath> sourceLibraryMap, boolean clear, boolean overrideCompliance) throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		List<IClasspathEntry> originalClasspath = clear ? List.of() : Arrays.asList(javaProject.getRawClasspath());
		ClasspathConfiguration context = new ClasspathConfiguration(model, javaProject,
				getClasspathAttributes(project, model), mapFirstSeenByPath(originalClasspath.stream()),
				new ArrayList<>());

		// add JRE and set compliance options
		String ee = getExecutionEnvironment(model.getBundleDescription());
		addContainerEntry(getEEPath(ee), context);
		setComplianceOptions(javaProject, ee, overrideCompliance);

		// add pde container
		addContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, context);

		// add own libraries/source
		addSourceAndLibraries(sourceLibraryMap != null ? sourceLibraryMap : Collections.emptyMap(), context);

		boolean isTestPlugin = hasTestPluginName(project);
		IClasspathEntry[] entries = collectInOriginalOrder(originalClasspath, context.reloaded, isTestPlugin);
		IJavaModelStatus validation = JavaConventions.validateClasspath(javaProject, entries, javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		return entries;
	}

	private static IClasspathEntry[] collectInOriginalOrder(List<IClasspathEntry> originalClasspath,
			List<IClasspathEntry> reloaded, boolean isTestPlugin) {
		// preserve original entries which eventually weren't reloaded
		Map<IPath, IClasspathEntry> resultingReloadedByPath = mapFirstSeenByPath(reloaded.stream());
		List<IClasspathEntry> result = new ArrayList<>(originalClasspath);
		result.replaceAll(e -> {
			IClasspathEntry replacement = resultingReloadedByPath.remove(pathWithoutEE(e.getPath()));
			return replacement != null ? replacement : e;
		});
		// using the order of reloading, append new entries (in the map still)
		result.addAll(resultingReloadedByPath.values());
		if (isTestPlugin) {
			// don't remove existing TEST attributes, but set if advised
			result.replaceAll(e -> updateTestAttribute(true, e));
		}
		return result.toArray(IClasspathEntry[]::new);
	}

	private static Map<IPath, IClasspathEntry> mapFirstSeenByPath(Stream<IClasspathEntry> entryStream) {
		return entryStream.collect(
				Collectors.toMap(e -> pathWithoutEE(e.getPath()), e -> e, (first, dupe) -> first, LinkedHashMap::new));
	}

	private static IPath pathWithoutEE(IPath path) {
		// The path member of IClasspathEntry for JRE_CONTAINER_PATH may
		// also declare an Execution Environment, which is an attribute.
		return PDECore.JRE_CONTAINER_PATH.isPrefixOf(path) ? PDECore.JRE_CONTAINER_PATH : path;
	}

	private static void addContainerEntry(IPath path, ClasspathConfiguration context) {
		IClasspathEntry original = context.originalByPath.get(pathWithoutEE(path));
		context.reloaded.add(JavaCore.newContainerEntry(path, //
				original != null ? original.getAccessRules() : null,
				original != null ? original.getExtraAttributes() : context.defaultAttrs,
				original != null ? original.isExported() : false));
	}

	private static void addSourceAndLibraries(Map<String, IPath> sourceLibraryMap, ClasspathConfiguration context)
			throws CoreException {
		IPluginLibrary[] libraries = context.model.getPluginBase().getLibraries();
		IBuild build = getBuild(context.javaProject.getProject());
		for (IPluginLibrary library : libraries) {
			IBuildEntry buildEntry = build == null ? null : build.getEntry("source." + library.getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				addSourceFolders(buildEntry, context);
			} else if (library.getName().equals(".")) { //$NON-NLS-1$
				addJARdPlugin(".", sourceLibraryMap, context); //$NON-NLS-1$
			} else {
				addLibraryEntry(library, sourceLibraryMap, context);
			}
		}
		if (libraries.length == 0) {
			if (build != null) {
				IBuildEntry buildEntry = build.getEntry("source.."); //$NON-NLS-1$
				if (buildEntry != null) {
					addSourceFolders(buildEntry, context);
				}
			} else if (ClasspathUtilCore.hasBundleStructure(context.model)) {
				addJARdPlugin(".", sourceLibraryMap, context); //$NON-NLS-1$
			}
		}
	}

	public static boolean hasTestPluginName(IProject project) {
		String pattern = PDECore.getDefault().getPreferencesManager().getString(ICoreConstants.TEST_PLUGIN_PATTERN);
		return pattern != null && !pattern.isEmpty() && Pattern.compile(pattern).matcher(project.getName()).find();
	}

	/**
	 * Returns true if the given project is a java project that has
	 * {@code IClasspathEntry#CPE_SOURCE source classpath-entries} that are all
	 * marked as {@code IClasspathAttribute#TEST test sources}.
	 *
	 * @return true if the given project is a test java project
	 */
	public static boolean hasTestOnlyClasspath(IProject project) {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null) {
			return false;
		}
		try {
			boolean hasSources = false;
			for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					hasSources = true;
					if (!entry.isTest()) {
						return false;
					}
				}
			}
			return hasSources; // if it has sources, all are test-sources
		} catch (JavaModelException e) { // assume no valid java-project
		}
		return false;
	}

	public static IClasspathEntry updateTestAttribute(boolean isTestPlugin, IClasspathEntry entry) {
		if (isTestPlugin == entry.isTest() || entry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
			return entry;
		}
		Stream<IClasspathAttribute> cpAttributes = Arrays.stream(entry.getExtraAttributes())
				.filter(e -> !e.getName().equals(IClasspathAttribute.TEST));
		if (isTestPlugin) {
			IClasspathAttribute testAttribute = JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true"); //$NON-NLS-1$
			cpAttributes = Stream.concat(cpAttributes, Stream.of(testAttribute));
		}
		return JavaCore.newSourceEntry(entry.getPath(), entry.getInclusionPatterns(), entry.getExclusionPatterns(),
				entry.getOutputLocation(), cpAttributes.toArray(IClasspathAttribute[]::new));
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

	private static void addSourceFolders(IBuildEntry buildEntry, ClasspathConfiguration context)
			throws CoreException {
		String[] folders = buildEntry.getTokens();
		IProject project = context.javaProject.getProject();
		for (String folder : folders) {
			IPath path = project.getFullPath().append(folder);
			IClasspathEntry orig = context.originalByPath.get(pathWithoutEE(path));
			if (orig != null) {
				context.reloaded.add(orig);
				continue;
			}
			if (project.findMember(folder) == null) {
				CoreUtility.createFolder(project.getFolder(folder));
			} else {
				IPackageFragmentRoot root = context.javaProject.getPackageFragmentRoot(path.toString());
				if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
					context.reloaded.add(root.getRawClasspathEntry());
					continue;
				}
			}
			context.reloaded.add(JavaCore.newSourceEntry(path));
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

	private static void addLibraryEntry(IPluginLibrary library, Map<String, IPath> sourceLibraryMap,
			ClasspathConfiguration context) {
		String name = ClasspathUtilCore.expandLibraryName(library.getName());
		IResource jarFile = context.javaProject.getProject().findMember(name);
		if (jarFile == null) {
			return;
		}

		IPath sourceAttachment = sourceLibraryMap.get(library.getName());
		boolean isExported = library.isExported();

		IPackageFragmentRoot root = context.javaProject.getPackageFragmentRoot(jarFile);
		try {
			if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
				IClasspathEntry oldEntry = root.getRawClasspathEntry();
				// If we have the same binary root but new or different source,
				// we
				// should recreate the entry. That is when the source
				// attachment:
				// - is not defined: the default could be available now, or
				// - is overridden with a different value.
				if ((sourceAttachment == null && oldEntry.getSourceAttachmentPath() != null)
						|| (sourceAttachment != null && sourceAttachment.equals(oldEntry.getSourceAttachmentPath()))) {
					context.reloaded.add(oldEntry);
					return;
				}
				isExported = oldEntry.isExported();
			}
		} catch (JavaModelException ignored) {
			// For example ELEMENT_NOT_ON_CLASSPATH is expected when
			// rawclasspath was changed manually.
			// In any case PDE can not solve any issue beside ignoring it.
		}
		reloadClasspathEntry(jarFile, name, sourceAttachment, isExported, context);
	}

	private static void addJARdPlugin(String libraryName, Map<String, IPath> sourceLibraryMap,
			ClasspathConfiguration context) {
		String filename = ClasspathUtilCore.getFilename(context.model);
		String name = ClasspathUtilCore.expandLibraryName(filename);
		IResource jarFile = context.javaProject.getProject().findMember(name);
		if (jarFile != null) {
			IPath sourceAttachment = sourceLibraryMap.get(libraryName);
			reloadClasspathEntry(jarFile, filename, sourceAttachment, true, context);
		}
	}

	private static void reloadClasspathEntry(IResource library, String fileName, IPath sourceAttachment,
			boolean isExported, ClasspathConfiguration context) {
		IClasspathEntry orig = context.originalByPath.get(pathWithoutEE(library.getFullPath()));
		if (orig != null && sourceAttachment == null) {
			sourceAttachment = orig.getSourceAttachmentPath();
		}
		IProject project = context.javaProject.getProject();
		IResource source = sourceAttachment != null ? project.findMember(sourceAttachment)
				: project.findMember(ClasspathUtilCore.getSourceZipName(fileName));
		sourceAttachment = source == null ? null : source.getFullPath();
		context.reloaded.add(JavaCore.newLibraryEntry(library.getFullPath(), sourceAttachment, null,
				orig != null ? orig.getAccessRules() : null, //
				orig != null ? orig.getExtraAttributes() : context.defaultAttrs,
				orig != null ? orig.isExported() : isExported));
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
		Map<String, String> options = null;
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
			String compliance = options.get(JavaCore.COMPILER_COMPLIANCE);
			Iterator<Entry<String, String>> iterator = options.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String> entry = iterator.next();
				String option = entry.getKey();
				String value = entry.getValue();
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

	public static IClasspathEntry[] computeClasspathEntries(IPluginModelBase model, IProject project) {
		IClasspathContainer container = new RequiredPluginsClasspathContainer(model, project);
		return container.getClasspathEntries();
	}

	public static void requestClasspathUpdate(IProject project) {
		if (project == null) {
			return;
		}
		requestClasspathUpdate(List.of(project));
	}

	public static void requestClasspathUpdate(Collection<IProject> updateProjects) {
		if (updateProjects == null || updateProjects.isEmpty()) {
			return;
		}
		fUpdateJob.addAll(updateProjects);
	}

	static void requestClasspathUpdate(IProject project, IClasspathContainer savedState) {
		fUpdateJob.add(project, savedState);
	}

	/**
	 * Job to update class path containers asynchronously. Avoids blocking the
	 * UI thread. The job is given a workspace lock so other jobs can't run on a
	 * stale classpath.
	 */
	private static final class UpdateClasspathsJob extends Job {

		private static final int WORK = 10_000;
		private final Queue<UpdateRequest> workQueue = new ConcurrentLinkedQueue<>();

		/**
		 * Constructs a new job.
		 */
		public UpdateClasspathsJob() {
			super(PDECoreMessages.PluginModelManager_1);
			// The job is given a workspace lock so other jobs can't run on a
			// stale classpath (bug 354993)
			setRule(ResourcesPlugin.getWorkspace().getRoot());
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == PluginModelManager.class || family == ClasspathComputer.class;
		}

		@Override
		protected IStatus run(IProgressMonitor jobMonitor) {
			SubMonitor monitor = SubMonitor.convert(jobMonitor, PDECoreMessages.PluginModelManager_1, WORK);
			PluginModelManager.getInstance().initialize(monitor.split(10));
			PluginModelManager modelManager = PluginModelManager.getInstance();
			Map<IJavaProject, IClasspathContainer> updateProjects = new LinkedHashMap<>();
			Map<IProject, IStatus> errorsPerProject = new LinkedHashMap<>();
			UpdateRequest request;
			while (!monitor.isCanceled() && (request = workQueue.poll()) != null) {
				monitor.setWorkRemaining(WORK);
				IProject project = request.project();
				if (project.exists() && project.isOpen()) {
					IPluginModelBase model = modelManager.findModel(project);
					if (model != null && PluginProject.isJavaProject(project)) {
						IJavaProject javaProject = JavaCore.create(project);
						RequiredPluginsClasspathContainer classpathContainer = new RequiredPluginsClasspathContainer(
								model, project);
						try {
							if (!isUpToDate(project, classpathContainer.computeEntries(), request.container())) {
								updateProjects.put(javaProject, classpathContainer);
								errorsPerProject.remove(project);
								saveState(project, classpathContainer);
							}
						} catch (CoreException e) {
							errorsPerProject.put(project, e.getStatus());
						}
						monitor.worked(1);
					}
				}
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (!updateProjects.isEmpty()) {
				int i = 0;
				int n = updateProjects.size();
				IJavaProject[] javaProjects = new IJavaProject[n];
				IClasspathContainer[] container = new IClasspathContainer[n];
				for (Entry<IJavaProject, IClasspathContainer> entry : updateProjects.entrySet()) {
					javaProjects[i] = entry.getKey();
					container[i] = entry.getValue();
					i++;
				}
				try {
					setProjectContainers(javaProjects, container, monitor);
				} catch (JavaModelException e) {
					return e.getStatus();
				}
			}
			IStatus[] errors = errorsPerProject.values().toArray(IStatus[]::new);
			if (errors.length == 0) {
				return Status.OK_STATUS;
			}
			if (errors.length == 1) {
				return errors[0];
			}
			MultiStatus overallStatus = new MultiStatus(ClasspathComputer.class, 0,
					PDECoreMessages.ClasspathComputer_failed);
			for (IStatus status : errors) {
				overallStatus.add(status);
			}
			return overallStatus;
		}

		/**
		 * Queues more projects/containers.
		 */
		void addAll(Collection<IProject> tocheck) {
			for (IProject project : tocheck) {
				workQueue.add(new UpdateRequest(project, null));
			}
			schedule();
		}

		void add(IProject project, IClasspathContainer classpathContainer) {
			if (project == null) {
				return;
			}
			workQueue.add(new UpdateRequest(project, classpathContainer));
			schedule();
		}

	}

	private static boolean isUpToDate(IProject project, IClasspathEntry[] currentEntries,
			IClasspathContainer previousClasspathContainer) {
		if (previousClasspathContainer == null) {
			if (PDECore.DEBUG_STATE) {
				System.out
						.println(String.format("%s need update because it has no state to compare", project.getName())); //$NON-NLS-1$
			}
			return false;
		}
		IClasspathEntry[] previousEntries = previousClasspathContainer.getClasspathEntries();
		if (previousEntries == null || previousEntries.length != currentEntries.length) {
			if (PDECore.DEBUG_STATE) {
				System.out.println(String.format("%s need update because entries do not match in size!", //$NON-NLS-1$
						project.getName()));
			}
			return false;
		}
		for (int i = 0; i < previousEntries.length; i++) {
			IClasspathEntry previous = previousEntries[i];
			IClasspathEntry current = currentEntries[i];
			if (!Objects.equals(current, previous)) {
				if (PDECore.DEBUG_STATE) {
					System.out.println(
							String.format("%s need update because entry at position %d is different:\n\t%s\n\t%s", //$NON-NLS-1$
									project.getName(), i, current, previous));
				}
				return false;
			}
		}
		return true;
	}

	private static void saveState(IProject project, RequiredPluginsClasspathContainer classpathContainer) {
		synchronized (project) {
			try {
				File stateFile = getStateFile(project);
				stateFile.getParentFile().mkdirs();
				try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(stateFile))) {
					PDEClasspathContainerSaveHelper.writeContainer(classpathContainer, stream);
				}
			} catch (Exception e) {
				// can't write then...
				if (PDECore.DEBUG_STATE) {
					System.err.println(String.format("Writing project state for %s failed!", project.getName())); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
	}

	static IClasspathContainer readState(IProject project) {
		synchronized (project) {
			try {
				File stateFile = getStateFile(project);
				try (InputStream stream = new FileInputStream(stateFile)) {
					IClasspathContainer container = Objects
							.requireNonNull(PDEClasspathContainerSaveHelper.readContainer(stream));
					if (PDECore.DEBUG_STATE) {
						System.out.println(String.format("%s is restored from previous state.", project.getName())); //$NON-NLS-1$
					}
					return container;
				}
			} catch (Exception e) {
				if (PDECore.DEBUG_STATE) {
					if (e instanceof FileNotFoundException) {
						System.out.println(String.format("%s has no saved state!", project.getName())); //$NON-NLS-1$
					} else {
						System.err.println(String.format("Restoring project state for %s failed!", project.getName())); //$NON-NLS-1$
						e.printStackTrace();
					}
				}
				return PDEClasspathContainerSaveHelper.emptyContainer();
			}
		}
	}

	static void setProjectContainers(IJavaProject[] javaProjects, IClasspathContainer[] container,
			IProgressMonitor monitor) throws JavaModelException {
		JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, javaProjects, container, monitor);
	}

	private static File getStateFile(IProject project) {
		return PDECore.getDefault().getStateLocation().append("cpc").append(project.getName()) //$NON-NLS-1$
				.toFile();
	}

	private static record UpdateRequest(IProject project, IClasspathContainer container) {

	}

}
