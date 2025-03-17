/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.sourcelookup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDEClasspathContainer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.launching.launcher.VMHelper;

public class PDESourceLookupDirector extends AbstractSourceLookupDirector {

	/**
	 * Cache of source containers by location and id (String & String)
	 */
	private final Map<String, List<ISourceContainer>> fSourceContainerMap = new LinkedHashMap<>();

	private ISourceContainer[] fJreSourceContainers;

	private static Set<String> fFilteredTypes;

	static {
		fFilteredTypes = new HashSet<>(3);
		fFilteredTypes.add(ProjectSourceContainer.TYPE_ID);
		fFilteredTypes.add(WorkspaceSourceContainer.TYPE_ID);
		fFilteredTypes.add("org.eclipse.debug.ui.containerType.workingSet"); //$NON-NLS-1$
	}

	/**
	 * Lazily initialized.
	 */
	private double fOSGiRuntimeVersion = Double.MIN_VALUE;

	@Override
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
	}

	@Override
	public boolean supportsSourceContainerType(ISourceContainerType type) {
		return !fFilteredTypes.contains(type.getId());
	}

	@Override
	public Object getSourceElement(Object element) {
		PDESourceLookupQuery query = new PDESourceLookupQuery(this, element);
		SafeRunner.run(query);
		Object result = query.getResult();
		return result != null ? result : super.getSourceElement(element);
	}

	@Override
	public Object[] findSourceElements(Object object) throws CoreException {
		Object[] sourceElements = null;
		if (object instanceof IJavaStackFrame || object instanceof IJavaObject || object instanceof IJavaReferenceType) {
			sourceElements = new Object[] {getSourceElement(object)};
		}
		if (sourceElements == null) {
			sourceElements = super.findSourceElements(object);
		}
		return sourceElements;
	}

	List<ISourceContainer> getSourceContainers(String location, String id) throws CoreException {

		List<ISourceContainer> containers = fSourceContainerMap.get(location);
		if (containers != null) {
			return containers;
		}

		List<IRuntimeClasspathEntry> result = new ArrayList<>();
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry == null) {
			return Collections.emptyList();
		}

		boolean match = false;

		IPluginModelBase[] models = entry.getWorkspaceModels();
		for (IPluginModelBase model : models) {
			if (isPerfectMatch(model, IPath.fromOSString(location))) {
				IResource resource = model.getUnderlyingResource();
				// if the plug-in matches a workspace model,
				// add the project and any libraries not coming via a container
				// to the list of source containers, in that order
				if (resource != null) {
					addProjectSourceContainers(resource.getProject(), result);
				}
				match = true;
				break;
			}
		}

		if (!match) {
			File file = new File(location);
			if (file.isFile()) {
				// in case of linked plug-in projects that map to an external JARd plug-in,
				// use source container that maps to the library in the linked project.
				ISourceContainer container = getArchiveSourceContainer(location);
				if (container != null) {
					containers = List.of(container);
					fSourceContainerMap.put(location, containers);
					return containers;
				}
			}

			models = entry.getExternalModels();
			for (IPluginModelBase model : models) {
				if (isPerfectMatch(model, IPath.fromOSString(location))) {
					// try all source zips found in the source code locations
					IClasspathEntry[] entries = PDEClasspathContainer.getExternalEntries(model);
					for (IClasspathEntry entrie : entries) {
						IRuntimeClasspathEntry rte = convertClasspathEntry(entrie);
						if (rte != null) {
							result.add(rte);
						}
					}
					break;
				}
			}
		}

		IRuntimeClasspathEntry[] entries = result.toArray(IRuntimeClasspathEntry[]::new);
		containers = List.of(JavaRuntime.getSourceContainers(entries));
		fSourceContainerMap.put(location, containers);
		return containers;
	}

	ISourceContainer[] getJreSourceContainers() throws CoreException {
		if (fJreSourceContainers != null) {
			return fJreSourceContainers;
		}

		IRuntimeClasspathEntry unresolvedJreEntry = VMHelper.getJREEntry(getLaunchConfiguration());
		IRuntimeClasspathEntry[] resolvedJreEntries = JavaRuntime.resolveRuntimeClasspathEntry(unresolvedJreEntry, getLaunchConfiguration());
		fJreSourceContainers = JavaRuntime.getSourceContainers(resolvedJreEntries);
		return fJreSourceContainers;
	}

	private boolean isPerfectMatch(IPluginModelBase model, IPath path) {
		return model == null ? false : path.equals(IPath.fromOSString(model.getInstallLocation()));
	}

	private IRuntimeClasspathEntry convertClasspathEntry(IClasspathEntry entry) {
		if (entry == null) {
			return null;
		}

		IPath srcPath = entry.getSourceAttachmentPath();
		if (srcPath != null && srcPath.segmentCount() > 0) {
			IRuntimeClasspathEntry rte = JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath());
			rte.setSourceAttachmentPath(srcPath);
			rte.setSourceAttachmentRootPath(entry.getSourceAttachmentRootPath());
			return rte;
		}
		return null;
	}

	private ISourceContainer getArchiveSourceContainer(String location) throws JavaModelException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile[] containers = root.findFilesForLocationURI(URIUtil.toURI(location));
		for (IFile container : containers) {
			IJavaElement element = JavaCore.create(container);
			if (element instanceof IPackageFragmentRoot archive) {
				IPath path = archive.getSourceAttachmentPath();
				if (path == null || path.segmentCount() == 0) {
					continue;
				}

				IPath rootPath = archive.getSourceAttachmentRootPath();
				boolean detectRootPath = rootPath != null && rootPath.segmentCount() > 0;

				IFile archiveFile = root.getFile(path);
				if (archiveFile.exists()) {
					return new ArchiveSourceContainer(archiveFile, detectRootPath);
				}

				File file = path.toFile();
				if (file.exists()) {
					return new ExternalArchiveSourceContainer(file.getAbsolutePath(), detectRootPath);
				}
			}
		}
		return null;
	}

	private void addProjectSourceContainers(IProject project, List<IRuntimeClasspathEntry> result) throws CoreException {
		if (project == null || !project.hasNature(JavaCore.NATURE_ID)) {
			return;
		}

		IJavaProject jProject = JavaCore.create(project);
		result.add(JavaRuntime.newProjectRuntimeClasspathEntry(jProject));

		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IRuntimeClasspathEntry rte = convertClasspathEntry(entry);
				if (rte != null) {
					result.add(rte);
				}
			}
		}

		// Add additional entries from contributed classpath container resolvers
		IBundleClasspathResolver[] resolvers = PDECore.getDefault().getClasspathContainerResolverManager().getBundleClasspathResolvers(project);
		for (IBundleClasspathResolver resolver : resolvers) {
			result.addAll(resolver.getAdditionalSourceEntries(jProject));
		}
	}

	@Override
	public synchronized void dispose() {
		fSourceContainerMap.values().stream().flatMap(List::stream).forEach(ISourceContainer::dispose);
		fSourceContainerMap.clear();
		super.dispose();
	}

	/**
	 * Returns the version of the OSGi runtime being debugged, based on the target platform.
	 * Cached per source lookup director.
	 *
	 * @return OSGi runtime version
	 */
	double getOSGiRuntimeVersion() {
		if (fOSGiRuntimeVersion == Double.MIN_VALUE) {
			fOSGiRuntimeVersion = TargetPlatformHelper.getTargetVersion();
		}
		return fOSGiRuntimeVersion;
	}

}
