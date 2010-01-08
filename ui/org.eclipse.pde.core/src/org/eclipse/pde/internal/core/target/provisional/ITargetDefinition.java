/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.provisional;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.environment.Constants;

/**
 * Defines a target platform. A target platform is a collection of bundles configured
 * for a specific environment.
 * <p>
 * Use the target platform service to create and load target definitions.  A definition can be
 * in one of three states unresolved, resolved or provisioned.  A target definition starts off
 * as unresolved.  An unresolved definition can be read and modified, but it can not return the
 * list of installable units in the target.  Resolving a target will have it collect installable
 * units (metadata) for all the bundles stored in it.  This includes generating metadata for local
 * bundle containers as well as contacting remote sites.  A resolved bundle has metadata for all of
 * its bundles, but the bundles may not be exist on the local file system.  A provisioned bundle
 * will download remote bundles and create a metadata profile for the target.
 * </p>
 * @see ITargetPlatformService
 * @since 3.5 
 */
public interface ITargetDefinition {

	/**
	 * Returns a handle to this target definition.
	 * 
	 * @see ITargetHandle
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
	 * Returns whether this target definition is in a resolved state.  A target in a resolved
	 * state has metadata for its contents.  If a problem occurred while resolving the target
	 * it will not be considered resolved.
	 * 
	 * @return <code>true</code> if the target is resolved
	 */
	public boolean isResolved();

	/**
	 * Resolves this target, collecting metadata for its contents.  Metadata may be generated
	 * for local bundles and remote repositories may be loaded.  The status returned by this 
	 * method can be accessed using {@link #getResolveStatus()}.  The target will not be provisioned
	 * after running this method and {@link #provision(IProgressMonitor)} must be called.
	 *  
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return status indicating result of the resolve operation.  May return a {@link MultiStatus}
	 */
	public IStatus resolve(IProgressMonitor monitor);

	/**
	 * Returns the status generated the last time {@link #resolve(IProgressMonitor)} was called on this
	 * target definition.  Will return <code>null</code> if this target has never been resolved.
	 * 
	 * @return status from last resolve operation or <code>null</code>
	 */
	public IStatus getResolveStatus();

	/**
	 * Returns all installable units available in this target.  Returns <code>null</code> if this
	 * target has not been resolved.
	 * 
	 * @return list of available installable units or <code>null</code>
	 */
	public IInstallableUnit[] getAvailableUnits();

	/**
	 * Returns a filtered list of installable units.  Takes the list of available units in the target {@link #getAvailableUnits()} 
	 * and filters anything not set to be included in this target {@link #setIncluded(InstallableUnitDescription[])}.
	 * If the list of included units is set to <code>null</code> this methods returns the same value as {@link #getAvailableUnits()}.
	 * If this target has not been resolved, this method returns <code>null</code>.
	 * 
	 * @return list of included installable units or <code>null</code>
	 */
	public IInstallableUnit[] getIncludedUnits();

	/**
	 * Returns an installable unit in this target with the same ID and version as the given InstallableUnitDescription.
	 * Returns <code>null</code> if this target has not been resolved or if no equivalent installable unit could be found.
	 * 
	 * @param unit installable unit description to look up an installable unit for
	 * @return an equivalent installable unit or <code>null</code>
	 */
	public IInstallableUnit getResolvedUnit(InstallableUnitDescription unit);

	/**
	 * TODO 
	 * @param monitor
	 * @return
	 */
	public InstallableUnitDescription[] getMissingUnits(IProgressMonitor monitor);

	/**
	 * Returns whether this target definition is in a provisioned state.  A target that is in a provisioned
	 * state has bundles and features on disk which can be added to the PDE State.  If a problem occurred 
	 * while provisioning the target it will not be considered provisioned.
	 * 
	 * @return <code>true</code> if the target is provisioned
	 */
	public boolean isProvisioned();

	/**
	 * Provisions this target, using the metadata in this target to collect physical
	 * bundles and features.  If this target is not resolved, this method will attempt
	 * to resolve it before provisioning.  
	 * <p>
	 * This method may contact remote repositories and download artifacts from them.
	 * </p>
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return status indicating result of the provision operation.  May return a {@link MultiStatus}
	 * @throws CoreException
	 */
	public IStatus provision(IProgressMonitor monitor);

	/**
	 * TODO Consider moving this API to a method on ITargetService that provides a "restored target"
	 * 
	 * Checks if a profile backing this target exists and if so, uses it to provision this target.  This
	 * method can be called without calling {@link #resolve(IProgressMonitor)}.  If no profile exists,
	 * there is a problem reading the profile or if one or more of the provisioned bundles do not exist
	 * on disk this method will return an error status.
	 * <p>
	 * This method will not contact remote sites.  The provisioned target may be out of date if repositories or
	 * directory content has changed. 
	 * </p>
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return status indicating result of the provision operation.  May return a {@link MultiStatus}
	 * @throws CoreException
	 */
	public IStatus provisionExisting(IProgressMonitor monitor);

	/**
	 * Returns the status generated the last time {@link #provision(IProgressMonitor)} was called on this
	 * target definition.  Will return <code>null</code> if this target has never been provisioned.
	 * 
	 * @return status from last provision operation or <code>null</code>
	 */
	public IStatus getProvisionStatus();

	/**
	 * Returns a list of {@link BundleInfo} objects describing the physical locations of the bundles
	 * in this target.  Will return <code>null</code> if this target was not successfully provisioned.
	 * 
	 * @return list of bundles in this target or <code>null</code>
	 */
	public BundleInfo[] getProvisionedBundles();

	/**
	 * Returns a list of {@link BundleInfo} objects describing the physical locations of the fetaures
	 * in this target.  Will return <code>null</code> if this target was not successfully provisioned.
	 * 
	 * @return list of features in this target or <code>null</code>
	 */
	public BundleInfo[] getProvisionedFeatures();

	/**
	 * Returns the list of bundle containers in this target.  The list may be empty.
	 * 
	 * @see IBundleContainer
	 * @return list of bundle containers, possibly empty
	 */
	public IBundleContainer[] getBundleContainers();

	/**
	 * Sets the list of bundle containers that provide bundles and features for this target.
	 * 
	 * @param locations list of locations to use, <code>null</code> arguments will be treated the same as an empty array
	 */
	public void setBundleContainers(IBundleContainer[] locations);

	/**
	 * Returns a list of metadata repository locations known to this target.  This list will not include any repositories generated
	 * for bundle containers.
	 * 
	 * @return list of metadata respository locations, possibly empty
	 */
	public URI[] getRepositories();

	/**
	 * Sets the list of metadata repository locations that this target can look for metadata in.
	 * 
	 * @param repos list of metadata locations, <code>null<c/ode> arguments will be treated the same as an empty array.
	 */
	public void setRepositories(URI[] repos);

	/**
	 * Returns the list of installable unit descriptions that will determine which installable units are to be included in the provisioned
	 * target.  The returned descriptions will have an id and may have a version.  If the target is set to include all bundles
	 * (no filtering is being done), this method will return <code>null</code>.
	 * 
	 * @see #getIncludedUnits()
	 * @return list of installable unit descriptions or <code>null</code>
	 */
	public InstallableUnitDescription[] getIncluded();

	/**
	 * Sets the list of installable unit descriptions used to filter the installable units that will be provisioned by this target.
	 * To include all IUs in the target, pass <code>null</code> as the argument.  The unit descriptions passed to this method must
	 * have an ID set.  They may have a version set.  Any other settings will be ignored.
	 * 
	 * @see #getIncludedUnits()
	 * @param included list of units to include in the target or <code>null</code> to include all units
	 */
	public void setIncluded(InstallableUnitDescription[] included);

	/**
	 * Returns the list of installable unit descriptions that will determine which installable units are to be optionally included in the provisioned
	 * target.  The returned descriptions will have an id and may have a version.  If the target is not considering any optional inclusions
	 * this method will return <code>null</code>.
	 * <p>
	 * Optional inclusions are not supported in the user interface.  {@link #getIncluded()} should be used instead.
	 * </p>
	 * @see #getIncludedUnits()
	 * @return list of installable unit descriptions or <code>null</code>
	 */
	public InstallableUnitDescription[] getOptional();

	/**
	 * Sets the list of installable unit descriptions used to optionally include installable units in the provisioned target
	 * To not consider optional inclusions, pass <code>null</code> as the argument.  The unit descriptions passed to this method must
	 * have an ID set.  They may have a version set.  Any other settings will be ignored.
	 * <p>
	 * Optional inclusions are not supported in the user interface.  {@link #setIncluded()} should be used instead.
	 * </p>
	 * @see #getIncludedUnits()
	 * @param included list of units descriptions or <code>null</code>
	 */
	public void setOptional(InstallableUnitDescription[] optional);

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
	 * be specified in the given bundle descriptions. 
	 * 
	 * @param bundles implicit dependencies or <code>null</code> if none
	 */
	public void setImplicitDependencies(InstallableUnitDescription[] bundles);

	/**
	 * Returns the implicit dependencies set on this target or <code>null</code> if none.
	 * Note that this does not resolve the actual bundles used as implicit dependencies - see
	 * {@link #resolveImplicitDependencies(IProgressMonitor)} for resolution.
	 * 
	 * @return implicit dependencies or <code>null</code>
	 */
	public InstallableUnitDescription[] getImplicitDependencies();

}
