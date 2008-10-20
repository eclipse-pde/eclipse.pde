/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * Utility class for creating new {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s
 * and for performing common tasks on them
 * 
 * @since 1.0.0
 */
public class ApiModelFactory {

	/**
	 * Next available bundle id
	 */
	private static long fNextId = 0L; 
	
	/**
	 * @return a viable int id for a bundle
	 */
	private static long getBundleID() {
		return fNextId++;
	}
	
	/**
	 * Creates and returns a new API component for this profile at the specified
	 * location or <code>null</code> if the location specified does not contain
	 * a valid API component. The component is not added to the profile.
	 * 
	 * @param location absolute path in the local file system to the API component
	 * @return API component or <code>null</code> if the location specified does not contain a valid
	 * 	API component
	 * @exception CoreException if unable to create the component
	 */
	public static IApiComponent newApiComponent(IApiBaseline profile, String location) throws CoreException {
		BundleApiComponent component = new BundleApiComponent(profile, location);
		if(component.isValidBundle()) {
			component.init(getBundleID());
			return component;
		}
		return null;
	}
	
	/**
	 * Creates and returns a new API component for this profile based on the given
	 * model or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component. The component is not added to the profile.
	 *
	 * @param model the given model
	 * @return API component or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component
	 * @exception CoreException if unable to create the component
	 */
	public static IApiComponent newApiComponent(IApiBaseline profile, IPluginModelBase model) throws CoreException {
		BundleDescription bundleDescription = model.getBundleDescription();
		if (bundleDescription == null) {
			return null;
		}
		String location = bundleDescription.getLocation();
		if (location == null) {
			return null;
		}
		IPath pathForLocation = new Path(location);
		BundleApiComponent component = null;
		IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (path != null && path.isPrefixOf(pathForLocation)) {
			if(isValidProject(location)) {
				component = new PluginProjectApiComponent(profile, location, model);
			}
		} else {
			component = new BundleApiComponent(profile, location);
		}
		if(component != null && component.isValidBundle()) {
			component.init(getBundleID());
			return component;
		}
		return null;
	}
	
	/**
	 * Returns if the specified location is a valid API project or not.
	 * <p>
	 * We accept projects that are plug-ins even if not API enabled (i.e.
	 * with API nature), as we still need them to make a complete
	 * API profile without resolution errors.
	 * </p> 
	 * @param location
	 * @return true if the location is valid, false otherwise
	 * @throws CoreException
	 */
	private static boolean isValidProject(String location) throws CoreException {
		IPath path = new Path(location);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
		return project != null && project.exists();
	}

	/**
	 * Creates a new empty {@link IApiBaseline} with the given name. Its execution
	 * environment will be automatically resolved when components are added
	 * to it.
	 * <p>
	 * Note, a baseline can only automatically resolve an execution environment
	 * when it is created within an Eclipse SDK. A baseline created in a non-OSGi
	 * environment must have its execution environment specified at creation
	 * time.
	 * </p>
	 *
	 * @param name baseline name
	 * @return a new empty {@link IApiBaseline}
	 */
	public static IApiBaseline newApiBaseline(String name) {
		return new ApiBaseline(name);
	}

	/**
	 * Creates a new empty API baseline with the specified execution environment.
	 * <p>
	 * The execution environment description file describes how an execution 
	 * environment profile is provided by or mapped to a specific JRE. The format for
	 * this file is described here
	 * <code>http://wiki.eclipse.org/index.php/Execution_Environment_Descriptions</code>.
	 * </p>
	 * @param name baseline name
	 * @param eeDescription execution environment description file
	 * @return a new {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the specified attributes 
	 */
	public static IApiBaseline newApiBaseline(String name, File eeDescription) throws CoreException {
		return new ApiBaseline(name, eeDescription);
	}
}
