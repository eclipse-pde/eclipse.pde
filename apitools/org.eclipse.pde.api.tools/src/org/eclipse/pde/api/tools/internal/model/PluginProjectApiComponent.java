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
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.CompositeApiDescription;
import org.eclipse.pde.api.tools.internal.ProjectApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

/**
 * An API component for a plug-in project in the workspace.
 * <p>
 * Note: this class requires a running workspace to be instantiated.
 * </p>
 * @since 1.0.0
 */
public class PluginProjectApiComponent extends BundleApiComponent {
	
	/**
	 * Constant used for controlling tracing in the plug-in workspace component
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the plug-in workspace component
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
		
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
	 * Constructs an API component for the given Java project in the specified profile.
	 * 
	 * @param profile the owning profile
	 * @param location the given location of the component
	 * @param model the given model
	 * @param project java project
	 * @throws CoreException if unable to create the API component
	 */
	public PluginProjectApiComponent(IApiBaseline profile, String location, IPluginModelBase model) throws CoreException {
		super(profile, location);
		IPath path = new Path(location);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
		this.fProject = JavaCore.create(project);
		this.fModel = model;
		setName(fModel.getResourceString(super.getName()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#isBinaryBundle()
	 */
	protected boolean isBinaryBundle() {
		return false;
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
			if (isApiDescriptionInitialized()) {
				try {
					IApiDescription description = getApiDescription();
					if (description instanceof ProjectApiDescription) {
						((ProjectApiDescription) description).disconnect(getBundleDescription());
					} else if (description instanceof CompositeApiDescription) {
						((CompositeApiDescription) description).disconnect(getBundleDescription());
					}
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
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
		IApiDescription apiDesc = ApiDescriptionManager.getDefault().getApiDescription(this, getBundleDescription());
		if (DEBUG) {
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
		if (DEBUG) {
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
			IFile prop = fProject.getProject().getFile("build.properties"); //$NON-NLS-1$
			if (prop.exists()) {
				WorkspaceBuildModel properties = new WorkspaceBuildModel(prop);
				IBuild build = properties.getBuild();
				IBuildEntry entry = build.getEntry("custom"); //$NON-NLS-1$
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
						if (buildEntry.getName().startsWith(IBuildEntry.JAR_PREFIX)) {
							String jar = buildEntry.getName().substring(IBuildEntry.JAR_PREFIX.length());
							String[] tokens = buildEntry.getTokens();
							if (tokens.length == 1) {
								IApiTypeContainer container = getApiTypeContainer(tokens[0], this);
								if (container != null) {
									fPathToOutputContainers.put(jar, container);
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
					}
				}
			}
			return super.createApiTypeContainers();
		}
		return Collections.EMPTY_LIST;
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
							cfc = new FolderApiTypeContainer(component, (IContainer) res);
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
						if (container.exists()) {
							cfc = new FolderApiTypeContainer(component, container);
							fOutputLocationToContainer.put(outputLocation, cfc);
						}
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
	
}
