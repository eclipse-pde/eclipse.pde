/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 490062
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

@SuppressWarnings("restriction")
public class DSAnnotationCompilationParticipant extends CompilationParticipant {

	private static final String DS_MANIFEST_KEY = "Service-Component"; //$NON-NLS-1$

	private static final String AP_MANIFEST_KEY = "Bundle-ActivationPolicy"; //$NON-NLS-1$

	static final String COMPONENT_ANNOTATION = "org.osgi.service.component.annotations.Component"; //$NON-NLS-1$

	static final String ANNOTATIONS_PACKAGE = COMPONENT_ANNOTATION.substring(0, COMPONENT_ANNOTATION.lastIndexOf('.'));

	private static final IPath COMPONENT_ANNOTATION_PATH = new Path(COMPONENT_ANNOTATION.replace('.',  '/'));

	private static final Pattern ACCESS_RULE_PATTERN = Pattern.compile("(\\*\\*)|\\*|\\?"); //$NON-NLS-1$

	private static final QualifiedName PROP_STATE = new QualifiedName(Activator.PLUGIN_ID, "state"); //$NON-NLS-1$

	private static final String STATE_FILENAME = "state.dat"; //$NON-NLS-1$

	static final String BUILDPATH_PROBLEM_MARKER = "org.eclipse.pde.ds.annotations.buildpath_problem"; //$NON-NLS-1$

	private static final Debug debug = Debug.getDebug("ds-annotation-builder"); //$NON-NLS-1$

	private final Map<IJavaProject, ProjectContext> processingContext = Collections.synchronizedMap(new HashMap<IJavaProject, ProjectContext>());

	@Override
	public boolean isAnnotationProcessor() {
		return true;
	}

	@Override
	public boolean isActive(IJavaProject project) {
		IPreferencesService prefs = Platform.getPreferencesService();
		boolean enabled = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_ENABLED, false, new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		if (!enabled)
			return false;

		IProject iproject = project.getProject();
		if (!iproject.isOpen() || !PDE.hasPluginNature(iproject))
			return false;

		if (WorkspaceModelManager.isBinaryProject(project.getProject()))
			return false;

		boolean autoClasspath = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_CLASSPATH, true, new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		if (autoClasspath)
			return true;

		try {
			IType annotationType = project.findType(COMPONENT_ANNOTATION);
			return annotationType != null && annotationType.isAnnotation();
		} catch (JavaModelException e) {
			Activator.log(e);
		}

		return false;
	}

	@Override
	public int aboutToBuild(IJavaProject project) {
		if (debug.isDebugging())
			debug.trace(String.format("About to build project: %s", project.getElementName())); //$NON-NLS-1$

		int result = READY_FOR_BUILD;

		int[] retval = new int[1];
		ProjectState state = getState(project, retval);
		result = retval[0];

		processingContext.put(project, new ProjectContext(state));

		if (state.getFormatVersion() != ProjectState.FORMAT_VERSION) {
			state.setFormatVersion(ProjectState.FORMAT_VERSION);
			result = NEEDS_FULL_BUILD;
		}

		IPreferencesService prefs = Platform.getPreferencesService();
		String path = prefs.getString(Activator.PLUGIN_ID, Activator.PREF_PATH, Activator.DEFAULT_PATH, new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		if (!path.equals(state.getPath())) {
			state.setPath(path);
			result = NEEDS_FULL_BUILD;
		}

		String specVersionStr = prefs.getString(Activator.PLUGIN_ID, Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name(),  new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		DSAnnotationVersion specVersion = getEnumValue(specVersionStr, DSAnnotationVersion.class, DSAnnotationVersion.V1_3);

		if (specVersion != state.getSpecVersion()) {
			state.setSpecVersion(specVersion);
			result = NEEDS_FULL_BUILD;
		}

		String errorLevelStr = prefs.getString(Activator.PLUGIN_ID, Activator.PREF_VALIDATION_ERROR_LEVEL, ValidationErrorLevel.error.name(), new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		ValidationErrorLevel errorLevel = getEnumValue(errorLevelStr, ValidationErrorLevel.class, ValidationErrorLevel.error);

		if (errorLevel != state.getErrorLevel()) {
			state.setErrorLevel(errorLevel);
			result = NEEDS_FULL_BUILD;
		}

		String missingUnbindMethodLevelStr = prefs.getString(Activator.PLUGIN_ID, Activator.PREF_MISSING_UNBIND_METHOD_ERROR_LEVEL, errorLevelStr, new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		ValidationErrorLevel missingUnbindMethodLevel = getEnumValue(missingUnbindMethodLevelStr, ValidationErrorLevel.class, errorLevel);

		if (missingUnbindMethodLevel != state.getMissingUnbindMethodLevel()) {
			state.setMissingUnbindMethodLevel(missingUnbindMethodLevel);
			result = NEEDS_FULL_BUILD;
		}

		Activator.getDefault().listenForClasspathPreferenceChanges(project);

		return result;
	}

	private <E extends Enum<E>> E getEnumValue(String property, Class<E> enumType, E defaultValue) {
		try {
			return Enum.valueOf(enumType, property);
		} catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	public static ProjectState getState(IJavaProject project) {
		return getState(project, null);
	}

	private static ProjectState getState(IJavaProject project, int[] result) {
		ProjectState state = null;
		try {
			Object value = project.getProject().getSessionProperty(PROP_STATE);
			if (value instanceof SoftReference<?>) {
				@SuppressWarnings("unchecked")
				SoftReference<ProjectState> ref = (SoftReference<ProjectState>) value;
				state = ref.get();
			}
		} catch (CoreException e) {
			Activator.log(e);
		}

		if (state == null) {
			try {
				state = loadState(project.getProject());
			} catch (IOException e) {
				Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error loading project state.", e)); //$NON-NLS-1$
			}

			if (state == null) {
				state = new ProjectState();
				if (result != null && result.length > 0)
					result[0] = NEEDS_FULL_BUILD;
			}

			try {
				project.getProject().setSessionProperty(PROP_STATE, new SoftReference<>(state));
			} catch (CoreException e) {
				Activator.log(e);
			}
		}

		return state;
	}

	private static ProjectState loadState(IProject project) throws IOException {
		File stateFile = getStateFile(project);
		if (!stateFile.canRead()) {
			if (debug.isDebugging())
				debug.trace(String.format("Missing or invalid project state file: %s", stateFile)); //$NON-NLS-1$

			return null;
		}

		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(stateFile))) {
			ProjectState state = (ProjectState) in.readObject();

			if (debug.isDebugging()) {
				debug.trace(String.format("Loaded state for project: %s", project.getName())); //$NON-NLS-1$
				for (String cuKey : state.getCompilationUnits())
					debug.trace(String.format("%s -> %s", cuKey, state.getModelFiles(cuKey))); //$NON-NLS-1$
			}

			return state;
		} catch (ClassNotFoundException e) {
			IOException ex = new IOException("Unable to deserialize project state."); //$NON-NLS-1$
			ex.initCause(e);
			throw ex;
		}
	}

	@Override
	public void buildFinished(IJavaProject project) {
		ProjectContext projectContext = processingContext.remove(project);
		if (projectContext != null) {
			ProjectState state = projectContext.getState();
			// check if unprocessed CUs still exist; if not, their mapped files are now abandoned
			HashSet<String> abandoned = new HashSet<>(projectContext.getAbandoned());
			for (String cuKey : projectContext.getUnprocessed()) {
				boolean exists = false;
				try {
					IJavaElement cu = project.findElement(new Path(cuKey));
					IResource file;
					if (cu != null && cu.getElementType() == IJavaElement.COMPILATION_UNIT
							&& (file = cu.getResource()) != null && file.exists()
							&& file.getProject().equals(project.getProject()))
						exists = true;
				} catch (JavaModelException e) {
					Activator.log(e);
				}

				if (!exists) {
					if (debug.isDebugging())
						debug.trace(String.format("Mapped CU %s no longer exists.", cuKey)); //$NON-NLS-1$

					Collection<String> dsKeys = state.removeMappings(cuKey);
					if (dsKeys != null)
						abandoned.addAll(dsKeys);
				}
			}

			// retain abandoned files that are still mapped elsewhere
			HashSet<String> retained = new HashSet<>();
			for (String cuKey : state.getCompilationUnits()) {
				Collection<String> dsKeys = state.getModelFiles(cuKey);
				if (dsKeys != null)
					retained.addAll(dsKeys);
			}

			try {
				IMarker[] cpMarkers = project.getProject().findMarkers(BUILDPATH_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);

				if (retained.isEmpty()) {
					for (IMarker marker : cpMarkers) {
						marker.delete();
					}
				} else {
					abandoned.removeAll(retained);

					// check if we need a permanent annotations classpath entry
					boolean markerNeeded = false;
					IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
					for (int i = roots.length - 1; i >= 0; --i) {
						IPackageFragmentRoot root = roots[i];
						IPackageFragment fragment = root.getPackageFragment(ANNOTATIONS_PACKAGE);
						if (!fragment.exists()) {
							continue;
						}

						IClasspathEntry entry = root.getResolvedClasspathEntry();
						boolean packageAccessible = true;
						IAccessRule[] accessRules = entry.getAccessRules();
						for (IAccessRule accessRule : accessRules) {
							if (!matches(COMPONENT_ANNOTATION_PATH, accessRule.getPattern())) {
								continue;
							}

							if (accessRule.getKind() == IAccessRule.K_NON_ACCESSIBLE) {
								packageAccessible = false;
							}

							break;
						}

						if (!packageAccessible) {
							continue;
						}

						IClasspathAttribute[] attrs = entry.getExtraAttributes();
						for (IClasspathAttribute attr : attrs) {
							if (Activator.CP_ATTRIBUTE.equals(attr.getName()) && Boolean.parseBoolean(attr.getValue())) {
								markerNeeded = true;
								break;
							}
						}

						break;
					}

					if (markerNeeded) {
						if (cpMarkers.length == 0) {
							IMarker marker = project.getProject().createMarker(BUILDPATH_PROBLEM_MARKER);
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
							marker.setAttribute(IMarker.MESSAGE, Messages.DSAnnotationCompilationParticipant_buildpathProblemMarker_message);
							marker.setAttribute(IMarker.LOCATION, Messages.DSAnnotationCompilationParticipant_buildpathProblemMarker_location);
						}
					} else {
						for (IMarker marker : cpMarkers) {
							marker.delete();
						}
					}
				}
			} catch (CoreException e) {
				Activator.log(e);
			}

			if (projectContext.isChanged()) {
				try {
					saveState(project.getProject(), state);
				} catch (IOException e) {
					Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error saving file mappings.", e)); //$NON-NLS-1$
				}
			}

			// delete all abandoned files
			ArrayList<IStatus> deleteStatuses = new ArrayList<>(2);
			for (String dsKey : abandoned) {
				IPath path = Path.fromPortableString(dsKey);

				if (debug.isDebugging()) {
					debug.trace(String.format("Deleting %s", path)); //$NON-NLS-1$
				}

				IFile file = PDEProject.getBundleRelativeFile(project.getProject(), path);
				if (file.exists()) {
					try {
						file.delete(true, null);
					} catch (CoreException e) {
						deleteStatuses.add(e.getStatus());
					}
				}
			}

			if (!deleteStatuses.isEmpty()) {
				Activator.log(new MultiStatus(Activator.PLUGIN_ID, 0, deleteStatuses.toArray(new IStatus[deleteStatuses.size()]), "Error deleting generated files.", null)); //$NON-NLS-1$
			}

			if (!retained.isEmpty() || !abandoned.isEmpty()) {
				updateProject(project.getProject(), retained, abandoned);
			}
		}

		if (debug.isDebugging()) {
			debug.trace(String.format("Build finished for project: %s", project.getElementName())); //$NON-NLS-1$
		}
	}

	private boolean matches(IPath path, IPath pattern) {
		if (pattern.hasTrailingSeparator()) {
			pattern = pattern.append("**"); //$NON-NLS-1$
		}

		StringBuffer buf = new StringBuffer();
		// TODO escape additional regex chars?
		Matcher m = ACCESS_RULE_PATTERN.matcher(pattern.toString().replace(".", "\\.")); //$NON-NLS-1$  //$NON-NLS-2$
		while (m.find()) {
			if ("**".equals(m.group())) { //$NON-NLS-1$
				m.appendReplacement(buf, ".*"); //$NON-NLS-1$
			} else if ("?".equals(m.group())) { //$NON-NLS-1$
				m.appendReplacement(buf, "[^/]"); //$NON-NLS-1$
			} else {
				m.appendReplacement(buf, "[^/]*"); //$NON-NLS-1$
			}
		}

		return Pattern.matches(m.appendTail(buf).toString(), path.toString());
	}

	private void saveState(IProject project, ProjectState state) throws IOException {
		File stateFile = getStateFile(project);

		if (debug.isDebugging()) {
			debug.trace(String.format("Saving state for project: %s", project.getName())); //$NON-NLS-1$
			for (String cuKey : state.getCompilationUnits()) {
				debug.trace(String.format("%s -> %s", cuKey, state.getModelFiles(cuKey))); //$NON-NLS-1$
			}
		}

		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(stateFile))) {
			out.writeObject(state);
		}
	}

	private void updateProject(IProject project, final Collection<String> retained, final Collection<String> abandoned) {
		PDEModelUtility.modifyModel(new ModelModification(project) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					updateManifest((IBundlePluginModelBase) model, retained, abandoned, project);
				}
			}
		}, null);

		// note: we can't combine both manifest and build.properties into a single edit
		PDEModelUtility.modifyModel(new ModelModification(PDEProject.getBuildProperties(project)) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBuildModel) {
					updateBuildProperties((IBuildModel) model, retained, abandoned);
				}
			}
		}, null);
	}

	private void updateManifest(IBundlePluginModelBase model, Collection<String> retained, Collection<String> abandoned, IProject project) {
		IBundleModel bundleModel = model.getBundleModel();
		LinkedHashSet<IPath> entries = new LinkedHashSet<>();
		collectManifestEntries(bundleModel, entries);

		boolean changed = false;
		for (String dsKey : abandoned) {
			IPath path = Path.fromPortableString(dsKey);
			changed |= entries.remove(path);
		}

		for (String dsKey : retained) {
			IPath path = Path.fromPortableString(dsKey);
			if (!isManifestEntryIncluded(entries, path)) {
				changed |= entries.add(path);
			}
		}

		if (!changed) {
			return;
		}

		StringBuilder buf = new StringBuilder();
		for (IPath entry : entries) {
			if (buf.length() > 0) {
				buf.append(",\n "); //$NON-NLS-1$
			}

			buf.append(entry.toString());
		}

		String value = buf.toString();

		if (debug.isDebugging()) {
			debug.trace(String.format("Setting manifest header in %s to %s: %s", model.getUnderlyingResource().getFullPath(), DS_MANIFEST_KEY, value)); //$NON-NLS-1$
		}

		// note: contrary to javadoc, setting header value to null does *not* remove it; setting it to empty string does
		bundleModel.getBundle().setHeader(DS_MANIFEST_KEY, value);

		boolean generateBAPL = Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID,
				Activator.PREF_GENERATE_BAPL, true,
				new IScopeContext[] { new ProjectScope(project.getProject()), InstanceScope.INSTANCE });
		if (generateBAPL) {
			if (debug.isDebugging()) {
				debug.trace(String.format("Setting manifest header in %s to %s: %s", //$NON-NLS-1$
						model.getUnderlyingResource().getFullPath(), AP_MANIFEST_KEY, "lazy")); //$NON-NLS-1$
			}

			bundleModel.getBundle().setHeader(AP_MANIFEST_KEY, "lazy"); //$NON-NLS-1$
		}
	}

	private void collectManifestEntries(IBundleModel bundleModel, Collection<IPath> entries) {
		String header = bundleModel.getBundle().getHeader(DS_MANIFEST_KEY);
		if (header == null) {
			return;
		}

		String[] elements = header.split("\\s*,\\s*"); //$NON-NLS-1$
		for (String element : elements) {
			if (element.length() != 0) {
				entries.add(new Path(element));
			}
		}
	}

	private boolean isManifestEntryIncluded(Collection<IPath> entries, IPath path) {
		for (IPath entry : entries) {
			if (entry.equals(path)) {
				return true;
			}

			if (entry.removeLastSegments(1).equals(path.removeLastSegments(1))) {
				// check if wildcard match (last path segment)
				Filter filter;
				try {
					filter = FrameworkUtil.createFilter("(filename=" + sanitizeFilterValue(entry.lastSegment()) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (InvalidSyntaxException e) {
					continue;
				}

				if (filter.matches(Collections.singletonMap("filename", path.lastSegment()))) { //$NON-NLS-1$
					return true;
				}
			}
		}

		return false;
	}

	private String sanitizeFilterValue(String value) {
		return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	private void updateBuildProperties(IBuildModel model, Collection<String> retained, Collection<String> abandoned) throws CoreException {
		IBuildEntry includes = model.getBuild().getEntry(IBuildEntry.BIN_INCLUDES);

		if (includes != null) {
			for (String dsKey : abandoned) {
				String path = Path.fromPortableString(dsKey).toString();
				if (includes.contains(path)) {
					includes.removeToken(path);
				}
			}
		}

		if (!retained.isEmpty()) {
			if (includes == null) {
				IBuildModelFactory factory = model.getFactory();
				includes = factory.createEntry(IBuildEntry.BIN_INCLUDES);
				model.getBuild().add(includes);
			}

			LinkedHashSet<IPath> entries = new LinkedHashSet<>();
			collectBuildEntries(includes, entries);

			for (String dsKey : retained) {
				IPath path = Path.fromPortableString(dsKey);
				if (!isBuildEntryIncluded(entries, path)) {
					includes.addToken(path.toString());
				}
			}
		}
	}

	private void collectBuildEntries(IBuildEntry includes, Collection<IPath> entries) {
		if (includes == null) {
			return;
		}

		for (String include : includes.getTokens()) {
			if ((include = include.trim()).length() != 0) {
				entries.add(new Path(include));
			}
		}
	}

	private boolean isBuildEntryIncluded(Collection<IPath> entries, IPath path) {
		for (IPath entry : entries) {
			if (entry.equals(path)) {
				return true;
			}

			if (entry.hasTrailingSeparator() && entry.isPrefixOf(path)) {
				return true;
			}

			// TODO support full Ant path patterns
		}

		return false;
	}

	@Override
	public void processAnnotations(BuildContext[] files) {
		// we need to process CUs in context of a project; separate them by project
		HashMap<IJavaProject, Map<ICompilationUnit, BuildContext>> filesByProject = new HashMap<>();
		for (BuildContext file : files) {
			if (debug.isDebugging()) {
				debug.trace(String.format("Creating compilation unit from file %s.", file.getFile().getFullPath())); //$NON-NLS-1$
			}

			ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file.getFile());
			if (cu == null) {
				if (debug.isDebugging()) {
					// TODO should we log instead? Don't want to spam the error log though
					debug.trace(String.format("Unable to create compilation unit from file %s.", file.getFile().getFullPath())); //$NON-NLS-1$
				}

				continue;
			}

			if (canSkipFile(cu)) {
				markAsAbandoned(cu);
				continue;
			}

			Map<ICompilationUnit, BuildContext> map = filesByProject.get(cu.getJavaProject());
			if (map == null) {
				map = new HashMap<>();
				filesByProject.put(cu.getJavaProject(), map);
			}

			map.put(cu, file);
		}

		// process all CUs in each project
		for (Map.Entry<IJavaProject, Map<ICompilationUnit, BuildContext>> entry : filesByProject.entrySet()) {
			if (debug.isDebugging()) {
				debug.trace(String.format("Processing compilation units in project %s.", entry.getKey().getElementName())); //$NON-NLS-1$
			}

			processAnnotations(entry.getKey(), entry.getValue());
		}
	}

	public boolean canSkipFile(ICompilationUnit cu) {
		IType primaryType = cu.findPrimaryType();
		if (primaryType == null) {
			return false;
		}

		try {
			return !containsComponent(primaryType);
		} catch (JavaModelException e) {
			return false;
		}
	}

	private boolean containsComponent(IType type) throws JavaModelException {

		IAnnotation annotationWithImport = type.getAnnotation("Component"); //$NON-NLS-1$
		IAnnotation fullyQualifiedAnnotation = type.getAnnotation(COMPONENT_ANNOTATION);

		boolean hasComponentAnnotation = annotationWithImport.exists() || fullyQualifiedAnnotation.exists();
		if (hasComponentAnnotation) {
			return true;
		}

		for (IJavaElement child : type.getChildren()) {
			if ((child instanceof IType) && containsComponent((IType) child)) {
				return true;
			}
		}

		return false;
	}

	public void markAsAbandoned(ICompilationUnit cu) {
		ProjectContext projectContext = processingContext.get(cu.getJavaProject());

		String cuKey = AnnotationProcessor.getCompilationUnitKey(cu);
		projectContext.getUnprocessed().remove(cuKey);

		ProjectState state = projectContext.getState();

		Collection<String> oldDSKeys = state.updateMappings(cuKey, new HashMap<>());
		if (oldDSKeys != null) {
			projectContext.getAbandoned().addAll(oldDSKeys);
		}
	}

	private void processAnnotations(IJavaProject javaProject, Map<ICompilationUnit, BuildContext> fileMap) {
		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setProject(javaProject);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		ProjectContext projectContext = processingContext.get(javaProject);
		ProjectState state = projectContext.getState();

		parser.setIgnoreMethodBodies(state.getErrorLevel() == ValidationErrorLevel.ignore);

		ICompilationUnit[] cuArr = fileMap.keySet().toArray(new ICompilationUnit[fileMap.size()]);
		parser.createASTs(cuArr, new String[0], new AnnotationProcessor(projectContext, fileMap), null);
	}

	public static boolean isManaged(IProject project) {
		try {
			if (project.getSessionProperty(PROP_STATE) != null) {
				return true;
			}

			File stateFile = getStateFile(project);
			return stateFile.canRead();
		} catch (CoreException e) {
			return false;
		}
	}

	private static File getStateFile(IProject project) {
		File workDir = project.getWorkingLocation(Activator.PLUGIN_ID).toFile();
		File stateFile = new File(workDir, STATE_FILENAME);
		return stateFile;
	}
}
