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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * An API component for a plug-in project in the workspace.
 * <p>
 * Note: this class requires a running workspace to be instantiated.
 * </p>
 * @since 1.0.0
 */
public class PluginProjectApiComponent extends BundleApiComponent implements ISaveParticipant {
	
	/**
	 * Constant used for controlling tracing in the plugin workspace component
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the plugin workspace component
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	/**
	 * Boolean flags to prevent the description and/or filters from being loaded
	 * during a saving cycle (if they have not already been loaded.
	 */
	private boolean fDescriptionCreated = false;
	private boolean fFiltersCreated = false;
	
	/**
	 * the path to the state where each projects' .api_settings is location
	 */
	private IPath STATE_PATH = ApiPlugin.getDefault().getStateLocation();
	
	/**
	 * Associated Java project
	 */
	private IJavaProject fProject;

	/**
	 * Associated IPluginModelBase object
	 */
	private IPluginModelBase fModel;

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
		STATE_PATH = STATE_PATH.append(fProject.getElementName());
		//TODO bad for performance?
		//the lifecycle of the participant is the lifecycle of the component
		ApiPlugin.getDefault().addSaveParticipant(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#dispose()
	 */
	public void dispose() {
		try {
			ApiPlugin.getDefault().removeSaveParticipant(this);
		} finally {
			super.dispose();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.BundleApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		long time = System.currentTimeMillis();
		IApiDescription apiDesc = new ApiDescription(getId());
		// first mark all packages as internal
		loadManifestApiDescription(apiDesc, getBundleDescription(), getPackageNames());
		try {
			// retrieve the location of the api description file in the metadata folder
			String xml = loadApiDescription(STATE_PATH.toFile());
			if (xml != null) {
				ApiDescriptionProcessor.annotateApiSettings(apiDesc, xml);
			} else {
				loadSourceTags(apiDesc);
			}
		} catch (IOException e) {
			abort("Unable to load api description", e); //$NON-NLS-1$
		}
		fDescriptionCreated = true;
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
						} catch (FileNotFoundException e) {
							abort("Unable to initialize from Javadoc tags", e); //$NON-NLS-1$
						} catch (IOException e) {
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
		ApiFilterStore store = new ApiFilterStore(getId());
		try {
			IPath path = getProjectSettingsPath(false);
			if(path != null) {
				String xml = loadApiFilters(path.toFile());
				if(xml != null) {
					try {
						ApiDescriptionProcessor.annotateApiFilters(store, xml);
					}
					catch(CoreException e) {
						abort("unable to load api filters", e); //$NON-NLS-1$
					}
				}
			}
			fFiltersCreated = true;
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		if (DEBUG) {
			System.out.println("Time to create api filter store for: ["+fProject.getElementName()+"] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return store;
	}
	
	/**
	 * @return the path to the .settings folder for the backing project, or <code>null</code>
	 * if there is no .settings folder
	 */
	private IPath getProjectSettingsPath(boolean create) throws CoreException {
		IPath path = null;
		IProject project  = fProject.getProject();
			if(project.exists()) {
			IFolder folder = project.getFolder(".settings"); //$NON-NLS-1$
			if(folder.exists()) {
				 return folder.getLocation();
			}
			else if(create) {
				folder.create(true, true, new NullProgressMonitor());
				return folder.getLocation();
			}
		}
		return path;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.BundleApiComponent#createClassFileContainers()
	 */
	protected List createClassFileContainers() throws CoreException {
		long time = System.currentTimeMillis();
		// collect output locations and libraries within the project
		List locations = new ArrayList();
		if (fProject.exists() && fProject.getProject().isOpen()) {
			IClasspathEntry entries[] = fProject.getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry classpathEntry = entries[i];
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = classpathEntry.getOutputLocation();
					if (path != null) {
						locations.add(path);
					}
				} else {
					if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
						classpathEntry = JavaCore.getResolvedClasspathEntry(classpathEntry);
					}
					if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						// TODO: check if the library is in the same project?
						locations.add(classpathEntry.getPath());
					}
				}
			}
		}
		// add the default location if not already included
		IPath def = fProject.getOutputLocation();
		if (!locations.contains(def)) {
			locations.add(def);
		}
		
		// create class file containers
		List containers = new ArrayList();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath path = null,
		      location = null;
		File file = null;
		for (int i = 0; i < locations.size(); i++) {
			path = (IPath) locations.get(i);
			location = root.getFile(path).getLocation();
			if(location == null) {
				location = path;
			}
			file = location.toFile();
			if (file.isDirectory()) {
				containers.add(new DirectoryClassFileContainer(file.getAbsolutePath()));
			} else {
				containers.add(new ArchiveClassFileContainer(file.getAbsolutePath()));
			}
		}
		if (DEBUG) {
			System.out.println("Time to create classfile containers for: ["+fProject.getElementName()+"] " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return containers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#export(java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void export(Map options, IProgressMonitor monitor) throws CoreException {
		abort("Plug-in project does not yet implement export.", null); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.BundleApiComponent#getName()
	 */
	public String getName() throws CoreException {
		return fModel.getResourceString(super.getName());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		try {
			if(DEBUG) {
				System.out.println("starting save cycle for plugin project component: ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			persistApiSettings();
			persistApiFilters();
		}
		catch(IOException ioe) {
			ApiPlugin.log(ioe);
		}
	}
	
	/**
	 * Util method to ensure the path to save to is created
	 * @return the string representation of the path to save to
	 * @throws CoreException
	 */
	private String checkStatePath() throws CoreException {
		String path = STATE_PATH.toOSString();
		File apiDescriptionFolder = new File(path);
		if (!apiDescriptionFolder.exists()) {
			if (!apiDescriptionFolder.mkdirs()) {
				abort("Could not create the folder to save settings: ["+getName()+"]", null); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return path;
	}
	
	/**
	 * Saves the .api_filters file for the component
	 * @throws IOException 
	 */
	private void persistApiFilters() throws CoreException, IOException {
		if(fFiltersCreated) {
			if(DEBUG) {
				System.out.println("persisting api filters for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			//save the .api_filters file
			IPath path = getProjectSettingsPath(true);
			ApiFilterStore filters = (ApiFilterStore) getFilterStore();
			String xml = filters.getStoreAsXml();
			Util.saveFile(new File(path.toOSString(), API_FILTERS_XML_NAME), xml);
		}
	}
	
	/**
	 * Saves the .api_settngs file for the component
	 * @throws IOException
	 * @throws CoreException
	 */
	private void persistApiSettings() throws IOException, CoreException {
		if(fDescriptionCreated) {
			if(DEBUG) {
				System.out.println("persisting api settings for plugin project component ["+fProject.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// save the .api_settings file 
			String path = checkStatePath();
			ApiSettingsXmlVisitor xmlVisitor = new ApiSettingsXmlVisitor(this);
			IApiDescription apidesc = getApiDescription();
			apidesc.accept(xmlVisitor);
			String xml = xmlVisitor.getXML();
			Util.saveFile(new File(path, API_DESCRIPTION_XML_NAME), xml);
		}
	}
}
