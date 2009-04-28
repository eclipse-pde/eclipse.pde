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
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
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

	// bundle containers
	private IBundleContainer[] fContainers;

	// handle
	private ITargetHandle fHandle;

	// implicit dependencies
	private BundleInfo[] fImplicit;

	/**
	 * Constructs a target definition based on the given handle. 
	 */
	TargetDefinition(ITargetHandle handle) {
		fHandle = handle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getArch()
	 */
	public String getArch() {
		return fArch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getBundleContainers()
	 */
	public IBundleContainer[] getBundleContainers() {
		return fContainers;
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
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setBundleContainers(org.eclipse.pde.internal.core.target.provisional.IBundleContainer[])
	 */
	public void setBundleContainers(IBundleContainer[] containers) {
		if (containers != null && containers.length == 0) {
			containers = null;
		}
		fContainers = containers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus resolve(IProgressMonitor monitor) {
		IBundleContainer[] containers = getBundleContainers();
		int num = 0;
		if (containers != null) {
			num = containers.length;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.TargetDefinition_1, num * 10);
		try {
			MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_2, null);
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (subMonitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					subMonitor.subTask(Messages.TargetDefinition_4);
					IStatus s = containers[i].resolve(this, subMonitor.newChild(10));
					if (!s.isOK()) {
						status.add(s);
					}
				}
			}
			if (status.isOK()) {
				return Status.OK_STATUS;
			}
			return status;
		} finally {
			subMonitor.done();
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#isResolved()
	 */
	public boolean isResolved() {
		IBundleContainer[] containers = getBundleContainers();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				IBundleContainer container = containers[i];
				if (!container.isResolved()) {
					return false;
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getBundleStatus()
	 */
	public IStatus getBundleStatus() {
		if (isResolved()) {
			IBundleContainer[] containers = getBundleContainers();
			if (containers != null) {
				MultiStatus result = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_5, null);
				for (int i = 0; i < containers.length; i++) {
					IBundleContainer container = containers[i];
					IStatus containerStatus = container.getBundleStatus();
					if (containerStatus != null) {
						result.add(containerStatus);
					}
				}
				if (result.isOK()) {
					// Return generic ok status instead of problem multi-status with no children
					return Status.OK_STATUS;
				}
				return result;
			}
			return Status.OK_STATUS;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getBundles()
	 */
	public IResolvedBundle[] getBundles() {
		return getBundles(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getAllBundles()
	 */
	public IResolvedBundle[] getAllBundles() {
		return getBundles(true);
	}

	/**
	 * Gathers and returns all or included bundles in this target or <code>null</code> if
	 * not resolved.
	 * 
	 * @param allBundles whether to consider all bundles, or just those included/optional
	 * @return bundles or <code>null</code>
	 */
	private IResolvedBundle[] getBundles(boolean allBundles) {
		if (isResolved()) {
			IBundleContainer[] containers = getBundleContainers();
			if (containers != null) {
				List all = new ArrayList();
				for (int i = 0; i < containers.length; i++) {
					IBundleContainer container = containers[i];
					IResolvedBundle[] bundles = null;
					if (allBundles) {
						bundles = container.getAllBundles();
					} else {
						bundles = container.getBundles();
					}
					for (int j = 0; j < bundles.length; j++) {
						IResolvedBundle rb = bundles[j];
						all.add(rb);
					}
				}
				return (IResolvedBundle[]) all.toArray(new IResolvedBundle[all.size()]);
			}
			return new IResolvedBundle[0];
		}
		return null;
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
	public IResolvedBundle[] getResolvedImplicitDependencies() {
		int size = 0;
		if (fImplicit != null) {
			size = fImplicit.length;
		}
		if (size == 0) {
			return new IResolvedBundle[0];
		}
		return AbstractBundleContainer.getMatchingBundles(getBundles(), fImplicit, null, null);
	}

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
		if (c1.length == c2.length) {
			for (int i = 0; i < c2.length; i++) {
				AbstractBundleContainer ac1 = (AbstractBundleContainer) c1[i];
				AbstractBundleContainer ac2 = (AbstractBundleContainer) c2[i];
				if (!ac1.isContentEqual(ac2)) {
					return false;
				}
			}
			return true;
		}
		return false;
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

	/**
	 * Returns the existing profile for this target definition or <code>null</code> if none.
	 *  
	 * @return profile or <code>null</code>
	 */
	public IProfile findProfile() {
		IProfileRegistry registry = AbstractTargetHandle.getProfileRegistry();
		if (registry != null) {
			AbstractTargetHandle handle = ((AbstractTargetHandle) getHandle());
			String id;
			try {
				id = handle.getProfileId();
				return registry.getProfile(id);
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/**
	 * Returns the profile for the this target handle, creating one if required.
	 * 
	 * @return profile
	 * @throws CoreException in unable to retrieve profile
	 */
	public IProfile getProfile() throws CoreException {
		IProfileRegistry registry = AbstractTargetHandle.getProfileRegistry();
		if (registry == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AbstractTargetHandle_0));
		}
		AbstractTargetHandle handle = ((AbstractTargetHandle) getHandle());
		String id = handle.getProfileId();
		IProfile profile = registry.getProfile(id);
		if (profile != null) {
			// ensure environment & NL settings are still the same (else we need a new profile)
			String property = generateEnvironmentProperties();
			String value = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
			boolean recreate = false;
			if (!property.equals(value)) {
				recreate = true;
			}
			if (!recreate) {
				property = generateNLProperty();
				value = profile.getProperty(IProfile.PROP_NL);
				if (!property.equals(value)) {
					recreate = true;
				}
			}
			if (!recreate) {
				// check top level IU's. If any have been removed from the containers that are
				// still in the profile, we need to recreate (rather than uninstall)
				Collector collector = profile.query(new IUProfilePropertyQuery(profile, AbstractTargetHandle.PROP_INSTALLED_IU, Boolean.toString(true)), new Collector(), null);
				Iterator iterator = collector.iterator();
				if (iterator.hasNext()) {
					Set installedIUs = new HashSet();
					while (iterator.hasNext()) {
						IInstallableUnit unit = (IInstallableUnit) iterator.next();
						installedIUs.add(new IUDescriptor(unit.getId(), unit.getVersion().toString()));
					}
					IBundleContainer[] containers = getBundleContainers();
					if (containers != null) {
						for (int i = 0; i < containers.length; i++) {
							if (containers[i] instanceof IUBundleContainer) {
								IUBundleContainer bc = (IUBundleContainer) containers[i];
								String[] ids = bc.getIds();
								Version[] versions = bc.getVersions();
								for (int j = 0; j < versions.length; j++) {
									installedIUs.remove(new IUDescriptor(ids[j], versions[j].toString()));
								}
							}
						}
					}
					if (!installedIUs.isEmpty()) {
						recreate = true;
					}
				}
			}
			if (recreate) {
				handle.deleteProfile();
				profile = null;
			}
		}
		if (profile == null) {
			// create profile
			Map properties = new HashMap();
			properties.put(IProfile.PROP_INSTALL_FOLDER, AbstractTargetHandle.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
			properties.put(IProfile.PROP_CACHE, AbstractTargetHandle.BUNDLE_POOL.toOSString());
			properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
			// set up environment & NL properly so OS specific fragments are down loaded/installed
			properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());
			properties.put(IProfile.PROP_NL, generateNLProperty());
			profile = registry.addProfile(id, properties);
		}
		return profile;
	}

	/**
	 * Generates the environment properties string for this target definition's p2 profile.
	 * 
	 * @return environment properties
	 */
	private String generateEnvironmentProperties() {
		// TODO: are there constants for these keys?
		StringBuffer env = new StringBuffer();
		String ws = getWS();
		if (ws == null) {
			ws = Platform.getWS();
		}
		env.append("osgi.ws="); //$NON-NLS-1$
		env.append(ws);
		env.append(","); //$NON-NLS-1$
		String os = getOS();
		if (os == null) {
			os = Platform.getOS();
		}
		env.append("osgi.os="); //$NON-NLS-1$
		env.append(os);
		env.append(","); //$NON-NLS-1$
		String arch = getArch();
		if (arch == null) {
			arch = Platform.getOSArch();
		}
		env.append("osgi.arch="); //$NON-NLS-1$
		env.append(arch);
		return env.toString();
	}

	/**
	 * Generates the NL property for this target definition's p2 profile.
	 * 
	 * @return NL profile property
	 */
	private String generateNLProperty() {
		String nl = getNL();
		if (nl == null) {
			nl = Platform.getNL();
		}
		return nl;
	}
}
