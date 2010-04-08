/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.provisional;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Defines a target platform. A target platform is a collection of bundles configured
 * for a specific environment.
 * 
 * @since 3.5 
 */
public interface ITargetDefinition {

	/**
	 * Returns the name of this target, or <code>null</code> if none
	 * 
	 * @return name or <code>null</code>
	 */
	public String getName();

	/**
	 * Sets the name of this target.
	 * 
	 * @param name target name or <code>null</code>
	 */
	public void setName(String name);

	/**
	 * Sets the JRE that this target definition should be built against, or <code>null</code>
	 * to use the workspace default JRE. JavaRuntime should be used to generate and parse
	 * JRE container paths.
	 * 
	 * @param containerPath JRE container path
	 * @see JavaRuntime
	 */
	public void setJREContainer(IPath containerPath);

	/**
	 * Returns JRE container path that this target definition should be built against,
	 * or <code>null</code> if the workspace default JRE should be used. JavaRuntime can be used
	 * to resolve JRE's and execution environments from a container path.
	 * 
	 * @return JRE container path or <code>null</code>
	 * @see JavaRuntime
	 */
	public IPath getJREContainer();

	/**
	 * Returns the identifier of the operating system this target is configured for,
	 * possibly <code>null</code>.
	 * 
	 * @return operating system identifier or <code>null</code> to default to the 
	 * 	running operating system
	 */
	public String getOS();

	/**
	 * Sets the operating system this target is configured for or <code>null</code> to
	 * default to the running operating system.
	 * 
	 * @param operating system identifier - one of the operating system constants
	 * 	defined by {@link Constants} or <code>null</code> to default to the running
	 * 	operating system
	 */
	public void setOS(String os);

	/**
	 * Returns the identifier of the window system this target is configured for,
	 * possibly <code>null</code>.
	 * 
	 * @return window system identifier - one of the window system constants
	 * 	defined by {@link Constants}, or <code>null</code> to default to the
	 * 	running window system
	 */
	public String getWS();

	/**
	 * Sets the window system this target is configured for or <code>null</code> to 
	 * default to the running window system.
	 * 
	 * @param window system identifier or <code>null</code> to default to the
	 * 	running window system
	 */
	public void setWS(String ws);

	/**
	 * Returns the identifier of the architecture this target is configured for,
	 * or <code>null</code> to default to the running architecture.
	 * 
	 * @return architecture identifier - one of the architecture constants
	 * 	defined by {@link Constants} or <code>null</code> to default to the running
	 * 	architecture
	 */
	public String getArch();

	/**
	 * Sets the architecture this target is configured for, or <code>null</code> to default
	 * to the running architecture.
	 * 
	 * @param architecture identifier or <code>null</code> to default to the
	 * 	running architecture.
	 */
	public void setArch(String arch);

	/**
	 * Returns the identifier of the locale this target is configured for, or <code>null</code>
	 * for default.
	 * 
	 * @return locale identifier or <code>null</code> for default
	 */
	public String getNL();

	/**
	 * Sets the locale this target is configured for or <code>null</code> for default.
	 * 
	 * @param locale identifier or <code>null</code> for default
	 */
	public void setNL(String nl);

	/**
	 * Returns the bundle containers defined by this target, possible <code>null</code>.
	 * 
	 * @return bundle containers or <code>null</code>
	 */
	public IBundleContainer[] getBundleContainers();

	/**
	 * Sets the bundle containers in this target definition or <code>null</code> if none.
	 * 
	 * @param containers bundle containers or <code>null</code>
	 */
	public void setBundleContainers(IBundleContainer[] containers);

	/**
	 * Sets a list of descriptors to filter the resolved plug-ins in this target.  The list may include both
	 * plug-ins and features.  To include all plug-ins in the target, pass <code>null</code> as the argument.
	 * <p>
	 * The descriptions passed to this method must have an ID set.  The version may be <code>null</code>
	 * to include any version of the matches the ID.  Only descriptors with a type of {@link NameVersionDescriptor#TYPE_FEATURE}
	 * or {@link NameVersionDescriptor#TYPE_PLUGIN} will be considered.
	 * </p>
	 * @see #getBundles()
	 * @see #getIncluded()
	 * @param included list of descriptors to include in the target or <code>null</code> to include all plug-ins
	 */
	public void setIncluded(NameVersionDescriptor[] included);

	/**
	 * Returns a list of descriptors that filter the resolved plug-ins in this target.  The list may include
	 * both plug-ins and features.  The returned descriptors will have an id, may have a version and will have
	 * either {@link NameVersionDescriptor#TYPE_FEATURE} or {@link NameVersionDescriptor#TYPE_PLUGIN} as their
	 * type.  If the target is set to include all units (no filtering is being done), this method will return 
	 * <code>null</code>.
	 * 
	 * @see #getBundles()
	 * @see #setIncluded()
	 * @return list of name version descriptors or <code>null</code>
	 */
	public NameVersionDescriptor[] getIncluded();

	/**
	 * Sets a list of descriptors used to add optional bundles to the resolved target.  To not use optional bundles
	 * pass <code>null</code> as the argument.  Only {@link NameVersionDescriptor}s with a type of {@link NameVersionDescriptor#TYPE_PLUGIN}
	 * will be considered. The unit descriptions passed to this method must have an ID set, but the version may be <code>null</code>
	 * to include any version of that plug-in.
	 * 
	 * @param included list of units to include in the target or <code>null</code> to not use optional bundles
	 */
	public void setOptional(NameVersionDescriptor[] optional);

	/**
	 * Returns a list of descriptors used to add optional bundles to the resolved target.  If optional
	 * bundles are not being used in this target this method will return <code>null</code>.  The returned
	 * descriptors will have an ID set, may have a version set and will have a type of {@link NameVersionDescriptor#TYPE_PLUGIN}.
	 * 
	 * @return list of name version descriptors or <code>null</code>
	 */
	public NameVersionDescriptor[] getOptional();

	/**
	 * Returns all bundles included in this target definition or <code>null</code>
	 * if this container is not resolved.  Takes all the bundles available from the
	 * set bundle containers (result returned by {@link #getAllBundles()} and applies
	 * the filters set by {@link #setIncluded(NameVersionDescriptor[])} and 
	 * {@link #setOptional(NameVersionDescriptor[])} to determine the final list of 
	 * bundles in this target.
	 * <p>
	 * Some of the returned bundles may have non-OK statuses.  These bundles may be missing some
	 * information (location, version, source target).  To get a bundle's status call
	 * {@link IResolvedBundle#getStatus()}.  You can also use {@link #getBundleStatus()} to
	 * get the complete set of problems.
	 * </p>
	 * @see #getBundleStatus()
	 * @return resolved bundles or <code>null</code>
	 */
	public IResolvedBundle[] getBundles();

	/**
	 * Returns the list of resolved bundles in this target definition or <code>null</code>. 
	 * Does not filter based on any includedBundles or optionalBundles set on bundle containers.
	 * Returns <code>null</code> if this target has not been resolved. 
	 * Use {@link #getBundles()} to get the filtered list of bundles.
	 *  
	 * @return collection of resolved bundles or <code>null</code>
	 */
	public IResolvedBundle[] getAllBundles();

	/**
	 * Returns the list of feature models available in this target or <code>null</code> if
	 * this target has not been resolved.
	 * 
	 * @return collection of feature models or <code>null</code>
	 */
	public IFeatureModel[] getAllFeatures();

	/**
	 * Resolves all bundles in this target definition by resolving each
	 * bundle container in this target definition.
	 * <p>
	 * Returns a multi-status containing any non-OK statuses produced when
	 * resolving each bundle container in this target.  An OK status will be
	 * returned if the resolution was successful.  A CANCEL status will be 
	 * returned if the monitor is canceled. For more information on the contents
	 * of the status see {@link IBundleContainer#resolve(ITargetDefinition, IProgressMonitor)}
	 * </p><p>
	 * Note that the returned status may be different than the result of 
	 * calling {@link #getBundleStatus()}.
	 * </p>
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolution status
	 * @throws CoreException if unable to resolve
	 */
	public IStatus resolve(IProgressMonitor monitor);

	/**
	 * Returns whether this target's bundle containers are currently in
	 * a resolved state.
	 * 
	 * @return whether this target's bundle containers are currently in
	 * a resolved state
	 */
	public boolean isResolved();

	/**
	 * Returns a multi-status containing the bundle status of all bundle containers
	 * in this target or <code>null</code> if this target has not been resolved.  For
	 * information on the statuses collected from the bundle containers see
	 * {@link IBundleContainer#getStatus()}.
	 * 
	 * @see #getBundles()
	 * @return multi-status containing status for each bundle container or <code>null</code>
	 */
	public IStatus getBundleStatus();

	/**
	 * Returns any program arguments that should be used when launching this target
	 * or <code>null</code> if none.
	 * 
	 * @return program arguments or <code>null</code> if none
	 */
	public String getProgramArguments();

	/**
	 * Sets any program arguments that should be used when launching this target
	 * or <code>null</code> if none.
	 * 
	 * @param args program arguments or <code>null</code>
	 */
	public void setProgramArguments(String args);

	/**
	 * Returns any VM arguments that should be used when launching this target
	 * or <code>null</code> if none.
	 * 
	 * @return VM arguments or <code>null</code> if none
	 */
	public String getVMArguments();

	/**
	 * Sets any VM arguments that should be used when launching this target
	 * or <code>null</code> if none.
	 * 
	 * @param args VM arguments or <code>null</code>
	 */
	public void setVMArguments(String args);

	/**
	 * Returns a handle to this target definition.
	 * 
	 * @return target handle
	 */
	public ITargetHandle getHandle();

	/**
	 * Sets implicit dependencies for this target. Bundles in this collection are always
	 * considered by PDE when computing plug-in dependencies. Only symbolic names need to
	 * be specified in the given descriptors. 
	 * 
	 * @param bundles implicit dependencies or <code>null</code> if none
	 */
	public void setImplicitDependencies(NameVersionDescriptor[] bundles);

	/**
	 * Returns the implicit dependencies set on this target or <code>null</code> if none.
	 * Note that this does not resolve the actual bundles used as implicit dependencies - see
	 * {@link #resolveImplicitDependencies(IProgressMonitor)} for resolution.
	 * 
	 * @return implicit dependencies or <code>null</code>
	 */
	public NameVersionDescriptor[] getImplicitDependencies();
}
