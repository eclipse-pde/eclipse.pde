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
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
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
 * @since 1.0.0
 */
public class ProjectComponent extends BundleComponent {
	
	/**
	 * Constant used to describe the custom build.properties entry
	 * @since 1.0.3
	 */
	public static final String ENTRY_CUSTOM = "custom"; //$NON-NLS-1$

	/**
	 * Constant used to describe build.properties that start with <code>extra.</code>
	 * @since 1.0.3
	 */
	public static final String EXTRA_PREFIX = "extra."; //$NON-NLS-1$

	/**
	 * Associated Java project
	 */
	private IJavaProject fProject = null;

	/**
	 * Associated IPluginModelBase object
	 */
	private IPluginModelBase fModel = null;
	
	/**
	 * A cache of bundle class path entries to class file containers.
	 */
	private Map fPathToOutputContainers = null;
	
	/**
	 * A cache of output location paths to corresponding class file containers.
	 */
	private Map fOutputLocationToContainer = null;

	/**
	 * Constructs an API component for the given Java project in the specified baseline.
	 * 
	 * @param baseline the owning API baseline
	 * @param location the given location of the component
	 * @param model the given model
	 * @param bundleid
	 * @throws CoreException if unable to create the API component
	 */
	public ProjectComponent(IApiBaseline baseline, String location, IPluginModelBase model, long bundleid) throws CoreException {
		super(baseline, location, bundleid);
		IPath path = new Path(location);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
		this.fProject = JavaCore.create(project);
		this.fModel = model;
		setName(fModel.getResourceString(super.getName()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#isBinaryBundle()
	 */
	protected boolean isBinary() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.BundleApiComponent#getBundleDescription(java.util.Dictionary, java.lang.String, long)
	 */
	protected BundleDescription getBundleDescription(Dictionary manifest, String location, long id) throws BundleException {
		try {
			return getModel().getBundleDescription();
		}
		catch(CoreException ce) {
			throw new BundleException(ce.getMessage());
		}
	}
	
	/**
	 * Returns the {@link IPluginModelBase} backing this component
	 * @return the {@link IPluginModelBase} or throws and exception, never retruns <code>null</code>
	 * @throws CoreException
	 */
	IPluginModelBase getModel() throws CoreException {
		if(fModel == null) {
			fModel = PluginRegistry.findModel(fProject.getProject());
			if(fModel == null) {
				abort(NLS.bind(CoreMessages.ProjectComponent_could_not_locate_model, fProject.getElementName()), null);
			}
		}
		return fModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#isApiEnabled()
	 */
	protected boolean isApiEnabled() {
		return Util.isApiProject(fProject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#dispose()
	 */
	public void dispose() {
		try {
			if(hasApiFilterStore()) {
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
		} 
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		finally {
			super.dispose();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#createLocalApiDescription()
	 */
	protected IApiDescription createLocalApiDescription() throws CoreException {
		long time = System.currentTimeMillis();
		if(Util.isApiProject(getJavaProject())) {
			setHasApiDescription(true);
		}
		IApiDescription apiDesc = ApiDescriptionManager.getManager().getApiDescription(this, getBundleDescription());
		if (ApiPlugin.DEBUG_PROJECT_COMPONENT) {
			System.out.println("Time to create api description for: ["+fProject.getElementName()+"] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return apiDesc;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#createApiFilterStore()
	 */
	protected IApiFilterStore createApiFilterStore() throws CoreException {
		long time = System.currentTimeMillis();
		IApiFilterStore store = new ApiFilterStore(getJavaProject());
		if (ApiPlugin.DEBUG_PROJECT_COMPONENT) {
			System.out.println("Time to create api filter store for: ["+fProject.getElementName()+"] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return store;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.BundleApiComponent#createClassFileContainers()
	 */
	protected synchronized List createApiTypeContainers() throws CoreException {
		// first populate build.properties cache so we can create class file containers
		// from bundle classpath entries
		fPathToOutputContainers = new HashMap(4);
		fOutputLocationToContainer = new HashMap(4);
		if (fProject.exists() && fProject.getProject().isOpen()) {
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
			if (model != null) {
				IBuildModel buildModel = PluginRegistry.createBuildModel(model);
				if (buildModel != null) {
					IBuild build = buildModel.getBuild();
					IBuildEntry entry = build.getEntry(ENTRY_CUSTOM); 
					if (entry != null) {
						String[] tokens = entry.getTokens();
						if (tokens.length == 1 && tokens[0].equals("true")) { //$NON-NLS-1$
							// hack : add the current output location for each classpath entries
							IClasspathEntry[] classpathEntries = fProject.getRawClasspath();
							List containers = new ArrayList();
							for (int i = 0; i < classpathEntries.length; i++) {
								IClasspathEntry classpathEntry = classpathEntries[i];
								switch(classpathEntry.getEntryKind()) {
									case IClasspathEntry.CPE_SOURCE :
										String containerPath = classpathEntry.getPath().removeFirstSegments(1).toString();
										IApiTypeContainer container = getApiTypeContainer(containerPath, this);
										if (container != null && !containers.contains(container)) {
											containers.add(container);
										}
										break;
									case IClasspathEntry.CPE_VARIABLE :
										classpathEntry = JavaCore.getResolvedClasspathEntry(classpathEntry);
										//$FALL-THROUGH$
									case IClasspathEntry.CPE_LIBRARY :
										IPath path = classpathEntry.getPath();
										if (Util.isArchive(path.lastSegment())) {
											IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
											if (resource != null) {
												// jar inside the workspace
												containers.add(new ArchiveApiTypeContainer(this, resource.getLocation().toOSString()));
											} else {
												// external jar
												containers.add(new ArchiveApiTypeContainer(this, path.toOSString()));
											}
										}
										break;
								}
							}
							if (!containers.isEmpty()) {
								IApiTypeContainer cfc = null;
								if (containers.size() == 1) {
									cfc = (IApiTypeContainer) containers.get(0);
								} else {
									cfc = new CompositeApiTypeContainer(this, containers);
								}
								fPathToOutputContainers.put(".", cfc); //$NON-NLS-1$
							}
						}
					} else {
						IBuildEntry[] entries = build.getBuildEntries();
						int length = entries.length;
						for (int i = 0; i < length; i++) {
							IBuildEntry buildEntry = entries[i];
							String name = buildEntry.getName();
							if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
								retrieveContainers(name, IBuildEntry.JAR_PREFIX, buildEntry);
							} else if (name.startsWith(EXTRA_PREFIX)) { 
								retrieveContainers(name, EXTRA_PREFIX, buildEntry); 
							}
						}
					}
				}
			}
			return super.createApiTypeContainers();
		}
		return Collections.EMPTY_LIST;
	}
	private void retrieveContainers(String name, String prefix, IBuildEntry buildEntry) throws CoreException {
		String jar = name.substring(prefix.length());
		String[] tokens = buildEntry.getTokens();
		if (tokens.length == 1) {
			IApiTypeContainer container = getApiTypeContainer(tokens[0], this);
			if (container != null) {
				IApiTypeContainer existingContainer = (IApiTypeContainer) this.fPathToOutputContainers.get(jar);
				if (existingContainer != null) {
					// concat both containers
					List allContainers = new ArrayList();
					allContainers.add(existingContainer);
					allContainers.add(container);
					IApiTypeContainer apiTypeContainer = new CompositeApiTypeContainer(this, allContainers);
					fPathToOutputContainers.put(jar, apiTypeContainer);
				} else {
					fPathToOutputContainers.put(jar, container);
				}
			}
		} else {
			List containers = new ArrayList();
			for (int j = 0; j < tokens.length; j++) {
				String currentToken = tokens[j];
				IApiTypeContainer container = getApiTypeContainer(currentToken, this);
				if (container != null && !containers.contains(container)) {
					containers.add(container);
				}
			}
			if (!containers.isEmpty()) {
				IApiTypeContainer existingContainer = (IApiTypeContainer) this.fPathToOutputContainers.get(jar);
				if (existingContainer != null) {
					// concat both containers
					containers.add(existingContainer);
				}
				IApiTypeContainer cfc = null;
				if (containers.size() == 1) {
					cfc = (IApiTypeContainer) containers.get(0);
				} else {
					cfc = new CompositeApiTypeContainer(this, containers);
				}
				fPathToOutputContainers.put(jar, cfc);
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#createClassFileContainer(java.lang.String)
	 */
	protected IApiTypeContainer createApiTypeContainer(String path) throws IOException, CoreException {
		if (this.fPathToOutputContainers == null) {
			baselineDisposed(getBaseline());
		}
		IApiTypeContainer container = (IApiTypeContainer) fPathToOutputContainers.get(path);
		if (container == null) {
			// could be a binary jar included in the plug-in, just look for it
			container = findApiTypeContainer(path);
		}
		return container;
	}
	
	/** 
	 * Finds and returns an existing {@link IApiTypeContainer} at the specified location
	 * in this project, or <code>null</code> if none.
	 * 
	 * @param location project relative path to the class file container
	 * @return {@link IApiTypeContainer} or <code>null</code>
	 */
	private IApiTypeContainer findApiTypeContainer(String location) {
		IResource res = fProject.getProject().findMember(new Path(location));
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
	 * Finds and returns an {@link IApiTypeContainer} for the specified
	 * source folder, or <code>null</code> if it does not exist. If the
	 * source folder shares an output location with a previous source
	 * folder, the output location is shared (a new one is not created).
	 * 
	 * @param location project relative path to the source folder
	 * @return {@link IApiTypeContainer} or <code>null</code>
	 */
	private IApiTypeContainer getApiTypeContainer(String location, IApiComponent component) throws CoreException {
		if (this.fOutputLocationToContainer == null) {
			baselineDisposed(getBaseline());
		}
		IResource res = fProject.getProject().findMember(new Path(location));
		if (res != null) {
			IPackageFragmentRoot root = fProject.getPackageFragmentRoot(res);
			if (root.exists()) {
				if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
					if (res.getType() == IResource.FOLDER) {
						// class file folder
						IPath location2 = res.getLocation();
						IApiTypeContainer cfc = (IApiTypeContainer) fOutputLocationToContainer.get(location2);
						if (cfc == null) {
							cfc = new ProjectTypeContainer(component, (IContainer) res);
							fOutputLocationToContainer.put(location2, cfc);
						}
						return cfc;
					}
				} else {
					IClasspathEntry entry = root.getRawClasspathEntry();
					IPath outputLocation = entry.getOutputLocation();
					if (outputLocation == null) {
						outputLocation = fProject.getOutputLocation();
					}
					IApiTypeContainer cfc = (IApiTypeContainer) fOutputLocationToContainer.get(outputLocation);
					if (cfc == null) {
						IPath projectFullPath = fProject.getProject().getFullPath();
						IContainer container = null;
						if (projectFullPath.equals(outputLocation)) {
							// The project is its own output location
							container = fProject.getProject();
						} else {
							container = fProject.getProject().getWorkspace().getRoot().getFolder(outputLocation);
						}
						cfc = new ProjectTypeContainer(component, container);
						fOutputLocationToContainer.put(outputLocation, cfc);
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
	 * Returns the cached API type container for the given package fragment root, or <code>null</code>
	 * if none. The given package fragment has to be a SOURCE package fragment - this method is only
	 * used by the project API description to obtain a class file corresponding to a compilation unit
	 * when tag scanning (to resolve signatures).
	 *  
	 * @param root source package fragment root
	 * @return API type container associated with the package fragment root, or <code>null</code> 
	 * 	if none
	 */
	public IApiTypeContainer getTypeContainer(IPackageFragmentRoot root) throws CoreException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			getApiTypeContainers(); // ensure initialized
			IResource resource = root.getResource();
			if (resource != null) {
				String location = resource.getProjectRelativePath().toString();
				return getApiTypeContainer(location, this);
			}
		}
		return null;
	}
	
}
