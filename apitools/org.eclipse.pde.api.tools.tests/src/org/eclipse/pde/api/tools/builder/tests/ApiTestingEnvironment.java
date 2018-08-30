/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.osgi.framework.Bundle;

/**
 * Environment used to test the {@link ApiAnalysisBuilder}. This environment
 * emulates a typical workbench environment
 *
 * @since 1.0.0
 */
public class ApiTestingEnvironment extends TestingEnvironment {

	protected static final IMarker[] NO_MARKERS = new IMarker[0];

	/**
	 * Whether to revert vs. reset the workspace
	 */
	private boolean fRevert = false;

	/**
	 * The default path to be used to revert the workspace to (if revert is
	 * enabled)
	 */
	private IPath fRevertSourcePath = null;

	/**
	 * Modified files for each build so that we can undo the changes
	 * incrementally rather than recreating the workspace for each test.
	 */
	private List<IPath> fAdded = new ArrayList<>();
	private List<IPath> fChanged = new ArrayList<>();
	private List<IPath> fRemoved = new ArrayList<>();

	public ApiTestingEnvironment() throws Exception {
		super();
		setTargetPlatform();
	}

	@Override
	public IPath addProject(String projectName, String compliance) throws UnsupportedOperationException {
		IJavaProject javaProject = createProject(projectName);
		IProject project = javaProject.getProject();
		if (compliance != null) {
			setProjectCompliance(javaProject, compliance);
		}
		return project != null ? project.getFullPath() : Path.EMPTY;
	}

	/**
	 * Sets the given compliance on the given project.
	 *
	 * @param project
	 * @param compliance
	 */
	public void setProjectCompliance(IJavaProject project, String compliance) {
		int requiredComplianceFlag = 0;
		String compilerVersion = null;
		if (JavaCore.VERSION_1_4.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_4;
			compilerVersion = JavaCore.VERSION_1_4;
		} else if (JavaCore.VERSION_1_5.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_5;
			compilerVersion = JavaCore.VERSION_1_5;
		} else if (JavaCore.VERSION_1_6.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_6;
			compilerVersion = JavaCore.VERSION_1_6;
		} else if (JavaCore.VERSION_1_7.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_7;
			compilerVersion = JavaCore.VERSION_1_7;
		} else if (JavaCore.VERSION_1_8.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_8;
			compilerVersion = JavaCore.VERSION_1_8;
		} else if (!JavaCore.VERSION_1_4.equals(compliance) && !JavaCore.VERSION_1_3.equals(compliance)) {
			throw new UnsupportedOperationException("Test framework doesn't support compliance level: " + compliance); //$NON-NLS-1$
		}
		if (requiredComplianceFlag != 0) {
			if ((AbstractCompilerTest.getPossibleComplianceLevels() & requiredComplianceFlag) == 0) {
				throw new RuntimeException("This test requires a " + compliance + " JRE"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			HashMap<String, String> options = new HashMap<>();
			options.put(JavaCore.COMPILER_COMPLIANCE, compilerVersion);
			options.put(JavaCore.COMPILER_SOURCE, compilerVersion);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, compilerVersion);
			project.setOptions(options);
		}
	}

	/**
	 * Creates a new plug-in project with the given name. If a project with the
	 * same name already exists in the testing workspace it will be deleted and
	 * new project created.
	 *
	 * @param projectName
	 * @return the newly created {@link IJavaProject} or <code>null</code> if
	 *         there is an exception creating the project
	 */
	protected IJavaProject createProject(String projectName) {
		IJavaProject jproject = null;
		try {
			IProject project = getWorkspace().getRoot().getProject(projectName);
			if (project.exists()) {
				project.delete(true, new NullProgressMonitor());
			}
			jproject = ProjectUtils.createPluginProject(projectName, new String[] {
					PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID });
			addProject(jproject.getProject());
		} catch (CoreException ce) {
			ApiPlugin.log(ce);
			ce.printStackTrace();
		}
		return jproject;
	}

	/**
	 * Performs a clean build on the project using the builder with the given
	 * builder id
	 *
	 * @param project
	 * @param builderid
	 */
	public void cleanBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.CLEAN_BUILD, builderid, null, null);
		} catch (CoreException e) {
		}
	}

	/**
	 * Incrementally builds the given project using the builder with the given
	 * builder id
	 *
	 * @param project
	 * @param builderid
	 */
	public void incrementalBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.INCREMENTAL_BUILD, builderid, null, null);
		} catch (CoreException e) {
		}
	}

	/**
	 * Performs a full build on the given project using the builder with the
	 * given builder id
	 *
	 * @param project
	 * @param builderid
	 */
	public void fullBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.FULL_BUILD, builderid, null, null);
		} catch (CoreException e) {
		}
	}

	/**
	 * returns all of the usage markers for the specified resource and its
	 * children
	 *
	 * @param resource
	 * @return all API usage problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#API_USAGE_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllUsageMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * returns all of the usage markers for the specified resource and its
	 * children
	 *
	 * @param resource
	 * @return all JDT problem markers that are on the resource backing the
	 *         given path
	 * @throws CoreException
	 */
	public IMarker[] getAllJDTMarkers(IPath path) throws CoreException {
		return getAllJDTMarkers(getResource(path));
	}

	/**
	 * returns all of the usage markers for the specified resource and its
	 * children
	 *
	 * @param resource
	 * @return all JDT problem markers on the given {@link IResource}
	 * @throws CoreException
	 */
	protected IMarker[] getAllJDTMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		IMarker[] javaModelMarkers = resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		IMarker[] buildpathMarkers = resource.findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		int javaModelMarkersLength = javaModelMarkers.length;
		int buildpathMarkersLength = buildpathMarkers.length;
		if (javaModelMarkersLength == 0) {
			return buildpathMarkers;
		} else if (buildpathMarkersLength == 0) {
			return javaModelMarkers;
		}
		IMarker[] allMarkers = new IMarker[javaModelMarkersLength + buildpathMarkersLength];
		System.arraycopy(javaModelMarkers, 0, allMarkers, 0, javaModelMarkersLength);
		System.arraycopy(buildpathMarkers, 0, allMarkers, javaModelMarkersLength, buildpathMarkersLength);
		return allMarkers;
	}

	/**
	 * Returns all of the unsupported Javadoc tag markers on the specified
	 * resource and all of its children.
	 *
	 * @param resource
	 * @return all unsupported tag problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#UNSUPPORTED_TAG_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllUnsupportedTagMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.UNSUPPORTED_TAG_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the unsupported annotation markers on the given resource
	 * and all of its children
	 *
	 * @param resource
	 * @return all unsupported annotation markers
	 * @throws CoreException
	 * @since 1.0.400
	 */
	protected IMarker[] getAllUnsupportedAnnotationMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.UNSUPPORTED_ANNOTATION_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the compatibility markers on the given resource and its
	 * children
	 *
	 * @param resource
	 * @return all compatibility problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#COMPATIBILITY_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllCompatibilityMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the API profile markers on the given resource and its
	 * children
	 *
	 * @param resource
	 * @return all API baseline problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#DEFAULT_API_BASELINE_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllApiBaselineMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the since tag markers on the given resource and its
	 * children
	 *
	 * @param resource
	 * @return all since tag problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#SINCE_TAGS_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllSinceTagMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the version markers on the given resource and its children
	 *
	 * @param resource
	 * @return all version problem markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#VERSION_NUMBERING_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllVersionMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the unused API problem filters markers on the given
	 * resource to infinite depth
	 *
	 * @param resource
	 * @return all unused problem filter markers
	 * @throws CoreException
	 *
	 * @see {@link IApiMarkerConstants#UNUSED_FILTER_PROBLEM_MARKER}
	 */
	protected IMarker[] getAllUnusedApiProblemFilterMarkers(IResource resource) throws CoreException {
		if (resource == null) {
			return NO_MARKERS;
		}
		if (!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}

	/**
	 * Returns all of the markers from the testing workspace
	 *
	 * @return all {@link IMarker}s currently set in the workspace
	 */
	public IMarker[] getMarkers() {
		return getMarkersFor(getWorkspaceRootPath());
	}

	/**
	 * Returns the collection of API problem markers for the given element
	 *
	 * @param root
	 * @return the array of {@link IMarker}s found on the resource that
	 *         corresponds to the given path
	 */
	public IMarker[] getMarkersFor(IPath root) {
		return getMarkersFor(root, null);
	}

	/**
	 * Return all problems with the specified element.
	 *
	 * @param path
	 * @param additionalMarkerType
	 * @return the array of {@link IMarker}s found on the resource that
	 *         corresponds to the given path
	 */
	public IMarker[] getMarkersFor(IPath path, String additionalMarkerType) {
		IResource resource = getResource(path);
		try {
			List<Object> problems = new ArrayList<>();
			addToList(problems, getAllUsageMarkers(resource));
			addToList(problems, getAllCompatibilityMarkers(resource));
			addToList(problems, getAllApiBaselineMarkers(resource));
			addToList(problems, getAllSinceTagMarkers(resource));
			addToList(problems, getAllVersionMarkers(resource));
			addToList(problems, getAllUnsupportedTagMarkers(resource));
			addToList(problems, getAllUnsupportedAnnotationMarkers(resource));
			addToList(problems, getAllUnusedApiProblemFilterMarkers(resource));

			// additional markers
			if (additionalMarkerType != null) {
				problems.addAll(Arrays.asList(resource.findMarkers(additionalMarkerType, true, IResource.DEPTH_INFINITE)));
			}
			return problems.toArray(new IMarker[problems.size()]);
		} catch (CoreException e) {
			// ignore
		}
		return new IMarker[0];
	}

	/**
	 * Looks up the {@link IResource} in the workspace from the given path
	 *
	 * @param path
	 * @return the {@link IResource} handle for the given path
	 */
	public IResource getResource(IPath path) {
		IResource resource;
		if (path.equals(getWorkspaceRootPath())) {
			resource = getWorkspace().getRoot();
		} else {
			IProject p = getProject(path);
			if (p != null && path.equals(p.getFullPath())) {
				resource = getProject(path.lastSegment());
			} else if (path.getFileExtension() == null) {
				resource = getWorkspace().getRoot().getFolder(path);
			} else {
				resource = getWorkspace().getRoot().getFile(path);
			}
		}
		return resource;
	}

	/**
	 * Adds the array of objects to the given list
	 *
	 * @param list
	 * @param objects
	 */
	private void addToList(List<Object> list, Object[] objects) {
		if (list == null || objects == null) {
			return;
		}
		if (objects.length == 0) {
			return;
		}
		for (Object object : objects) {
			list.add(object);
		}
	}

	@Override
	public ApiProblem[] getProblems() {
		return (ApiProblem[]) super.getProblems();
	}

	/**
	 * Returns the current workspace {@link IApiProfile}
	 *
	 * @return the workspace baseline
	 */
	protected IApiBaseline getWorkspaceProfile() {
		return ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}

	@Override
	public ApiProblem[] getProblemsFor(IPath path, String additionalMarkerType) {
		IMarker[] markers = getMarkersFor(path, additionalMarkerType);
		ArrayList<ApiProblem> problems = new ArrayList<>();
		for (IMarker marker : markers) {
			problems.add(new ApiProblem(marker));
		}
		return problems.toArray(new ApiProblem[problems.size()]);
	}

	@Override
	public void removeProject(IPath projectPath) {
		IJavaProject project = getJavaProject(projectPath);
		if (project != null) {
			try {
				project.getProject().delete(true, new NullProgressMonitor());
			} catch (CoreException ce) {

			}
		}
	}

	@Override
	public void resetWorkspace() {
		try {
			if (fRevert) {
				try {
					revertWorkspace();
				} catch (Exception e) {
					// in case we have an exception reverting a file, just toast
					// it all
					deleteWorkspace();
				}
			} else {
				deleteWorkspace();
			}
		} catch (Exception e) {
			// dump the trace
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=275005
			e.printStackTrace();
		} finally {
			// clear all changes
			fAdded.clear();
			fChanged.clear();
			fRemoved.clear();
		}
	}

	/**
	 * Completely deletes the workspace
	 *
	 * @since 1.1
	 */
	void deleteWorkspace() {
		super.resetWorkspace();
		// clean up any left over projects from other tests
		IProject[] projects = getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			try {
				project.delete(true, new NullProgressMonitor());
			} catch (CoreException ce) {
				// help with debugging
				ce.printStackTrace();
			}
		}
	}

	/**
	 * Sets the default revert source path to the given path.
	 *
	 * @param path
	 */
	public void setRevertSourcePath(IPath path) {
		fRevertSourcePath = path;
	}

	/**
	 * @return the currently set source path to use when reverting changes to
	 *         the workspace.
	 */
	public IPath getRevertSourcePath() {
		return fRevertSourcePath;
	}

	/**
	 * Reverts changes in the workspace - added, removed, changed files
	 *
	 * @throws Exception if something happens trying to revert the workspace
	 *             contents
	 */
	public void revertWorkspace() throws Exception {
		// remove each added file
		Iterator<IPath> iterator = fAdded.iterator();
		while (iterator.hasNext()) {
			IPath path = iterator.next();
			deleteWorkspaceFile(path);
		}

		// revert each changed file
		iterator = fChanged.iterator();
		IPath revert = getRevertSourcePath();
		if (revert != null) {
			IPath path = null;
			while (iterator.hasNext()) {
				path = iterator.next();
				updateWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(ApiBuilderTest.TEST_SOURCE_ROOT).append(getRevertSourcePath()).append(path));
			}

			// replace each deleted file
			iterator = fRemoved.iterator();
			while (iterator.hasNext()) {
				path = iterator.next();
				createWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(ApiBuilderTest.TEST_SOURCE_ROOT).append(getRevertSourcePath()).append(path));
			}
		}
	}

	/**
	 * Deletes the workspace file at the specified location (full path).
	 *
	 * @param workspaceLocation
	 * @throws Exception
	 */
	private void deleteWorkspaceFile(IPath workspaceLocation) throws Exception {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		try {
			file.delete(true, null);
		} catch (CoreException e) {
			// try to bring the resource in to sync an re-delete
			file.refreshLocal(IResource.DEPTH_ONE, null);
			file.delete(true, null);
		}
	}

	/**
	 * Updates the contents of a workspace file at the specified location (full
	 * path), with the contents of a local file at the given replacement
	 * location (absolute path).
	 *
	 * @param workspaceLocation
	 * @param replacementLocation
	 * @throws Exception
	 */
	private void updateWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		File replacement = replacementLocation.toFile();
		try (FileInputStream stream = new FileInputStream(replacement);) {
			file.setContents(stream, false, true, null);
		}
	}

	/**
	 * Updates the contents of a workspace file at the specified location (full
	 * path), with the contents of a local file at the given replacement
	 * location (absolute path).
	 *
	 * @param workspaceLocation
	 * @param replacementLocation
	 * @throws Exception
	 */
	private void createWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		File replacement = replacementLocation.toFile();
		try (FileInputStream stream = new FileInputStream(replacement)) {
			file.create(stream, false, null);
		}
	}

	/**
	 * Notes a file was added during the test, to be undone
	 *
	 * @param path
	 */
	public void added(IPath path) {
		fAdded.add(path);
	}

	public void changed(IPath path) {
		fChanged.add(path);
	}

	public void removed(IPath path) {
		fRemoved.add(path);
	}

	@Override
	public IPath addFile(IPath root, String fileName, String contents) {
		IPath path = root.append(fileName);
		IFile file = getWorkspace().getRoot().getFile(path);
		if (file.exists()) {
			changed(path);
		} else {
			added(path);
		}
		return super.addFile(root, fileName, contents);
	}

	/**
	 * Returns the listing of projects in the order the workspace has computed
	 * they should be built. This method calls out to
	 * {@link org.eclipse.core.resources.IWorkspace#computeProjectOrder(IProject[])}
	 * , which can slow down testing with successive calls.
	 *
	 * @return a build-ordered listing of the workspace projects
	 */
	public IProject[] getProjectBuildOrder() {
		return getWorkspace().computeProjectOrder(getWorkspace().getRoot().getProjects()).projects;
	}

	@Override
	public IPath addClass(IPath packagePath, String className, String contents) {
		IPath filePath = packagePath.append(className + ".java"); //$NON-NLS-1$
		if (getWorkspace().getRoot().getFile(filePath).exists()) {
			changed(filePath);
		} else {
			added(filePath);
		}
		return super.addClass(packagePath, className, contents);
	}

	/**
	 * Sets whether to revert the workspace rather than reset.
	 *
	 * @param revert
	 */
	public void setRevert(boolean revert) {
		fRevert = revert;
	}

	public static void setTargetPlatform() throws CoreException, InterruptedException, IOException {
		ITargetPlatformService tpService = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		if (tpService.getWorkspaceTargetDefinition() != null
				&& tpService.getWorkspaceTargetDefinition().getBundles() == null) {
			Job job = new LoadTargetDefinitionJob(tpService.getWorkspaceTargetDefinition());
			job.schedule();
			job.join();
		}
		boolean coreRuntimeFound = false;
		for (TargetBundle bundle : tpService.getWorkspaceTargetDefinition().getBundles()) {
			if ("org.eclipse.core.runtime".equals(bundle.getBundleInfo().getSymbolicName())) { //$NON-NLS-1$
				coreRuntimeFound = true;
			}
		}
		if (!coreRuntimeFound) {
			ITargetDefinition targetDef = tpService.newTarget();
			targetDef.setName("Current bundles target platform"); //$NON-NLS-1$
			Bundle[] bundles = Platform.getBundle("org.eclipse.core.runtime").getBundleContext().getBundles(); //$NON-NLS-1$
			List<ITargetLocation> bundleContainers = new ArrayList<>();
			Set<File> locations = new HashSet<>();
			for (Bundle bundle : bundles) {
				File loc = FileLocator.getBundleFile(bundle);
				File parentFile = loc.getParentFile();
				boolean hasMultiplePluginFolders = Arrays.stream(parentFile.listFiles()).filter(File::isDirectory)
						.filter(file -> new File(file, "META-INF/MANIFEST.MF").isFile()).count() > 1; //$NON-NLS-1$
				if (!hasMultiplePluginFolders && !locations.contains(parentFile)) {
					bundleContainers.add(new DirectoryBundleContainer(loc.getParent()));
					locations.add(parentFile);
				}
			}
			targetDef.setTargetLocations(bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
			targetDef.setArch(Platform.getOSArch());
			targetDef.setOS(Platform.getOS());
			targetDef.setWS(Platform.getWS());
			targetDef.setNL(Platform.getNL());
			// targetDef.setJREContainer()
			tpService.saveTargetDefinition(targetDef);

			Job job = new LoadTargetDefinitionJob(targetDef);
			job.schedule();
			job.join();
		}
	}
}
