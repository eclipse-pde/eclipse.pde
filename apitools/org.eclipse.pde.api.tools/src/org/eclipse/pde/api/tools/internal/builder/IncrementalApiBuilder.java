/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.builder.ReferenceCollection;
import org.eclipse.jdt.internal.core.builder.State;
import org.eclipse.jdt.internal.core.builder.StringSet;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.TypeAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Version;

/**
 * Used to incrementally build changed Java types
 *
 * @since 3.5
 */
public class IncrementalApiBuilder {

	private static final int UNKNOWN = 0;
	private static final int CLASS_FILE = 1;
	private static final int JAVA__FILE = 2;

	// bit mask for kinds of changes
	private static final int STRUCTURAL = 0x0001;
	private static final int DESCRIPTION = 0x0002;

	class Change {
		int changeKind; // STUCTURAL | DESCRIPTION
		int fileKind; // JAVA | CLASS
		int deltaKind;
		IProject project;
		IFile resource;
		String typeName;

		/**
		 * Creates a change of the specified kinds.
		 *
		 * @param kind bit mask of STUCTURAL | DESCRIPTION
		 * @param deltaKind resource delta kind
		 * @param resource file that changed
		 * @param typeName associated qualified type name
		 * @param fileKind JAVA_FILE or CLASS_FILE
		 */
		Change(int kind, int deltaKind, IFile resource, String typeName, int fileKind) {
			this.changeKind = kind;
			this.deltaKind = deltaKind;
			this.resource = resource;
			this.project = resource.getProject();
			this.typeName = typeName;
			this.fileKind = fileKind;
		}

		/**
		 * IF the given {@link IProject} is the same as the project context, or
		 * is contained in the listing of interesting projects
		 *
		 * @param lproject
		 * @param others
		 * @return <code>true</code> if the project is the same or in the
		 *         interesting list <code>false</code> otherwise
		 */
		boolean isContained(IProject lproject, HashSet<IProject> others) {
			return this.project.equals(lproject) || (others != null && others.contains(this.project));
		}
	}

	/**
	 * Visits a resource delta to collect changes that need to be built
	 */
	class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		List<Change> changes = new ArrayList<>();
		boolean buildpathChanged = false;

		/**
		 * Constructs a new visitor, noting whether the build path of the
		 * project has changed since the last build.
		 *
		 * @param pathChanged
		 */
		ResourceDeltaVisitor(boolean pathChanged) {
			buildpathChanged = pathChanged;
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			switch (delta.getResource().getType()) {
				case IResource.ROOT:
				case IResource.PROJECT:
				case IResource.FOLDER: {
					return true;
				}
				case IResource.FILE: {
					IFile resource = (IFile) delta.getResource();
					String fileName = resource.getName();
					if (Util.isClassFile(fileName)) {
						if (delta.getKind() == IResourceDelta.REMOVED) {
							String typename = resolveTypeName(resource, CLASS_FILE);
							if (typename != null) {
								if (ApiPlugin.DEBUG_BUILDER) {
									System.out.println("ApiAnalysisBuilder: Found removed class file " + typename); //$NON-NLS-1$
								}
								changes.add(new Change(STRUCTURAL, delta.getKind(), resource, typename, CLASS_FILE));
							}
						} else if (buildpathChanged && delta.getKind() == IResourceDelta.CHANGED) {
							if ((delta.getFlags() & IResourceDelta.CONTENT) > 0) {
								// re-building due to build path (class path
								// changes, project add/remove, target platform
								// changes)
								// TODO: for now use structural change, but
								// really, we just need to re-analyze API use in
								// case of
								// required bundle API description changes
								String typename = resolveTypeName(resource, CLASS_FILE);
								if (typename != null) {
									changes.add(new Change(STRUCTURAL, delta.getKind(), resource, typename, CLASS_FILE));
								}
							}
						}

					} else if (Util.isJavaFileName(fileName)) {
						String type = resolveTypeName(resource, JAVA__FILE);
						if (type != null) {
							Change change = new Change(STRUCTURAL, delta.getKind(), resource, type, JAVA__FILE);
							changes.add(change);
							// check if description has changed
							IApiComponent component = workspaceBaseline.getApiComponent(resource.getProject());
							if (component != null) {
								try {
									IApiAnnotations annotations = component.getApiDescription().resolveAnnotations(Factory.typeDescriptor(type.replace('/', '.')));
									if (annotations instanceof TypeAnnotations) {
										TypeAnnotations ta = (TypeAnnotations) annotations;
										if (ta.getBuildStamp() == BuildStamps.getBuildStamp(resource.getProject())) {
											// note description change in
											// addition to structure
											change.changeKind |= DESCRIPTION;
										}
									}
								} catch (CoreException e) {
									ApiPlugin.log(e);
								}
							}
						}
					}
					break;
				}
				default:
					break;
			}
			return false;
		}
	}

	ApiAnalysisBuilder builder = null;
	BuildContext context = null;
	IApiBaseline workspaceBaseline = null;

	/**
	 * Constructor
	 *
	 * @param project the current project context being built
	 * @param delta the {@link IResourceDelta} from the build framework
	 * @param buildstate the current build state from the
	 *            {@link org.eclipse.jdt.internal.core.builder.JavaBuilder}
	 */
	public IncrementalApiBuilder(ApiAnalysisBuilder builder) {
		this.builder = builder;
	}

	/**
	 * Incrementally builds using the
	 * {@link org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer}
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
		this.workspaceBaseline = wbaseline;
		SubMonitor localmonitor = SubMonitor.convert(monitor, NLS.bind(BuilderMessages.IncrementalBuilder_builder_for_project, project.getName()), 1);
		this.context = new BuildContext();
		try {
			String[] projectNames = buildstate.getReexportedComponents();
			HashSet<IProject> depprojects = null;
			if (projectNames.length != 0) {
				depprojects = new HashSet<>();
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IProject pj = null;
				for (String projectName : projectNames) {
					pj = root.getProject(projectName);
					if (pj.isAccessible()) {
						// select only projects that don't exist in the
						// reference baseline
						if (baseline != null && baseline.getApiComponent(projectName) == null) {
							depprojects.add(pj);
						}
					}
				}
			}
			// check if the build path has changed
			long prev = buildstate.getBuildPathCRC();
			long curr = BuildState.computeBuildPathCRC(project);
			ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(curr != prev);
			for (IResourceDelta delta : deltas) {
				delta.accept(visitor);
			}
			buildContext(project, state, visitor.changes, depprojects);
			build(project, baseline, wbaseline, state, buildstate, localmonitor.split(1));
		} catch (OperationCanceledException oce) {
			// do nothing, but don't forward it
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304315
			if (ApiPlugin.DEBUG_BUILDER) {
				System.out.println("ApiAnalysisBuilder: Trapped OperationCanceledException"); //$NON-NLS-1$
			}
		} finally {
			this.context.dispose();
		}
	}

	/**
	 * Builds an API delta using the default baseline (from the workspace
	 * settings and the current
	 *
	 * @param project
	 * @param baseline the baseline to compare to
	 * @param wbaseline the current workspace baseline
	 * @param state the current JDT build state
	 * @param buildstate the current API tools build state
	 * @param monitor
	 */
	void build(final IProject project, final IApiBaseline baseline, final IApiBaseline wbaseline, final State state, BuildState buildstate, IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.api_analysis_on_0, 3);
		localmonitor.subTask(NLS.bind(BuilderMessages.ApiAnalysisBuilder_finding_affected_source_files, project.getName()));
		if (this.context.hasTypes()) {
			IPluginModelBase currentModel = this.builder.getCurrentModel();
			if (currentModel != null) {
				String id = currentModel.getBundleDescription().getSymbolicName();
				IApiComponent comp = wbaseline.getApiComponent(id);
				Set<IApiComponent> apiComponentMultiple = wbaseline.getAllApiComponents(id);
				if (!apiComponentMultiple.isEmpty()) {
					// add the exact match
					for (Iterator<IApiComponent> iterator = apiComponentMultiple.iterator(); iterator.hasNext();) {
						IApiComponent iApiComponent = iterator.next();
						Version workspaceBaselineVersion = new Version(iApiComponent.getVersion());// removes
																									// qualifier
						Version currentProjectVersion = currentModel.getBundleDescription().getVersion();
						if (new Version(currentProjectVersion.getMajor(), currentProjectVersion.getMinor(), currentProjectVersion.getMicro()).compareTo(workspaceBaselineVersion) == 0) {
							comp = iApiComponent;
							break;
						}
					}
				}
				if (comp == null) {
					return;
				}
				extClean(project, buildstate, localmonitor.split(1));
				this.builder.getAnalyzer().analyzeComponent(buildstate, null, null, baseline, comp, this.context, localmonitor.split(1));
				localmonitor.split(1);
				this.builder.createMarkers();
			}
		}
	}

	/**
	 * Records the type name from the given IFile as a dependent type in the
	 * given build context
	 *
	 * @param file
	 * @param kind mask of STRUCTURAL and/or DESCRIPTION
	 */
	private void addDependentTypeToContext(IFile file, int kind) {
		String type = resolveTypeName(file, JAVA__FILE);
		if (type == null) {
			return;
		}
		if ((STRUCTURAL & kind) > 0) {
			if (!this.context.containsStructuralChange(type)) {
				this.builder.cleanupCompatibilityMarkers(file);
			}
		}
		if ((DESCRIPTION & kind) > 0) {
			if (!this.context.containsDescriptionChange(type) && !this.context.containsDescriptionDependent(type)) {
				this.builder.cleanupUsageMarkers(file);
				this.builder.cleanUnusedFilterMarkers(file);
				this.context.recordDescriptionDependent(type);
			}
		}
		addInnerTypesToDependents(file, kind);
	}

	/**
	 * Collects the inner types from the compilation unit
	 *
	 * @param file
	 * @param mask of STRUCTURAL and/or DESCRIPTION
	 */
	private void addInnerTypesToDependents(IFile file, int kind) {
		ICompilationUnit unit = (ICompilationUnit) JavaCore.create(file);
		IType[] types = null;
		try {
			types = unit.getAllTypes();
			String typename = null;
			for (IType type : types) {
				typename = type.getFullyQualifiedName('$');
				if ((DESCRIPTION & kind) > 0) {
					if (!this.context.containsDescriptionChange(typename) && !this.context.containsDescriptionDependent(typename)) {
						this.context.recordDescriptionDependent(typename);
					}
				}
			}
		} catch (JavaModelException jme) {
			// do nothing, just don't consider types
		}
	}

	/**
	 * Collects the inner types from the compilation unit
	 *
	 * @param file
	 * @param mask of STRUCTURAL and/or DESCRIPTION
	 */
	private void addInnerTypes(IFile file, int kind) {
		ICompilationUnit unit = (ICompilationUnit) JavaCore.create(file);
		IType[] types = null;
		try {
			types = unit.getAllTypes();
			String typename = null;
			for (IType type : types) {
				typename = type.getFullyQualifiedName('$');
				if ((STRUCTURAL & kind) > 0) {
					if (!this.context.containsStructuralChange(typename)) {
						this.context.recordStructuralChange(typename);
					}
				}
				if ((DESCRIPTION & kind) > 0) {
					if (!this.context.containsDescriptionChange(typename)) {
						this.context.recordDescriptionChanged(typename);
					}
				}
			}
		} catch (JavaModelException jme) {
			// do nothing, just don't consider types
		}
	}

	/**
	 * Constructs a build context based on the current JDT build state and known
	 * changes.
	 *
	 * @param project the current project being built
	 * @param state the current JDT build state
	 * @param list of changes
	 */
	void buildContext(final IProject project, State state, List<Change> changes, HashSet<IProject> depprojects) {
		StringSet structural = null;
		StringSet description = null;
		for (Change change : changes) {
			boolean contained = change.isContained(project, depprojects);
			if ((change.changeKind & STRUCTURAL) > 0) {
				// don't analyze dependents of removed types
				if (change.deltaKind != IResourceDelta.REMOVED) {
					if (structural == null) {
						structural = new StringSet(16);
					}
					structural.add(change.typeName);
				}
				// only add to structural types if contained in the project
				// being built
				if (contained) {
					context.recordStructuralChange(change.typeName);
					if (change.deltaKind == IResourceDelta.REMOVED) {
						context.recordRemovedType(change.typeName);
					}
				}
			}
			if ((change.changeKind & DESCRIPTION) > 0) {
				if (description == null) {
					description = new StringSet(16);
				}
				description.add(change.typeName);
				// only add to description changes if contained in the project
				// being built
				if (contained) {
					context.recordDescriptionChanged(change.typeName);
				}
			}
			if (contained) {
				if (change.fileKind == JAVA__FILE) {
					this.builder.cleanupMarkers(change.resource);
					addInnerTypes(change.resource, change.changeKind);
				} else {
					// look up the source file
					String path = (String) state.typeLocators.get(change.typeName);
					if (path != null) {
						IResource member = this.builder.getProject().findMember(path);
						if (member != null && member.getType() == IResource.FILE) {
							IFile source = (IFile) member;
							this.builder.cleanupMarkers(source);
							addInnerTypes(source, change.changeKind);
						}
					}
				}
			}
		}
		// only resolve dependents once for case of 1 type changed and is both
		// structural and description
		if (changes.size() == 1 && structural != null && description != null) {
			String[] types = structural.values;
			if (types.length > 0) {
				addDependents(project, state, types, STRUCTURAL | DESCRIPTION);
			}
		} else {
			if (structural != null) {
				String[] types = structural.values;
				if (types.length > 0) {
					addDependents(project, state, types, STRUCTURAL);
				}
			}
			if (description != null) {
				String[] types = description.values;
				if (types.length > 0) {
					addDependents(project, state, types, DESCRIPTION);
				}
			}
		}
	}

	/**
	 * Adds the dependent files from the current build context based on the
	 * current JDT build state to either the structural or description
	 * dependents.
	 *
	 * @param project the current project being built
	 * @param state the current JDT build state
	 * @param types dot and $ qualified names of base types that changed
	 * @param kind mask of STRUCTURAL or DESCRIPTION
	 */
	private void addDependents(final IProject project, State state, String[] types, int kind) {
		StringSet packages = new StringSet(16);
		StringSet typenames = new StringSet(16);
		for (String type : types) {
			if (type != null) {
				splitName(type, packages, typenames);
			}
		}
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are
		// just 'X'
		char[][][] internedQualifiedNames = ReferenceCollection.internQualifiedNames(packages);
		// if a well known qualified name was found then we can skip over these
		if (internedQualifiedNames.length < packages.elementSize) {
			internedQualifiedNames = null;
		}
		char[][] internedSimpleNames = ReferenceCollection.internSimpleNames(typenames, true);
		// if a well known name was found then we can skip over these
		if (internedSimpleNames.length < typenames.elementSize) {
			internedSimpleNames = null;
		}
		Object[] keyTable = state.getReferences().keyTable;
		Object[] valueTable = state.getReferences().valueTable;
		IFile file = null;
		String typeLocator = null;
		for (int i = 0; i < valueTable.length; i++) {
			typeLocator = (String) keyTable[i];
			if (typeLocator != null) {
				ReferenceCollection refs = (ReferenceCollection) valueTable[i];
				if (refs.includes(internedQualifiedNames, internedSimpleNames, null)) {
					file = project.getFile(typeLocator);
					if (file == null) {
						continue;
					}
					if (ApiPlugin.DEBUG_BUILDER) {
						System.out.println("ApiAnalysisBuilder:   adding affected source file " + file.getName()); //$NON-NLS-1$
					}
					addDependentTypeToContext(file, kind);
				}
			}
		}
	}

	/**
	 * Adds a type to search for dependents of in considered projects for an
	 * incremental build
	 *
	 * @param path
	 */
	void splitName(String typename, StringSet packages, StringSet simpleTypes) {
		// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are
		// just 'X'
		int idx = typename.lastIndexOf('/');
		String packageName = (idx < 0 ? Util.EMPTY_STRING : typename.substring(0, idx));
		String typeName = (idx < 0 ? typename : typename.substring(idx + 1, typename.length()));
		idx = typeName.indexOf('$');
		if (idx > 0) {
			typeName = typeName.substring(0, idx);
		}
		if (simpleTypes.add(typeName) && packages.add(packageName) && ApiPlugin.DEBUG_BUILDER) {
			System.out.println("ApiAnalysisBuilder:   will look for dependents of " + typeName + " in " + packageName); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Returns an array of type names, and cleans up markers for the specified
	 * resource
	 *
	 * @param project the project being built
	 * @param state the current build state for the given project
	 * @param monitor
	 */
	void extClean(final IProject project, BuildState state, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
		// clean up the state -
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=271110
		String[] types = this.context.getRemovedTypes();
		for (String type : types) {
			state.cleanup(type);
		}
		subMonitor.split(1);
		IResource resource = project.findMember(ApiAnalysisBuilder.MANIFEST_PATH);
		if (resource != null) {
			try {
				// TODO we should find a way to cache markers to type names,
				// that way to get all
				// the manifest markers for a given type name is time of O(1)
				IMarker[] markers = resource.findMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				String tname = null;
				for (IMarker marker : markers) {
					tname = Util.getTypeNameFromMarker(marker);
					if (this.context.containsStructuralChange(tname)) {
						marker.delete();
					}
				}
				subMonitor.split(1);
				// TODO we should find a way to cache markers to type names,
				// that way to get all
				// the manifest markers for a given type name is time of O(1)
				markers = resource.findMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
				for (IMarker marker : markers) {
					tname = Util.getTypeNameFromMarker(marker);
					if (this.context.containsStructuralChange(tname)) {
						marker.delete();
					}
				}
				subMonitor.split(1);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	/**
	 * Resolves the java path from the given resource
	 *
	 * @param resource
	 * @param kind CLASS_FILE, JAVA_FILE, or UNKNOWN
	 * @return the resolved path or <code>null</code> if the resource is not
	 *         part of the java model
	 */
	String resolveTypeName(IResource resource, int kind) {
		IPath typepath = resource.getFullPath();
		int type = kind;
		if (kind == UNKNOWN) {
			if (Util.isClassFile(resource.getName())) {
				type = CLASS_FILE;
			} else if (Util.isJavaFileName(resource.getName())) {
				type = JAVA__FILE;
			}
		}
		HashSet<IPath> paths = null;
		switch (type) {
			case JAVA__FILE:
				paths = this.builder.src_locs.get(resource.getProject());
				break;
			case CLASS_FILE:
				paths = this.builder.output_locs.get(resource.getProject());
				break;
			default:
				break;
		}
		if (paths != null) {
			for (IPath path : paths) {
				if (path.isPrefixOf(typepath)) {
					typepath = typepath.removeFirstSegments(path.segmentCount()).removeFileExtension();
					return typepath.toString();
				}
			}
		}
		return null;
	}
}
