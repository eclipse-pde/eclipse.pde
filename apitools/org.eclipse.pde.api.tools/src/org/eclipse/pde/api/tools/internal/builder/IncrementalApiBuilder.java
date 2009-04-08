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

import java.util.HashSet;
import java.util.Iterator;
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
	HashSet changedtypes = new HashSet(16);
	HashSet removedtypes = new HashSet(16);
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
	 * @param baseline the baseline to compare with
	 * @param wbaseline the workspace baseline
	 * @param deltas the deltas to be built
	 * @param state the current JDT build state
	 * @param buildstate the current API tools build state
	 * @param monitor
	 * @throws CoreException
	 */
	public void build(IApiBaseline baseline, IApiBaseline wbaseline, IResourceDelta[] deltas, State state, BuildState buildstate, IProgressMonitor monitor) throws CoreException {
		IProject project = this.builder.getProject();
		SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(BuilderMessages.IncrementalBuilder_builder_for_project, project.getName()), 10);
		try {
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
			build(project, baseline, wbaseline, state, buildstate, localmonitor.newChild(1));
		}
		finally {
			if(!localmonitor.isCanceled()) {
				localmonitor.done();
			}
			this.changedtypes.clear();
			this.removedtypes.clear();
			this.typenames.clear();
			this.packages.clear();
		}
	}
	
	/**
	 * Builds an API delta using the default profile (from the workspace settings and the current
	 * @param project
	 * @param baseline the baseline to compare to
	 * @param wbaseline the current workspace baseline
	 * @param state the current JDT build state
	 * @param buildstate the current API tools build state
	 * @param monitor
	 */
	void build(final IProject project, IApiBaseline baseline, IApiBaseline wbaseline, final State state, BuildState buildstate, IProgressMonitor monitor) throws CoreException {
		try {
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
					String id = currentModel.getBundleDescription().getSymbolicName();
					IApiComponent comp = wbaseline.getApiComponent(id);
					if(comp == null) {
						return;
					}
					HashSet tnames = new HashSet(typesize),
						 	cnames = new HashSet(typesize);
					collectAllQualifiedNames(project, buildstate, this.changedtypes, tnames, cnames, localmonitor.newChild(1));
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
			if(monitor != null) {
				monitor.done();
			}
		}
	}
	
	/**
	 * Collects the complete set of affected source files from the current project context based on the current JDT build state.
	 * 
	 * @param project the current project being built
	 * @param state the current JDT build state
	 * @param typesToCheck the set of type names that we want to append to
	 */
	void collectAffectedSourceFiles(final IProject project, State state, Set typesToCheck) {
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
		for (int i = 0; i < valueTable.length; i++) {
			typeLocator =  (String) keyTable[i];
			if (typeLocator != null) {
				ReferenceCollection refs = (ReferenceCollection) valueTable[i];
				if (refs.includes(internedQualifiedNames, internedSimpleNames, null)) {
					file = project.getFile(typeLocator);
					if (file == null) {
						continue;
					}
					if (ApiAnalysisBuilder.DEBUG) {
						System.out.println("  adding affected source file " + file.getName()); //$NON-NLS-1$
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
	void findAffectedSourceFiles(IResourceDelta binaryDelta) {
		IResource resource = binaryDelta.getResource();
		if(resource.getType() == IResource.FILE) {
			if (Util.isClassFile(resource.getName())) {
				IPath typePath = resolveJavaPathFromResource(resource);
				if(typePath == null) {
					return;
				}
				switch (binaryDelta.getKind()) {
					case IResourceDelta.REMOVED : {
						if (ApiAnalysisBuilder.DEBUG) {
							System.out.println("Found removed class file " + typePath); //$NON-NLS-1$
						}
						//directly add the removed type
						this.removedtypes.add(typePath.toString().replace('/', '.'));
					}
						//$FALL-THROUGH$
					case IResourceDelta.ADDED : {
						if (ApiAnalysisBuilder.DEBUG) {
							System.out.println("Found added class file " + typePath); //$NON-NLS-1$
						}
						addDependentsOf(typePath);
						return;
					}
					case IResourceDelta.CHANGED : {
						if ((binaryDelta.getFlags() & IResourceDelta.CONTENT) == 0) {
							return; // skip it since it really isn't changed
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
			System.out.println("  will look for dependents of " + typeName + " in " + packageName);  //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Returns an array of type names, and cleans up markers for the specified resource
	 * @param project the project being built
	 * @param state the current build state for the given project 
	 * @param alltypes the listing of {@link IFile}s to get qualified names from
	 * @param changedtypes the listing of {@link IFile}s that have actually changed (from the {@link IResourceDelta}
	 * @param tnames the list to collect all type names into (including inner member names)
	 * @param cnames the list to collect the changed type names into
	 * @param monitor
	 */
	void collectAllQualifiedNames(final IProject project, BuildState state, final HashSet alltypes, HashSet tnames, HashSet cnames, final IProgressMonitor monitor) {
		IType[] types = null;
		IFile file = null;
		ICompilationUnit unit = null;
		IType type = null;
		for (Iterator iterator = alltypes.iterator(); iterator.hasNext(); ) {
			file = (IFile) iterator.next();
			unit = (ICompilationUnit) JavaCore.create(file);
			if(!unit.exists()) {
				continue;
			}
			type = unit.findPrimaryType();
			if(type == null) {
				continue;
			}
			this.builder.cleanupMarkers(file);
			this.builder.updateMonitor(monitor, 0);
			cnames.add(type.getFullyQualifiedName());
			try {
				types = unit.getAllTypes();
				for (int i = 0; i < types.length; i++) {
					if (types[i].isMember()) {
						tnames.add(types[i].getFullyQualifiedName('$'));
					} else {
						tnames.add(types[i].getFullyQualifiedName());
					}
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e.getStatus());
			}
			this.builder.updateMonitor(monitor, 0);
		}
		// inject removed types inside changed type names so that we can properly detect type removal
		String typename = null;
		for(Iterator iter = this.removedtypes.iterator(); iter.hasNext();) {
			typename = (String) iter.next();
			//clean up the state - https://bugs.eclipse.org/bugs/show_bug.cgi?id=271110
			state.cleanup(typename);
			cnames.add(typename);
		}
		IResource resource = project.findMember(ApiAnalysisBuilder.MANIFEST_PATH);
		if (resource != null) {
			try {
				//TODO we should find a way to cache markers to type names, that way to get all
				//the manifest markers for a given type name is time of O(1)
				IMarker[] markers = resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				String tname = null; 
				for (int i = 0; i < markers.length; i++) {
					tname = Util.getTypeNameFromMarker(markers[i]);
					if(tnames.contains(tname) || cnames.contains(tname)) {
						markers[i].delete();
					}
				}
				//TODO we should find a way to cache markers to type names, that way to get all
				//the manifest markers for a given type name is time of O(1)
				markers = resource.findMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				for (int i = 0; i < markers.length; i++) {
					tname = Util.getTypeNameFromMarker(markers[i]);
					if(tnames.contains(tname) || cnames.contains(tname)) {
						markers[i].delete();
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
