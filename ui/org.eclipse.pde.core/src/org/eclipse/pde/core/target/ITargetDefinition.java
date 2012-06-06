/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.environment.Constants;

/**
 * Defines a target platform. A target platform is a collection of bundles and
 * features configured for a specific environment.
 * 
 * @see ITargetPlatformService Use the target platform service to work with target definitions
 * 
 * @since 3.8
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITargetDefinition {

	/**
	 * Resolves all contents of this target definition by resolving each
	 * {@link ITargetLocation} in this target definition.
	 * <p>
	 * Returns a {@link MultiStatus} containing any non-OK statuses produced 
	 * when resolving each {@link ITargetLocation}.  An OK status will be
	 * returned if no non-OK statuses are returned from the locations. A
	 * CANCEL status will be returned if the monitor is cancelled.
	 * </p><p>
	 * For more information on how a target resolves, see 
	 * {@link ITargetLocation#resolve(ITargetDefinition, IProgressMonitor)}
	 * </p>
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolution multi-status
	 */
	public IStatus resolve(IProgressMonitor monitor);

	/**
	 * Returns whether all {@link ITargetLocation}s in this target currently in
	 * a resolved state.
	 * 
	 * @return <code>true</code> if all locations are currently resolved
	 */
	public boolean isResolved();

	/**
	 * Returns all bundles included in this target definition or <code>null</code>
	 * if this container is not resolved. Takes all the bundles available from the
	 * set target locations (returned by {@link #getAllBundles()} and applies
	 * the filters (returned by {@link #getIncluded()})
	 * to determine the final list of bundles in this target.
	 * <p>
	 * Some of the returned bundles may have non-OK statuses. These bundles may be 
	 * missing some information (location, version, source target). To get a bundle's 
	 * status call {@link TargetBundle#getStatus()}. Calling {@link #getStatus()} 
	 * will return all problems in this target definition.
	 * </p>
	 * @return resolved bundles or <code>null</code>
	 */
	public TargetBundle[] getBundles();

	/**
	 * Returns a list of all resolved bundles in this target definition or <code>null</code>. 
	 * Does not filter based on any filters ({@link #getIncluded()}.
	 * Returns <code>null</code> if this target has not been resolved. 
	 * Use {@link #getBundles()} to get the filtered list of bundles.
	 * <p>
	 * Some of the returned bundles may have non-OK statuses. These bundles may be 
	 * missing some information (location, version, source target). To get a bundle's 
	 * status call {@link TargetBundle#getStatus()}. Calling {@link #getStatus()} 
	 * will return all problems in this target definition.
	 * </p>
	 *  
	 * @return collection of resolved bundles or <code>null</code>
	 */
	public TargetBundle[] getAllBundles();

	/**
	 * Returns the list of feature models available in this target or <code>null</code> if
	 * this target has not been resolved.
	 * 
	 * @return collection of feature models or <code>null</code>
	 */
	public TargetFeature[] getAllFeatures();

	/**
	 * Returns a {@link MultiStatus} containing all problems with this target.
	 * Returns an OK status if there are no problems.  Returns <code>null</code>
	 * if this target has not been resolved.
	 * </p><p>
	 * The returned status will include all non-OK statuses returned by {@link #resolve(IProgressMonitor)}
	 * as well as any non-OK statuses found in {@link TargetBundle}s returned by {@link #getBundles()}. 
	 * For more information on the statuses that can be returned see {@link ITargetLocation#getStatus()}
	 * and {@link TargetBundle#getStatus()}.
	 * </p>
	 * 
	 * @return {@link MultiStatus} containing all problems with this target or <code>null</code>
	 */
	public IStatus getStatus();

	/**
	 * Returns a handle to this target definition.
	 * 
	 * @return target handle
	 */
	public ITargetHandle getHandle();

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
	 * Returns the locations defined by this target, possible <code>null</code>.
	 * 
	 * @return target locations or <code>null</code>
	 */
	public ITargetLocation[] getTargetLocations();

	/**
	 * Sets the locations in this target definition or <code>null</code> if none.
	 * 
	 * @param containers target locations or <code>null</code>
	 */
	public void setTargetLocations(ITargetLocation[] containers);

	/**
	 * Returns a list of descriptors that filter the resolved plug-ins in this target.  The list may include
	 * both plug-ins and features.  The returned descriptors will have an id, may have a version and will have
	 * either {@link NameVersionDescriptor#TYPE_FEATURE} or {@link NameVersionDescriptor#TYPE_PLUGIN} as their
	 * type.  If the target is set to include all units (no filtering is being done), this method will return 
	 * <code>null</code>.
	 * 
	 * @see #getBundles()
	 * @see #setIncluded(NameVersionDescriptor[])
	 * @return list of name version descriptors or <code>null</code>
	 */
	public NameVersionDescriptor[] getIncluded();

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
	 * Returns JRE container path that this target definition should be built against,
	 * or <code>null</code> if the workspace default JRE should be used. JavaRuntime can be used
	 * to resolve JRE's and execution environments from a container path.
	 * 
	 * @return JRE container path or <code>null</code>
	 * @see JavaRuntime
	 */
	public IPath getJREContainer();

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
	 * @param os operating system identifier - one of the operating system constants
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
	 * @param ws window system identifier or <code>null</code> to default to the
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
	 * @param arch architecture identifier or <code>null</code> to default to the
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
	 * @param nl locale identifier or <code>null</code> for default
	 */
	public void setNL(String nl);

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
	 * Sets implicit dependencies for this target. Bundles in this collection are always
	 * considered by PDE when computing plug-in dependencies. Only symbolic names need to
	 * be specified in the given descriptors. 
	 * 
	 * @param bundles implicit dependencies or <code>null</code> if none
	 */
	public void setImplicitDependencies(NameVersionDescriptor[] bundles);

	/**
	 * Returns the implicit dependencies set on this target or <code>null</code> if none.
	 * 
	 * @return implicit dependencies or <code>null</code>
	 */
	public NameVersionDescriptor[] getImplicitDependencies();
}
