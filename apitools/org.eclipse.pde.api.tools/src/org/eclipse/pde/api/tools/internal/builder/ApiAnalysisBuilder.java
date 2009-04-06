/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import com.ibm.icu.text.MessageFormat;

/**
 * Builder for creating API tooling resource markers
 * @since 1.0.0
 */
public class ApiAnalysisBuilder extends IncrementalProjectBuilder {
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Project relative path to the manifest file.
	 */
	static final IPath MANIFEST_PATH = new Path(JarFile.MANIFEST_NAME);
	
	/**
	 * Project relative path to the .api_filters file
	 */
	static final IPath FILTER_PATH = new Path(".settings").append(IApiCoreConstants.API_FILTERS_XML_NAME); //$NON-NLS-1$
	
	/**
	 * Empty listing of projects to be returned by the builder if there is nothing to do
	 */
	static final IProject[] NO_PROJECTS = new IProject[0];

	/**
	 * Constant representing the name of the 'source' attribute on API tooling markers.
	 * Value is <code>Api Tooling</code>
	 */
	static final String SOURCE = "Api Tooling"; //$NON-NLS-1$
	
	/**
	 * Method used for initializing tracing in the API tool builder
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * The current project for which this builder was defined
	 */
	private IProject currentproject = null;
	
	/**
	 * The API analyzer for this builder
	 */
	private IApiAnalyzer analyzer = null;
	
	/**
	 * Maps prerequisite projects to their output location(s)
	 */
	private HashMap projecttooutputlocations = new HashMap();
	
	/**
	 * Current build state
	 */
	private BuildState buildstate = null;
	
	/**
	 * Cleans up markers associated with API tooling on the given resource.
	 * 
	 * @param resource
	 */
	void cleanupMarkers(IResource resource) {
		cleanUnusedFilterMarkers(resource);
		cleanupUsageMarkers(resource);
		cleanupCompatibilityMarkers(resource);
		cleanupUnsupportedTagMarkers(resource);
	}
	
	/**
	 * Cleans up unsupported Javadoc tag markers on the specified resource
	 * @param resource
	 */
	void cleanupUnsupportedTagMarkers(IResource resource) {
		try {
			if(DEBUG) {
				System.out.println("cleaning unsupported tag problems"); //$NON-NLS-1$
			}
			resource.deleteMarkers(IApiMarkerConstants.UNSUPPORTED_TAG_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Cleans up only API compatibility markers on the given {@link IResource}
	 * @param resource the given resource
	 */
	void cleanupCompatibilityMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				if (resource.getType() == IResource.PROJECT) {
					// on full builds
					resource.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
					resource.deleteMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
					resource.deleteMarkers(IApiMarkerConstants.API_COMPONENT_RESOLUTION_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
				}
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * cleans up only API usage markers from the given {@link IResource}
	 * @param resource
	 */
	void cleanupUsageMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Cleans up the unused API filter problems from the given resource
	 * @param resource
	 */
	void cleanUnusedFilterMarkers(IResource resource) {
		try {
			if(resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			}
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce.getStatus());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		this.currentproject = getProject();
		if (!this.currentproject.isAccessible() || !this.currentproject.hasNature(ApiPlugin.NATURE_ID) || hasBeenBuilt(this.currentproject)) {
			return NO_PROJECTS;
		}
		if (DEBUG) {
			System.out.println("\nStarting build of " + this.currentproject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		updateMonitor(monitor, 0);
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_builder, 2);
		final IProject[] projects = getRequiredProjects(true);
		IApiBaseline baseline = ApiPlugin.getDefault().getApiBaselineManager().getDefaultApiBaseline();
		try {
			switch(kind) {
				case FULL_BUILD : {
					if (DEBUG) {
						System.out.println("Performing full build as requested by user"); //$NON-NLS-1$
					}
					buildAll(baseline, localMonitor.newChild(1));
					break;
				}
				case AUTO_BUILD :
				case INCREMENTAL_BUILD : {
					this.buildstate = getLastBuiltState(currentproject);
					if (this.buildstate == null) {
						buildAll(baseline, localMonitor.newChild(1));
						break;
					}
					else if(worthDoingFullBuild(projects)) {
						buildAll(baseline, localMonitor.newChild(1));
					}
					IResourceDelta[] deltas = getDeltas(projects);
					if(deltas.length < 1) {
						buildAll(baseline, localMonitor.newChild(1));
					}
					else {	
						IResourceDelta manifest = null;
						IResourceDelta filters = null;
						for (int i = 0; i < deltas.length; i++) {
							manifest = deltas[i].findMember(MANIFEST_PATH);
							if(manifest != null) {
								break;
							}
							filters = deltas[i].findMember(FILTER_PATH);
							if(filters != null){
								break;
							}
						}
						if (manifest != null || filters != null) {
							if (DEBUG) {
								System.out.println("Performing full build since MANIFEST.MF or .api_filters was modified"); //$NON-NLS-1$
	 						}
							buildAll(baseline, localMonitor.newChild(1));
						}
						else {
							IncrementalApiBuilder builder = new IncrementalApiBuilder(this);
							builder.build(baseline, deltas, this.buildstate, localMonitor.newChild(1));
						}
					}
				}	
			}
			updateMonitor(monitor, 0);
		} catch(CoreException e) {
			IStatus status = e.getStatus();
			if (status == null || status.getCode() != ApiPlugin.REPORT_BASELINE_IS_DISPOSED) {
				throw e;
			}
			ApiPlugin.log(e);
		} finally {
			updateMonitor(monitor, 0);
			if(this.analyzer != null) {
				this.analyzer.dispose();
				this.analyzer = null;
			}
			if(baseline != null) {
				baseline.close();
			}
			if(monitor != null) {
				monitor.done();
			}
			if (this.buildstate != null) {
				for(int i = 0, max = projects.length; i < max; i++) {
					IProject project = projects[i];
					if (Util.isApiProject(project)) {
						this.buildstate.addApiToolingDependentProject(project.getName());
					}
				}
				saveBuiltState(this.currentproject, this.buildstate);
				this.buildstate = null;
			}
		}
		if (DEBUG) {
			System.out.println("Finished build of " + this.currentproject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return projects;
	}
	
	private boolean worthDoingFullBuild(IProject[] projects) {
		Set apiToolingDependentProjects = this.buildstate.getApiToolingDependentProjects();
		for (int i = 0, max = projects.length; i < max; i++) {
			IProject currentProject = projects[i];
			if (Util.isApiProject(currentProject)) {
				if (apiToolingDependentProjects.contains(currentProject.getName())) {
					continue;
				}
				return true;
			} else if (apiToolingDependentProjects.contains(currentProject.getName())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Performs a full build for the project
	 * @param monitor
	 */
	void buildAll(IApiBaseline baseline, IProgressMonitor monitor) throws CoreException {
		IApiBaseline wsprofile = null;
		try {
			clearLastState();
			this.buildstate = new BuildState();
			SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 4);
			localMonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_initializing_analyzer, currentproject.getName()));
			cleanupMarkers(this.currentproject);
			IPluginModelBase currentModel = getCurrentModel();
			if (currentModel != null) {
				localMonitor.subTask(BuilderMessages.building_workspace_profile);
				wsprofile = getWorkspaceProfile();
				updateMonitor(localMonitor, 1);
				if (wsprofile == null) {
					if (DEBUG) {
						System.err.println("Could not retrieve a workspace profile"); //$NON-NLS-1$
					}
					return;
				}
				String id = currentModel.getBundleDescription().getSymbolicName();
				// Compatibility checks
				IApiComponent apiComponent = wsprofile.getApiComponent(id);
				if(apiComponent != null) {
					getAnalyzer().analyzeComponent(this.buildstate, null, null, baseline, apiComponent, null, null, localMonitor.newChild(1));
					updateMonitor(localMonitor, 1);
					createMarkers();
					updateMonitor(localMonitor, 1);
				}
			}
		}
		finally {
			if(wsprofile != null) {
				wsprofile.close();
			}
			if(monitor != null) {
				monitor.done();
			}
		}
	}
	
	/**
	 * Creates new markers are for the listing of problems added to this reporter.
	 * If no problems have been added to this reporter, or we are not running in the framework,
	 * no work is done.
	 */
	protected void createMarkers() {
		try {
			IResource manifest = Util.getManifestFile(this.currentproject);
			if(manifest != null)  {
				manifest.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			}
			this.currentproject.deleteMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			this.currentproject.deleteMarkers(IApiMarkerConstants.API_COMPONENT_RESOLUTION_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		IApiProblem[] problems = getAnalyzer().getProblems();
		String type = null;
		for(int i = 0; i < problems.length; i++) {
			int category = problems[i].getCategory();
			type = getProblemTypeFromCategory(category, problems[i].getKind());
			if(type == null) {
				continue;
			}
			if(DEBUG) {
				System.out.println("creating marker for: "+problems[i].toString()); //$NON-NLS-1$
			}
			createMarkerForProblem(category, type, problems[i]);
		}
	}
	
	/**
	 * Returns the {@link IApiMarkerConstants} problem type given the 
	 * problem category
	 * @param category
	 * @return the problem type or <code>null</code>
	 */
	private String getProblemTypeFromCategory(int category, int kind) {
		switch(category) {
			case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION : {
				return IApiMarkerConstants.API_COMPONENT_RESOLUTION_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_API_BASELINE: {
				return IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_COMPATIBILITY: {
				return IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_SINCETAGS: {
				return IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_USAGE: {
				if(kind == IApiProblem.UNSUPPORTED_TAG_USE) {
					return IApiMarkerConstants.UNSUPPORTED_TAG_PROBLEM_MARKER;
				}
				if(kind == IApiProblem.UNUSED_PROBLEM_FILTERS) {
					return IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER;
				}
				return IApiMarkerConstants.API_USAGE_PROBLEM_MARKER;
			}
			case IApiProblem.CATEGORY_VERSION: {
				return IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER;
			}
		}
		return null;
	}

	/**
	 * Creates an {@link IMarker} on the resource specified
	 * in the problem (via its path) with the given problem
	 * attributes
	 * @param problem the problem to create a marker from
	 */
	private void createMarkerForProblem(int category, String type, IApiProblem problem) {
		IResource resource = resolveResource(problem);
		if(resource == null) {
			return;
		}
		try {
			IMarker marker = resource.createMarker(type);
			int line = problem.getLineNumber();
			switch(category) {
				case IApiProblem.CATEGORY_VERSION :
				case IApiProblem.CATEGORY_API_BASELINE :
				case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION : {
					break;
				}
				default : {
					line++;
				}
			}
			marker.setAttributes(
					new String[] {
							IMarker.MESSAGE,
							IMarker.SEVERITY,
							IMarker.LINE_NUMBER,
							IMarker.CHAR_START,
							IMarker.CHAR_END,
							IMarker.SOURCE_ID,
							IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID},
					new Object[] {
							problem.getMessage(),
							new Integer(ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(problem), this.currentproject)),
							new Integer(line),
							new Integer(problem.getCharStart()),
							new Integer(problem.getCharEnd()),
							ApiAnalysisBuilder.SOURCE,
							new Integer(problem.getId())
					}
				);
			//add message arguments, if any
			String[] args = problem.getMessageArguments();
			if(args.length > 0) {
				marker.setAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, createArgAttribute(args));
			}
			String typeName = problem.getTypeName();
			if (typeName != null) {
				marker.setAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_TYPE_NAME, typeName);
			}
			//add all other extra arguments, if any
			if(problem.getExtraMarkerAttributeIds().length > 0) {
				marker.setAttributes(problem.getExtraMarkerAttributeIds(), problem.getExtraMarkerAttributeValues());
			}
		} catch (CoreException e) {
			//ignore and continue
			return;
		}
	}
	
	/**
	 * Resolves the resource from the path in the problem, returns <code>null</code> in 
	 * the following cases: 
	 * <ul>
	 * <li>The resource is not found in the parent project (findMember() returns null)</li>
	 * <li>The resource is not accessible (isAccessible() returns false</li>
	 * </ul>
	 * @param problem the problem to get the resource for
	 * @return the resource or <code>null</code>
	 */
	private IResource resolveResource(IApiProblem problem) {
		String resourcePath = problem.getResourcePath();
		if (resourcePath == null) {
			return null;
		}
		IResource resource = currentproject.findMember(new Path(resourcePath));
		if(resource == null) {
			return null;
		}
		if(!resource.isAccessible()) {
			return null;
		}
		return resource;
	}
	
	/**
	 * Creates a single string attribute from an array of strings. Uses the '#' char as 
	 * a delimiter
	 * @param args
	 * @return a single string attribute from an array or arguments
	 */
	private String createArgAttribute(String[] args) {
		StringBuffer buff = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			buff.append(args[i]);
			if(i < args.length-1) {
				buff.append("#"); //$NON-NLS-1$
			}
		}
		return buff.toString();
	}
	
	/**
	 * Updates the given monitor with the given tick count and polls for cancellation. If the monitor
	 * is cancelled an {@link OperationCanceledException} is thrown
	 * @param monitor
	 * @param ticks
	 * @throws OperationCanceledException
	 */
	void updateMonitor(IProgressMonitor monitor, int ticks) throws OperationCanceledException {
		if(monitor != null) {
			monitor.worked(ticks);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void clean(IProgressMonitor monitor) throws CoreException {
		this.currentproject = getProject();
		SubMonitor localmonitor = SubMonitor.convert(monitor, MessageFormat.format(BuilderMessages.CleaningAPIDescription, new String[] {this.currentproject.getName()}), 2);
		try {
			// clean up all existing markers
			cleanupUsageMarkers(this.currentproject);
			cleanupCompatibilityMarkers(this.currentproject);
			cleanupUnsupportedTagMarkers(this.currentproject);
			this.currentproject.deleteMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			updateMonitor(localmonitor, 1);
			//clean up the .api_settings
			cleanupApiDescription(this.currentproject);
			updateMonitor(localmonitor, 1);
		}
		finally {
			clearLastState();
			localmonitor.done();
		}
	}

	/**
	 * Cleans the .api_settings file for the given project
	 * @param project
	 */
	private void cleanupApiDescription(IProject project) {
		if(project != null && project.exists()) {
			ApiDescriptionManager.getDefault().clean(JavaCore.create(project), true, true);
		}
	}
	
	/**
	 * @return the current {@link IPluginModelBase} based on the current project for this builder
	 */
	IPluginModelBase getCurrentModel() {
		IPluginModelBase[] workspaceModels = PluginRegistry.getWorkspaceModels();
		IPath location = this.currentproject.getLocation();
		IPluginModelBase currentModel = null;
		BundleDescription desc = null;
		loop: for (int i = 0, max = workspaceModels.length; i < max; i++) {
			desc = workspaceModels[i].getBundleDescription();
			if(desc != null) {
				Path path = new Path(desc.getLocation());
				if (path.equals(location)) {
					currentModel = workspaceModels[i];
					break loop;
				}
			}
			else if(DEBUG) {
				System.out.println("Tried to look up bundle description for: " + workspaceModels[i].toString()); //$NON-NLS-1$
			}
		}
		return currentModel;
	}

	/**
	 * Returns a listing of deltas for this project and for dependent projects
	 * @param projects
	 * @return
	 */
	private IResourceDelta[] getDeltas(IProject[] projects) {
		if(DEBUG) {
			System.out.println("Searching for deltas for build of project: "+this.currentproject.getName()); //$NON-NLS-1$
		}
		ArrayList deltas = new ArrayList();
		IResourceDelta delta = getDelta(this.currentproject);
		if(delta != null) {
			if (DEBUG) {
				System.out.println("Found a delta: " + delta); //$NON-NLS-1$
			}
			deltas.add(delta);
		}
		for(int i = 0; i < projects.length; i++) {
			delta = getDelta(projects[i]);
			if(delta != null) {
				if (DEBUG) {
					System.out.println("Found a delta: " + delta); //$NON-NLS-1$
				}
				deltas.add(delta);
			}
		}
		return (IResourceDelta[]) deltas.toArray(new IResourceDelta[deltas.size()]);
	}

	/**
	 * Returns the API analyzer to use with this instance of the builder
	 * @return the API analyzer to use
	 */
	protected synchronized IApiAnalyzer getAnalyzer() {
		if(this.analyzer == null) {
			this.analyzer = new BaseApiAnalyzer();
		}
		return this.analyzer;
	}
	
	/**
	 * Returns the complete listing of required projects from the classpath of the backing project
	 * @param includeBinaryPrerequisites
	 * @return
	 * @throws CoreException
	 */
	private IProject[] getRequiredProjects(boolean includebinaries) throws CoreException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		if (this.currentproject == null || workspaceRoot == null) { 
			return new IProject[0];
		}
		ArrayList projects = new ArrayList();
		try {
			IJavaProject javaProject = JavaCore.create(this.currentproject);
			HashSet blocations = new HashSet();
			blocations.add(javaProject.getOutputLocation());
			projecttooutputlocations.put(this.currentproject, blocations);
			IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
			for (int i = 0, l = entries.length; i < l; i++) {
				IClasspathEntry entry = entries[i];
				IPath path = entry.getPath();
				IProject p = null;
				switch (entry.getEntryKind()) {
					case IClasspathEntry.CPE_PROJECT : {
						p = workspaceRoot.getProject(path.lastSegment()); // missing projects are considered too
						if (isOptional(entry) && !p.hasNature(ApiPlugin.NATURE_ID)) {// except if entry is optional 
							p = null;
						}
						break;
					}
					case IClasspathEntry.CPE_LIBRARY : {
						if (includebinaries && path.segmentCount() > 1) {
							// some binary resources on the class path can come from projects that are not included in the project references
							IResource resource = workspaceRoot.findMember(path.segment(0));
							if (resource instanceof IProject) {
								p = (IProject) resource;
							}
						}
						break;
					}
					case IClasspathEntry.CPE_SOURCE: {
						IPath entrypath = entry.getOutputLocation();
						if(entrypath != null) {
							blocations.add(entrypath);
						}
					}
				}
				if (p != null && !projects.contains(p)) {
					projects.add(p);
					//try to derive all of the output locations for each of the projects
					javaProject = JavaCore.create(p);
					HashSet bins = new HashSet();
					if(javaProject.exists()) {
						bins.add(javaProject.getOutputLocation());
						IClasspathEntry[] source = javaProject.getRawClasspath();
						IPath entrypath = null;
						for(int j = 0; j < source.length; j++) {
							if(source[j].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								entrypath = source[j].getOutputLocation();
								if(entrypath != null) {
									bins.add(entrypath);
								}
							}
						}
						this.projecttooutputlocations.put(p, bins);
					}
				}
			}
		} 
		catch(JavaModelException e) {
			return new IProject[0];
		}
		IProject[] result = new IProject[projects.size()];
		projects.toArray(result);
		return result;
	}

	/**
	 * @return the workspace {@link IApiProfile}
	 */
	IApiBaseline getWorkspaceProfile() throws CoreException {
		return ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}
	
	/**
	 * Returns the output paths of the given project or <code>null</code> if none have been computed
	 * @param project
	 * @return the output paths for the given project or <code>null</code>
	 */
	HashSet getProjectOutputPaths(IProject project) {
		return (HashSet) this.projecttooutputlocations.get(project);
	}
	
	/**
	 * Returns is the given classpath entry is optional or not
	 * @param entry
	 * @return true if the specified {@link IClasspathEntry} is optional, false otherwise
	 */
	private boolean isOptional(IClasspathEntry entry) {
		IClasspathAttribute[] attribs = entry.getExtraAttributes();
		for (int i = 0, length = attribs.length; i < length; i++) {
			IClasspathAttribute attribute = attribs[i];
			if (IClasspathAttribute.OPTIONAL.equals(attribute.getName()) && "true".equals(attribute.getValue())) //$NON-NLS-1$
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return NLS.bind(BuilderMessages.ApiAnalysisBuilder_builder_for_project, this.currentproject.getName());
	}
	
	/**
	 * Return the last built state for the given project, or null if none
	 */
	public static BuildState getLastBuiltState(IProject project) throws CoreException {
		if (!Util.isApiProject(project)) {
			// should never be requested on non-Java projects
			return null;
		}
		return readState(project);
	}
	
	/**
	 * Reads the build state for the relevant project.
	 */
	static BuildState readState(IProject project) throws CoreException {
		File file = getSerializationFile(project);
		if (file != null && file.exists()) {
			try {
				DataInputStream in= new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
				try {
					return BuildState.read(in);
				} finally {
					if (DEBUG) {
						System.out.println("Saved state thinks last build failed for " + project.getName()); //$NON-NLS-1$
					}
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Platform.PLUGIN_ERROR, "Error reading last build state for project "+ project.getName(), e)); //$NON-NLS-1$
			}
		} else if (DEBUG) {
			if (file == null) {
				System.out.println("Project does not exist: " + project); //$NON-NLS-1$
			} else {
				System.out.println("Build state file " + file.getPath() + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	/**
	 * Sets the last built state for the given project, or null to reset it.
	 */
	public static void setLastBuiltState(IProject project, BuildState state) throws CoreException {
		if (Util.isApiProject(project)) {
			// should never be requested on non-Java projects
			if (state != null) {
				saveBuiltState(project, state);
			} else {
				try {
					File file = getSerializationFile(project);
					if (file != null && file.exists()) {
						file.delete();
					}
				} catch(SecurityException se) {
					// could not delete file: cannot do much more
				}
			}
		}
	}	

	/**
	 * Returns the File to use for saving and restoring the last built state for the given project.
	 */
	static File getSerializationFile(IProject project) {
		if (!project.exists()) {
			return null;
		}
		IPath workingLocation = project.getWorkingLocation(ApiPlugin.PLUGIN_ID);
		return workingLocation.append("state.dat").toFile(); //$NON-NLS-1$
	}

	/**
	 * Saves the current build state
	 * @param project
	 * @param state
	 * @throws CoreException
	 */
	static void saveBuiltState(IProject project, BuildState state) throws CoreException {
		if (DEBUG) {
			System.out.println("Saving build state for project: "+project.getName()); //$NON-NLS-1$
		}
		File file = getSerializationFile(project);
		if (file == null) return;
		long t = 0;
		if (DEBUG) {
			t = System.currentTimeMillis();
		}
		try {
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			try {
				BuildState.write(state, out);
			} finally {
				out.close();
			}
		} catch (RuntimeException e) {
			try {
				file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
			throw new CoreException(
				new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, Platform.PLUGIN_ERROR,
					NLS.bind(BuilderMessages.build_cannotSaveState, project.getName()), e)); 
		} catch (IOException e) {
			try {
				file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
			throw new CoreException(
				new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, Platform.PLUGIN_ERROR,
					NLS.bind(BuilderMessages.build_cannotSaveState, project.getName()), e)); 
		}
		if (DEBUG) {
			t = System.currentTimeMillis() - t;
			System.out.println(NLS.bind(BuilderMessages.build_saveStateComplete, String.valueOf(t))); 
		}
	}

	/**
	 * Clears the last build state by setting it to <code>null</code>
	 * @throws CoreException
	 */
	void clearLastState() throws CoreException {
		setLastBuiltState(this.currentproject, null);
	}
}
