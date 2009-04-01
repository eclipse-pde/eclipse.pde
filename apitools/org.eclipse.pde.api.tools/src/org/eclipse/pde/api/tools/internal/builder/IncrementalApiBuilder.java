/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.builder.ReferenceCollection;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.jdt.internal.core.builder.StringSet;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Used to incrementally build changed Java types
 * 
 * @since 3.5
 */
public class IncrementalApiBuilder {

	/**
	 * Visits a resource delta to collect changes that need to be built
	 */
	class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		HashSet projects = null;
		IProject project = null;
		
		/**
		 * Constructor
		 * @param project
		 * @param projects
		 */
		public ResourceDeltaVisitor(IProject project, HashSet projects) {
			this.project = project;
			this.projects = projects;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			switch (delta.getResource().getType()) {
				case IResource.ROOT:
				case IResource.PROJECT:
				case IResource.FOLDER: {
					return true; 
				}
				case IResource.FILE: {
					IResource resource = delta.getResource();
					String fileName = resource.getName();
					if (Util.isClassFile(fileName)) {
						findAffectedSourceFiles(delta);
					} else if (Util.isJavaFileName(fileName)) {
						IProject project = resource.getProject();
						if (this.project.equals(project)) {
							if (delta.getKind() == IResourceDelta.ADDED) {
								IncrementalApiBuilder.this.addremovedeltas.add(delta);
							}
							IncrementalApiBuilder.this.changedtypes.add(resource);
						} 
						else if (this.projects != null && this.projects.contains(project)) {
							IncrementalApiBuilder.this.changedtypes.add(resource);
						}
					}
				}
			}
			return false;
		}
	}

	ApiAnalysisBuilder builder = null;
	//IProject project = null;
	HashSet changedtypes = new HashSet(16);
	HashSet addremovedeltas = new HashSet(8);
	StringSet typenames = new StringSet(16);
	StringSet packages = new StringSet(16);
	
	
	/**
	 * Constructor
	 * @param project the current project context being built
	 * @param delta the {@link IResourceDelta} from the build framework
	 * @param buildstate the current build state from the {@link org.eclipse.jdt.internal.core.builder.JavaBuilder}
	 */
	public IncrementalApiBuilder(ApiAnalysisBuilder builder) {
		this.builder = builder;
	}
	
	/**
	 * Incrementally builds using the {@link org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer}
	 * from the given {@link ApiAnalysisBuilder}
	 * 
	 * @param baseline
	 * @param deltas
	 * @param monitor
	 * @throws CoreException
	 */
	public void build(IApiBaseline baseline, IResourceDelta[] deltas, BuildState buildstate, IProgressMonitor monitor) throws CoreException {
		IProject project = this.builder.getProject();
		SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(BuilderMessages.IncrementalBuilder_builder_for_project, project.getName()), 10);
		try {
			State state = (State)JavaModelManager.getJavaModelManager().getLastBuiltState(project, localmonitor.newChild(1));
			if(state == null) {
				this.builder.buildAll(baseline, localmonitor);
				return;
			}
			String[] projectNames = buildstate.getReexportedComponents();
			HashSet depprojects = null;
			if (projectNames.length != 0) {
				depprojects = new HashSet();
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IProject pj = null;
				for (int i = 0, max = projectNames.length; i < max; i++) {
					pj = root.getProject(projectNames[i]);
					if (pj.isAccessible()) {
						// select only projects that don't exist in the reference baseline
						if (baseline != null && baseline.getApiComponent(projectNames[i]) == null) {
							depprojects.add(pj);
						}
					}
				}
			}
			ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(project, depprojects);
			for (int i = 0; i < deltas.length; i++) {
				deltas[i].accept(visitor);
			}
			build(project, state, baseline, buildstate, localmonitor.newChild(1));
		}
		finally {
			if(!localmonitor.isCanceled()) {
				localmonitor.done();
			}
			this.changedtypes.clear();
			this.addremovedeltas.clear();
			this.typenames.clear();
			this.packages.clear();
		}
	}
	
	/**
	 * Builds an API delta using the default profile (from the workspace settings and the current
	 * @param project
	 * workspace profile
	 * @param state
	 * @param monitor
	 */
	private void build(final IProject project, final State state, IApiBaseline baseline, BuildState buildstate, IProgressMonitor monitor) throws CoreException {
		IApiBaseline wsprofile = null;
		try {
			// clear the old state so a full build will occur if this one is cancelled or terminates prematurely
			this.builder.clearLastState();
			SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 1);
			collectAffectedSourceFiles(project, state, this.changedtypes);
			int typesize = this.changedtypes.size();
			this.builder.updateMonitor(localmonitor, 1);
			localmonitor.setWorkRemaining(1+(typesize != 0 ? 5 : 0));
			localmonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_finding_affected_source_files, project.getName()));
			this.builder.updateMonitor(localmonitor, 0);
			if (typesize != 0) {
				IPluginModelBase currentModel = this.builder.getCurrentModel();
				if (currentModel != null) {
					wsprofile = this.builder.getWorkspaceProfile();
					if (wsprofile == null) {
						if (ApiAnalysisBuilder.DEBUG) {
							System.err.println("Could not retrieve a workspace profile"); //$NON-NLS-1$
						}
						return;
					}
					String id = currentModel.getBundleDescription().getSymbolicName();
					IApiComponent comp = wsprofile.getApiComponent(id);
					if(comp == null) {
						return;
					}
					List tnames = new ArrayList(typesize),
						 cnames = new ArrayList(typesize);
					collectAllQualifiedNames(project, this.changedtypes, tnames, cnames, localmonitor.newChild(1));
					this.builder.updateMonitor(localmonitor, 1);
					this.builder.getAnalyzer().analyzeComponent(buildstate, 
							null, 
							null, 
							baseline, 
							comp, 
							(String[])tnames.toArray(new String[tnames.size()]), 
							(String[])cnames.toArray(new String[cnames.size()]), 
							localmonitor.newChild(1));
					this.builder.updateMonitor(localmonitor, 1);
					this.builder.createMarkers();
					this.builder.updateMonitor(localmonitor, 1);
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
	 * Collects the complete set of affected source files from the current project context based on the current JDT build state.
	 * 
	 * @param project
	 * @param state
	 * @param typesToCheck
	 */
	private void collectAffectedSourceFiles(final IProject project, State state, Set typesToCheck) {
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
		char[][][] internedQualifiedNames = ReferenceCollection.internQualifiedNames(this.packages);
		// if a well known qualified name was found then we can skip over these
		if (internedQualifiedNames.length < this.packages.elementSize) {
			internedQualifiedNames = null;
		}
		char[][] internedSimpleNames = ReferenceCollection.internSimpleNames(this.typenames, true);
		// if a well known name was found then we can skip over these
		if (internedSimpleNames.length < this.typenames.elementSize) {
			internedSimpleNames = null;
		}
		Object[] keyTable = state.getReferences().keyTable;
		Object[] valueTable = state.getReferences().valueTable;
		IFile file = null;
		String typeLocator = null;
		next : for (int i = 0, l = valueTable.length; i < l; i++) {
			typeLocator =  (String) keyTable[i];
			if (typeLocator != null) {
				ReferenceCollection refs = (ReferenceCollection) valueTable[i];
				if (refs.includes(internedQualifiedNames, internedSimpleNames, null)) {
					file = project.getFile(typeLocator);
					if (file == null) {
						continue next;
					}
					if (ApiAnalysisBuilder.DEBUG) {
						System.out.println("  adding affected source file " + typeLocator); //$NON-NLS-1$
					}
					typesToCheck.add(file);
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
				switch (binaryDelta.getKind()) {
					case IResourceDelta.REMOVED :
						this.addremovedeltas.add(binaryDelta);
						//$FALL-THROUGH$
					case IResourceDelta.ADDED : {
						IPath typePath = resolveJavaPathFromResource(resource);
						if(typePath == null) {
							return;
						}
						if (ApiAnalysisBuilder.DEBUG) {
							System.out.println("Found added/removed class file " + typePath); //$NON-NLS-1$
						}
						addDependentsOf(typePath);
						return;
					}
					case IResourceDelta.CHANGED : {
						if ((binaryDelta.getFlags() & IResourceDelta.CONTENT) == 0) {
							return; // skip it since it really isn't changed
						}
						IPath typePath = resolveJavaPathFromResource(resource);
						if(typePath == null) {
							return;
						}
						if (ApiAnalysisBuilder.DEBUG) {
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
	 * Adds a type to search for dependents of in considered projects for an incremental build
	 * 
	 * @param path
	 */
	void addDependentsOf(IPath path) {
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
		path = path.setDevice(null);
		String packageName = path.removeLastSegments(1).toString();
		String typeName = path.lastSegment();
		int memberIndex = typeName.indexOf('$');
		if (memberIndex > 0) {
			typeName = typeName.substring(0, memberIndex);
		}
		if (this.typenames.add(typeName) && this.packages.add(packageName) && ApiAnalysisBuilder.DEBUG) {
			System.out.println("  will look for dependents of " + typeName + " in " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
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
	private void collectAllQualifiedNames(final IProject project, final HashSet alltypes, List tnames, List cnames, final IProgressMonitor monitor) {
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
			this.builder.updateMonitor(monitor, 0);
			this.builder.cleanupUnsupportedTagMarkers(file);
			this.builder.updateMonitor(monitor, 0);
			this.builder.cleanupCompatibilityMarkers(file);
			this.builder.updateMonitor(monitor, 0);
			cnames.add(type.getFullyQualifiedName());
			try {
				this.builder.cleanupUsageMarkers(file);
				this.builder.updateMonitor(monitor, 0);
				types = unit.getAllTypes();
				String tname = null;
				for (int i = 0; i < types.length; i++) {
					IType type2 = types[i];
					if (type2.isMember()) {
						tname = type2.getFullyQualifiedName('$');
					} else {
						tname = type2.getFullyQualifiedName();
					}
					tnames.add(tname);
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e.getStatus());
			}
			this.builder.updateMonitor(monitor, 0);
		}
		// inject removed types inside changed type names so that we can properly detect type removal
		IResourceDelta delta = null;
		for (Iterator iterator = this.addremovedeltas.iterator(); iterator.hasNext(); ) {
			delta = (IResourceDelta) iterator.next();
			if (delta.getKind() != IResourceDelta.REMOVED) {
				continue;
			}
			IResource resource = delta.getResource();
			IPath typePath = resolveJavaPathFromResource(resource);
			if(typePath == null) {
				continue;
			}
			// record removed type names (package + type)
			StringBuffer buffer = new StringBuffer();
			String[] segments = typePath.segments();
			for (int i = 0, max = segments.length; i < max; i++) {
				if (i > 0) {
					buffer.append('.');
				}
				buffer.append(segments[i]);
			}
			cnames.add(String.valueOf(buffer));
		}
		// clean up markers on added deltas
		if (!this.addremovedeltas.isEmpty()) {
			IResource manifestFile = Util.getManifestFile(project);
			if (manifestFile != null) {
				try {
					IMarker[] markers = manifestFile.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
					for (int i = 0, max = markers.length; i < max; i++) {
						IMarker marker = markers[i];
						String typeName = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_TYPE_NAME, null);
						if (typeName != null) {
							for (Iterator iterator = this.addremovedeltas.iterator(); iterator.hasNext(); ) {
								delta = (IResourceDelta) iterator.next();
								if (delta.getKind() != IResourceDelta.ADDED) {
									continue;
								}
								ICompilationUnit unit = (ICompilationUnit) JavaCore.create(delta.getResource());
								if(!unit.exists()) {
									continue;
								}
								IType type = unit.findPrimaryType();
								if(type == null) {
									continue;
								}
								if (typeName.equals(type.getFullyQualifiedName())) {
									marker.delete();
									return;
								} else {
									// check secondary types
									try {
										types = unit.getAllTypes();
										for (int j = 0; j < types.length; j++) {
											IType type2 = types[i];
											String fullyQualifiedName = null;
											if (type2.isMember()) {
												fullyQualifiedName = type2.getFullyQualifiedName('$');
											} else {
												fullyQualifiedName = type2.getFullyQualifiedName();
											}
											if (typeName.equals(fullyQualifiedName)) {
												marker.delete();
												return;
											}
										}
									} catch (JavaModelException e) {
										ApiPlugin.log(e.getStatus());
									}
								}
							}
						}
					}
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
		}
		IResource resource = project.findMember(ApiAnalysisBuilder.MANIFEST_PATH);
		if (resource != null) {
			try {
				IMarker[] markers = resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				loop: for (int i = 0, max = markers.length; i < max; i++) {
					IMarker marker = markers[i];
					String typeNameFromMarker = Util.getTypeNameFromMarker(marker);
					for (Iterator iterator = tnames.iterator(); iterator.hasNext(); ) {
						String typeName = (String) iterator.next();
						if (typeName.equals(typeNameFromMarker)) {
							marker.delete();
							continue loop;
						}
					}
				}
				markers = resource.findMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				loop: for (int i = 0, max = markers.length; i < max; i++) {
					IMarker marker = markers[i];
					String typeNameFromMarker = Util.getTypeNameFromMarker(marker);
					for (Iterator iterator = tnames.iterator(); iterator.hasNext(); ) {
						String typeName = (String) iterator.next();
						if (typeName.equals(typeNameFromMarker)) {
							marker.delete();
							continue loop;
						}
					}
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}
	
	/**
	 * Resolves the java path from the given resource
	 * @param resource
	 * @return the resolved path or <code>null</code> if the resource is not part of the java model
	 */
	IPath resolveJavaPathFromResource(IResource resource) {
		IJavaElement element = JavaCore.create(resource);
		if(element != null) {
			switch(element.getElementType()) {
				case IJavaElement.CLASS_FILE: {
					org.eclipse.jdt.core.IClassFile classfile = (org.eclipse.jdt.core.IClassFile) element;
					IType type = classfile.getType();
					HashSet paths = this.builder.getProjectOutputPaths(resource.getProject());
					if(paths == null) {
						return null;
					}
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
}
