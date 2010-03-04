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
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
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

	// included and optional filtering
	private NameVersionDescriptor[] fIncluded;
	private NameVersionDescriptor[] fOptional;

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
	private NameVersionDescriptor[] fImplicit;

	// internal settings for UI mode (how content is displayed to the user
	private int fUIMode = MODE_PLUGIN;
	public static final int MODE_PLUGIN = 0;
	public static final int MODE_FEATURE = 1;

	// internal caches for feature based targets
	private IFeatureModel[] fFeatureModels;
	private IResolvedBundle[] fOtherBundles;

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
		// Clear the feature model cache as it is based on the bundle container locations
		fFeatureModels = null;
		fOtherBundles = null;

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
				// Check if the containers have any resolution problems
				MultiStatus result = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_5, null);
				for (int i = 0; i < containers.length; i++) {
					IBundleContainer container = containers[i];
					IStatus containerStatus = container.getStatus();
					if (containerStatus != null && !containerStatus.isOK()) {
						result.add(containerStatus);
					}
				}

				// Check if any of the included bundles have problems
				// build status from bundle list
				IResolvedBundle[] bundles = getBundles();
				for (int i = 0; i < bundles.length; i++) {
					if (!bundles[i].getStatus().isOK()) {
						result.add(bundles[i].getStatus());
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
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setIncluded(org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor[])
	 */
	public void setIncluded(NameVersionDescriptor[] included) {
		fIncluded = included;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getIncluded()
	 */
	public NameVersionDescriptor[] getIncluded() {
		return fIncluded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#setOptional(org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor[])
	 */
	public void setOptional(NameVersionDescriptor[] optional) {
		fOptional = optional;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetDefinition#getOptional()
	 */
	public NameVersionDescriptor[] getOptional() {
		return fOptional;
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
					IResolvedBundle[] bundles = container.getBundles();
					for (int j = 0; j < bundles.length; j++) {
						IResolvedBundle rb = bundles[j];
						all.add(rb);
					}
				}

				IResolvedBundle[] allResolvedBundles = (IResolvedBundle[]) all.toArray(new IResolvedBundle[all.size()]);
				if (allBundles) {
					return allResolvedBundles;
				}
				return filterBundles(allResolvedBundles, getIncluded());
			}
			return new IResolvedBundle[0];
		}
		return null;
	}

	private IResolvedBundle[] filterBundles(IResolvedBundle[] bundles, NameVersionDescriptor[] filter) {
		if (filter == null) {
			// All bundles are included, but still need to check for optional bundles
			IBundleContainer parent = fContainers != null && fContainers.length > 0 ? fContainers[0] : null;
			return getMatchingBundles(bundles, null, fOptional, parent);
		}
		if (filter.length == 0) {
			return new IResolvedBundle[0];
		}

		// If there are features, don't set errors for missing bundles as they are caused by missing OS specific fragments
		boolean containsFeatures = false;

		List included = new ArrayList();
		// For feature filters, get the list of included bundles, for bundle filters just add them to the list
		for (int i = 0; i < filter.length; i++) {
			if (filter[i].getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				included.add(filter[i]);
			} else if (filter[i].getType() == NameVersionDescriptor.TYPE_FEATURE) {
				containsFeatures = true;
				IFeatureModel[] features = getFeatureModels();
				IFeatureModel bestMatch = null;
				for (int j = 0; j < features.length; j++) {
					if (features[j].getFeature().getId().equals(filter[i].getId())) {
						if (filter[i].getVersion() != null) {
							// Try to find an exact feature match
							if (filter[i].getVersion().equals(features[j].getFeature().getVersion())) {
								// Exact match
								bestMatch = features[j];
								break;
							}
						} else if (bestMatch != null) {
							// If no version specified take the highest version
							Version v1 = Version.parseVersion(features[j].getFeature().getVersion());
							Version v2 = Version.parseVersion(bestMatch.getFeature().getVersion());
							if (v1.compareTo(v2) > 0) {
								bestMatch = features[j];
							}
						}

						if (bestMatch == null) {
							// If we can't find a version match, just take any name match
							bestMatch = features[j];
						}
					}
				}

				// Add the required plugins from the feature to the list of includes
				if (bestMatch != null) {
					IFeaturePlugin[] plugins = bestMatch.getFeature().getPlugins();
					for (int j = 0; j < plugins.length; j++) {
						included.add(new NameVersionDescriptor(plugins[j].getId(), plugins[j].getVersion()));
					}
				}
			}
		}

		// Return matching bundles
		IBundleContainer parent = fContainers != null && fContainers.length > 0 ? fContainers[0] : null;
		return getMatchingBundles(bundles, (NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]), fOptional, containsFeatures ? null : parent);
	}

	/**
	 * Returns bundles from the specified collection that match the symbolic names
	 * and/or version in the specified criteria. When no version is specified
	 * the newest version (if any) is selected.
	 * <p>
	 * If a parent error container is specified, bundles listed in the included and optional filters that
	 * are not found in the given collection will be added as IResolvedBundles with error statuses explaining
	 * the problem.  If no parent container is specified, missing included and optional bundles will be ignored.
	 * </p> 
	 * @param collection bundles to resolve against match criteria
	 * @param included bundles to include or <code>null</code> if no restrictions
	 * @param optional optional bundles or <code>null</code> of no optional bundles
	 * @param errorParentContainer 
	 * 
	 * @return bundles that match this container's restrictions
	 */
	static IResolvedBundle[] getMatchingBundles(IResolvedBundle[] collection, NameVersionDescriptor[] included, NameVersionDescriptor[] optional, IBundleContainer errorParentContainer) {
		if (included == null && optional == null) {
			return collection;
		}
		// map bundles names to available versions
		Map bundleMap = new HashMap(collection.length);
		for (int i = 0; i < collection.length; i++) {
			IResolvedBundle resolved = collection[i];
			List list = (List) bundleMap.get(resolved.getBundleInfo().getSymbolicName());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(resolved.getBundleInfo().getSymbolicName(), list);
			}
			list.add(resolved);
		}
		List resolved = new ArrayList();
		if (included == null) {
			for (int i = 0; i < collection.length; i++) {
				resolved.add(collection[i]);
			}
		} else {
			for (int i = 0; i < included.length; i++) {
				BundleInfo info = new BundleInfo(included[i].getId(), included[i].getVersion(), null, BundleInfo.NO_LEVEL, false);
				IResolvedBundle bundle = resolveBundle(bundleMap, info, false, errorParentContainer);
				if (bundle != null) {
					resolved.add(bundle);
				}
			}
		}
		if (optional != null) {
			for (int i = 0; i < optional.length; i++) {
				BundleInfo option = new BundleInfo(optional[i].getId(), optional[i].getVersion(), null, BundleInfo.NO_LEVEL, false);
				IResolvedBundle resolveBundle = resolveBundle(bundleMap, option, true, errorParentContainer);
				if (resolveBundle != null) {
					IStatus status = resolveBundle.getStatus();
					if (status.isOK()) {
						// add to list if not there already
						if (!resolved.contains(resolveBundle)) {
							resolved.add(resolveBundle);
						}
					} else {
						// missing optional bundle - add it to the list
						resolved.add(resolveBundle);
					}
				}
			}
		}
		return (IResolvedBundle[]) resolved.toArray(new IResolvedBundle[resolved.size()]);
	}

	/**
	 * Resolves a bundle for the given info from the given map. The map contains
	 * keys of symbolic names and values are lists of {@link IResolvedBundle}'s available
	 * that match the names.
	 * <p>
	 * If an parent container for errors is provided, if a resolve bundle matching the requirements cannot be found
	 * a IResolvedBundle will be returned containing an status.  If no parent container is specified,
	 * missing bundles will result in a return value of <code>null</code>
	 * </p>
	 * 
	 * @param bundleMap available bundles to resolve against
	 * @param info name and version to match against
	 * @param optional whether the bundle is optional
	 * @param errorParentContainer bundle container the resolved bundle belongs too
	 * @return resolved bundle or <code>null</code>
	 */
	private static IResolvedBundle resolveBundle(Map bundleMap, BundleInfo info, boolean optional, IBundleContainer errorParentContainer) {
		List list = (List) bundleMap.get(info.getSymbolicName());
		if (list != null) {
			String version = info.getVersion();
			if (version == null) {
				// select newest
				if (list.size() > 1) {
					// sort the list
					Collections.sort(list, new Comparator() {
						public int compare(Object o1, Object o2) {
							BundleInfo b1 = ((IResolvedBundle) o1).getBundleInfo();
							BundleInfo b2 = ((IResolvedBundle) o2).getBundleInfo();
							return b1.getVersion().compareTo(b2.getVersion());
						}
					});
				}
				// select the last one
				ResolvedBundle rb = (ResolvedBundle) list.get(list.size() - 1);
				rb.setOptional(optional);
				return rb;
			}
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				IResolvedBundle bundle = (IResolvedBundle) iterator.next();
				if (bundle.getBundleInfo().getVersion().equals(version)) {
					((ResolvedBundle) bundle).setOptional(optional);
					return bundle;
				}
			}
			// VERSION DOES NOT EXIST
			if (errorParentContainer == null) {
				return null;
			}
			int sev = IStatus.ERROR;
			String message = NLS.bind(Messages.AbstractBundleContainer_1, new Object[] {info.getVersion(), info.getSymbolicName()});
			if (optional) {
				sev = IStatus.INFO;
				message = NLS.bind(Messages.AbstractBundleContainer_2, new Object[] {info.getVersion(), info.getSymbolicName()});
			}
			return new ResolvedBundle(info, errorParentContainer, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null), null, optional, false);
		}
		// DOES NOT EXIST
		if (errorParentContainer == null) {
			return null;
		}
		int sev = IStatus.ERROR;
		String message = NLS.bind(Messages.AbstractBundleContainer_3, info.getSymbolicName());
		if (optional) {
			sev = IStatus.INFO;
			message = NLS.bind(Messages.AbstractBundleContainer_4, info.getSymbolicName());
		}
		return new ResolvedBundle(info, errorParentContainer, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_DOES_NOT_EXIST, message, null), null, optional, false);
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
	 * Returns whether the content of this definition is equal to the content of the specified definition.
	 * 
	 * @param definition
	 * @return whether the content of this definition is equal to the content of the specified definition
	 */
	public boolean isContentEqual(ITargetDefinition definition) {
		if (isNullOrEqual(getName(), definition.getName()) && isNullOrEqual(getArch(), definition.getArch()) && isNullOrEqual(getNL(), definition.getNL()) && isNullOrEqual(getOS(), definition.getOS()) && isNullOrEqual(getWS(), definition.getWS()) && isNullOrEqual(getProgramArguments(), definition.getProgramArguments()) && isNullOrEqual(getVMArguments(), definition.getVMArguments()) && isNullOrEqual(getJREContainer(), definition.getJREContainer())) {
			// Check includes/optional
			if (isNullOrEqual(getIncluded(), definition.getIncluded()) && isNullOrEqual(getOptional(), definition.getOptional())) {
				// Check containers
				IBundleContainer[] c1 = getBundleContainers();
				IBundleContainer[] c2 = definition.getBundleContainers();
				if (areContainersEqual(c1, c2)) {
					// Check implicit dependencies
					return isNullOrEqual(getImplicitDependencies(), definition.getImplicitDependencies());
				}
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
			// Check includes/optional
			if (isNullOrEqual(getIncluded(), definition.getIncluded()) && isNullOrEqual(getOptional(), definition.getOptional())) {
				// Check containers
				IBundleContainer[] c1 = getBundleContainers();
				IBundleContainer[] c2 = definition.getBundleContainers();
				if (areContainersEqual(c1, c2)) {
					// Check implicit dependencies
					return isNullOrEqual(getImplicitDependencies(), definition.getImplicitDependencies());
				}
			}
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

	/**
	 * Returns whether the arrays have equal contents or are both <code>null</code>.
	 * 
	 * @param objects1
	 * @param objects2
	 * @return whether the arrays have equal contents or are both <code>null</code>
	 */
	private boolean isNullOrEqual(Object[] objects1, Object[] objects2) {
		if (objects1 == null) {
			return objects2 == null;
		}
		if (objects2 == null) {
			return false;
		}
		if (objects1.length == objects2.length) {
			for (int i = 0; i < objects1.length; i++) {
				if (!objects1[i].equals(objects2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
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
	 * Returns whether software site containers are configured to provision for all environments
	 * versus a single environment.
	 * 
	 * @return whether all environments will be provisioned
	 */
	private boolean isAllEnvironments() {
		IBundleContainer[] containers = getBundleContainers();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof IUBundleContainer) {
					IUBundleContainer iu = (IUBundleContainer) containers[i];
					if (iu.getIncludeAllEnvironments()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns the mode used to provision this target - slice versus plan or <code>null</code> if
	 * this target has no software sites.
	 * 
	 * @return provisioning mode or <code>null</code>
	 */
	private String getProvisionMode() {
		IBundleContainer[] containers = getBundleContainers();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof IUBundleContainer) {
					IUBundleContainer iu = (IUBundleContainer) containers[i];
					if (iu.getIncludeAllRequired()) {
						return TargetDefinitionPersistenceHelper.MODE_PLANNER;
					}
					return TargetDefinitionPersistenceHelper.MODE_SLICER;
				}
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
			boolean recreate = false;
			// check if all environments setting is the same
			boolean all = false;
			String value = profile.getProperty(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS);
			if (value != null) {
				all = Boolean.valueOf(value).booleanValue();
				if (!Boolean.toString(isAllEnvironments()).equals(value)) {
					recreate = true;
				}
			}
			// ensure environment & NL settings are still the same (else we need a new profile)
			String property = null;
			if (!recreate && !all) {
				property = generateEnvironmentProperties();
				value = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
				if (!property.equals(value)) {
					recreate = true;
				}
			}
			// check provisioning mode: slice versus plan
			String mode = getProvisionMode();
			if (mode != null) {
				value = profile.getProperty(AbstractTargetHandle.PROP_PROVISION_MODE);
				if (!mode.equals(value)) {
					recreate = true;
				}
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
				IUProfilePropertyQuery propertyQuery = new IUProfilePropertyQuery(AbstractTargetHandle.PROP_INSTALLED_IU, Boolean.toString(true));
				IQueryResult queryResult = profile.query(propertyQuery, null);
				Iterator iterator = queryResult.iterator();
				if (iterator.hasNext()) {
					Set installedIUs = new HashSet();
					while (iterator.hasNext()) {
						IInstallableUnit unit = (IInstallableUnit) iterator.next();
						installedIUs.add(new NameVersionDescriptor(unit.getId(), unit.getVersion().toString()));
					}
					IBundleContainer[] containers = getBundleContainers();
					if (containers != null) {
						for (int i = 0; i < containers.length; i++) {
							if (containers[i] instanceof IUBundleContainer) {
								IUBundleContainer bc = (IUBundleContainer) containers[i];
								String[] ids = bc.getIds();
								Version[] versions = bc.getVersions();
								for (int j = 0; j < versions.length; j++) {
									installedIUs.remove(new NameVersionDescriptor(ids[j], versions[j].toString()));
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
			String mode = getProvisionMode();
			if (mode != null) {
				properties.put(AbstractTargetHandle.PROP_PROVISION_MODE, mode);
				properties.put(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS, Boolean.toString(isAllEnvironments()));
			}
			profile = registry.addProfile(id, properties);
		}
		return profile;
	}

	/**
	 * Returns the set of feature models available in this target, will return a cached copy if available
	 * 
	 * @return set of features available in this target, possibly empty.
	 */
	public IFeatureModel[] getFeatureModels() {
		if (fFeatureModels != null) {
			return fFeatureModels;
		}

		IBundleContainer[] containers = getBundleContainers();

		String path = null;
		if (containers != null && containers.length > 0) {
			try {
				path = ((AbstractBundleContainer) containers[0]).getLocation(true);
			} catch (CoreException e) {
				PDECore.log(e);
				return new IFeatureModel[0];
			}
		}
		if (path == null) {
			path = TargetPlatform.getDefaultLocation();
		} else {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				path = manager.performStringSubstitution(path);
			} catch (CoreException e) {
				PDECore.log(e);
				return new IFeatureModel[0];
			}
		}

		ArrayList additional = new ArrayList();
		// secondary containers are considered additional
		if (containers != null && containers.length > 1) {
			for (int i = 1; i < containers.length; i++) {
				try {
					additional.add(((AbstractBundleContainer) containers[i]).getLocation(true));
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
		}

		fFeatureModels = ExternalFeatureModelManager.createModels(path, additional, null);
		return fFeatureModels;
	}

	/**
	 * Returns the set of IResolvedBundle available in this target that are not part of any features, will return a cached copy if available
	 * 
	 * @return set of resolved bundles available in this target that don't belong to any features, possibly empty
	 */
	public IResolvedBundle[] getOtherBundles() {
		if (fOtherBundles != null) {
			return fOtherBundles;
		}

		IResolvedBundle[] allBundles = getAllBundles();
		Map remaining = new HashMap();
		for (int i = 0; i < allBundles.length; i++) {
			remaining.put(allBundles[i].getBundleInfo().getSymbolicName(), allBundles[i]);
		}

		IFeatureModel[] features = getFeatureModels();
		for (int i = 0; i < features.length; i++) {
			IFeaturePlugin[] plugins = features[i].getFeature().getPlugins();
			for (int j = 0; j < plugins.length; j++) {
				remaining.remove(plugins[j].getId());
			}
		}

		Collection values = remaining.values();
		fOtherBundles = (IResolvedBundle[]) values.toArray(new IResolvedBundle[values.size()]);
		return fOtherBundles;
	}

	/**
	 * Convenience method to return the set of IFeatureModels that are included in this
	 * target as well as any other included plug-ins as IResolvedBundles (that are not part 
	 * of the features). Will return <code>null</code> if this target has not been resolved.
	 * 
	 * @see #getFeatureModels()
	 * @see #getOtherBundles()
	 * @return set of IFeatureModels and IResolvedBundles or <code>mull</code>
	 */
	public Object[] getFeaturesAndBundles() {
		if (!isResolved()) {
			return null;
		}

		IFeatureModel[] allFeatures = getFeatureModels();
		IResolvedBundle[] allExtraBundles = getOtherBundles();
		NameVersionDescriptor[] included = getIncluded();
		NameVersionDescriptor[] optional = getOptional();

		if (included == null && optional == null) {
			Set result = new HashSet();
			result.addAll(Arrays.asList(allFeatures));
			result.addAll(Arrays.asList(allExtraBundles));
			return result.toArray();
		}

		List result = new ArrayList();
		for (int i = 0; i < included.length; i++) {
			if (included[i].getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				for (int j = 0; j < allExtraBundles.length; j++) {
					if (allExtraBundles[j].getBundleInfo().getSymbolicName().equals(included[i].getId())) {
						result.add(allExtraBundles[j]);
					}
				}
			} else if (included[i].getType() == NameVersionDescriptor.TYPE_FEATURE) {
				for (int j = 0; j < allFeatures.length; j++) {
					if (allFeatures[j].getFeature().getId().equals(included[i].getId())) {
						result.add(allFeatures[j]);
					}
				}
			}
		}

		if (optional != null) {
			for (int i = 0; i < optional.length; i++) {
				for (int j = 0; j < allExtraBundles.length; j++) {
					if (allExtraBundles[j].getBundleInfo().getSymbolicName().equals(optional[i].getId())) {
						result.add(allExtraBundles[j]);
					}
				}
			}
		}

		return result.toArray();
	}

	public int getUIMode() {
		return fUIMode;
	}

	public void setUIMode(int mode) {
		fUIMode = mode;
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
