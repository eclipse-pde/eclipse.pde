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
package org.eclipse.pde.api.tools.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
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
	 * Constructs an API component for the given Java project in the specified profile.
	 * 
	 * @param profile the owning profile
	 * @param location the given location of the component
	 * @param model the given model
	 * @param project java project
	 * @throws CoreException if unable to create the API component
	 */
	public PluginProjectApiComponent(IApiProfile profile, String location, IPluginModelBase model) throws CoreException {
		super(profile,location);
		IPath path = new Path(location);
		IProject project = ApiProfile.ROOT.getProject(path.lastSegment());
		this.fProject = JavaCore.create(project);
		this.fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#isBinaryBundle()
	 */
	protected boolean isBinaryBundle() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#dispose()
	 */
	public void dispose() {
		try {
			if (hasApiDescription()) {
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
		IApiDescription apiDesc = null;
		if(Util.isApiProject(getJavaProject())) {
			apiDesc = ApiDescriptionManager.getDefault().getApiDescription(getJavaProject(), getBundleDescription());
		}
		else {
			apiDesc = super.createLocalApiDescription();
		}
		if (DEBUG) {
			System.out.println("Time to create api description for: ["+fProject.getElementName()+"] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return apiDesc;
	}
	
	/**
	 * Processes all source tags in this project, annotating the given API description.
	 * 
	 * @param apiDescription API description
	 * @throws CoreException if something goes wrong
	 */
	protected void loadSourceTags(IApiDescription apiDescription) throws CoreException {
		List sourceRoots = new ArrayList();
		if (fProject.exists() && fProject.getProject().isOpen()) {
			IClasspathEntry entries[] = fProject.getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry classpathEntry = entries[i];
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPackageFragmentRoot[] roots = fProject.findPackageFragmentRoots(classpathEntry);
					for (int j = 0; j < roots.length; j++) {
						sourceRoots.add(roots[j]);
					}
				}
			}
		}
		TagScanner scanner = TagScanner.newScanner();
		Iterator iterator = sourceRoots.iterator();
		while (iterator.hasNext()) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) iterator.next();
			IJavaElement[] pkgs = root.getChildren();
			for (int i = 0; i < pkgs.length; i++) {
				if (pkgs[i] instanceof IPackageFragment) {
					IPackageFragment pkg = (IPackageFragment) pkgs[i];
					ICompilationUnit[] units = pkg.getCompilationUnits();
					for (int j = 0; j < units.length; j++) {
						ICompilationUnit unit = units[j];
						CompilationUnit cu = new CompilationUnit(unit.getResource().getLocation().toOSString());
						try {
							scanner.scan(cu, apiDescription, this);
						} catch (CoreException e) {
							abort("Unable to initialize from Javadoc tags", e); //$NON-NLS-1$
						}
					}
				}
			}
		}
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
	protected List createClassFileContainers() throws CoreException {
		// first populate build.properties cache so we can create class file containers
		// from bundle classpath entries
		fPathToOutputContainers = new HashMap(8);
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
									IClassFileContainer container = getSourceFolderContainer(containerPath, this.getId());
									if (container != null) {
										containers.add(container);
									}
									break;
								case IClasspathEntry.CPE_VARIABLE :
									classpathEntry = JavaCore.getResolvedClasspathEntry(classpathEntry);
								case IClasspathEntry.CPE_LIBRARY :
									IPath path = classpathEntry.getPath();
									if (Util.isArchive(path.lastSegment())) {
										IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
										if (resource != null) {
											// jar inside the workspace
											containers.add(new ArchiveClassFileContainer(resource.getLocation().toOSString(), this.getId()));
										} else {
											// external jar
											containers.add(new ArchiveClassFileContainer(path.toOSString(), this.getId()));
										}
									}
									break;
							}
						}
						if (containers.size() != 0) {
							fPathToOutputContainers.put(".", new CompositeClassFileContainer(containers, this.getId())); //$NON-NLS-1$
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
								IClassFileContainer container = getSourceFolderContainer(tokens[0], this.getId());
								if (container != null) {
									fPathToOutputContainers.put(jar, container);
								}
							} else {
								List containers = new ArrayList();
								for (int j = 0; j < tokens.length; j++) {
									String currentToken = tokens[j];
									IClassFileContainer container = getSourceFolderContainer(currentToken, this.getId());
									if (container != null) {
										containers.add(container);
									}
								}
								if (!containers.isEmpty()) {
									fPathToOutputContainers.put(jar, new CompositeClassFileContainer(containers, this.getId()));
								}
							}
						}
					}
				}
			}
			return super.createClassFileContainers();
		}
		return Collections.EMPTY_LIST;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#createClassFileContainer(java.lang.String)
	 */
	protected IClassFileContainer createClassFileContainer(String path) throws IOException {
		IClassFileContainer container = (IClassFileContainer) fPathToOutputContainers.get(path);
		if (container == null) {
			// could be a binary jar included in the plug-in, just look for it
			container = findClassFileContainer(path);
		}
		return container;
	}
	
	/** 
	 * Finds and returns an existing class file container at the specified location
	 * in this project, or <code>null</code> if none.
	 * 
	 * @param location project relative path to the class file container
	 * @return class file container or null
	 */
	private IClassFileContainer findClassFileContainer(String location) {
		IResource res = fProject.getProject().findMember(new Path(location));
		if (res != null) {
			if (res.getType() == IResource.FILE) {
				return new ArchiveClassFileContainer(res.getLocation().toOSString(), this.getId());
			} else {
				return new DirectoryClassFileContainer(res.getLocation().toOSString(), this.getId());
			}
		}
		return null;
	}
	
	/** 
	 * Finds and returns a class file container for the specified
	 * source folder, or <code>null</code> if it does not exist.
	 * 
	 * @param location project relative path to the source folder
	 * @return class file container or <code>null</code>
	 */
	private IClassFileContainer getSourceFolderContainer(String location, String id) {
		IResource res = fProject.getProject().findMember(new Path(location));
		if (res != null) {
			IPackageFragmentRoot root = fProject.getPackageFragmentRoot(res);
			if (root.exists()) {
				return new SourceFolderClassFileContainer(root, id);
			}
		}
		return null;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#getName()
	 */
	public String getName() throws CoreException {
		return fModel.getResourceString(super.getName());
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
