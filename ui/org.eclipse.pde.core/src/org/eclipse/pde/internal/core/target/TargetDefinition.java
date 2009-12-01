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
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
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
	private BundleInfo[] fImplicit;

	/**
	 * Set of BundleInfo descriptions to be included in the target
	 */
	private Set fIncluded;

	/**
	 * Set of BundleInfo descriptions that are optionally included in the target 
	 */
	private Set fOptional;

	/**
	 * Handle that controls the persistence of this target
	 */
	private ITargetHandle fHandle;

	/**
	 * Helper object encapsulating target resolution code, if this target is not resolved this can be <code>null</code>
	 */
	private TargetResolver fResolver;

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
		if (repos != null && repos.length == 0) {
			repos = null;
		}
		fRepos = repos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus resolve(IProgressMonitor monitor) {
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
	public IInstallableUnit[] getIncludedUnits(IProgressMonitor monitor) {
		if (isResolved()) {
			Collection included = fResolver.calculateIncludedIUs(monitor);
			return (IInstallableUnit[]) included.toArray(new IInstallableUnit[included.size()]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getMissingUnits(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public BundleInfo[] getMissingUnits(IProgressMonitor monitor) {
		if (isResolved()) {
			return new BundleInfo[0];

			// TODO Problems here
//			Collection missing = fResolver.calculateMissingIUs(monitor);
//			return (BundleInfo[]) missing.toArray(new BundleInfo[missing.size()]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#provision(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus provision(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMon = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 100);
		if (!isResolved()) {
			resolve(subMon.newChild(50));
		}
		subMon.setWorkRemaining(50);

		// If the target failed to resolve then return the resolve status
		if (!isResolved()) {
			return getResolveStatus();
		}

		return fResolver.provision(subMon.newChild(50));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getProvisionedBundles()
	 */
	public BundleInfo[] getProvisionedBundles() {
		if (isResolved()) {
			return fResolver.getProvisionedBundles();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getProvisionedFeatures()
	 */
	public BundleInfo[] getProvisionedFeatures() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getIncluded()
	 */
	public BundleInfo[] getIncluded() {
		if (fIncluded == null) {
			return null;
		}
		return (BundleInfo[]) fIncluded.toArray(new BundleInfo[fIncluded.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#addIncluded(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void addIncluded(BundleInfo[] toAdd) {
		if (fIncluded == null) {
			fIncluded = new HashSet();
		}
		for (int i = 0; i < toAdd.length; i++) {
			fIncluded.add(toAdd[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#removeIncluded(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void removeIncluded(BundleInfo[] toRemove) {
		if (fIncluded != null) {
			for (int i = 0; i < toRemove.length; i++) {
				fIncluded.remove(toRemove[i]);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#clearIncluded()
	 */
	public void clearIncluded() {
		fIncluded = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getOptional()
	 */
	public BundleInfo[] getOptional() {
		if (fOptional == null) {
			return null;
		}
		return (BundleInfo[]) fOptional.toArray(new BundleInfo[fOptional.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#addOptional(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void addOptional(BundleInfo[] toAdd) {
		if (fOptional == null) {
			fOptional = new HashSet();
		}
		for (int i = 0; i < toAdd.length; i++) {
			fOptional.add(toAdd[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#removeOptional(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void removeOptional(BundleInfo[] toRemove) {
		if (fOptional != null) {
			for (int i = 0; i < toRemove.length; i++) {
				fOptional.remove(toRemove[i]);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#clearOptional()
	 */
	public void clearOptional() {
		fOptional = null;
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
	public BundleInfo[] getImplicitDependencies() {
		return fImplicit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getResolvedImplicitDependencies()
	 */
//	public IResolvedBundle[] getResolvedImplicitDependencies() {
//		// TODO Use new API
//		return null;
//		int size = 0;
//		if (fImplicit != null) {
//			size = fImplicit.length;
//		}
//		if (size == 0) {
//			return new IResolvedBundle[0];
//		}
//		return AbstractBundleContainer.getMatchingBundles(getBundles(), fImplicit, null, null);
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setImplicitDependencies(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void setImplicitDependencies(BundleInfo[] bundles) {
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
	 * Returns whether the content of this definition is equal to the content of the specified definition.
	 * 
	 * @param definition
	 * @return whether the content of this definition is equal to the content of the specified definition
	 */
	public boolean isContentEqual(ITargetDefinition definition) {
		if (isNullOrEqual(getName(), definition.getName()) && isNullOrEqual(getArch(), definition.getArch()) && isNullOrEqual(getNL(), definition.getNL()) && isNullOrEqual(getOS(), definition.getOS()) && isNullOrEqual(getWS(), definition.getWS()) && isNullOrEqual(getProgramArguments(), definition.getProgramArguments()) && isNullOrEqual(getVMArguments(), definition.getVMArguments()) && isNullOrEqual(getJREContainer(), definition.getJREContainer())) {
			// check containers and implicit dependencies
			IBundleContainer[] c1 = getBundleContainers();
			IBundleContainer[] c2 = definition.getBundleContainers();
			if (areContainersEqual(c1, c2)) {
				return areEqual(getImplicitDependencies(), definition.getImplicitDependencies());
			}
		}
		return false;
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
		if (isNullOrEqual(getArch(), definition.getArch()) && isNullOrEqual(getNL(), definition.getNL()) && isNullOrEqual(getOS(), definition.getOS()) && isNullOrEqual(getWS(), definition.getWS()) && isArgsNullOrEqual(getProgramArguments(), definition.getProgramArguments()) && isArgsNullOrEqual(getVMArguments(), definition.getVMArguments()) && isNullOrEqual(getJREContainer(), definition.getJREContainer())) {
			// check containers and implicit dependencies
			IBundleContainer[] c1 = getBundleContainers();
			IBundleContainer[] c2 = definition.getBundleContainers();
			if (areContainersEqual(c1, c2)) {
				return areEqual(getImplicitDependencies(), definition.getImplicitDependencies());
			}
		}
		return false;
	}

	private boolean areEqual(BundleInfo[] c1, BundleInfo[] c2) {
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
		buf.append("\nEnv: ").append(fOS).append("/").append(fWS).append("/").append(fArch).append("/").append(fNL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		buf.append("\nJRE: ").append(fJREContainer); //$NON-NLS-1$
		buf.append("\nArgs: ").append(fProgramArgs).append("/").append(fVMArgs); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nImplicit: ").append(fImplicit == null ? "null" : Integer.toString(fImplicit.length)); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nHandle: ").append(fHandle.toString()); //$NON-NLS-1$
		return buf.toString();
	}

}
