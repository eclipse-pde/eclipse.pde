/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;

/**
 * Utility class for creating new {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s
 * and for performing common tasks on them
 * 
 * @since 1.0.0
 */
public class ApiModelFactory {

	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	public static final IApiComponent[] NO_COMPONENTS = new IApiComponent[0];
	
	/**
	 * {@link FilenameFilter} for CVS files
	 * @since 1.0.1
	 */
	static class CVSNameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return !name.equalsIgnoreCase(CVS_FOLDER_NAME);
		}
	}
	
	private static CVSNameFilter fgCvsFilter = new CVSNameFilter();
	
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
	 * Creates and returns a new API component for this baseline at the specified
	 * location or <code>null</code> if the location specified does not contain
	 * a valid API component. The component is not added to the baseline.
	 * 
	 * @param location absolute path in the local file system to the API component
	 * @return API component or <code>null</code> if the location specified does not contain a valid
	 * 	API component
	 * @exception CoreException if unable to create the component
	 */
	public static IApiComponent newApiComponent(IApiBaseline baseline, String location) throws CoreException {
		BundleComponent component = new BundleComponent(baseline, location, getBundleID());
		if(component.isValidBundle()) {
			return component;
		}
		return null;
	}
	
	/**
	 * Creates and returns a new API component for this baseline based on the given
	 * model or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component. The component is not added to the baseline.
	 *
	 * @param baseline
	 * @param model the given model
	 * @return API component or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component
	 * @exception CoreException if unable to create the component
	 */
	public static IApiComponent newApiComponent(IApiBaseline baseline, IPluginModelBase model) throws CoreException {
		BundleDescription bundleDescription = model.getBundleDescription();
		if (bundleDescription == null) {
			return null;
		}
		String location = bundleDescription.getLocation();
		if (location == null) {
			return null;
		}
		BundleComponent component = null;
		if (isBinaryProject(location)) {
			component = new BundleComponent(baseline, location, getBundleID());
		} else {
			component = new ProjectComponent(baseline, location, model, getBundleID());
		}
		if(component.isValidBundle()) {
			return component;
		}
		return null;
	}
	
	/**
	 * Returns if the specified location is an imported binary project.
	 * <p>
	 * We accept projects that are plug-ins even if not API enabled (i.e.
	 * with API nature), as we still need them to make a complete
	 * API baseline without resolution errors.
	 * </p> 
	 * @param location
	 * @return true if the location is an imported binary project, false otherwise
	 * @throws CoreException
	 */
	private static boolean isBinaryProject(String location) throws CoreException {
		IPath path = new Path(location);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
		return project != null && (!project.exists() || Util.isBinaryProject(project));
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
	 * @param location the given baseline's location
	 * @return a new empty {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the specified attributes 
	 */
	public static IApiBaseline newApiBaseline(String name, String location) throws CoreException {
		return new ApiBaseline(name, null, location);
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
	 * @param location the given baseline's location
	 * @return a new {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the specified attributes 
	 */
	public static IApiBaseline newApiBaseline(String name, File eeDescription, String location) throws CoreException {
		return new ApiBaseline(name, eeDescription, location);
	}
	
	/**
	 * Collects API components for the bundles part of the specified installation and adds them to the baseline. The
	 * components that were added to the baseline are returned.
	 * 
	 * @param baseline The baseline to add the components to
	 * @param installLocation location of an installation that components are collected from
	 * @param monitor progress monitor or <code>null</code>, the caller is responsible for calling {@link IProgressMonitor#done()} 
	 * @return List of API components that were added to the baseline, possibly empty, never <code>null</code>
	 * @throws CoreException If problems occur getting components or modifying the baseline
	 */
	public static IApiComponent[] addComponents(IApiBaseline baseline, String installLocation, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.configuring_baseline, 50);
		IApiComponent[] result = null;
		try {
			// Acquire the service
			ITargetPlatformService service = null;
			ApiPlugin plugin = ApiPlugin.getDefault();
			if (plugin != null){
				service = (ITargetPlatformService) ApiPlugin.getDefault().acquireService(ITargetPlatformService.class.getName());
				Util.updateMonitor(subMonitor, 1);
				ITargetLocation container = service.newProfileLocation(installLocation, null);
				ITargetDefinition definition = service.newTarget();
				subMonitor.subTask(Messages.resolving_target_definition);
				container.resolve(definition, subMonitor.newChild(30));
				Util.updateMonitor(subMonitor, 1);
				TargetBundle[] bundles = container.getBundles();
				List components = new ArrayList();
				if (bundles.length > 0) {
					subMonitor.setWorkRemaining(bundles.length);
					for (int i = 0; i < bundles.length; i++) {
						Util.updateMonitor(subMonitor, 1);
							if (!bundles[i].isSourceBundle()) {
								IApiComponent component = ApiModelFactory.newApiComponent(baseline, URIUtil.toFile(bundles[i].getBundleInfo().getLocation()).getAbsolutePath());
								if (component != null) {
									subMonitor.subTask(NLS.bind(Messages.adding_component__0, component.getSymbolicName()));
									components.add(component);
								}
							}
					}
				}
				result = (IApiComponent[])components.toArray(new IApiComponent[components.size()]);
			} else {
				// The target platform service is unavailable (OSGi isn't running), add components by searching the plug-ins directory
				File dir = new File(installLocation);
				if(dir.exists()) {
					File[] files = dir.listFiles(fgCvsFilter);
					if(files == null) {
						return NO_COMPONENTS;
					}
					List components = new ArrayList();
					for (int i = 0; i < files.length; i++) {
						File bundle = files[i];
						IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
						if(component != null) {
							components.add(component);
						}
					}
					result = (IApiComponent[]) components.toArray(new IApiComponent[components.size()]);
				}
			}
			if(result != null) {
				baseline.addApiComponents(result);
				return result;
			}
			return NO_COMPONENTS;
		}
		finally {
			subMonitor.done();
		}
	}
}