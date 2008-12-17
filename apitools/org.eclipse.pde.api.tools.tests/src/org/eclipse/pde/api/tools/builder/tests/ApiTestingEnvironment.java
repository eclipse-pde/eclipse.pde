/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.compatibility.CompatibilityTest;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.internal.core.natures.PDE;

/**
 * Environment used to test the {@link ApiAnalysisBuilder}.
 * This environment emulates a typical workbench environment
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
	 * The default path to be used to revert the workspace to (if revert is enabled)
	 */
	private IPath fRevertSourcePath = null;
	
	/**
	 * Modified files for each build so that we can undo the changes incrementally
	 * rather than recreating the workspace for each test.
	 */
	private List<IPath> fAdded = new ArrayList<IPath>();
	private List<IPath> fChanged = new ArrayList<IPath>();
	private List<IPath> fRemoved = new ArrayList<IPath>();
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#addProject(java.lang.String, java.lang.String)
	 */
	public IPath addProject(String projectName, String compliance) throws UnsupportedOperationException {
		IJavaProject javaProject = createProject(projectName);
		IProject project  = javaProject.getProject();
		int requiredComplianceFlag = 0;
		String compilerVersion = null;
		if (CompilerOptions.VERSION_1_5.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_5;
			compilerVersion = CompilerOptions.VERSION_1_5;
		}
		else if (CompilerOptions.VERSION_1_6.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_6;
			compilerVersion = CompilerOptions.VERSION_1_6;
		}
		else if (CompilerOptions.VERSION_1_7.equals(compliance)) {
			requiredComplianceFlag = AbstractCompilerTest.F_1_7;
			compilerVersion = CompilerOptions.VERSION_1_7;
		}
		else if (!CompilerOptions.VERSION_1_4.equals(compliance) && !CompilerOptions.VERSION_1_3.equals(compliance)) {
			throw new UnsupportedOperationException("Test framework doesn't support compliance level: " + compliance);
		}
		if (requiredComplianceFlag != 0) {
			if ((AbstractCompilerTest.getPossibleComplianceLevels() & requiredComplianceFlag) == 0) {
				throw new RuntimeException("This test requires a " + compliance + " JRE");
			}
			HashMap<String, String> options = new HashMap<String, String>();
			options.put(CompilerOptions.OPTION_Compliance, compilerVersion);
			options.put(CompilerOptions.OPTION_Source, compilerVersion);
			options.put(CompilerOptions.OPTION_TargetPlatform, compilerVersion);
			javaProject.setOptions(options);
		}
		return project != null ? project.getFullPath() : Path.EMPTY;
	}
	
	/**
	 * Creates a new plugin project with the given name.
	 * If a project with the same name already exists in the testing workspace
	 * it will be deleted and new project created.
	 * @param projectName
	 * @return the newly created {@link IJavaProject}
	 */
	protected IJavaProject createProject(String projectName) {
		IJavaProject jproject = null;
		try {
			IProject project = getWorkspace().getRoot().getProject(projectName);
			if(project.exists()) {
				project.delete(true, new NullProgressMonitor());
			}
			jproject = ProjectUtils.createPluginProject(projectName, new String[] {PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID});
			addProject(jproject.getProject());
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		return jproject;
	}
	
	/**
	 * Performs a clean build on the project using the builder with the given builder id
	 * @param project
	 * @param builderid
	 */
	public void cleanBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.CLEAN_BUILD, builderid, null, null);
		} catch (CoreException e) {}
	}
	
	/**
	 * Incrementally builds the given project using the builder with the given builder id
	 * @param project
	 * @param builderid
	 */
	public void incrementalBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.INCREMENTAL_BUILD, builderid, null, null);
		} catch (CoreException e) {}
	}
	
	/**
	 * Performs a full build on the given project using the builder with the given builder id
	 * @param project
	 * @param builderid
	 */
	public void fullBuild(IProject project, String builderid) {
		try {
			getProject(project.getName()).build(IncrementalProjectBuilder.FULL_BUILD, builderid, null, null);
		} catch (CoreException e) {}
	}
	
	/**
	 * returns all of the usage markers for the specified resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllUsageMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * returns all of the usage markers for the specified resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	public IMarker[] getAllJDTMarkers(IPath path) throws CoreException {
		return getAllJDTMarkers(getResource(path));
	}

	/**
	 * returns all of the usage markers for the specified resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllJDTMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	/**
	 * Returns all of the unsupported Javadoc tag markers on the specified resource
	 * and all of its children.
	 * @param resource
	 * @return
	 */
	protected IMarker[] getAllUnsupportedTagMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.UNSUPPORTED_TAG_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the compatibility markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllCompatibilityMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the API profile markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllAPIProfileMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the since tag markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllSinceTagMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the version markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllVersionMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the unused API problem filters markers on the given resource to infinite depth
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllUnusedApiProblemFilterMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return NO_MARKERS;
		}
		if(!resource.isAccessible()) {
			return NO_MARKERS;
		}
		return resource.findMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the markers from the testing workspace
	 * @return
	 */
	public IMarker[] getMarkers() {
		return getMarkersFor(getWorkspaceRootPath());
	}
	
	/**
	 * Returns the collection of API problem markers for the given element
	 * @param root
	 * @return
	 */
	public IMarker[] getMarkersFor(IPath root) {
		return getMarkersFor(root, null);
	}
	
	/**
	 * Return all problems with the specified element.
	 * @param path
	 * @param additionalMarkerType
	 * @return
	 */
	public IMarker[] getMarkersFor(IPath path, String additionalMarkerType){
		IResource resource = getResource(path);
		try {
			ArrayList<IMarker> problems = new ArrayList<IMarker>();
			addToList(problems, getAllUsageMarkers(resource));
			addToList(problems, getAllCompatibilityMarkers(resource));
			addToList(problems, getAllAPIProfileMarkers(resource));
			addToList(problems, getAllSinceTagMarkers(resource));
			addToList(problems, getAllVersionMarkers(resource));
			addToList(problems, getAllUnsupportedTagMarkers(resource));
			addToList(problems, getAllUnusedApiProblemFilterMarkers(resource));
			
			//additional markers
			if(additionalMarkerType != null) {
				problems.addAll(Arrays.asList(resource.findMarkers(additionalMarkerType, true, IResource.DEPTH_INFINITE)));
			}
			return (IMarker[]) problems.toArray(new IMarker[problems.size()]);
		} catch(CoreException e){
			// ignore
		}
		return new IMarker[0];
	}

	public IResource getResource(IPath path) {
		IResource resource;
		if(path.equals(getWorkspaceRootPath())){
			resource = getWorkspace().getRoot();
		} else {
			IProject p = getProject(path);
			if(p != null && path.equals(p.getFullPath())) {
				resource = getProject(path.lastSegment());
			} else if(path.getFileExtension() == null) {
				resource = getWorkspace().getRoot().getFolder(path);
			} else {
				resource = getWorkspace().getRoot().getFile(path);
			}
		}
		return resource;
	}
	
	private void addToList(List list, Object[] objects) {
		if(list == null || objects == null) {
			return;
		}
		if(objects.length == 0) {
			return;
		}
		for(int i = 0; i < objects.length; i++) {
			list.add(objects[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#getProblems()
	 */
	public ApiProblem[] getProblems() {
		return (ApiProblem[]) super.getProblems();
	}
	
	/**
	 * Returns the current workspace {@link IApiProfile}
	 * @return
	 */
	protected IApiBaseline getWorkspaceProfile() {
		return ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#getProblemsFor(org.eclipse.core.runtime.IPath, java.lang.String)
	 */
	public ApiProblem[] getProblemsFor(IPath path, String additionalMarkerType){	
		IMarker[] markers = getMarkersFor(path, additionalMarkerType);
		ArrayList<ApiProblem> problems = new ArrayList<ApiProblem>();
		for(int i = 0; i < markers.length; i++) {
			problems.add(new ApiProblem(markers[i]));
		}
		return (ApiProblem[]) problems.toArray(new ApiProblem[problems.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#removeProject(org.eclipse.core.runtime.IPath)
	 */
	public void removeProject(IPath projectPath) {
		IJavaProject project = getJavaProject(projectPath);
		if(project != null) {
			try {
				project.getProject().delete(true, new NullProgressMonitor());
			}
			catch(CoreException ce) {
				
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#resetWorkspace()
	 */
	public void resetWorkspace() {
		try {
			if (fRevert) {
				revertWorkspace();
			} else {
				super.resetWorkspace();
				//clean up any left over projects from other tests
				IProject[] projects = getWorkspace().getRoot().getProjects();
				for(int i = 0; i < projects.length; i++) {
					try {
						projects[i].delete(true, new NullProgressMonitor());
					}
					catch(CoreException ce) {
						
					}
				}
			}
		} finally {
			// clear all changes
			fAdded.clear();
			fChanged.clear();
			fRemoved.clear();
		}
	}
	
	/**
	 * Sets the default revert source path to the given path.
	 * This path is only consulted if the workspace is to be reverted. Setting the path to <code>null</code>
	 * will result in all workspace modifications being deleted if a call to {@link #revertWorkspace()} is made.
	 * @param path
	 */
	protected void setRevertSourcePath(IPath path) {
		fRevertSourcePath = path;
	}
	
	/**
	 * @return the currently set source path to use when reverting changes to the workspace.
	 */
	protected IPath getRevertSourcePath() {
		return fRevertSourcePath;
	}
	
	/**
	 * Reverts changes in the workspace - added, removed, changed files
	 */
	public void revertWorkspace() {
		/*if(fRevertSourcePath == null) {
			fAdded.clear();
			fChanged.clear();
			fRemoved.clear();
			return;
		}*/
		
		// remove each added file
		Iterator<IPath> iterator = fAdded.iterator();
		while (iterator.hasNext()) {
			IPath path = (IPath) iterator.next();
			deleteWorkspaceFile(path);
		}
		
		// revert each changed file
		iterator = fChanged.iterator();
		while (iterator.hasNext()) {
			IPath path = (IPath) iterator.next();
			IPath repl = TestSuiteHelper.getPluginDirectoryPath().
				append(ApiBuilderTest.TEST_SOURCE_ROOT).append(CompatibilityTest.BASELINE).append(path);
			updateWorkspaceFile(path, repl);
		}
		
		// replace each deleted file
		iterator = fRemoved.iterator();
		while (iterator.hasNext()) {
			IPath path = (IPath) iterator.next();
			IPath repl = TestSuiteHelper.getPluginDirectoryPath().
				append(ApiBuilderTest.TEST_SOURCE_ROOT).append(CompatibilityTest.BASELINE).append(path);
			createWorkspaceFile(path, repl);
		}
		
	}
	
	/**
	 * Deletes the workspace file at the specified location (full path).
	 * 
	 * @param workspaceLocation
	 */
	private void deleteWorkspaceFile(IPath workspaceLocation) {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		try {
			file.delete(true, null);
		} catch (CoreException e) {
			try {
				//try to bring the resource in to sync an re-delete
				file.refreshLocal(IResource.DEPTH_ONE, null);
				file.delete(true, null);
			} catch (CoreException e1) {
				ApiPlugin.log(e1);
				return;
			}
			ApiPlugin.log(e);
		}
	}		
	
	/**
	 * Updates the contents of a workspace file at the specified location (full path),
	 * with the contents of a local file at the given replacement location (absolute path).
	 * 
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	private void updateWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		File replacement = replacementLocation.toFile();
		try {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(replacement);
				file.setContents(stream, false, true, null);
			}
			finally {
				if(stream != null) {
					stream.close();
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}	

	/**
	 * Updates the contents of a workspace file at the specified location (full path),
	 * with the contents of a local file at the given replacement location (absolute path).
	 * 
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	private void createWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) {
		IFile file = getWorkspace().getRoot().getFile(workspaceLocation);
		File replacement = replacementLocation.toFile();
		try {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(replacement);
				file.create(stream, false, null);
			}
			finally {
				if(stream != null) {
					stream.close();
				}
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Notes a file was added during the test, to be undone
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#addFile(org.eclipse.core.runtime.IPath, java.lang.String, java.lang.String)
	 */
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
	 * Returns the listing of projects in the order the workspace has computed they should be built.
	 * This method calls out to {@link org.eclipse.core.resources.IWorkspace#computeProjectOrder(IProject[])}, which
	 * can slow down testing with successive calls.
	 * 
	 * @return a build-ordered listing of the workspace projects
	 */
	public IProject[] getProjectBuildOrder() {
		return getWorkspace().computeProjectOrder(getWorkspace().getRoot().getProjects()).projects;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#addClass(org.eclipse.core.runtime.IPath, java.lang.String, java.lang.String)
	 */
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
}
