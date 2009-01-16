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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.environment.Constants;

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
	 * Returns the description of this target or <code>null</code> if none.
	 * 
	 * @return target description
	 */
	public String getDescription();

	/**
	 * Sets the description of this target, possibly <code>null</code>.
	 * 
	 * @param description target description or <code>null</code>
	 */
	public void setDescription(String description);

	/**
	 * Sets the execution environment this target requires to run. An execution 
	 * environment is specified by its associated OSGi profile identifier - for
	 * example, <code>J2SE-1.4</code>.
	 * 
	 * @param environment execution environment identifier
	 */
	public void setExecutionEnvironment(String environment);

	/**
	 * Returns the identifier of the execution environment this target requires to run.
	 * 
	 * @return execution environment identifier
	 */
	public String getExecutionEnvironment();

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
	 * Resolves and returns all executable bundles in this target definition, possibly empty.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @return all executable bundles in this target definition
	 * @throws CoreException if unable to resolve
	 */
	public BundleInfo[] resolveBundles(IProgressMonitor monitor) throws CoreException;

	/**
	 * Resolves and returns all source bundles in this target definition, possibly empty.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @return all source bundles in this target definition
	 * @throws CoreException if unable to resolve
	 */
	public BundleInfo[] resolveSourceBundles(IProgressMonitor monitor) throws CoreException;

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

	/**
	 * Resolves and returns implicit dependencies against the actual bundles contained
	 * in this target. Matches symbolic names and optional versions of implicit dependencies
	 * against the actual bundles in this target.
	 *  
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolved implicit dependencies
	 * @throws CoreException if unable to resolve
	 */
	public BundleInfo[] resolveImplicitDependencies(IProgressMonitor monitor) throws CoreException;
}
