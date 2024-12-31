/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
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
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.ExternalFileTargetHandle;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;
import org.eclipse.pde.internal.core.target.WorkspaceFileTargetHandle;

/**
 * Utility class for creating new
 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s and
 * for performing common tasks on them
 *
 * @since 1.0.0
 */
public class ApiModelFactory {

	public static final IApiComponent[] NO_COMPONENTS = new IApiComponent[0];

	/**
	 * Prefix for API baseline locations indicates the baseline was derived from a
	 * target definition. These locations must be compatible with
	 * {@link IPath#fromPortableString(String)}.
	 */
	private static final String TARGET_PREFIX = "target:"; //$NON-NLS-1$

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
	 * Creates and returns a new API component for this baseline at the
	 * specified location or <code>null</code> if the location specified does
	 * not contain a valid API component. The component is not added to the
	 * baseline.
	 *
	 * @param location absolute path in the local file system to the API
	 *            component
	 * @return API component or <code>null</code> if the location specified does
	 *         not contain a valid API component
	 * @exception CoreException if unable to create the component
	 */
	public static IApiComponent newApiComponent(IApiBaseline baseline, String location) throws CoreException {
		BundleComponent component = new BundleComponent(baseline, location, getBundleID());
		if (component.isValidBundle()) {
			return component;
		}
		return null;
	}

	/**
	 * Creates and returns a new API component for this baseline based on the
	 * given model or <code>null</code> if the given model cannot be resolved or
	 * does not contain a valid API component. The component is not added to the
	 * baseline.
	 *
	 * @param model the given model
	 * @return API component or <code>null</code> if the given model cannot be
	 *         resolved or does not contain a valid API component
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
		BundleComponent component;
		IResource resource = model.getUnderlyingResource();
		IProject project = resource != null ? resource.getProject() : null;
		if (project != null && project.exists() && !Util.isBinaryProject(project)) {
			component = new ProjectComponent(baseline, location, model, getBundleID());
		} else {
			component = new BundleComponent(baseline, location, getBundleID());
		}
		if (component.isValidBundle()) {
			return component;
		}
		return null;
	}

	/**
	 * Creates a new empty {@link IApiBaseline} with the given name. Its
	 * execution environment will be automatically resolved when components are
	 * added to it.
	 * <p>
	 * Note, a baseline can only automatically resolve an execution environment
	 * when it is created within an Eclipse SDK. A baseline created in a
	 * non-OSGi environment must have its execution environment specified at
	 * creation time.
	 * </p>
	 *
	 * @param name baseline name
	 * @return a new empty {@link IApiBaseline}
	 */
	public static IApiBaseline newApiBaseline(String name) {
		return new ApiBaseline(name);
	}

	/**
	 * Creates a new empty API baseline with the specified execution
	 * environment.
	 * <p>
	 * The execution environment description file describes how an execution
	 * environment profile is provided by or mapped to a specific JRE. The
	 * format for this file is described here
	 * <code>http://wiki.eclipse.org/index.php/Execution_Environment_Descriptions</code>
	 * .
	 * </p>
	 *
	 * @param name baseline name
	 * @param eeFile execution environment description file
	 * @return a new {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the
	 *             specified attributes
	 */
	public static IApiBaseline newApiBaseline(String name, File eeFile) throws CoreException {
		return newApiBaseline(name, eeFile != null ? new ExecutionEnvironmentDescription(eeFile) : null, null);
	}

	/**
	 * Creates a new empty {@link IApiBaseline} with the given name. Its
	 * execution environment will be automatically resolved when components are
	 * added to it.
	 * <p>
	 * Note, a baseline can only automatically resolve an execution environment
	 * when it is created within an Eclipse SDK. A baseline created in a
	 * non-OSGi environment must have its execution environment specified at
	 * creation time.
	 * </p>
	 *
	 * @param name baseline name
	 * @param location the given baseline's location
	 * @return a new empty {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the
	 *             specified attributes
	 */
	public static IApiBaseline newApiBaseline(String name, String location) throws CoreException {
		return newApiBaseline(name, (ExecutionEnvironmentDescription) null, location);
	}

	/**
	 * Creates a new empty API baseline with the specified execution
	 * environment.
	 * <p>
	 * The execution environment description file describes how an execution
	 * environment profile is provided by or mapped to a specific JRE. The
	 * format for this file is described here
	 * <code>http://wiki.eclipse.org/index.php/Execution_Environment_Descriptions</code>
	 * .
	 * </p>
	 *
	 * @param name baseline name
	 * @param eeFile execution environment description file
	 * @param location the given baseline's location
	 * @return a new {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the
	 *             specified attributes
	 */
	public static IApiBaseline newApiBaseline(String name, File eeFile, String location) throws CoreException {
		return newApiBaseline(name, eeFile != null ? new ExecutionEnvironmentDescription(eeFile) : null, location);
	}

	/**
	 * Creates a new empty API baseline with the specified execution environment.
	 * <p>
	 * The execution environment description describes how an execution environment
	 * profile is provided by or mapped to a specific JRE.
	 * </p>
	 *
	 * @param name          baseline name
	 * @param ee execution environment description
	 * @param location      the given baseline's location
	 * @return a new {@link IApiBaseline}
	 * @throws CoreException if unable to create a new baseline with the specified
	 *                       attributes
	 */
	public static IApiBaseline newApiBaseline(String name, ExecutionEnvironmentDescription ee, String location)
			throws CoreException {
		return new ApiBaseline(name, ee, location);
	}

	/**
	 * Collects API components for the bundles part of the specified
	 * installation and adds them to the baseline. The components that were
	 * added to the baseline are returned.
	 *
	 * @param baseline The baseline to add the components to
	 * @param installLocation location of an installation that components are
	 *            collected from
	 * @param monitor progress monitor or <code>null</code>, the caller is
	 *            responsible for calling {@link IProgressMonitor#done()}
	 * @return List of API components that were added to the baseline, possibly
	 *         empty, never <code>null</code>
	 * @throws CoreException If problems occur getting components or modifying
	 *             the baseline
	 */
	public static IApiComponent[] addComponents(IApiBaseline baseline, String installLocation, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.configuring_baseline, 50);
		IApiComponent[] result = null;
		try {
			// Acquire the service
			ITargetPlatformService service = null;
			ApiPlugin plugin = ApiPlugin.getDefault();
			if (plugin != null) {
				service = ApiPlugin.getDefault().acquireService(ITargetPlatformService.class);
				subMonitor.split(1);
				ITargetLocation container = service.newProfileLocation(installLocation, null);
				ITargetDefinition definition = service.newTarget();
				subMonitor.subTask(Messages.resolving_target_definition);
				container.resolve(definition, subMonitor.split(30));
				subMonitor.split(1);
				TargetBundle[] bundles = container.getBundles();
				List<IApiComponent> components = new ArrayList<>();
				if (bundles.length > 0) {
					subMonitor.setWorkRemaining(bundles.length);
					for (TargetBundle bundle : bundles) {
						subMonitor.split(1);
						if (!bundle.isSourceBundle()) {
							IApiComponent component = ApiModelFactory.newApiComponent(baseline, URIUtil.toFile(bundle.getBundleInfo().getLocation()).getAbsolutePath());
							if (component != null) {
								subMonitor.subTask(NLS.bind(Messages.adding_component__0, component.getSymbolicName()));
								components.add(component);
							}
						}
					}
				}
				result = components.toArray(new IApiComponent[components.size()]);
			} else {
				// The target platform service is unavailable (OSGi isn't
				// running), add components by searching the plug-ins directory
				File dir = new File(installLocation);
				if (dir.exists()) {
					File[] files = dir.listFiles();
					if (files == null) {
						return NO_COMPONENTS;
					}
					List<IApiComponent> components = new ArrayList<>();
					for (File bundle : files) {
						IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
						if (component != null) {
							components.add(component);
						}
					}
					result = components.toArray(new IApiComponent[components.size()]);
				}
			}
			if (result != null) {
				baseline.addApiComponents(result);
				return result;
			}
			return NO_COMPONENTS;
		} finally {
			subMonitor.done();
		}
	}

	public static IApiBaseline newApiBaselineFromTarget(String name, ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		IApiBaseline baseline = new ApiBaseline(name);

		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.configuring_baseline, 50);
		try {
			IStatus result = definition.resolve(subMonitor.split(30));
			if (!result.isOK()) {
				throw new CoreException(result);
			}
			subMonitor.split(1);
			TargetBundle[] bundles = definition.getBundles();
			List<IApiComponent> components = new ArrayList<>();
			if (bundles.length > 0) {
				subMonitor.setWorkRemaining(bundles.length);
				for (TargetBundle bundle : bundles) {
					subMonitor.split(1);
					if (!bundle.isSourceBundle()) {
						IApiComponent component = ApiModelFactory.newApiComponent(baseline, URIUtil.toFile(bundle.getBundleInfo().getLocation()).getAbsolutePath());
						if (component != null) {
							subMonitor.subTask(NLS.bind(Messages.adding_component__0, component.getSymbolicName()));
							components.add(component);
						}
					}
				}
			}
			baseline.addApiComponents(components.toArray(new IApiComponent[components.size()]));
			baseline.setLocation(generateTargetLocation(definition));
			return baseline;
		} finally {
			subMonitor.done();
		}
	}

	/**
	 * Create predictable location description for a target definition. Form is
	 * <code>target:/[target hashcode}/definitionLocation</code>. A location must be
	 * compatible with {@link IPath#fromPortableString(String)}.
	 *
	 * @param definition the target platform definition
	 * @return an encoded location
	 * @see #isDerivedFromTarget
	 * @see #getDefinitionIdentifier
	 */
	private static String generateTargetLocation(ITargetDefinition definition) {
		StringBuilder sb = new StringBuilder(TARGET_PREFIX);
		sb.append(IPath.SEPARATOR);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1"); //$NON-NLS-1$
			try (DigestOutputStream output = new DigestOutputStream(OutputStream.nullOutputStream(), digest)) {
				TargetDefinitionPersistenceHelper.persistXML(definition, output);
			}
			for (byte b : digest.digest()) {
				sb.append(Integer.toHexString(b & 0xFF));
			}
		} catch (Exception e) {
			// can't record a hashcode then...
		}
		sb.append(IPath.SEPARATOR);
		sb.append(getDefinitionIdentifier(definition));
		return sb.toString();
	}

	/**
	 * Return a stable identifier for the provided definition.
	 *
	 * @return a stable identifier, in portable OS format as per
	 *         {@link IPath#fromPortableString(String)}.
	 */
	private static String getDefinitionIdentifier(ITargetDefinition definition) {
		ITargetHandle targetHandle = definition.getHandle();
		// It would be nicer if ITargetHandle had #getURI() or something
		String location;
		if (targetHandle instanceof WorkspaceFileTargetHandle workspaceHandle) {
			IFile file = workspaceHandle.getTargetFile();
			location = file.getFullPath().toPortableString();
		} else if (targetHandle instanceof ExternalFileTargetHandle externalHandle) {
			URI uri = externalHandle.getLocation();
			location = uri.toASCIIString();
		} else {
			// LocalTargetHandle#toString() returns file name,
			// and hope any other impls do the same
			location = targetHandle.toString();
		}
		return location.replace(':', IPath.SEPARATOR);
	}

	/**
	 * Return true if the provided profile seems to have been derived from the
	 * given target definition. The target definition may have evolved since
	 * originally created.
	 *
	 * @param profile the API profile
	 * @param definition the target definition
	 * @return true if the profile was derived from the given definition
	 */
	public static boolean isDerivedFromTarget(IApiBaseline profile, ITargetDefinition definition) {
		// strip off the scheme and sequence number and compare
		String location = profile.getLocation();
		// location should be minimally "target://X" for some identifier X
		if (location == null || !location.startsWith(TARGET_PREFIX) || location.length() <= TARGET_PREFIX.length() + 3) {
			return false;
		}
		int seqEnd = -1;
		if (location.charAt(TARGET_PREFIX.length()) != IPath.SEPARATOR) {
			location = location.replace(File.separatorChar, '/');
			if (location.charAt(TARGET_PREFIX.length()) != IPath.SEPARATOR) {
				return false;
			}
			seqEnd = location.indexOf(IPath.SEPARATOR, TARGET_PREFIX.length() + 2);
			if (seqEnd == -1) {
				return false;
			}
			seqEnd--;
		}
		// 2 = ':/'
		if (seqEnd == -1) {
			seqEnd = location.indexOf(IPath.SEPARATOR, TARGET_PREFIX.length() + 2);
		}
		String targetIdentifier = location.substring(seqEnd + 1);
		return targetIdentifier.equals(getDefinitionIdentifier(definition));
	}

	/**
	 * Return true if the provided profile was derived from a target definition.
	 */
	public static boolean isDerivedFromTarget(IApiBaseline profile) {
		return profile.getLocation() != null && profile.getLocation().startsWith(ApiModelFactory.TARGET_PREFIX);
	}

	/**
	 * Return true if the provided profile is up-to-date with the given target
	 * definition
	 *
	 * @param profile the API profile
	 * @param definition the target definition
	 * @return true if the profile is up-to-date
	 */
	public static boolean isUpToDateWithTarget(IApiBaseline profile, ITargetDefinition definition) {
		// The target's sequence number, if any, is generated into the location
		return profile.getLocation() != null && profile.getLocation().equals(generateTargetLocation(definition));
	}
}