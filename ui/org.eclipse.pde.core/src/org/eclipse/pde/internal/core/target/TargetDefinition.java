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
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.net.URI;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.xml.sax.SAXException;

/**
 * Target definition implementation.
 * 
 * @since 3.5
 */
public class TargetDefinition implements ITargetDefinition {

	// name and description
	private String fName;

	// arguments
	private String fProgramArgs;
	private String fVMArgs;

	// environment settings
	private IPath fJREContainer;
	private String fArch;
	private String fOS;
	private String fWS;
	private String fNL;

	/**
	 * Set of bundle containers in this target definition
	 */
	private IBundleContainer[] fContainers;

	/**
	 * Collection of explicit repository locations to include in this target
	 */
	private URI[] fRepos;

	/**
	 * Set of BundleInfo objects that will be used as implicit dependencies
	 */
	private NameVersionDescriptor[] fImplicit;

	/**
	 * Set of NameVersionDescriptor descriptions to be included in the target
	 */
	private NameVersionDescriptor[] fIncluded;

	/**
	 * Set of NameVersionDescriptor descriptions that are optionally included in the target 
	 */
	private NameVersionDescriptor[] fOptional;

	/**
	 * Handle that controls the persistence of this target
	 */
	private ITargetHandle fHandle;

	/**
	 * Helper object encapsulating target resolution code, if this target is not resolved this can be <code>null</code>
	 */
	private TargetResolver fResolver;

	/**
	 * Helper object encapsulating target provisioning code, if this target is not provisioned, this can be <code>null</code>
	 */
	private TargetProvisioner fProvisioner;

	/**
	 * Constructs a target definition based on the given handle. 
	 */
	TargetDefinition(ITargetHandle handle) {
		fHandle = handle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getLocations()
	 */
	public IBundleContainer[] getBundleContainers() {
		if (fContainers == null) {
			return new IBundleContainer[0];
		}
		return fContainers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setLocations(org.eclipse.pde.internal.core.target.provisional.IBundleContainer[])
	 */
	public void setBundleContainers(IBundleContainer[] locations) {
		// No longer resolved
		fResolver = null;
		fProvisioner = null;
		if (locations != null && locations.length == 0) {
			locations = null;
		}
		fContainers = locations;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getRepositories()
	 */
	public URI[] getRepositories() {
		if (fRepos == null) {
			return new URI[0];
		}
		return fRepos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setRepositories(java.net.URI[])
	 */
	public void setRepositories(URI[] repos) {
		// No longer resolved
		fResolver = null;
		fProvisioner = null;
		if (repos != null && repos.length == 0) {
			repos = null;
		}
		fRepos = repos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus resolve(IProgressMonitor monitor) {
		fProvisioner = null;
		fResolver = new TargetResolver(this);
		return fResolver.resolve(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#isResolved()
	 */
	public boolean isResolved() {
		return fResolver != null && fResolver.getStatus() != null && (fResolver.getStatus().getSeverity() == IStatus.OK || fResolver.getStatus().getSeverity() == IStatus.WARNING);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getResolveStatus()
	 */
	public IStatus getResolveStatus() {
		return fResolver != null ? fResolver.getStatus() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getAvailableUnits()
	 */
	public IInstallableUnit[] getAvailableUnits() {
		if (isResolved()) {
			Collection available = fResolver.getAvailableIUs();
			return (IInstallableUnit[]) available.toArray(new IInstallableUnit[available.size()]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getIncludedUnits()
	 */
	public IInstallableUnit[] getIncludedUnits() {
		if (isResolved()) {
			Collection included = fResolver.calculateIncludedIUs();
			return (IInstallableUnit[]) included.toArray(new IInstallableUnit[included.size()]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#isProvisioned()
	 */
	public boolean isProvisioned() {
		return fProvisioner != null && fProvisioner.getStatus() != null && (fProvisioner.getStatus().getSeverity() == IStatus.OK || fProvisioner.getStatus().getSeverity() == IStatus.WARNING);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#provision(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus provision(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 100);
		if (!isResolved()) {
			resolve(subMon.newChild(50));
		}
		subMon.setWorkRemaining(50);

		// If the target failed to resolve then return the resolve status
		if (!isResolved()) {
			return getResolveStatus();
		}

		fProvisioner = new TargetProvisioner(this, fResolver);
		return fProvisioner.provision(subMon.newChild(50));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#provisionExisting(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus provisionExisting(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, "Provisioning target from existing profile", 50);
		fProvisioner = new TargetProvisioner(this, fResolver);
		return fProvisioner.provisionExisting(subMon);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getProvisionStatus()
	 */
	public IStatus getProvisionStatus() {
		return fProvisioner != null ? fProvisioner.getStatus() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getProvisionedBundles()
	 */
	public BundleInfo[] getProvisionedBundles() {
		if (isProvisioned()) {
			return fProvisioner.getProvisionedBundles();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getIncluded()
	 */
	public NameVersionDescriptor[] getIncluded() {
		return fIncluded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setIncluded(org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor[])
	 */
	public void setIncluded(NameVersionDescriptor[] included) {
		// We are no longer provisioned
		fProvisioner = null;
		fIncluded = included;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getOptional()
	 */
	public NameVersionDescriptor[] getOptional() {
		return fOptional;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setOptional(org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor[])
	 */
	public void setOptional(NameVersionDescriptor[] optional) {
		// No longer provisioned
		fProvisioner = null;
		fOptional = optional;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getArch()
	 */
	public String getArch() {
		return fArch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getNL()
	 */
	public String getNL() {
		return fNL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getOS()
	 */
	public String getOS() {
		return fOS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getProgramArguments()
	 */
	public String getProgramArguments() {
		return fProgramArgs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getVMArguments()
	 */
	public String getVMArguments() {
		return fVMArgs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getWS()
	 */
	public String getWS() {
		return fWS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setArch(java.lang.String)
	 */
	public void setArch(String arch) {
		fArch = arch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setNL(java.lang.String)
	 */
	public void setNL(String nl) {
		fNL = nl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setOS(java.lang.String)
	 */
	public void setOS(String os) {
		fOS = os;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setProgramArguments(java.lang.String)
	 */
	public void setProgramArguments(String args) {
		if (args != null && args.length() == 0) {
			args = null;
		}
		fProgramArgs = args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setVMArguments(java.lang.String)
	 */
	public void setVMArguments(String args) {
		if (args != null && args.length() == 0) {
			args = null;
		}
		fVMArgs = args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setWS(java.lang.String)
	 */
	public void setWS(String ws) {
		fWS = ws;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getHandle()
	 */
	public ITargetHandle getHandle() {
		return fHandle;
	}

	/**
	 * Non-API method that returns an installable unit in this target with the same ID and version as the given {@link NameVersionDescriptor}.
	 * Returns <code>null</code> if this target has not been resolved or if no equivalent installable unit could be found.
	 * 
	 * @param unit installable unit description to look up an installable unit for
	 * @return an equivalent installable unit or <code>null</code>
	 */
	public IInstallableUnit getResolvedUnit(NameVersionDescriptor unit) {
		if (isResolved()) {
			return fResolver.getUnit(unit);
		}
		return null;
	}

	/**
	 * Non-API method to get the set of provisioned bundles that are source bundles.
	 * Used by the import operation.  Will return <code>null</code> if this target
	 * has not been successfully provisionined.
	 * 
	 * @return list of source bundles in this target or <code>null</code>
	 */
	public BundleInfo[] getProvisionedSourceBundles() {
		if (isProvisioned()) {
			return fProvisioner.getSourceBundles();
		}
		return null;
	}

	/**
	 * Build contents from the given stream.
	 * 
	 * @param stream input stream
	 * @throws CoreException if an error occurs
	 */
	void setContents(InputStream stream) throws CoreException {
		try {
			fArch = null;
			fContainers = null;
			fImplicit = null;
			fJREContainer = null;
			fName = null;
			fNL = null;
			fOS = null;
			fProgramArgs = null;
			fVMArgs = null;
			fWS = null;
			TargetDefinitionPersistenceHelper.initFromXML(this, stream);
		} catch (ParserConfigurationException e) {
			abort(Messages.TargetDefinition_0, e);
		} catch (SAXException e) {
			abort(Messages.TargetDefinition_0, e);
		} catch (IOException e) {
			abort(Messages.TargetDefinition_0, e);
		}
	}

	/**
	 * Persists contents to the given stream.
	 * 
	 * @param stream output stream
	 * @throws CoreException if an error occurs
	 */
	void write(OutputStream stream) throws CoreException {
		try {
			TargetDefinitionPersistenceHelper.persistXML(this, stream);
		} catch (IOException e) {
			abort(Messages.TargetDefinition_3, e);
		} catch (ParserConfigurationException e) {
			abort(Messages.TargetDefinition_3, e);
		} catch (TransformerException e) {
			abort(Messages.TargetDefinition_3, e);
		}
	}

	/**
	 * Throws a core exception with the given message and underlying exception (possibly
	 * <code>null</code>).
	 * 
	 * @param message message
	 * @param e underlying cause of the exception or <code>null</code>
	 * @throws CoreException
	 */
	private void abort(String message, Exception e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, message, e));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getImplicitDependencies()
	 */
	public NameVersionDescriptor[] getImplicitDependencies() {
		return fImplicit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setImplicitDependencies(org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor[])
	 */
	public void setImplicitDependencies(NameVersionDescriptor[] bundles) {
		if (bundles != null && bundles.length == 0) {
			bundles = null;
		}
		fImplicit = bundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getJREContainer()
	 */
	public IPath getJREContainer() {
		return fJREContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setJREContainer(org.eclipse.core.runtime.IPath)
	 */
	public void setJREContainer(IPath containerPath) {
		fJREContainer = containerPath;
	}

	/**
	 * Returns whether the content of this definition is equivalent to the content of the
	 * specified definition (excluding name/description).
	 * 
	 * @param definition
	 * @return whether the content of this definition is equivalent to the content of the
	 * specified definition
	 */
	public boolean isContentEquivalent(ITargetDefinition definition) {
		// Environment settings
		if (isNullOrEqual(getArch(), definition.getArch()) && isNullOrEqual(getNL(), definition.getNL()) && isNullOrEqual(getOS(), definition.getOS()) && isNullOrEqual(getWS(), definition.getWS()) && isArgsNullOrEqual(getProgramArguments(), definition.getProgramArguments()) && isArgsNullOrEqual(getVMArguments(), definition.getVMArguments()) && isNullOrEqual(getJREContainer(), definition.getJREContainer())) {
			// Containers
			if (areContainersEqual(getBundleContainers(), definition.getBundleContainers())) {
				// Explicit repos
				if (areEqual(getRepositories(), definition.getRepositories())) {
					// Included
					if (areEqual(getIncluded(), definition.getIncluded())) {
						// Optional
						if (areEqual(getOptional(), definition.getOptional())) {
							// Implicit
							if (areEqual(getImplicitDependencies(), definition.getImplicitDependencies())) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private boolean areEqual(Object[] c1, Object[] c2) {
		if (c1 == null) {
			return c2 == null;
		}
		if (c2 == null) {
			return false;
		}
		if (c1.length == c2.length) {
			for (int i = 0; i < c2.length; i++) {
				if (!c1[i].equals(c2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean isNullOrEqual(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	private boolean isArgsNullOrEqual(String args1, String args2) {
		if (args1 == null) {
			return args2 == null;
		}
		if (args2 == null) {
			return false;
		}
		String[] a1 = DebugPlugin.parseArguments(args1);
		String[] a2 = DebugPlugin.parseArguments(args2);
		if (a1.length == a2.length) {
			for (int i = 0; i < a1.length; i++) {
				if (!a1[i].equals(a2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private boolean areContainersEqual(IBundleContainer[] c1, IBundleContainer[] c2) {
		if (c1 == null) {
			return c2 == null;
		}
		if (c2 == null) {
			return false;
		}
		if (c1.length != c2.length) {
			return false;
		}

		for (int i = 0; i < c2.length; i++) {
			if (!c1[i].isContentEqual(c2[i])) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fName != null ? fName : "No Name"); //$NON-NLS-1$
		if (fContainers == null) {
			buf.append("\n\tNo containers"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < fContainers.length; i++) {
				buf.append("\n\t").append(fContainers.toString()); //$NON-NLS-1$
			}
		}
		if (fRepos != null && fRepos.length > 0) {
			buf.append("\nRepos: "); //$NON-NLS-1$
			for (int i = 0; i < fRepos.length; i++) {
				buf.append(URIUtil.toUnencodedString(fRepos[i]));
			}
		}
		buf.append("\nEnv: ").append(fOS).append("/").append(fWS).append("/").append(fArch).append("/").append(fNL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		buf.append("\nJRE: ").append(fJREContainer); //$NON-NLS-1$
		buf.append("\nArgs: ").append(fProgramArgs).append("/").append(fVMArgs); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nImplicit: ").append(fImplicit == null ? "null" : Integer.toString(fImplicit.length)); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nHandle: ").append(fHandle.toString()); //$NON-NLS-1$
		return buf.toString();
	}
}
