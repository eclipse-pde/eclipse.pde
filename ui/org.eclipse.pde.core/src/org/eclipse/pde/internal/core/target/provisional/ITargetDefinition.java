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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.environment.Constants;

/**
 * Defines a target platform. A target platform is a collection of bundles configured
 * for a specific environment.
 * 
 * @since 3.5 
 */
public interface ITargetDefinition {

	public boolean isResolved();

	/**
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return
	 */
	public IStatus resolve(IProgressMonitor monitor);

	public IStatus getResolveStatus();

	public IBundleContainer[] getBundleContainers();

	public void setBundleContainers(IBundleContainer[] locations);

	public URI[] getRepositories();

	public void setRepositories(URI[] repos);

	public IInstallableUnit[] getAvailableUnits();

	/**
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	    @return
	 */
	public IInstallableUnit[] getIncludedUnits(IProgressMonitor monitor);

	// TODO Consider using InstallableUnitDescription instead of BundleInfo

	public BundleInfo[] getMissingUnits(IProgressMonitor monitor);

	public IStatus provision(IProgressMonitor monitor) throws CoreException;

	public BundleInfo[] getProvisionedBundles();

	public BundleInfo[] getProvisionedFeatures();

	public void addIncluded(BundleInfo[] toAdd);

	public void removeIncluded(BundleInfo[] toRemove);

	public void clearIncluded();

	public void addOptional(BundleInfo[] toAdd);

	public void removeOptional(BundleInfo[] toRemove);

	public void clearOptional();

	public BundleInfo[] getIncluded();

	public BundleInfo[] getOptional();

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
	 * be specified in the given bundle descriptions. 
	 * 
	 * @param bundles implicit dependencies or <code>null</code> if none
	 */
	public void setImplicitDependencies(BundleInfo[] bundles);

	/**
	 * Returns the implicit dependencies set on this target or <code>null</code> if none.
	 * Note that this does not resolve the actual bundles used as implicit dependencies - see
	 * {@link #resolveImplicitDependencies(IProgressMonitor)} for resolution.
	 * 
	 * @return implicit dependencies or <code>null</code>
	 */
	public BundleInfo[] getImplicitDependencies();

//	/**
//	 * Returns implicit dependencies resolved against the actual bundles contained in this target
//	 * or <code>null</code> if this target has not been resolved. Matches symbolic names and optional
//	 * versions of implicit dependencies against the actual bundles in this target.
//	 *  
//	 * @return resolved implicit dependencies or <code>null</code>
//	 */
//	public IResolvedBundle[] getResolvedImplicitDependencies();
}
