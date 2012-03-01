/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
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
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;

import com.ibm.icu.text.MessageFormat;

/**
 * Builder for creating API Tools resource markers
 * @since 1.0.0
 */
public class ApiAnalysisBuilder extends IncrementalProjectBuilder {
	/**
	 * Project relative path to the .settings folder
	 * @since 1.0.1
	 */
	static final IPath SETTINGS_PATH = new Path(".settings"); //$NON-NLS-1$
	
	/**
	 * Project relative path to the build.properties file
	 */
	public static final IPath BUILD_PROPERTIES_PATH = new Path("build.properties"); //$NON-NLS-1$

	/**
	 * Project relative path to the manifest file.
	 */
	public static final IPath MANIFEST_PATH = new Path(JarFile.MANIFEST_NAME);
	
	/**
	 * {@link Comparator} to sort {@link ManifestElement}s
	 * @since 1.0.3
	 */
	static final Comparator fgManifestElementComparator = new Comparator() {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof ManifestElement && o2 instanceof ManifestElement) {
				return ((ManifestElement)o1).getValue().compareTo(((ManifestElement)o2).getValue());
			}
			return 0;
		}
	};
	
	/**
	 * Array of header names that we care about when a manifest delta is discovered
	 * @since 1.0.3
	 */
	public static final HashSet IMPORTANT_HEADERS = new HashSet(7);
	
	static {
		IMPORTANT_HEADERS.add(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
		IMPORTANT_HEADERS.add(Constants.BUNDLE_VERSION);
		IMPORTANT_HEADERS.add(Constants.REQUIRE_BUNDLE);
		IMPORTANT_HEADERS.add(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		IMPORTANT_HEADERS.add(Constants.EXPORT_PACKAGE);
		IMPORTANT_HEADERS.add(Constants.IMPORT_PACKAGE);
		IMPORTANT_HEADERS.add(Constants.BUNDLE_CLASSPATH);
	}
	
	/**
	 * Project relative path to the .api_filters file
	 */
	static final IPath FILTER_PATH = SETTINGS_PATH.append(IApiCoreConstants.API_FILTERS_XML_NAME);
	
	/**
	 * Empty listing of projects to be returned by the builder if there is nothing to do
	 */
	static final IProject[] NO_PROJECTS = new IProject[0];

	/**
	 * Constant representing the name of the 'source' attribute on API Tools markers.
	 * Value is <code>API Tools</code>
	 */
	static final String SOURCE = "API Tools"; //$NON-NLS-1$
	
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
	HashMap output_locs = new HashMap();
	
	/**
	 * Maps pre-requisite projects to their source locations
	 */
	HashMap src_locs = new HashMap();
	
	/**
	 * Current build state
	 */
	private BuildState buildstate = null;
	
	/**
	 * Cleans up markers associated with API Tools on the given resource.
	 * 
	 * @param resource
	 */
	void cleanupMarkers(IResource resource) {
		cleanUnusedFilterMarkers(resource);
		cleanupUsageMarkers(resource);
		cleanupCompatibilityMarkers(resource);
		cleanupUnsupportedTagMarkers(resource);
		cleanApiUseScanMarkers(resource);
		cleanupFatalMarkers(resource);
	}
	
	/**
	 * Cleans up API use scan breakage related markers on the specified resource
	 * @param resource
	 */
	void cleanApiUseScanMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				if (ApiPlugin.DEBUG_BUILDER) {
					System.out.println("cleaning api use problems"); //$NON-NLS-1$
				}
				resource.deleteMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);

				IProject project = resource.getProject();
				IMarker[] markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				for (int i = 0; i < markers.length; i++) {
					String typeName = markers[i].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
					IJavaElement adaptor = (IJavaElement) resource.getAdapter(IJavaElement.class);					
					if (adaptor != null && adaptor instanceof ICompilationUnit) {
						IType typeroot = ((ICompilationUnit)adaptor).findPrimaryType();
						if (typeroot != null && typeName != null && typeName.startsWith(typeroot.getFullyQualifiedName())) {
							markers[i].delete();
						}
					}
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * Cleans up unsupported Javadoc tag markers on the specified resource
	 * @param resource
	 */
	void cleanupUnsupportedTagMarkers(IResource resource) {
		try {
			if(resource != null && resource.isAccessible()) {
				if(ApiPlugin.DEBUG_BUILDER) {
					System.out.println("cleaning unsupported tag problems"); //$NON-NLS-1$
				}
				resource.deleteMarkers(IApiMarkerConstants.UNSUPPORTED_TAG_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			}
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
				if(resource.getType() != IResource.PROJECT) {
					IProject pj = resource.getProject();
					if(pj != null) {
						pj.deleteMarkers(IApiMarkerConstants.API_USAGE_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
					}
				}
			}
		} catch(CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * cleans up only fatal problem markers from the given {@link IResource}
	 * @param resource
	 */
	void cleanupFatalMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.FATAL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
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
		if (shouldAbort(this.currentproject)) {
			return NO_PROJECTS;
		}
		// update build time stamp
		BuildStamps.incBuildStamp(this.currentproject);
		if (ApiPlugin.DEBUG_BUILDER) {
			System.out.println("\nApiAnalysis builder - Starting build of " + this.currentproject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_builder, 8);
		IApiBaseline wbaseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
		if (wbaseline == null) {
			if (ApiPlugin.DEBUG_BUILDER) {
				System.err.println("Could not retrieve a workspace baseline");  //$NON-NLS-1$
			}
			return NO_PROJECTS;
		}
		final IProject[] projects = getRequiredProjects(true);
		IApiBaseline baseline = ApiPlugin.getDefault().getApiBaselineManager().getDefaultApiBaseline();
		try {
			switch(kind) {
				case FULL_BUILD : {
					if (ApiPlugin.DEBUG_BUILDER) {
						System.out.println("Performing full build as requested by user"); //$NON-NLS-1$
					}
					buildAll(baseline, wbaseline, localMonitor.newChild(1));
					break;
				}
				case AUTO_BUILD :
				case INCREMENTAL_BUILD : {
					this.buildstate = BuildState.getLastBuiltState(currentproject);
					if (this.buildstate == null) {
						buildAll(baseline, wbaseline, localMonitor.newChild(1));
						break;
					}
					else if(worthDoingFullBuild(projects)) {
						buildAll(baseline, wbaseline, localMonitor.newChild(1));
						break;
					}
					else {
						IResourceDelta[] deltas = getDeltas(projects);
						if(deltas.length < 1) {
							buildAll(baseline, wbaseline, localMonitor.newChild(1));
						}
						else {	
							IResourceDelta filters = null;
							boolean full = false;
							for (int i = 0; i < deltas.length; i++) {
								full = shouldFullBuild(deltas[i]);
								if(full) {
									break;
								}
								filters = deltas[i].findMember(FILTER_PATH);
								if(filters != null){
									switch(filters.getKind()) {
										case IResourceDelta.ADDED:
										case IResourceDelta.REMOVED: {
											full = true;
											break;
										}
										case IResourceDelta.CHANGED: {
											full = (filters.getFlags() & (IResourceDelta.REPLACED | IResourceDelta.CONTENT)) > 0;
											break;
										}
									}
									if(full) {
										break;
									}
								}
							}
							if (full) {
								if (ApiPlugin.DEBUG_BUILDER) {
									System.out.println("Performing full build since MANIFEST.MF or .api_filters was modified"); //$NON-NLS-1$
		 						}
								buildAll(baseline, wbaseline, localMonitor.newChild(1));
							}
							else {
								State state = (State)JavaModelManager.getJavaModelManager().getLastBuiltState(this.currentproject, localMonitor.newChild(1));
								if(state == null) {
									buildAll(baseline, wbaseline, localMonitor.newChild(1));
									break;
								}
								BuildState.setLastBuiltState(this.currentproject, null);
								IncrementalApiBuilder builder = new IncrementalApiBuilder(this);
								builder.build(baseline, wbaseline, deltas, state, this.buildstate, localMonitor.newChild(1));
							}
						}
					}
				}	
			}
			Util.updateMonitor(localMonitor, 0);
		
		}
		catch (OperationCanceledException oce) {
			//do nothing, but don't forward it
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=304315
			if(ApiPlugin.DEBUG_BUILDER) {
				System.out.println("Trapped OperationCanceledException"); //$NON-NLS-1$
			}
		}
		catch(CoreException e) {
			IStatus status = e.getStatus();
			if (status == null || status.getCode() != ApiPlugin.REPORT_BASELINE_IS_DISPOSED) {
				throw e;
			}
			ApiPlugin.log(e);
		} finally {
			try {
				Util.updateMonitor(localMonitor, 0);
				if(this.analyzer != null) {
					this.analyzer.dispose();
					this.analyzer = null;
				}
				if(projects.length < 1) {
					//if this build cycle indicates that more projects need to be built do not close 
					//the baselines yet, they might be re-read by another build cycle
					if(baseline != null) {
						baseline.close();
					}
				}
				Util.updateMonitor(localMonitor, 0);
				if (this.buildstate != null) {
					for(int i = 0, max = projects.length; i < max; i++) {
						IProject project = projects[i];
						if (Util.isApiProject(project)) {
							this.buildstate.addApiToolingDependentProject(project.getName());
						}
					}
					this.buildstate.setBuildPathCRC(BuildState.computeBuildPathCRC(this.currentproject));
					IFile manifest = (IFile) currentproject.findMember(MANIFEST_PATH);
					if(manifest != null && manifest.exists()) {
						try {
							this.buildstate.setManifestState(ManifestElement.parseBundleManifest(manifest.getContents(), null));
						}
						catch (Exception e) {
							ApiPlugin.log(e);
						}
					}
					IPluginModelBase base = PluginRegistry.findModel(currentproject);
					if(base != null) {
						try {
							IBuildModel model = PluginRegistry.createBuildModel(base);
							if(model != null) {
								this.buildstate.setBuildPropertiesState(model);
							}
						}
						catch(CoreException ce) {
							ApiPlugin.log(ce);
						}
					}
					BuildState.saveBuiltState(this.currentproject, this.buildstate);
					this.buildstate = null;
					Util.updateMonitor(monitor, 0);
				}
				if(localMonitor != null) {
					localMonitor.done();
				}
			}
			catch(OperationCanceledException oce) {
				//do nothing, but don't forward it
				//https://bugs.eclipse.org/bugs/show_bug.cgi?id=304315
				if(ApiPlugin.DEBUG_BUILDER) {
					System.out.println("Trapped OperationCanceledException"); //$NON-NLS-1$
				}
			}
		}
		if (ApiPlugin.DEBUG_BUILDER) {
			System.out.println("Finished build of " + this.currentproject.getName() + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return projects;
	}
	
	/**
	 * Returns if the backing project should be fully built, based on the delta
	 * 
	 * @param delta the {@link IResourceDelta} to examine
	 * @return <code>true</code> if the project should have a full build, <code>false</code> otherwise
	 * @since 1.0.3
	 */
	boolean shouldFullBuild(IResourceDelta delta) {
		switch(delta.getKind()) {
			case IResourceDelta.CHANGED: {
				IResourceDelta subdelta = delta.findMember(MANIFEST_PATH);
				if(subdelta != null) {
					IFile file = (IFile) subdelta.getResource();
					return file.getProject().equals(currentproject) && compareManifest(file, buildstate);
				}
				subdelta = delta.findMember(BUILD_PROPERTIES_PATH);
				if(subdelta != null) {
					IFile file = (IFile) subdelta.getResource();
					return file.getProject().equals(currentproject) && compareBuildProperties(buildstate);
				}
				break;
			}
		}
		return false;
	}
	
	/**
	 * Compares the current <code>MANIFEST.MF</code> against the saved state. If the {@link BuildState} is <code>null</code>
	 * or there is no saved state for the current project context a full build is assumed.
	 * 
	 * @param manifest the handle to the <code>MANIFEST.MF</code> file
	 * @param state the current {@link BuildState} or <code>null</code>
	 * @return <code>true</code> if there are changes that require a full build, <code>false</code> otherwise
	 * @since 1.0.3
	 */
	boolean compareManifest(IFile manifest, BuildState state) {
		if(state != null) {
			try {
				Map stateheaders = state.getManifestState();
				if(stateheaders != null) {
					Map headers = ManifestElement.parseBundleManifest(manifest.getContents(), null);
					Entry entry = null;
					for (Iterator i = stateheaders.entrySet().iterator(); i.hasNext();) {
						entry = (Entry) i.next();
						String key = (String) entry.getKey();
						String value = (String) headers.get(key);
						ManifestElement[] e1 = ManifestElement.parseHeader(key, value);
						ManifestElement[] e2 = ManifestElement.parseHeader(key, (String) entry.getValue());
						if(e1 != null && e2!= null && e1.length == e2.length) {
							Arrays.sort(e1, fgManifestElementComparator);
							Arrays.sort(e2, fgManifestElementComparator);
							for (int j = 0; j < e1.length; j++) {
								String[] v1 = e1[j].getValueComponents();
								String[] v2 = e2[j].getValueComponents();
								//compare value bits
								if(v1.length == v2.length) {
									Arrays.sort(v1);
									Arrays.sort(v2);
									for (int k = 0; k < v2.length; k++) {
										if(!v1[k].equals(v2[k])) {
											return true;
										}
									}
								}
								else {
									return true;
								}
								//compare directives
								Enumeration e = e1[j].getDirectiveKeys();
								if(e != null) {
									while (e.hasMoreElements()) {
										String key2 = (String) e.nextElement();
										if(!Util.equalsOrNull(e1[j].getDirective(key2), e2[j].getDirective(key2))) {
											return true;
										}
									}
								}
								//compare attributes
								e = e1[j].getKeys();
								if(e != null) {
									while (e.hasMoreElements()) {
										String key2 = (String) e.nextElement();
										if(!Util.equalsOrNull(e1[j].getAttribute(key2), e2[j].getAttribute(key2))) {
											return true;
										}
									}
								}
							}
						}
						else {
							return true;
						}
					}
					return false;
				}
			}
			catch(Exception e) {
				ApiPlugin.log(e);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Compares the current <code>build.properties</code> against the saved one. If the given {@link BuildState} is <code>null</code>
	 * or there is no saved state for the current project context a full build is assumed.
	 * 
	 * @param state the current {@link BuildState} or <code>null</code> 
	 * @return <code>true</code> if there are changes that require a full build, <code>false</code> otherwise
	 * @since 1.0.3
	 */
	boolean compareBuildProperties(BuildState state) {
		if(state != null) {
			Map map = state.getBuildPropertiesState();
			if(map != null) {
				IPluginModelBase base = PluginRegistry.findModel(currentproject);
				if(base != null) {
					try {
						IBuildModel model = PluginRegistry.createBuildModel(base);
						if(model != null) {
							IBuild ibuild = model.getBuild();
							Entry entry;
							for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
								entry = (Entry) i.next();
								IBuildEntry be = ibuild.getEntry((String) entry.getKey());
								if(be != null && !entry.getValue().equals(Util.deepToString(be.getTokens()))) {
									return true;
								}
							}
						}
					}
					catch(CoreException ce) {
						ApiPlugin.log(ce);
						return false;
					}
				}
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Returns if the builder should abort the build of the given project.
	 * The build decides to abort if one of the following are true:
	 * <ul>
	 * <li>The project is not accessible</li>
	 * <li>The project does not have the API tools nature</li>
	 * <li>The project has already been built - as decided by the build framework</li>
	 * <li>The project has fatal JDT errors that prevent the creation of class files</li>
	 * </ul>
	 * @param project
	 * @return true if the builder should abort building the given project, false otherwise
	 * @throws CoreException
	 * @see {@link #hasBeenBuilt(IProject)}
	 * @see {@link #hasFatalProblems(IProject)}
	 * @since 1.1
	 */
	boolean shouldAbort(IProject project) throws CoreException {
		return !project.isAccessible() || 
			   !project.hasNature(ApiPlugin.NATURE_ID) || 
			   hasBeenBuilt(project) ||
			   hasFatalProblems(project);
	}
	
	/**
	 * Returns if the project we are about to build has fatal JDT problems that prevent 
	 * class files from being built
	 * @param project
	 * @return true if the given project has fatal JDT problems
	 * @see 
	 * @throws CoreException
	 * @see {@link org.eclipse.jdt.core.IJavaModelMarker#BUILDPATH_PROBLEM_MARKER}
	 * @since 1.1
	 */
	boolean hasFatalProblems(IProject project) throws CoreException {
		IMarker[] problems = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		if(problems.length > 0) {
			cleanupMarkers(project);
			IApiProblem problem = ApiProblemFactory.newFatalProblem(
					Path.EMPTY.toString(), 
					new String[] {project.getName()}, 
					IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM);
			createMarkerForProblem(
					IApiProblem.CATEGORY_FATAL_PROBLEM, 
					IApiMarkerConstants.FATAL_PROBLEM_MARKER, 
					problem);
			return true;
		}
		cleanupFatalMarkers(project);
		return false;
	}
	
	/**
	 * if its worth doing a full build considering the given set if projects
	 * @param projects projects to check the build state for
	 * @return true if a full build should take place, false otherwise
	 */
	boolean worthDoingFullBuild(IProject[] projects) {
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
	 * @param baseline the default baseline 
	 * @param wbaseline the workspace baseline
	 * @param monitor
	 */
	void buildAll(IApiBaseline baseline, IApiBaseline wbaseline, IProgressMonitor monitor) throws CoreException {
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 4);
		try {
			BuildState.setLastBuiltState(this.currentproject, null);
			this.buildstate = new BuildState();
			localMonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_initializing_analyzer, currentproject.getName()));
			cleanupMarkers(this.currentproject);
			IPluginModelBase currentModel = getCurrentModel();
			if (currentModel != null) {
				localMonitor.subTask(BuilderMessages.building_workspace_profile);
				Util.updateMonitor(localMonitor, 1);
				String id = currentModel.getBundleDescription().getSymbolicName();
				// Compatibility checks
				IApiComponent apiComponent = wbaseline.getApiComponent(id);
				if(apiComponent != null) {
					getAnalyzer().analyzeComponent(this.buildstate, null, null, baseline, apiComponent, new BuildContext(), localMonitor.newChild(1));
					Util.updateMonitor(localMonitor, 1);
					createMarkers();
					Util.updateMonitor(localMonitor, 1);
				}
			}
		}
		finally {
			if(localMonitor != null) {
				localMonitor.done();
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
			if(ApiPlugin.DEBUG_BUILDER) {
				System.out.println("creating marker for: " + problems[i].toString()); //$NON-NLS-1$
			}
			createMarkerForProblem(category, type, problems[i]);
		}
	}
	
	/**
	 * Returns the {@link IApiMarkerConstants} problem type given the 
	 * problem category
	 * @param category the problem category - see {@link IApiProblem} for problem categories
	 * @param kind the kind of the problem - see {@link IApiProblem} for problem kinds
	 * @return the problem type or <code>null</code>
	 */
	String getProblemTypeFromCategory(int category, int kind) {
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
			case IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM : {
				return IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER;
			}
		}
		return null;
	}

	/**
	 * Creates an {@link IMarker} on the resource specified
	 * in the problem (via its path) with the given problem
	 * attributes
	 * @param category the category of the problem - see {@link IApiProblem} for categories
	 * @param type the marker type to create - see {@link IApiMarkerConstants} for types
	 * @param problem the problem to create a marker from
	 */
	void createMarkerForProblem(int category, String type, IApiProblem problem) {
		IResource resource = resolveResource(problem);
		if(resource == null) {
			return;
		}
		try {
			if (category == IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM) {
				IMarker[] markers = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
				for (int i = 0; i < markers.length; i++) {
					String msg = markers[i].getAttribute(IMarker.MESSAGE, null);
					if (msg == null || msg.equalsIgnoreCase(problem.getMessage())){
						int markerSeverity = markers[i].getAttribute(IMarker.SEVERITY, 0);
						int problemSeverity = ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(problem), this.currentproject);
						if (markerSeverity == problemSeverity) {
							return; // Marker already exists
						}
					} else {
						markers[i].delete(); // create the marker afresh
					}
				}
			}			
			IMarker marker = resource.createMarker(type);
			int line = problem.getLineNumber();
			switch(category) {
				case IApiProblem.CATEGORY_VERSION :
				case IApiProblem.CATEGORY_API_BASELINE :
				case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION : 
				case IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM : {
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
			if (ApiPlugin.DEBUG_BUILDER) {
				System.out.println("Created the marker: " + marker.getId() + " - " + marker.getAttributes().entrySet()); //$NON-NLS-1$ //$NON-NLS-2$
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
	IResource resolveResource(IApiProblem problem) {
		String resourcePath = problem.getResourcePath();
		if (resourcePath == null) {
			return null;
		}
		IResource resource = currentproject.findMember(new Path(resourcePath));
		if(resource == null) {
			//might be re-exported try to look it up
			IJavaProject jp = JavaCore.create(currentproject);
			try {
				IType type = jp.findType(problem.getTypeName());
				if(type != null) {
					return type.getResource();
				}
			}
			catch(JavaModelException jme) {
				//do nothing
			}
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
	String createArgAttribute(String[] args) {
		StringBuffer buff = new StringBuffer();
		for(int i = 0; i < args.length; i++) {
			buff.append(args[i]);
			if(i < args.length-1) {
				buff.append("#"); //$NON-NLS-1$
			}
		}
		return buff.toString();
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
			Util.updateMonitor(localmonitor, 1);
			//clean up the .api_settings
			cleanupApiDescription(this.currentproject);
			Util.updateMonitor(localmonitor, 1);
		}
		finally {
			BuildState.setLastBuiltState(this.currentproject, null);
			localmonitor.done();
		}
	}

	/**
	 * Cleans the .api_settings file for the given project
	 * @param project
	 */
	void cleanupApiDescription(IProject project) {
		if(project != null && project.exists()) {
			ApiDescriptionManager.getManager().clean(JavaCore.create(project), true, false);
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
			else if(ApiPlugin.DEBUG_BUILDER) {
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
	IResourceDelta[] getDeltas(IProject[] projects) {
		if(ApiPlugin.DEBUG_BUILDER) {
			System.out.println("Searching for deltas for build of project: "+this.currentproject.getName()); //$NON-NLS-1$
		}
		ArrayList deltas = new ArrayList();
		IResourceDelta delta = getDelta(this.currentproject);
		if(delta != null) {
			if (ApiPlugin.DEBUG_BUILDER) {
				System.out.println("Found a delta: " + delta); //$NON-NLS-1$
			}
			deltas.add(delta);
		}
		for(int i = 0; i < projects.length; i++) {
			delta = getDelta(projects[i]);
			if(delta != null) {
				if (ApiPlugin.DEBUG_BUILDER) {
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
	 * @return the list of projects required 
	 * @throws CoreException
	 */
	IProject[] getRequiredProjects(boolean includebinaries) throws CoreException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		if (this.currentproject == null || workspaceRoot == null) { 
			return new IProject[0];
		}
		ArrayList projects = new ArrayList();
		try {
			IJavaProject javaProject = JavaCore.create(this.currentproject);
			HashSet blocations = new HashSet();
			blocations.add(javaProject.getOutputLocation());
			this.output_locs.put(this.currentproject, blocations);
			HashSet slocations = new HashSet();
			IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if(roots[i].isArchive()) {
					continue;
				}
				slocations.add(roots[i].getPath());
			}
			this.src_locs.put(this.currentproject, slocations);
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
					HashSet srcs = new HashSet();
					if(javaProject.exists()) {
						bins.add(javaProject.getOutputLocation());
						IClasspathEntry[] source = javaProject.getRawClasspath();
						IPath entrypath = null;
						for(int j = 0; j < source.length; j++) {
							if(source[j].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								srcs.add(source[j].getPath());
								entrypath = source[j].getOutputLocation();
								if(entrypath != null) {
									bins.add(entrypath);
								}
							}
						}
						this.output_locs.put(p, bins);
						this.src_locs.put(p, srcs);
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
	 * Returns the output paths of the given project or <code>null</code> if none have been computed
	 * @param project
	 * @return the output paths for the given project or <code>null</code>
	 */
	HashSet getProjectOutputPaths(IProject project) {
		return (HashSet) this.output_locs.get(project);
	}
	
	/**
	 * Returns is the given classpath entry is optional or not
	 * @param entry
	 * @return true if the specified {@link IClasspathEntry} is optional, false otherwise
	 */
	boolean isOptional(IClasspathEntry entry) {
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
}
