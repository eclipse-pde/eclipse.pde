/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.ReferenceCollection;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.jdt.internal.core.builder.StringSet;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer;
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
	 * Visits a resource delta to determine if the manifest has been modified.
	 */
	private class ManifestVisitor implements IResourceDeltaVisitor {
		
		private boolean fManifestModified = false;

		/**
		 * Returns whether the manifest file was modified.
		 * 
		 * @return whether the manifest file was modified
		 */
		boolean isManifiestModified() {
			return fManifestModified;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			switch (delta.getResource().getType()) {
			case IResource.ROOT:
			case IResource.PROJECT:
				return !fManifestModified;
			case IResource.FOLDER:
				return !fManifestModified; 
			case IResource.FILE:
				if (delta.getResource().getProjectRelativePath().equals(MANIFEST_PATH)) {
					fManifestModified = true;
					break;
				}
				IResource resource = delta.getResource();
				String fileName = resource.getName();
				if (Util.isClassFile(fileName)) {
					findAffectedSourceFiles(delta);
				} else if (Util.isJavaFileName(fileName) && fCurrentProject.equals(resource.getProject())) {
					fChangedTypes.add(resource);
					fTypesToCheck.add(resource);
				}
			}
			return false;
		}
		
	}
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Project relative path to the manifest file.
	 */
	private static final IPath MANIFEST_PATH = new Path(JarFile.MANIFEST_NAME);
	
	/**
	 * Internal flag used to determine what created the marker, as there is overlap for reference kinds and deltas
	 */
	public static final int REF_TYPE_FLAG = 0;

	/**
	 * Constant representing the name of the 'source' attribute on API tooling markers.
	 * Value is <code>Api Tooling</code>
	 */
	public static final String SOURCE = "Api Tooling"; //$NON-NLS-1$
	
	/**
	 * Method used for initializing tracing in the API tool builder
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	/**
	 * The current project for which this builder was defined
	 */
	private IProject fCurrentProject = null;
	
	/**
	 * The API analyzer for this builder
	 */
	private IApiAnalyzer fAnalyzer = null;
	
	/**
	 * Maps prerequisite projects to their output location(s)
	 */
	private HashMap fProjectToOutputLocations = new HashMap();
	
	/**
	 * List of type names to lookup for each project context to find dependents of
	 */
	private StringSet fTypes = new StringSet(3);
	
	/**
	 * List of package names to qualify type names
	 */
	private StringSet fPackages = new StringSet(3);
	
	/**
	 * The type that we want to check for API problems
	 */
	private HashSet fTypesToCheck = new HashSet();
	
	/**
	 * The set of changed types that came directly from the delta 
	 */
	private HashSet fChangedTypes = new HashSet(5);
	
	/**
	 * Current build state
	 */
	private BuildState fBuildState;
	
	/**
	 * Cleans up markers associated with API tooling on the given resource.
	 * 
	 * @param resource
	 */
	public static void cleanupMarkers(IResource resource) {
		cleanupUsageMarkers(resource);
		cleanupCompatibiltiyMarkers(resource);
		cleanupUnsupportedTagMarkers(resource);
	}
	
	/**
	 * Cleans up unsupported Javadoc tag markers on the specified resource
	 * @param resource
	 */
	private static void cleanupUnsupportedTagMarkers(IResource resource) {
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
	 * @param resource
	 */
	private static void cleanupCompatibiltiyMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				if (resource.getType() == IResource.PROJECT) {
					// on full builds
					resource.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
					resource.deleteMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);				}
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * cleans up only API usage markers from the given {@link IResource}
	 * @param resource
	 */
	private static void cleanupUsageMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Adds a type to search for dependents of in considered projects for an incremental build
	 * 
	 * @param path
	 */
	private void addDependentsOf(IPath path) {
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
		path = path.setDevice(null);
		String packageName = path.removeLastSegments(1).toString();
		String typeName = path.lastSegment();
		int memberIndex = typeName.indexOf('$');
		if (memberIndex > 0) {
			typeName = typeName.substring(0, memberIndex);
		}
		if (fTypes.add(typeName) && fPackages.add(packageName) && DEBUG) {
			System.out.println("  will look for dependents of " + typeName + " in " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		fCurrentProject = getProject();
		fAnalyzer = getAnalyzer();
		if (fCurrentProject == null || !fCurrentProject.isAccessible() || !fCurrentProject.hasNature(ApiPlugin.NATURE_ID) ||
				hasBeenBuilt(fCurrentProject)) {
			return new IProject[0];
		}
		if (DEBUG) {
			System.out.println("\nStarting build of " + fCurrentProject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		updateMonitor(monitor, 0);
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_builder, 2);
		IProject[] projects = getRequiredProjects(true);
		try {
			switch(kind) {
				case FULL_BUILD : {
					if (DEBUG) {
						System.out.println("Performing full build as requested by user"); //$NON-NLS-1$
					}
					buildAll(localMonitor);
					break;
				}
				case AUTO_BUILD :
				case INCREMENTAL_BUILD : {
					IResourceDelta[] deltas = getDeltas(projects);
					boolean manifestModified = false;
					ManifestVisitor visitor = new ManifestVisitor();
					for (int i = 0; i < deltas.length; i++) {
						deltas[i].accept(visitor);
						if (visitor.isManifiestModified()) {
							manifestModified = true;
							break;
						}
					}
					if (manifestModified) {
						if (DEBUG) {
							System.out.println("Performing full build since MANIFEST.MF was modified"); //$NON-NLS-1$
						}
						buildAll(localMonitor);
					} else if (deltas.length == 0) {
						if (DEBUG) {
							System.out.println("Performing full build since deltas are missing after incremental request"); //$NON-NLS-1$
						}
						buildAll(localMonitor);
					} else {
						State state = (State)JavaModelManager.getJavaModelManager().getLastBuiltState(fCurrentProject, new NullProgressMonitor());
						if (state == null) {
							buildAll(localMonitor);
						} else {
							fBuildState = getLastBuiltState(fCurrentProject);
							if (fBuildState == null) {
								buildAll(localMonitor);
							} else {
								build(state, localMonitor);
							}
						}
					}
					break;
				}
			}
			updateMonitor(monitor, 0);
		} finally {
			fTypes.clear();
			fPackages.clear();
			fTypesToCheck.clear();
			fChangedTypes.clear();
			fProjectToOutputLocations.clear();
			updateMonitor(monitor, 0);
			createMarkers();
			fAnalyzer.dispose();
			localMonitor.done();
			if (fBuildState != null) {
				saveBuiltState(fCurrentProject, fBuildState);
				fBuildState = null;
			}
		}
		if (DEBUG) {
			System.out.println("Finished build of " + fCurrentProject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return projects;
	}
	
	/**
	 * Performs a full build for the project
	 * @param monitor
	 */
	private void buildAll(IProgressMonitor monitor) throws CoreException {
		clearLastState();
		fBuildState = new BuildState();
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 3);
		localMonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_initializing_analyzer, fCurrentProject.getName()));
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		cleanupMarkers(fCurrentProject);
		cleanupUnsupportedTagMarkers(fCurrentProject);
		IPluginModelBase currentModel = getCurrentModel();
		if (currentModel != null) {
			localMonitor.subTask(BuilderMessages.building_workspace_profile);
			IApiProfile wsprofile = getWorkspaceProfile();
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
				fAnalyzer.analyzeComponent(fBuildState, null, profile, apiComponent, null, null, localMonitor);
				updateMonitor(localMonitor, 1);
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
			fCurrentProject.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			fCurrentProject.deleteMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		IApiProblem[] problems = fAnalyzer.getProblems();
		String type = null;
		for(int i = 0; i < problems.length; i++) {
			type = getProblemTypeFromCategory(problems[i].getCategory(), problems[i].getKind());
			if(type == null) {
				continue;
			}
			if(DEBUG) {
				System.out.println("creating marker for: "+problems[i].toString()); //$NON-NLS-1$
			}
			createMarkerForProblem(type, problems[i]);
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
			case IApiProblem.CATEGORY_API_PROFILE: {
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
	private void createMarkerForProblem(String type, IApiProblem problem) {
		IResource resource = resolveResource(problem);
		if(resource == null) {
			return;
		}
		try {
			IMarker marker = resource.createMarker(type);
			marker.setAttributes(
					new String[] {IMarker.MESSAGE,
							IMarker.SEVERITY,
							IMarker.LINE_NUMBER,
							IMarker.CHAR_START,
							IMarker.CHAR_END,
							IMarker.SOURCE_ID,
							IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID},
					new Object[] {problem.getMessage(),
							new Integer(problem.getSeverity()),
							new Integer(problem.getLineNumber()),
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
		IResource resource = fCurrentProject.findMember(new Path(resourcePath));
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
	private void updateMonitor(IProgressMonitor monitor, int ticks) throws OperationCanceledException {
		if(monitor != null) {
			monitor.worked(ticks);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}
	
	/**
	 * Builds an API delta using the default profile (from the workspace settings and the current
	 * workspace profile
	 * @param state
	 * @param monitor
	 */
	private void build(final State state, IProgressMonitor monitor) throws CoreException {
		clearLastState(); // so if the build fails, a full build will be triggered
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 4);
		localMonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_finding_affected_source_files, fCurrentProject.getName()));
		updateMonitor(localMonitor, 0);
		collectAffectedSourceFiles(state);
		updateMonitor(localMonitor, 1);
		if (fTypesToCheck.size() != 0) {
			IPluginModelBase currentModel = getCurrentModel();
			if (currentModel != null) {
				IApiProfile wsprofile = getWorkspaceProfile();
				if (wsprofile == null) {
					if (DEBUG) {
						System.err.println("Could not retrieve a workspace profile"); //$NON-NLS-1$
					}
					return;
				}
				String id = currentModel.getBundleDescription().getSymbolicName();
				IApiComponent apiComponent = wsprofile.getApiComponent(id);
				if(apiComponent == null) {
					return;
				}
				List tnames = new ArrayList(fTypesToCheck.size()),
					 cnames = new ArrayList(fChangedTypes.size());
				collectAllQualifiedNames(fTypesToCheck, fChangedTypes, tnames, cnames, localMonitor);
				updateMonitor(localMonitor, 1);
				fAnalyzer.analyzeComponent(fBuildState, null, profile, apiComponent, (String[])tnames.toArray(new String[tnames.size()]), (String[])cnames.toArray(new String[cnames.size()]), localMonitor);
				updateMonitor(localMonitor, 1);
			}
		}
	}
	
	/**
	 * Returns an array of type names, and cleans up markers for the specified resource
	 * @param alltypes the listing of {@link IFile}s to get qualified names from
	 * @param changedtypes the listing of {@link IFile}s that have actually changed (from the {@link IResourceDelta}
	 * @param tnames the list to collect all type names into (including inner member names)
	 * @param cnames the list to collect the changed type names into
	 * @param monitor
	 */
	private void collectAllQualifiedNames(final HashSet alltypes, final HashSet changedtypes, List tnames, List cnames, final IProgressMonitor monitor) {
		IType[] types = null;
		IFile file = null;
		for (Iterator iterator = alltypes.iterator(); iterator.hasNext(); ) {
			file = (IFile) iterator.next();
			ICompilationUnit unit = (ICompilationUnit) JavaCore.create(file);
			if(!unit.exists()) {
				continue;
			}
			IType type = unit.findPrimaryType();
			if(type == null) {
				continue;
			}
			updateMonitor(monitor, 0);
			if(changedtypes.contains(file)) {
				cleanupUnsupportedTagMarkers(file);
				updateMonitor(monitor, 0);
				cleanupCompatibiltiyMarkers(file);
				updateMonitor(monitor, 0);
				cnames.add(type.getFullyQualifiedName());
			}
			try {
				cleanupUsageMarkers(file);
				updateMonitor(monitor, 0);
				types = unit.getAllTypes();
				for (int i = 0; i < types.length; i++) {
					tnames.add(types[i].getFullyQualifiedName('$'));
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e.getStatus());
			}
			updateMonitor(monitor, 0);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void clean(IProgressMonitor monitor) throws CoreException {
		fCurrentProject = getProject();
		SubMonitor localmonitor = SubMonitor.convert(monitor, MessageFormat.format(BuilderMessages.CleaningAPIDescription, new String[] {fCurrentProject.getName()}), 2);
		try {
			// clean up all existing markers
			cleanupUsageMarkers(fCurrentProject);
			cleanupCompatibiltiyMarkers(fCurrentProject);
			cleanupUnsupportedTagMarkers(fCurrentProject);
			updateMonitor(localmonitor, 1);
			//clean up the .api_settings
			cleanupApiDescription(fCurrentProject);
			updateMonitor(localmonitor, 1);
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Cleans the .api_settings file for the given project
	 * @param project
	 */
	private void cleanupApiDescription(IProject project) {
		if(project != null && project.exists()) {
			ApiDescriptionManager.getDefault().clean(JavaCore.create(project), true, false);
		}
	}
	/**
	 * Collects the complete set of affected source files from the current project context based on the current JDT build state.
	 * 
	 * @param state
	 */
	private void collectAffectedSourceFiles(State state) {
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
		char[][][] internedQualifiedNames = ReferenceCollection.internQualifiedNames(fPackages);
		// if a well known qualified name was found then we can skip over these
		if (internedQualifiedNames.length < fPackages.elementSize) {
			internedQualifiedNames = null;
		}
		char[][] internedSimpleNames = ReferenceCollection.internSimpleNames(fTypes);
		// if a well known name was found then we can skip over these
		if (internedSimpleNames.length < fTypes.elementSize) {
			internedSimpleNames = null;
		}
		Object[] keyTable = state.getReferences().keyTable;
		Object[] valueTable = state.getReferences().valueTable;
		next : for (int i = 0, l = valueTable.length; i < l; i++) {
			String typeLocator = (String) keyTable[i];
			if (typeLocator != null) {
				ReferenceCollection refs = (ReferenceCollection) valueTable[i];
				if (refs.includes(internedQualifiedNames, internedSimpleNames)) {
					IFile file = fCurrentProject.getFile(typeLocator);
					if (file == null) {
						continue next;
					}
					if (DEBUG) {
						System.out.println("  adding affected source file " + typeLocator); //$NON-NLS-1$
					}
					fTypesToCheck.add(file);
				}
			}
		}
	}
	
	

	/**
	 * Finds affected source files for a resource that has changed that either contains class files or is itself a class file
	 * @param binaryDelta
	 */
	private void findAffectedSourceFiles(IResourceDelta binaryDelta) {
		IResource resource = binaryDelta.getResource();
		if(resource.getType() == IResource.FILE) {
			if (Util.isClassFile(resource.getName())) {
				IPath typePath = resolveJavaPathFromResource(resource);
				if(typePath == null) {
					return;
				}
				switch (binaryDelta.getKind()) {
					case IResourceDelta.ADDED :
					case IResourceDelta.REMOVED : {
						if (DEBUG) {
							System.out.println("Found added/removed class file " + typePath); //$NON-NLS-1$
						}
						addDependentsOf(typePath);
						return;
					}
					case IResourceDelta.CHANGED : {
						if ((binaryDelta.getFlags() & IResourceDelta.CONTENT) == 0) {
							return; // skip it since it really isn't changed
						}
						if (DEBUG) {
							System.out.println("Found changed class file " + typePath); //$NON-NLS-1$
						}
						addDependentsOf(typePath);
					}
				}
				return;
			}
		}
	}

	/**
	 * @return the current {@link IPluginModelBase} based on the current project for this builder
	 */
	private IPluginModelBase getCurrentModel() {
		IPluginModelBase[] workspaceModels = PluginRegistry.getWorkspaceModels();
		IPath location = fCurrentProject.getLocation();
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
			System.out.println("Searching for deltas for build of project: "+fCurrentProject.getName()); //$NON-NLS-1$
		}
		ArrayList deltas = new ArrayList();
		IResourceDelta delta = getDelta(fCurrentProject);
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
	 * Returns the API analyzer to use with this instance of the  builder
	 * @return the API analyzer to use
	 */
	protected IApiAnalyzer getAnalyzer() {
		return new BaseApiAnalyzer();
	}
	
	/**
	 * Returns the complete listing of required projects from the classpath of the backing project
	 * @param includeBinaryPrerequisites
	 * @return
	 * @throws CoreException
	 */
	private IProject[] getRequiredProjects(boolean includebinaries) throws CoreException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		if (fCurrentProject == null || workspaceRoot == null) { 
			return new IProject[0];
		}
		ArrayList projects = new ArrayList();
		try {
			IJavaProject javaProject = JavaCore.create(fCurrentProject);
			HashSet blocations = new HashSet();
			blocations.add(javaProject.getOutputLocation());
			fProjectToOutputLocations.put(fCurrentProject, blocations);
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
						fProjectToOutputLocations.put(p, bins);
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
	private IApiProfile getWorkspaceProfile() {
		return ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
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

	/**
	 * Resolves the java path from the given resource
	 * @param resource
	 * @return the resolved path or <code>null</code> if the resource is not part of the java model
	 */
	private IPath resolveJavaPathFromResource(IResource resource) {
		IJavaElement element = JavaCore.create(resource);
		if(element != null) {
			switch(element.getElementType()) {
				case IJavaElement.CLASS_FILE: {
					org.eclipse.jdt.core.IClassFile classfile = (org.eclipse.jdt.core.IClassFile) element;
					IType type = classfile.getType();
					HashSet paths = (HashSet) fProjectToOutputLocations.get(resource.getProject());
					IPath prefix = null;
					for(Iterator iter = paths.iterator(); iter.hasNext();) {
						prefix = (IPath) iter.next();
						if(prefix.isPrefixOf(type.getPath())) {
							return type.getPath().removeFirstSegments(prefix.segmentCount()).removeFileExtension();
						}
					}
					break;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Builder for project: ["+fCurrentProject.getName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
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
	protected static BuildState readState(IProject project) throws CoreException {
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
	private static File getSerializationFile(IProject project) {
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
	private static void saveBuiltState(IProject project, BuildState state) throws CoreException {
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
	private void clearLastState() throws CoreException {
		setLastBuiltState(fCurrentProject, null);
	}
}
