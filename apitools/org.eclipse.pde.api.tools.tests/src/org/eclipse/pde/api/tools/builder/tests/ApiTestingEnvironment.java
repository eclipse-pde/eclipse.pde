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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.internal.core.natures.PDE;

/**
 * Environment used to test the {@link ApiAnalysisBuilder}.
 * This environment emulates a typical workbench environment
 * 
 * @since 1.0.0
 */
public class ApiTestingEnvironment extends TestingEnvironment {
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#addProject(java.lang.String, java.lang.String)
	 */
	public IPath addProject(String projectName, String compliance) throws UnsupportedOperationException {
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
		IProject project = null;
		if (requiredComplianceFlag != 0) {
			if ((AbstractCompilerTest.getPossibleComplianceLevels() & requiredComplianceFlag) == 0)
				throw new RuntimeException("This test requires a " + compliance + " JRE");
			try {
				IJavaProject javaProject = ProjectUtils.createPluginProject(projectName, new String[] {PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID});
				project = javaProject.getProject();
				
				HashMap options = new HashMap();
				options.put(CompilerOptions.OPTION_Compliance, compilerVersion);
				options.put(CompilerOptions.OPTION_Source, compilerVersion);
				options.put(CompilerOptions.OPTION_TargetPlatform, compilerVersion);
				javaProject.setOptions(options);
				addProject(project);
			}
			catch(CoreException ce) {
				//ignore
			}
		}
		return project != null ? project.getFullPath() : Path.EMPTY;
	}
	
	/**
	 * returns all of the usage markers for the specified resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllUsageMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return new IMarker[0];
		}
		if(!resource.isAccessible()) {
			return new IMarker[0];
		}
		return resource.findMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the binary compatibility markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllBinaryMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return new IMarker[0];
		}
		if(!resource.isAccessible()) {
			return new IMarker[0];
		}
		return resource.findMarkers(IApiMarkerConstants.BINARY_COMPATIBILITY_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the API profile markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllAPIProfileMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return new IMarker[0];
		}
		if(!resource.isAccessible()) {
			return new IMarker[0];
		}
		return resource.findMarkers(IApiMarkerConstants.DEFAULT_API_PROFILE_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Returns all of the since tag markers on the given resource and its children
	 * @param resource
	 * @return
	 * @throws CoreException
	 */
	protected IMarker[] getAllSinceTagMarkers(IResource resource) throws CoreException {
		if(resource == null) {
			return new IMarker[0];
		}
		if(!resource.isAccessible()) {
			return new IMarker[0];
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
			return new IMarker[0];
		}
		if(!resource.isAccessible()) {
			return new IMarker[0];
		}
		return resource.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
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
		try {
			ArrayList problems = new ArrayList();
			problems.addAll(Arrays.asList(getAllUsageMarkers(resource)));
			problems.addAll(Arrays.asList(getAllBinaryMarkers(resource)));
			problems.addAll(Arrays.asList(getAllAPIProfileMarkers(resource)));
			problems.addAll(Arrays.asList(getAllSinceTagMarkers(resource)));
			problems.addAll(Arrays.asList(getAllVersionMarkers(resource)));
			
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
	protected IApiProfile getWorkspaceProfile() {
		return ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.TestingEnvironment#getProblemsFor(org.eclipse.core.runtime.IPath, java.lang.String)
	 */
	public ApiProblem[] getProblemsFor(IPath path, String additionalMarkerType){	
		IMarker[] markers = getMarkersFor(path, additionalMarkerType);
		ArrayList problems = new ArrayList();
		for(int i = 0; i < markers.length; i++) {
			problems.add(new ApiProblem(markers[i]));
		}
		return (ApiProblem[]) problems.toArray(new ApiProblem[problems.size()]);
	}
}
