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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.ReferenceCollection;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.jdt.internal.core.builder.StringSet;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemReporter;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.util.SinceTagVersion;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

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
				return true;
			case IResource.FOLDER:
				return delta.getResource().getProjectRelativePath().isPrefixOf(MANIFEST_PATH); 
			case IResource.FILE:
				if (delta.getResource().getProjectRelativePath().equals(MANIFEST_PATH)) {
					fManifestModified = true;
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
	 * Cleans up markers associated with API tooling on the given resource.
	 * 
	 * @param resource
	 */
	public static void cleanupMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				if (resource.getType() == IResource.PROJECT) {
					// on full builds
					resource.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
					resource.deleteMarkers(IApiMarkerConstants.DEFAULT_API_PROFILE_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);				}
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
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
	 * List of package names to qualify type names
	 */
	private StringSet fPackages = new StringSet(3);
	
	/**
	 * The problem reporter for this builder 
	 */
	private IApiProblemReporter fProblemReporter = null;
	/**
	 * Maps prereq projects to their output location(s)
	 */
	private HashMap fProjectToOutputLocations = new HashMap();
	
	/**
	 * List of type names to lookup for each project context to find dependents of
	 */
	private StringSet fTypes = new StringSet(3);

	/**
	 * The type that we want to check for API problems
	 */
	private ArrayList fTypesToCheck = new ArrayList();
	
	/**
	 * Current build state
	 */
	private BuildState buildState;
	
	/**
	 * List of pending deltas for which the @since tags should be checked
	 */
	private List pendingDeltaInfos = new ArrayList(3);

	private boolean addAPIProblem(IApiProblem problem) {
		if (problem != null) {
			return fProblemReporter.addProblem(problem);
		}
		return false;
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
		fProblemReporter = getProblemReporter();
		fTypesToCheck.clear();
		if (fCurrentProject == null || !fCurrentProject.isAccessible() || !fCurrentProject.hasNature(ApiPlugin.NATURE_ID) ||
				hasBeenBuilt(fCurrentProject)) {
			return new IProject[0];
		}
		if (DEBUG) {
			System.out.println("\nStarting build of " + fCurrentProject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
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
							this.buildState = getLastBuiltState(this.fCurrentProject);
							if (this.buildState == null) {
								buildAll(localMonitor);
							} else {
								buildDeltas(deltas, state, localMonitor);
							}
						}
					}
					break;
				}
			}
			if (monitor != null && monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		} finally {
			fTypes.clear();
			fPackages.clear();
			fProjectToOutputLocations.clear();
			checkDefaultProfileSet();
			if (monitor != null && monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			try {
				fProblemReporter.createMarkers();
			} finally {
				fProblemReporter.dispose();
				localMonitor.done();
			}
			saveBuiltState(this.fCurrentProject, this.buildState);
			this.buildState = null;
		}
		if (DEBUG) {
			System.out.println("Finished build of " + this.fCurrentProject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
			IMarker[] markers = getMarkers(fCurrentProject);
			if (markers.length == 0) {
				// no marker created
				System.out.println("No markers created on: ["+fCurrentProject.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				for (int i = 0, max = markers.length; i < max; i++) {
					System.out.println(markers[i]);
				}
			}
		}
		return projects;
	}
	
	/**
	 * Performs a full build for the workspace
	 * @param monitor
	 */
	private void buildAll(IProgressMonitor monitor) throws CoreException {
		clearLastState();
		this.buildState = new BuildState();
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 3);
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		cleanupMarkers(this.fCurrentProject);
		if (profile == null) {
			return;
		}
		// retrieve all .class files from the current project
		IPluginModelBase currentModel = getCurrentModel();
		if (currentModel != null) {
			localMonitor.subTask(BuilderMessages.building_workspace_profile);
			IApiProfile wsprofile = getWorkspaceProfile();
			localMonitor.worked(1);
			if (monitor != null && monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
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
				localMonitor.subTask(MessageFormat.format(BuilderMessages.checking_compatibility, new String[] {fCurrentProject.getName()}));
				compareProfiles(profile.getApiComponent(id), apiComponent);
				localMonitor.worked(1);
				if (monitor != null && monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				// API usage checks
				IApiSearchScope scope = Factory.newScope(new IApiComponent[]{apiComponent});
				localMonitor.subTask(MessageFormat.format(BuilderMessages.checking_api_usage, new String[] {fCurrentProject.getName()}));
				checkApiUsage(apiComponent, scope, localMonitor);
				localMonitor.worked(1);
				if (monitor != null && monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
		}
	}
	
	/**
	 * Builds an API delta using the default profile (from the workspace settings and the current
	 * workspace profile
	 * @param delta
	 */
	private void buildDeltas(IResourceDelta[] deltas, final State state, IProgressMonitor monitor) throws CoreException {
		clearLastState(); // so if the build fails, a full build will be triggered
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		if (profile == null) {
			return;
		}
		List flattenDeltas = new ArrayList();
		for(int i = 0; i < deltas.length; i++) {
			flatten0(deltas[i], flattenDeltas);
		}
		for (Iterator iterator = flattenDeltas.iterator(); iterator.hasNext();) {
			IResourceDelta resourceDelta = (IResourceDelta) iterator.next();
			if (DEBUG) {
				switch(resourceDelta.getKind()) {
					case IResourceDelta.ADDED :
						System.out.print("ADDED"); //$NON-NLS-1$
						break;
					case IResourceDelta.CHANGED :
						System.out.print("CHANGED"); //$NON-NLS-1$
						break;
					case IResourceDelta.CONTENT :
						System.out.print("CONTENT"); //$NON-NLS-1$
						break;
					case IResourceDelta.REMOVED :
						System.out.print("REMOVED"); //$NON-NLS-1$
						break;
					case IResourceDelta.REPLACED :
						System.out.print("REPLACED"); //$NON-NLS-1$
						break;
				}
				System.out.print(" - "); //$NON-NLS-1$
			}
			IResource resource = resourceDelta.getResource();
			if (DEBUG) {
				System.out.println(resource);
			}
			IPath location = resource.getLocation();
			String fileName = location.lastSegment();
			if(resource.getType() == IResource.FILE) {
				if (Util.isClassFile(fileName)) {
					findAffectedSourceFiles(resourceDelta);
				}
			}
		}
		collectAffectedSourceFiles(state);
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
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
				IApiComponent reference = profile.getApiComponent(id);
				List scopeElements = new ArrayList(); // build search scope for API usage scan
				String className = null;
				IJavaProject javaProject = JavaCore.create(fCurrentProject);
				for (Iterator iterator = fTypesToCheck.iterator(); iterator.hasNext(); ) {
					IFile file = (IFile) iterator.next();
					cleanupMarkers(file);
					ICompilationUnit unit = (ICompilationUnit) JavaCore.create(file);
					if(!unit.exists()) {
						continue;
					}
					IType type = unit.findPrimaryType();
					if(type == null) {
						continue;
					}
					className = type.getFullyQualifiedName();
					if(reference != null && javaProject != null) {
						compareProfiles(javaProject, new String(className), reference, apiComponent);
					}
					if (monitor != null && monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					try {
						IType[] allTypes = unit.getAllTypes();
						for (int i = 0; i < allTypes.length; i++) {
							scopeElements.add(Util.getType(new String(allTypes[i].getFullyQualifiedName('$'))));
						}
					} catch (JavaModelException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
				checkApiUsage(apiComponent, Factory.newTypeScope(apiComponent, (IReferenceTypeDescriptor[]) scopeElements.toArray(new IReferenceTypeDescriptor[scopeElements.size()])), null);
				createSecondaryProblems(reference, apiComponent, javaProject);
				if (monitor != null && monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			fTypesToCheck.clear();
		} else if (DEBUG) {
			System.out.println("No type to check"); //$NON-NLS-1$
		}
	}

	/**
	 * Checks the version number of the API component and creates problem markers as needed
	 * @param javaProject
	 * @param reference
	 * @param component
	 */
	private void checkApiComponentVersion(IApiComponent reference, IApiComponent component) {
		IApiProblem problem = null;
		String refversionval = reference.getVersion();
		String compversionval = component.getVersion();
		Version refversion = new Version(refversionval);
		Version compversion = new Version(compversionval);
		Version newversion = null;
		if (DEBUG) {
			System.out.println("reference version of " + reference.getId() + " : " + refversion); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("component version of " + component.getId() + " : " + compversion); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IDelta[] breakingChanges = this.buildState.getBreakingChanges();
		if (breakingChanges.length != 0) {
			// make sure that the major version has been incremented
			if (compversion.getMajor() <= refversion.getMajor()) {
				newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier());
				problem = createVersionProblem(
						IApiProblem.MAJOR_VERSION_CHANGE,
						new String[] {
							compversionval,
							refversionval
						},
						true,
						String.valueOf(newversion),
						collectDetails(breakingChanges));
			}
		} else {
			IDelta[] compatibleChanges = this.buildState.getCompatibleChanges();
			if (compatibleChanges.length != 0) {
				// only new API have been added
				if (compversion.getMajor() != refversion.getMajor()) {
					// major version should be identical
					newversion = new Version(refversion.getMajor(), compversion.getMinor() + 1, 0, compversion.getQualifier());
					problem = createVersionProblem(
							IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE,
							new String[] {
								compversionval,
								refversionval
							},
							false,
							String.valueOf(newversion),
							collectDetails(breakingChanges));
				} else if (compversion.getMinor() <= refversion.getMinor()) {
					// the minor version should be incremented
					newversion = new Version(compversion.getMajor(), compversion.getMinor() + 1, 0, compversion.getQualifier());
					problem = createVersionProblem(
							IApiProblem.MINOR_VERSION_CHANGE, 
							new String[] {
								compversionval,
								refversionval
							},
							false,
							String.valueOf(newversion),
							collectDetails(breakingChanges));
				}
			}
		}
		addAPIProblem(problem);
	}

	private String collectDetails(IDelta[] deltas) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		for (int i = 0, max = deltas.length; i < max ; i++) {
			printWriter.print("- "); //$NON-NLS-1$
			printWriter.println(deltas[i].getMessage());
		}
		printWriter.flush();
		printWriter.close();
		return String.valueOf(writer.getBuffer());
	}
	/**
	 * Checks for illegal API usage in the specified component, creating problem
	 * markers as required.
	 * 
	 * @param profile profile being analyzed
	 * @param component component being built
	 * @param scope scope being built
	 * @param monitor progress monitor
	 */
	private void checkApiUsage(IApiComponent component, IApiSearchScope scope, IProgressMonitor monitor) {
		if(ignoreApiUsageScan()) {
			return;
		}
		ApiUseAnalyzer analyzer = new ApiUseAnalyzer();
		try {
			long start = System.currentTimeMillis();
			IApiProblem[] illegal = analyzer.findIllegalApiUse(component, scope, monitor);
			long end = System.currentTimeMillis();
			if (DEBUG) {
				System.out.println("API usage scan: " + (end- start) + " ms\t" + illegal.length + " problems"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}		
			if (illegal.length > 0) {
				for (int i = 0; i < illegal.length; i++) {
					addAPIProblem(illegal[i]);
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Checks to see if there is a default API profile set in the workspace,
	 * if not create a marker
	 */
	private void checkDefaultProfileSet() {
		if(ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile() == null) {
			if(DEBUG) {
				System.out.println("No default API profile, adding marker to ["+fCurrentProject.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			IApiProblem problem = ApiProblemFactory.newApiProfileProblem(fCurrentProject.getProjectRelativePath().toPortableString(),
					null,
					new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID},
					new Object[] {new Integer(IApiMarkerConstants.DEFAULT_API_PROFILE_MARKER_ID)},
					-1,
					-1,
					-1,
					IElementDescriptor.T_RESOURCE,
					IApiProblem.API_PROFILE_MISSING);
			addAPIProblem(problem);
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
			cleanupMarkers(fCurrentProject);
			if(!localmonitor.isCanceled()) {
				localmonitor.worked(1);
			}
			//clean up the .api_settings
			cleanupApiDescription(fCurrentProject);
			if(!localmonitor.isCanceled()) {
				localmonitor.worked(1);
			}
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
					if (file == null || fTypesToCheck.contains(file)) {
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
	 * Compares the two given profiles and generates an {@link IDelta}
	 * @param reference
	 * @param component
	 */
	private void compareProfiles(IApiComponent reference, IApiComponent component) {
		long time = System.currentTimeMillis();
		IDelta delta = null;
		if (reference == null) {
			delta = new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.ADDED, IDelta.API_COMPONENT, null, component.getId(), component.getId());
		} else {
			try {
				delta = ApiComparator.compare(reference, component, VisibilityModifiers.API);
			} catch(Exception e) {
				ApiPlugin.log(e);
			} finally {
				if (DEBUG) {
					System.out.println("Time spent for " + component.getId() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				this.pendingDeltaInfos.clear();
			}
		}
		if (delta == null) {
			if (DEBUG) System.err.println("An error occured while comparing"); //$NON-NLS-1$
			return;
		}
		if (delta != ApiComparator.NO_DELTA) {
			List allDeltas = Util.collectAllDeltas(delta);
			if (allDeltas.size() != 0) {
				IJavaProject javaProject = JavaCore.create(this.fCurrentProject);
				if (javaProject == null) return;
				for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
					IDelta localDelta = (IDelta) iterator.next();
					processDelta(javaProject, localDelta, reference, component);
				}
				if (!this.pendingDeltaInfos.isEmpty()) {
					// process the list
					for (Iterator iterator = this.pendingDeltaInfos.iterator(); iterator.hasNext();) {
						Delta currentDelta = (Delta) iterator.next();
						processSinceTags(javaProject, currentDelta, component);
					}
				}
				createSecondaryProblems(reference, component, javaProject);
			}
			if (DEBUG) {
				System.out.println("Complete"); //$NON-NLS-1$
			}
		} else if (DEBUG) {
			System.out.println("No delta"); //$NON-NLS-1$
		}
	}

	private void createSecondaryProblems(IApiComponent reference,
			IApiComponent component, IJavaProject javaProject) {
		if (reference == null || component == null) {
			return;
		}
		checkApiComponentVersion(reference, component);
	}
	
	/**
	 * Compares the given type between the two API components
	 * @param typeName the type to check in each component
	 * @param reference 
	 * @param component
	 */
	private void compareProfiles(IJavaProject javaProject, String typeName, IApiComponent reference, IApiComponent component) {
		ICompilationUnit compilationUnit = null;
		if (DEBUG) {
			System.out.println("comparing profiles ["+reference.getId()+"] and ["+component.getId()+"] for type ["+typeName+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		IResource compilationUnitResource = null;
		try {
			IType type = getType(javaProject, typeName);
			if (type != null) {
				compilationUnit = type.getCompilationUnit();
				if (compilationUnit != null) {
					compilationUnitResource = compilationUnit.getCorrespondingResource();
					if (DEBUG) {
						if (compilationUnitResource != null) {
							IMarker[] markers = getMarkers(compilationUnitResource);
							for (int i = 0, max = markers.length; i < max; i++) {
								System.out.println(markers[i]);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore, we cannot create markers in this case
		}
		IClassFile classFile = null;
		try {
			classFile = component.findClassFile(typeName);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		if (classFile == null) {
			if (DEBUG) {
				System.err.println("Could not retrieve class file for " + typeName + " in " + component.getId()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return;
		}
		this.buildState.cleanup(typeName);
		IDelta delta = null;
		long time = System.currentTimeMillis();
		try {
			delta = ApiComparator.compare(classFile, reference, component, reference.getProfile(), component.getProfile(), VisibilityModifiers.API);
		} catch(Exception e) {
			ApiPlugin.log(e);
		} finally {
			if (DEBUG) {
				System.out.println("Time spent for " + typeName + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			this.pendingDeltaInfos.clear();
		}
		if (delta == null) {
			if (DEBUG) {
				System.err.println("An error occured while comparing"); //$NON-NLS-1$
			}
			return;
		}
		if (delta != ApiComparator.NO_DELTA) {
			List allDeltas = Util.collectAllDeltas(delta);
			for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
				IDelta localDelta = (IDelta) iterator.next();
				processDelta(javaProject, localDelta, reference, component);
			}
			if (!this.pendingDeltaInfos.isEmpty()) {
				// process the list
				for (Iterator iterator = this.pendingDeltaInfos.iterator(); iterator.hasNext();) {
					Delta currentDelta = (Delta) iterator.next();
					processSinceTags(javaProject, currentDelta, component);
				}
			}
			if (DEBUG) {
				System.out.println("Complete"); //$NON-NLS-1$
			}
		} else if (DEBUG) {
			System.out.println("No delta"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Creates an {@link IApiProblem} for the given delta on the backing resource of the specified 
	 * {@link ICompilationUnit}
	 * @param delta
	 * @param compilationUnit
	 * @param project
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private void addCompatibilityProblem(IDelta delta, IJavaProject project, IApiComponent reference, IApiComponent component) {
		if (reference == null) {
			// This would be the case for addition of api component 
			return;
		}
		try {
			Version referenceVersion = new Version(reference.getVersion());
			Version componentVersion = new Version(component.getVersion());
			if (referenceVersion.getMajor() < componentVersion.getMajor()) {
				// API breakage are ok in this case
				this.buildState.addBreakingChange(delta);
				return;
			}
			IResource resource = null;
			IType type = null;
			try {
				type = project.findType(delta.getTypeName().replace('$', '.'));
			} catch (JavaModelException e) {
				ApiPlugin.log(e);
			}
			if (type == null) {
				IResource manifestFile = Util.getManifestFile(this.fCurrentProject);
				if (manifestFile == null) {
					// Cannot retrieve the manifest.mf file
					return;
				}
				resource = manifestFile;
			} else {
				ICompilationUnit unit = type.getCompilationUnit();
				if (unit != null) {
					resource = unit.getCorrespondingResource();
					if (resource == null) {
						return;
					}
				} else {
					IResource manifestFile = Util.getManifestFile(this.fCurrentProject);
					if (manifestFile == null) {
						// Cannot retrieve the manifest.mf file
						return;
					}
					resource = manifestFile;
				}
			}
			// retrieve line number, char start and char end
			int lineNumber = 1;
			int charStart = -1;
			int charEnd = 1;
			IMember member = Util.getIMember(delta, project);
			if (member != null) {
				ISourceRange range = member.getNameRange();
				charStart = range.getOffset();
				charEnd = charStart + range.getLength();
				try {
					IDocument document = Util.getDocument(member.getCompilationUnit());
					lineNumber = document.getLineOfOffset(charStart);
				} catch (BadLocationException e) {
					// ignore
				}
			}
			IApiProblem apiProblem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
					delta.getArguments(),
					new String[] {
						IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
						IApiMarkerConstants.API_MARKER_ATTR_ID
					},
					new Object[] {
						member == null ? null : member.getHandleIdentifier(),
						new Integer(IApiMarkerConstants.COMPATIBILITY_MARKER_ID),
					},
					lineNumber,
					charStart,
					charEnd,
					IApiProblem.CATEGORY_COMPATIBILITY,
					delta.getElementType(),
					delta.getKind(),
					delta.getFlags());
			if (addAPIProblem(apiProblem)) {
				this.buildState.addBreakingChange(delta);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Creates a marker to denote a problem with the since tag (existence or correctness) for a member
	 * and returns it, or <code>null</code>
	 * @param kind
	 * @param messageargs
	 * @param compilationUnit
	 * @param member
	 * @param version
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createSinceTagProblem(int kind, final String[] messageargs, final Delta info, 
			IMember member, final String version) {
		try {
			// create a marker on the member for missing @since tag
			IResource resource = null;
			ICompilationUnit unit = null;
			try {
				unit = member.getCompilationUnit();
				if (unit != null) {
					resource = unit.getCorrespondingResource();
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e);
			}
			if (resource == null) {
				return null;
			}
			int lineNumber = 1;
			int charStart = 0;
			int charEnd = 1;
			ISourceRange range = member.getNameRange();
			charStart = range.getOffset();
			charEnd = charStart + range.getLength();
			try {
				// unit cannot be null
				IDocument document = Util.getDocument(unit);
				lineNumber = document.getLineOfOffset(charStart);
			} catch (BadLocationException e) {
				ApiPlugin.log(e);
			}
			return ApiProblemFactory.newApiSinceTagProblem(resource.getProjectRelativePath().toPortableString(), 
					messageargs, 
					new String[] {IApiMarkerConstants.MARKER_ATTR_VERSION, IApiMarkerConstants.API_MARKER_ATTR_ID, IApiMarkerConstants.MARKER_ATTR_HANDLE_ID}, 
					new Object[] {version, new Integer(IApiMarkerConstants.SINCE_TAG_MARKER_ID), member.getHandleIdentifier()}, 
					lineNumber, 
					charStart, 
					charEnd, 
					info.getElementType(), 
					kind);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	
	/**
	 * Creates a marker on a manifest file for a version numbering problem and returns it
	 * or <code>null</code> 
	 * @param kind
	 * @param messageargs
	 * @param breakage
	 * @param version
	 * @param description the description of details
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createVersionProblem(int kind, final String[] messageargs, boolean breakage, String version, String description) {
		IResource manifestFile = Util.getManifestFile(this.fCurrentProject);
		if (manifestFile == null) {
			// Cannot retrieve the manifest.mf file
			return null;
		}
		// this error should be located on the manifest.mf file
		// first of all we check how many api breakage marker are there
		int lineNumber = -1;
		int charStart = 0;
		int charEnd = 1;
		char[] contents = null;
		if (manifestFile.getType() == IResource.FILE) {
			IFile file = (IFile) manifestFile;
			InputStream inputStream = null;
			LineNumberReader reader = null;
			try {
				inputStream = file.getContents(true);
				contents = Util.getInputStreamAsCharArray(inputStream, -1, IApiCoreConstants.UTF_8);
				reader = new LineNumberReader(new BufferedReader(new StringReader(new String(contents))));
				int lineCounter = 0;
				String line = null;
				loop: while ((line = reader.readLine()) != null) {
					lineCounter++;
					if (line.startsWith(Constants.BUNDLE_VERSION)) {
						lineNumber = lineCounter;
						break loop;
					}
				}
			} catch (CoreException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}
		if (lineNumber != -1 && contents != null) {
			// initialize char start, char end
			int index = CharOperation.indexOf(Constants.BUNDLE_VERSION.toCharArray(), contents, true);
			loop: for (int i = index + Constants.BUNDLE_VERSION.length() + 1, max = contents.length; i < max; i++) {
				char currentCharacter = contents[i];
				if (CharOperation.isWhitespace(currentCharacter)) {
					continue;
				}
				charStart = i;
				break loop;
			}
			loop: for (int i = charStart + 1, max = contents.length; i < max; i++) {
				switch(contents[i]) {
					case '\r' :
					case '\n' :
						charEnd = i;
						break loop;
				}
			}
		} else {
			lineNumber = 1;
		}
		return ApiProblemFactory.newApiVersionNumberProblem(manifestFile.getProjectRelativePath().toPortableString(), 
				messageargs, 
				new String[] {
					IApiMarkerConstants.MARKER_ATTR_VERSION,
					IApiMarkerConstants.API_MARKER_ATTR_ID,
					IApiMarkerConstants.VERSION_NUMBERING_ATTR_DESCRIPTION,
				}, 
				new Object[] {
					version,
					new Integer(IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID),
					description
				}, 
				lineNumber, 
				charStart, 
				charEnd, 
				IElementDescriptor.T_RESOURCE, 
				kind);
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
	 * recursively flattens the given delta into a list of individual deltas
	 * @param delta
	 * @param flattenDeltas
	 */
	private void flatten0(IResourceDelta delta, List flattenDeltas) {
		IResourceDelta[] deltas = delta.getAffectedChildren();
		int length = deltas.length;
		if (length != 0) {
			for (int i = 0; i < length; i++) {
				flatten0(deltas[i], flattenDeltas);
			}
		} else {
			flattenDeltas.add(delta);
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
	 * Returns the complete set of API tooling markers currently on the specified resource
	 * @param resource
	 * @return the complete set of API tooling markers
	 */
	private IMarker[] getMarkers(IResource resource) {
		try {
			if (resource != null && resource.exists()) {
				ArrayList markers = new ArrayList();
				markers.addAll(Arrays.asList(resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE)));
				markers.addAll(Arrays.asList(resource.findMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE)));
				markers.addAll(Arrays.asList(resource.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE)));
				markers.addAll(Arrays.asList(resource.findMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE)));
				markers.addAll(Arrays.asList(resource.findMarkers(IApiMarkerConstants.DEFAULT_API_PROFILE_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE)));
				return (IMarker[]) markers.toArray(new IMarker[markers.size()]);
			}
		} catch(CoreException e) {}
		return new IMarker[0];
	}

	/**
	 * Returns the problem reporter to use for this instance of the builder
	 * @return the problem reporter to use
	 */
	protected IApiProblemReporter getProblemReporter() {
		return new ApiProblemReporter(fCurrentProject);
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
	 * Find a java type within a java project. Converts '$' to '.' as needed
	 * @param javaProject
	 * @param typeName
	 * @return the java type or <code>null</code> if not found
	 * @throws JavaModelException
	 */
	private IType getType(IJavaProject javaProject, String typeName) throws JavaModelException {
		IType type = javaProject.findType(typeName);
		if (type != null) {
			return type;
		}
		if (typeName.indexOf('$') != -1) {
			// might be a member type
			return javaProject.findType(typeName.replace('$', '.'));
		}
		return null;
	}

	/**
	 * @return the workspace {@link IApiProfile}
	 */
	private IApiProfile getWorkspaceProfile() {
		return ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
	}

	/**
	 * @return if the API usage scan should be ignored
	 */
	private boolean ignoreApiUsageScan() {
		boolean ignore = true;
		ignore &= ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_EXTEND, fCurrentProject) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_IMPLEMENT, fCurrentProject) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_INSTANTIATE, fCurrentProject) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.ILLEGAL_REFERENCE, fCurrentProject) == ApiPlugin.SEVERITY_IGNORE;
		return ignore;
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
	 * Processes a delta to know if we need to check for since tag or version numbering problems
	 * @param javaProject
	 * @param delta
	 * @param compilationUnit
	 * @param reference
	 * @param component
	 */
	private void processDelta(IJavaProject javaProject, IDelta delta, IApiComponent reference, IApiComponent component) {
		if (DeltaProcessor.isCompatible(delta)) {
			this.buildState.addCompatibleChange(delta);
			if (delta.getKind() == IDelta.ADDED) {
				int modifiers = delta.getModifiers();
				if (Util.isPublic(modifiers)) {
					// if public, we always want to check @since tags
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
						case IDelta.TYPE :
							if (DEBUG) {
								String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
								System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
							}
							this.pendingDeltaInfos.add(delta);
							break;
					}
				} else if (Util.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
					// if protected, we only want to check @since tags if the enclosing class can be subclassed
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.FIELD :
						case IDelta.TYPE :
							if (DEBUG) {
								String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
								System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
							}
							this.pendingDeltaInfos.add(delta);
							break;
					}
				}
			}
		} else {
			if (delta.getKind() == IDelta.ADDED) {
				// if public, we always want to check @since tags
				switch(delta.getFlags()) {
					case IDelta.TYPE_MEMBER :
					case IDelta.METHOD :
					case IDelta.CONSTRUCTOR :
					case IDelta.ENUM_CONSTANT :
					case IDelta.METHOD_WITH_DEFAULT_VALUE :
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
					case IDelta.FIELD :
						// ensure that there is a @since tag for the corresponding member
						if (delta.getKind() == IDelta.ADDED && Util.isVisible(delta)) {
							if (DEBUG) {
								String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
								System.err.println(deltaDetails + " is not compatible"); //$NON-NLS-1$
							}
							this.pendingDeltaInfos.add(delta);
						}
				}
			}
			addCompatibilityProblem(delta, javaProject, reference, component);
		}
	}

	/**
	 * Processes an {@link IMember} to determine if it needs an @since tag. If it does and one
	 * is not present or the version of the tag is incorrect, a marker is created
	 * @param javaProject
	 * @param compilationUnit
	 * @param member
	 * @param component
	 */
	private void processSinceTags(
			final IJavaProject javaProject,
			final Delta info,
			final IApiComponent component) {
		IMember member = Util.getIMember(info, javaProject);
		if (member == null) {
			return;
		}
		ICompilationUnit cunit = member.getCompilationUnit();
		if (cunit == null) {
			return;
		}
		try {
			if (! cunit.isConsistent()) {
				cunit.makeConsistent(null);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		IApiProblem problem = null;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		String contents = null;
		ISourceRange nameRange = null;
		try {
			contents = cunit.getBuffer().getContents();
			nameRange = member.getNameRange();
		} catch (JavaModelException e) {
			ApiPlugin.log(e);
			return;
		}
		parser.setSource(contents.toCharArray());
		if (nameRange == null) {
			return;
		}
		try {
			int offset = nameRange.getOffset();
			parser.setFocalPosition(offset);
			parser.setResolveBindings(false);
			Map options = javaProject.getOptions(true);
			options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
			parser.setCompilerOptions(options);
			final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
			SinceTagChecker visitor = new SinceTagChecker(offset);
			unit.accept(visitor);
			String componentVersionString = component.getVersion();
			try {
				if (visitor.hasNoComment() || visitor.isMissing()) {
					StringBuffer buffer = new StringBuffer();
					Version componentVersion = new Version(componentVersionString);
					buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
					problem = createSinceTagProblem(IApiProblem.SINCE_TAG_MISSING, null, 
										 info, member, String.valueOf(buffer));
				} else if (visitor.hasJavadocComment()) {
					// we don't want to flag block comment
					String sinceVersion = visitor.getSinceVersion();
					if (sinceVersion != null) {
						/*
						 * Check the validity of the @since version
						 * It cannot be greater than the component version and
						 * it cannot contain more than two fragments.
						 */
						SinceTagVersion tagVersion = new SinceTagVersion(sinceVersion);
						if (Util.getFragmentNumber(sinceVersion) > 2 || tagVersion.getVersion() == null) {
							// @since version cannot have more than 2 fragments
							// create a marker on the member for missing @since tag
							StringBuffer buffer = new StringBuffer();
							if (tagVersion.pluginName() != null) {
								buffer.append(tagVersion.pluginName()).append(' ');
							}
							Version componentVersion = new Version(componentVersionString);
							buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
							problem = createSinceTagProblem(IApiProblem.SINCE_TAG_MALFORMED,
												 new String[] {sinceVersion},
												 info, member, String.valueOf(buffer));
						} else {
							StringBuffer accurateVersionBuffer = new StringBuffer();
							Version componentVersion = new Version(componentVersionString);
							accurateVersionBuffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
							String accurateVersion = String.valueOf(accurateVersionBuffer);
							if (Util.isDifferentVersion(sinceVersion, accurateVersion)) {
								// report invalid version number
								StringBuffer buffer = new StringBuffer();
								if (tagVersion.pluginName() != null) {
									buffer.append(tagVersion.pluginName()).append(' ');
								}
								Version version = new Version(accurateVersion);
								buffer.append(version.getMajor()).append('.').append(version.getMinor());
								String accurateSinceTagValue = String.valueOf(buffer);
								problem = createSinceTagProblem(IApiProblem.SINCE_TAG_INVALID,
													 new String[] {sinceVersion, accurateSinceTagValue},
													 info, member, accurateSinceTagValue);
							}
						}
					}
				}
			} catch (IllegalArgumentException e) {
				ApiPlugin.log(e);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		addAPIProblem(problem);
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
					return BuildState.read(project, in);
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
		if (!project.exists()) return null;
		IPath workingLocation = project.getWorkingLocation(ApiPlugin.PLUGIN_ID);
		return workingLocation.append("state.dat").toFile(); //$NON-NLS-1$
	}

	private static void saveBuiltState(IProject project, BuildState state) throws CoreException {
		if (DEBUG) {
			System.out.println(NLS.bind(BuilderMessages.build_saveStateProgress, project.getName()));
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

	private void clearLastState() throws CoreException {
		setLastBuiltState(this.fCurrentProject, null);
	}
}
