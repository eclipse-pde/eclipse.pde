/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.CoreMessages;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.BundleException;

/**
 * An API component for a plug-in project in the workspace.
 * <p>
 * Note: this class requires a running workspace to be instantiated.
 * </p>
 *
 * @since 1.0.0
 */
public class ProjectComponent extends BundleComponent {

	/**
	 * Constant used to describe the custom build.properties entry
	 *
	 * @since 1.0.3
	 */
	public static final String ENTRY_CUSTOM = "custom"; //$NON-NLS-1$

	/**
	 * Constant used to describe build.properties that start with
	 * <code>extra.</code>
	 *
	 * @since 1.0.3
	 */
	public static final String EXTRA_PREFIX = "extra."; //$NON-NLS-1$

	/**
	 * Associated Java project
	 */
	private final IJavaProject fProject;

	/**
	 * Associated IPluginModelBase object
	 */
	private IPluginModelBase fModel;

	/**
	 * A cache of bundle class path entries to class file containers.
	 */
	private volatile Map<String, IApiTypeContainer> fPathToOutputContainers;

	/**
	 * A cache of output location paths to corresponding class file containers.
	 */
	private volatile Map<IPath, IApiTypeContainer> fOutputLocationToContainer;

	/**
	 * Constructs an API component for the given Java project in the specified
	 * baseline.
	 *
	 * @param baseline the owning API baseline
	 * @param location the given location of the component
	 * @param model the given model
	 * @throws CoreException if unable to create the API component
	 */
	public ProjectComponent(IApiBaseline baseline, String location, IPluginModelBase model, long bundleid) throws CoreException {
		super(baseline, location, bundleid);
		IPath path = IPath.fromOSString(location);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
		this.fProject = JavaCore.create(project);
		this.fModel = model;
	}

	@Override
	protected void setName(String newname) {
		// Override to use the translated name from the plug-in model
		super.setName(fModel.getResourceString(newname));
	}

	@Override
	protected boolean isBinary() {
		return false;
	}

	@Override
	protected BundleDescription getBundleDescription(Map<String, String> manifest, String location, long id) throws BundleException {
		try {
			BundleDescription result = getModel().getBundleDescription();
			if (result == null) {
				throw new BundleException("Cannot find manifest for bundle at " + location); //$NON-NLS-1$
			}
			return result;
		} catch (CoreException ce) {
			throw new BundleException(ce.getMessage());
		}
	}

	/**
	 * Returns the {@link IPluginModelBase} backing this component
	 *
	 * @return the {@link IPluginModelBase} or throws and exception, never
	 *         retruns <code>null</code>
	 */
	IPluginModelBase getModel() throws CoreException {
		if (fModel == null) {
			fModel = PluginRegistry.findModel(fProject.getProject());
			if (fModel == null) {
				abort(NLS.bind(CoreMessages.ProjectComponent_could_not_locate_model, fProject.getElementName()), null);
			}
		}
		return fModel;
	}

	@Override
	protected boolean isApiEnabled() {
		return Util.isApiProject(fProject);
	}

	@Override
	public void dispose() {
		if (isDisposed()) {
			return;
		}
		try {
			if (hasApiFilterStore()) {
				getFilterStore().dispose();
			}
			fModel = null;
			if (fOutputLocationToContainer != null) {
				fOutputLocationToContainer.clear();
				fOutputLocationToContainer = null;
			}
			if (fPathToOutputContainers != null) {
				fPathToOutputContainers.clear();
				fPathToOutputContainers = null;
			}
		} catch (CoreException ce) {
			ApiPlugin.log(ce);
		} finally {
			super.dispose();
		}
	}

	@Override
	protected IApiDescription createLocalApiDescription() throws CoreException {
		long time = System.currentTimeMillis();
		if (Util.isApiProject(getJavaProject())) {
			setHasApiDescription(true);
		}
		IApiDescription apiDesc = ApiDescriptionManager.getManager().getApiDescription(this, getBundleDescription());
		if (ApiPlugin.DEBUG_PROJECT_COMPONENT) {
			System.out.println("Time to create api description for: [" + fProject.getElementName() + "] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return apiDesc;
	}

	@Override
	protected IApiFilterStore createApiFilterStore() throws CoreException {
		long time = System.currentTimeMillis();
		IApiFilterStore store = new ApiFilterStore(getJavaProject());
		if (ApiPlugin.DEBUG_PROJECT_COMPONENT) {
			System.out.println("Time to create api filter store for: [" + fProject.getElementName() + "] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return store;
	}

	@Override
	protected List<IApiTypeContainer> createApiTypeContainers() throws CoreException {
		if (isDisposed()) {
			baselineDisposed(getBaseline());
		}
		// first populate build.properties cache so we can create class file
		// containers
		// from bundle classpath entries
		fPathToOutputContainers = new ConcurrentHashMap<>(4);
		fOutputLocationToContainer = new ConcurrentHashMap<>(4);
		if (fProject.exists() && fProject.getProject().isOpen()) {
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
			if (model != null) {
				createContainersFromProjectModel(model, this, fPathToOutputContainers, fOutputLocationToContainer);
			}
			return super.createApiTypeContainers();
		}
		return Collections.emptyList();
	}

	private static void createContainersFromProjectModel(IPluginModelBase model, ProjectComponent project,
			Map<String, IApiTypeContainer> pathToOutputContainers,
			Map<IPath, IApiTypeContainer> outputLocationToContainer) throws CoreException {
		IBuildModel buildModel = PluginRegistry.createBuildModel(model);
		if (buildModel == null) {
			return;
		}
		IBuild build = buildModel.getBuild();
		IBuildEntry entry = build.getEntry(ENTRY_CUSTOM);
		if (entry != null) {
			String[] tokens = entry.getTokens();
			if (tokens.length == 1 && tokens[0].equals("true")) { //$NON-NLS-1$
				// hack : add the current output location for each
				// classpath entries
				IClasspathEntry[] classpathEntries = project.fProject.getRawClasspath();
				List<IApiTypeContainer> containers = new ArrayList<>();
				for (IClasspathEntry classpathEntrie : classpathEntries) {
					IClasspathEntry classpathEntry = classpathEntrie;
					switch (classpathEntry.getEntryKind())
						{
						case IClasspathEntry.CPE_SOURCE:
							String containerPath = classpathEntry.getPath().removeFirstSegments(1).toString();
							IApiTypeContainer container = getApiTypeContainer(containerPath, project,
									outputLocationToContainer);
							if (container != null && !containers.contains(container)) {
								containers.add(container);
						}
							break;
						case IClasspathEntry.CPE_VARIABLE:
							classpathEntry = JavaCore.getResolvedClasspathEntry(classpathEntry);
							//$FALL-THROUGH$
						case IClasspathEntry.CPE_LIBRARY:
							IPath path = classpathEntry.getPath();
							if (Util.isArchive(path.lastSegment())) {
								IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
								if (resource != null) {
									// jar inside the workspace
									containers.add(
											new ArchiveApiTypeContainer(project, resource.getLocation().toOSString()));
								} else {
									// external jar
									containers.add(new ArchiveApiTypeContainer(project, path.toOSString()));
								}
						}
							break;
						default:
							break;
					}
				}
				if (!containers.isEmpty()) {
					IApiTypeContainer cfc = null;
					if (containers.size() == 1) {
						cfc = containers.get(0);
					} else {
						cfc = new CompositeApiTypeContainer(project, containers);
					}
					pathToOutputContainers.put(".", cfc); //$NON-NLS-1$
				}
			}
		} else {
			IBuildEntry[] entries = build.getBuildEntries();
			int length = entries.length;
			for (int i = 0; i < length; i++) {
				IBuildEntry buildEntry = entries[i];
				String name = buildEntry.getName();
				if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
					retrieveContainers(name, IBuildEntry.JAR_PREFIX, buildEntry, project, pathToOutputContainers,
							outputLocationToContainer);
				} else if (name.startsWith(EXTRA_PREFIX)) {
					retrieveContainers(name, EXTRA_PREFIX, buildEntry, project, pathToOutputContainers,
							outputLocationToContainer);
				}
			}
		}
	}

	private static void retrieveContainers(String name, String prefix, IBuildEntry buildEntry, ProjectComponent project,
			Map<String, IApiTypeContainer> pathToOutputContainers,
			Map<IPath, IApiTypeContainer> outputLocationToContainer) throws CoreException {
		String jar = name.substring(prefix.length());
		String[] tokens = buildEntry.getTokens();
		if (tokens.length == 1) {
			IApiTypeContainer container = getApiTypeContainer(tokens[0], project, outputLocationToContainer);
			if (container != null) {
				IApiTypeContainer existingContainer = pathToOutputContainers.get(jar);
				if (existingContainer != null) {
					// concat both containers
					List<IApiTypeContainer> allContainers = new ArrayList<>();
					allContainers.add(existingContainer);
					allContainers.add(container);
					IApiTypeContainer apiTypeContainer = new CompositeApiTypeContainer(project, allContainers);
					pathToOutputContainers.put(jar, apiTypeContainer);
				} else {
					pathToOutputContainers.put(jar, container);
				}
			}
		} else {
			List<IApiTypeContainer> containers = new ArrayList<>();
			for (String currentToken : tokens) {
				IApiTypeContainer container = getApiTypeContainer(currentToken, project, outputLocationToContainer);
				if (container != null && !containers.contains(container)) {
					containers.add(container);
				}
			}
			if (!containers.isEmpty()) {
				IApiTypeContainer existingContainer = pathToOutputContainers.get(jar);
				if (existingContainer != null) {
					// concat both containers
					containers.add(existingContainer);
				}
				IApiTypeContainer cfc = null;
				if (containers.size() == 1) {
					cfc = containers.get(0);
				} else {
					cfc = new CompositeApiTypeContainer(project, containers);
				}
				pathToOutputContainers.put(jar, cfc);
			}
		}
	}

	@Override
	protected IApiTypeContainer createApiTypeContainer(String path) throws CoreException {
		if (isDisposed() || this.fPathToOutputContainers == null) {
			baselineDisposed(getBaseline());
		}
		IApiTypeContainer container = fPathToOutputContainers.get(path);
		if (container == null) {
		// could be a binary jar included in the plug-in, just look for it
			container = findApiTypeContainer(path);
		}
		return container;
	}

	/**
	 * Finds and returns an existing {@link IApiTypeContainer} at the specified
	 * location in this project, or <code>null</code> if none.
	 *
	 * @param location project relative path to the class file container
	 * @return {@link IApiTypeContainer} or <code>null</code>
	 */
	private IApiTypeContainer findApiTypeContainer(String location) {
		IResource res = fProject.getProject().findMember(IPath.fromOSString(location));
		if (res != null) {
			if (res.getType() == IResource.FILE) {
				return new ArchiveApiTypeContainer(this, res.getLocation().toOSString());
			} else {
				return new DirectoryApiTypeContainer(this, res.getLocation().toOSString());
			}
		}
		return null;
	}

	/**
	 * Finds and returns an {@link IApiTypeContainer} for the specified source
	 * folder, or <code>null</code> if it does not exist. If the source folder
	 * shares an output location with a previous source folder, the output
	 * location is shared (a new one is not created).
	 *
	 * @param location project relative path to the source folder
	 * @return {@link IApiTypeContainer} or <code>null</code>
	 */
	private static IApiTypeContainer getApiTypeContainer(String location, ProjectComponent component,
			Map<IPath, IApiTypeContainer> outputLocationToContainer) throws CoreException {
		IJavaProject project = component.fProject;
		IResource res = project.getProject().findMember(IPath.fromOSString(location));
		if (res != null) {
			IPackageFragmentRoot root = project.getPackageFragmentRoot(res);
			if (root.exists()) {
				if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
					if (res.getType() == IResource.FOLDER) {
						// class file folder
						IPath location2 = res.getLocation();
						IApiTypeContainer cfc = outputLocationToContainer.get(location2);
						if (cfc == null) {
							cfc = new ProjectTypeContainer(component, (IContainer) res);
							outputLocationToContainer.put(location2, cfc);
						}
						return cfc;
					}
				} else {
					IClasspathEntry entry = root.getRawClasspathEntry();
					IPath outputLocation = entry.getOutputLocation();
					if (outputLocation == null) {
						outputLocation = project.getOutputLocation();
					}
					IApiTypeContainer cfc = outputLocationToContainer.get(outputLocation);
					if (cfc == null) {
						IPath projectFullPath = project.getProject().getFullPath();
						IContainer container = null;
						if (projectFullPath.equals(outputLocation)) {
							// The project is its own output location
							container = project.getProject();
						} else {
							container = project.getProject().getWorkspace().getRoot().getFolder(outputLocation);
						}
						cfc = new ProjectTypeContainer(component, container);
						outputLocationToContainer.put(outputLocation, cfc);
					}
					return cfc;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the Java project associated with this component.
	 *
	 * @return associated Java project
	 */
	public IJavaProject getJavaProject() {
		return fProject;
	}

	/**
	 * Returns the cached API type container for the given package fragment
	 * root, or <code>null</code> if none. The given package fragment has to be
	 * a SOURCE package fragment - this method is only used by the project API
	 * description to obtain a class file corresponding to a compilation unit
	 * when tag scanning (to resolve signatures).
	 *
	 * @param root source package fragment root
	 * @return API type container associated with the package fragment root, or
	 *         <code>null</code> if none
	 */
	public IApiTypeContainer getTypeContainer(IPackageFragmentRoot root) throws CoreException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			if (isDisposed()) {
				baselineDisposed(getBaseline());
			}
			getApiTypeContainers(); // ensure initialized
			IResource resource = root.getResource();
			if (resource != null) {
				String location = resource.getProjectRelativePath().toString();
				return getApiTypeContainer(location, this, fOutputLocationToContainer);
			}
		}
		return null;
	}

}
