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
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
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
	private ITargetLocation[] fContainers;

	// handle
	private ITargetHandle fHandle;

	// implicit dependencies
	private NameVersionDescriptor[] fImplicit;

	// internal settings for UI mode (how content is displayed to the user
	private int fUIMode = MODE_PLUGIN;
	public static final int MODE_PLUGIN = 0;
	public static final int MODE_FEATURE = 1;

	// cache of features found for a given location, maps a string path location to a array of IFeatureModels (IFeatureModel[])
	private Map fFeaturesInLocation = new HashMap();

	// internal cache for features.  A target managed by features will contain a set of features as well as a set of plug-ins that don't belong to a feature
	private TargetFeature[] fFeatures;
	private TargetBundle[] fOtherBundles;

	private int fSequenceNumber = -1;

	/**
	 * Constructs a target definition based on the given handle. 
	 */
	TargetDefinition(ITargetHandle handle) {
		fHandle = handle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getArch()
	 */
	public String getArch() {
		return fArch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getBundleContainers()
	 */
	public ITargetLocation[] getTargetLocations() {
		return fContainers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getNL()
	 */
	public String getNL() {
		return fNL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getOS()
	 */
	public String getOS() {
		return fOS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getProgramArguments()
	 */
	public String getProgramArguments() {
		return fProgramArgs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getVMArguments()
	 */
	public String getVMArguments() {
		return fVMArgs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getWS()
	 */
	public String getWS() {
		return fWS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setArch(java.lang.String)
	 */
	public void setArch(String arch) {
		incrementSequenceNumber();
		fArch = arch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setNL(java.lang.String)
	 */
	public void setNL(String nl) {
		incrementSequenceNumber();
		fNL = nl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setOS(java.lang.String)
	 */
	public void setOS(String os) {
		incrementSequenceNumber();
		fOS = os;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setProgramArguments(java.lang.String)
	 */
	public void setProgramArguments(String args) {
		if (args != null && args.length() == 0) {
			args = null;
		}
		fProgramArgs = args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setVMArguments(java.lang.String)
	 */
	public void setVMArguments(String args) {
		if (args != null && args.length() == 0) {
			args = null;
		}
		fVMArgs = args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setWS(java.lang.String)
	 */
	public void setWS(String ws) {
		incrementSequenceNumber();
		fWS = ws;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setBundleContainers(org.eclipse.pde.core.target.ITargetLocation[])
	 */
	public void setTargetLocations(ITargetLocation[] locations) {
		incrementSequenceNumber();
		// Clear the feature model cache as it is based on the bundle container locations
		fFeatures = null;
		fOtherBundles = null;

		if (locations != null && locations.length == 0) {
			locations = null;
		}

		fContainers = locations;

		if (locations == null) {
			fIncluded = null;
		} else {
			for (int i = 0; i < locations.length; i++) {
				if (locations[i] instanceof AbstractBundleContainer) {
					((AbstractBundleContainer) locations[i]).associateWithTarget(this);
				}
			}
		}
	}

	/**
	 * Clears the any models that are cached for the given container location.
	 * 
	 * @param location location to clear cache for or <code>null</code> to clear all cached models
	 */
	public void flushCaches(String location) {
		// Clear the feature model cache as it is based on the bundle container locations
		fFeatures = null;
		fOtherBundles = null;
		if (location == null) {
			fFeaturesInLocation.clear();
		} else {
			fFeaturesInLocation.remove(location);
		}
		if (fContainers == null) {
			fIncluded = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#resolve(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus resolve(IProgressMonitor monitor) {
		ITargetLocation[] containers = getTargetLocations();
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
	 * @see org.eclipse.pde.core.target.ITargetDefinition#isResolved()
	 */
	public boolean isResolved() {
		ITargetLocation[] containers = getTargetLocations();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				ITargetLocation container = containers[i];
				if (!container.isResolved()) {
					return false;
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getBundleStatus()
	 */
	public IStatus getStatus() {
		if (isResolved()) {
			ITargetLocation[] containers = getTargetLocations();
			if (containers != null) {
				// Check if the containers have any resolution problems
				MultiStatus result = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_5, null);
				for (int i = 0; i < containers.length; i++) {
					ITargetLocation container = containers[i];
					IStatus containerStatus = container.getStatus();
					if (containerStatus != null && !containerStatus.isOK()) {
						result.add(containerStatus);
					}
				}

				// Check if any of the included bundles have problems
				// build status from bundle list
				TargetBundle[] bundles = getBundles();
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
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setIncluded(org.eclipse.pde.core.target.NameVersionDescriptor[])
	 */
	public void setIncluded(NameVersionDescriptor[] included) {
		fIncluded = included;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getIncluded()
	 */
	public NameVersionDescriptor[] getIncluded() {
		return fIncluded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getBundles()
	 */
	public TargetBundle[] getBundles() {
		return getBundles(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getAllBundles()
	 */
	public TargetBundle[] getAllBundles() {
		return getBundles(true);
	}

	/**
	 * Gathers and returns all or included bundles in this target or <code>null</code> if
	 * not resolved.
	 * 
	 * @param allBundles whether to consider all bundles, or just those included/optional
	 * @return bundles or <code>null</code>
	 */
	private TargetBundle[] getBundles(boolean allBundles) {
		if (isResolved()) {
			ITargetLocation[] containers = getTargetLocations();
			if (containers != null) {
				List all = new ArrayList();
				for (int i = 0; i < containers.length; i++) {
					ITargetLocation container = containers[i];
					TargetBundle[] bundles = container.getBundles();
					if (bundles != null) {
						for (int j = 0; j < bundles.length; j++) {
							TargetBundle rb = bundles[j];
							all.add(rb);
						}
					}
				}

				TargetBundle[] allResolvedBundles = (TargetBundle[]) all.toArray(new TargetBundle[all.size()]);
				if (allBundles) {
					return allResolvedBundles;
				}
				return filterBundles(allResolvedBundles, getIncluded());
			}
			return new TargetBundle[0];
		}
		return null;
	}

	private TargetBundle[] filterBundles(TargetBundle[] bundles, NameVersionDescriptor[] filter) {
		if (filter == null) {
			// No filtering to do
			return bundles;
		}
		if (filter.length == 0) {
			return new TargetBundle[0];
		}

		// If there are features, don't set errors for missing bundles as they are caused by missing OS specific fragments
		boolean containsFeatures = false;

		// If there are any included features that are missing, add errors as resolved bundles (the same thing we would do for missing bundles)
		List missingFeatures = new ArrayList();

		List included = new ArrayList();
		// For feature filters, get the list of included bundles, for bundle filters just add them to the list
		for (int i = 0; i < filter.length; i++) {
			if (filter[i].getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				included.add(filter[i]);
			} else if (filter[i].getType() == NameVersionDescriptor.TYPE_FEATURE) {
				containsFeatures = true;
				TargetFeature[] features = getAllFeatures();
				TargetFeature bestMatch = null;
				for (int j = 0; j < features.length; j++) {
					TargetFeature feature = features[j];
					if (feature.getId().equals(filter[i].getId())) {
						if (filter[i].getVersion() != null) {
							// Try to find an exact feature match
							if (filter[i].getVersion().equals(feature.getVersion())) {
								// Exact match
								bestMatch = feature;
								break;
							}
						} else if (bestMatch != null) {
							// If no version specified take the highest version
							Version v1 = Version.parseVersion(feature.getVersion());
							Version v2 = Version.parseVersion(bestMatch.getVersion());
							if (v1.compareTo(v2) > 0) {
								bestMatch = feature;
							}
						}

						if (bestMatch == null) {
							// If we can't find a version match, just take any name match
							bestMatch = feature;
						}
					}
				}

				// Add the required plugins from the feature to the list of includes
				if (bestMatch != null) {
					NameVersionDescriptor[] plugins = bestMatch.getPlugins();
					for (int j = 0; j < plugins.length; j++) {
						included.add(plugins[j]);
					}
				} else {
					missingFeatures.add(filter[i]);
				}
			}
		}

		// Return matching bundles, if we are organizing by feature, do not create invalid target bundles for missing bundle includes
		List result = getMatchingBundles(bundles, (NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]), !containsFeatures);

		// Add in missing features as resolved bundles with error statuses
		if (containsFeatures && !missingFeatures.isEmpty()) {
			for (Iterator iterator = missingFeatures.iterator(); iterator.hasNext();) {
				NameVersionDescriptor missing = (NameVersionDescriptor) iterator.next();
				BundleInfo info = new BundleInfo(missing.getId(), missing.getVersion(), null, BundleInfo.NO_LEVEL, false);
				String message = NLS.bind(Messages.TargetDefinition_RequiredFeatureCouldNotBeFound, missing.getId());
				Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, message, null);
				result.add(new InvalidTargetBundle(info, status));
			}
		}

		return (TargetBundle[]) result.toArray(new TargetBundle[result.size()]);
	}

	/**
	 * Returns bundles from the specified collection that match the symbolic names
	 * and/or version in the specified criteria. When no version is specified
	 * the newest version (if any) is selected.
	 * <p>
	 * If handleMissingBundles is <code>true</code>, the returned list will contain {@link InvalidTargetBundle}s
	 * for any included filters that do not have a matching bundle in the collection. The invalid bundles
	 * will contain statuses describing what couldn't be matched.
	 * </p>
	 * @param collection bundles to resolve against match criteria
	 * @param included bundles to include or <code>null</code> if no restrictions
	 * @param handleMissingBundles whether to create {@link InvalidTargetBundle}s for missing includes
	 * 
	 * @return list of IResolvedBundle bundles that match this container's restrictions
	 */
	static List getMatchingBundles(TargetBundle[] collection, NameVersionDescriptor[] included, boolean handleMissingBundles) {
		if (included == null) {
			ArrayList result = new ArrayList();
			result.addAll(Arrays.asList(collection));
			return result;
		}
		// map bundles names to available versions
		Map bundleMap = new HashMap(collection.length);
		for (int i = 0; i < collection.length; i++) {
			TargetBundle resolved = collection[i];
			List list = (List) bundleMap.get(resolved.getBundleInfo().getSymbolicName());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(resolved.getBundleInfo().getSymbolicName(), list);
			}
			list.add(resolved);
		}
		List resolved = new ArrayList();

		for (int i = 0; i < included.length; i++) {
			BundleInfo info = new BundleInfo(included[i].getId(), included[i].getVersion(), null, BundleInfo.NO_LEVEL, false);
			TargetBundle bundle = resolveBundle(bundleMap, info, handleMissingBundles);
			if (bundle != null) {
				resolved.add(bundle);
			}
		}

		return resolved;
	}

	/**
	 * Resolves a bundle for the given info from the given map. The map contains
	 * keys of symbolic names and values are lists of {@link TargetBundle}'s available
	 * that match the names.
	 * <p>
	 * If handleMissingBundles is <code>true</code>, a {@link InvalidTargetBundle} will be created and 
	 * returned if the give info does not match up with a map entry. The returned bundle will have
	 * a status giving more details on what is missing. If handleMissingBundles is <code>false</code>,
	 * <code>null</code> will be returned.
	 * </p>
	 * 
	 * @param bundleMap available bundles to resolve against
	 * @param info name and version to match against
	 * @param handleMissingBundles whether to return an {@link InvalidTargetBundle} for a info that does not match with a map entry or <code>null</code>
	 * @return resolved bundle or <code>null</code>
	 */
	private static TargetBundle resolveBundle(Map bundleMap, BundleInfo info, boolean handleMissingBundles) {
		List list = (List) bundleMap.get(info.getSymbolicName());
		if (list != null) {
			String version = info.getVersion();
			if (version == null || version.equals(BundleInfo.EMPTY_VERSION)) {
				// select newest
				if (list.size() > 1) {
					// sort the list
					Collections.sort(list, new Comparator() {
						public int compare(Object o1, Object o2) {
							BundleInfo b1 = ((TargetBundle) o1).getBundleInfo();
							BundleInfo b2 = ((TargetBundle) o2).getBundleInfo();
							try {
								Version v1 = Version.create(b1.getVersion());
								Version v2 = Version.create(b2.getVersion());
								return v1.compareTo(v2);
							} catch (IllegalArgumentException e) {
								// If one of the bundles has a bad version
								PDECore.log(e);
								return b1.getVersion().compareTo(b2.getVersion());
							}
						}
					});
				}
				// select the last one
				TargetBundle rb = (TargetBundle) list.get(list.size() - 1);
				return rb;
			}
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				TargetBundle bundle = (TargetBundle) iterator.next();
				if (bundle.getBundleInfo().getVersion().equals(version)) {
					return bundle;
				}
			}

			// VERSION DOES NOT EXIST
			if (!handleMissingBundles) {
				return null;
			}
			int sev = IStatus.ERROR;
			String message = NLS.bind(Messages.AbstractBundleContainer_1, new Object[] {info.getVersion(), info.getSymbolicName()});
			IStatus status = new Status(sev, PDECore.PLUGIN_ID, TargetBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null);
			return new InvalidTargetBundle(info, status);
		}
		// DOES NOT EXIST
		if (!handleMissingBundles) {
			return null;
		}
		int sev = IStatus.ERROR;
		String message = NLS.bind(Messages.AbstractBundleContainer_3, info.getSymbolicName());
		IStatus status = new Status(sev, PDECore.PLUGIN_ID, TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, message, null);
		return new InvalidTargetBundle(info, status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getHandle()
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
			fIncluded = null;
			fName = null;
			fNL = null;
			fOS = null;
			fProgramArgs = null;
			fVMArgs = null;
			fWS = null;
			fSequenceNumber = 0;
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
		} catch (SAXException e) {
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
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getImplicitDependencies()
	 */
	public NameVersionDescriptor[] getImplicitDependencies() {
		return fImplicit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setImplicitDependencies(org.eclipse.pde.core.target.NameVersionDescriptor[])
	 */
	public void setImplicitDependencies(NameVersionDescriptor[] bundles) {
		if (bundles != null && bundles.length == 0) {
			bundles = null;
		}
		fImplicit = bundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getJREContainer()
	 */
	public IPath getJREContainer() {
		return fJREContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#setJREContainer(org.eclipse.core.runtime.IPath)
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
			if (isNullOrEqual(getIncluded(), definition.getIncluded())) {
				// Check containers
				ITargetLocation[] c1 = getTargetLocations();
				ITargetLocation[] c2 = definition.getTargetLocations();
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
			if (isNullOrEqual(getIncluded(), definition.getIncluded())) {
				// Check containers
				ITargetLocation[] c1 = getTargetLocations();
				ITargetLocation[] c2 = definition.getTargetLocations();
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

	private boolean areContainersEqual(ITargetLocation[] c1, ITargetLocation[] c2) {
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
	 * Returns a set of feature models that exist in the provided location.  If
	 * the locationPath is <code>null</code> the default target platform location
	 * will be used.  The locationPath string may container string variables which
	 * will be resolved.  This target definition may cache the feature models for
	 * faster retrieval. 
	 * 
	 * TODO When to clear the cache
	 * 
	 * @param locationPath string path to the directory containing features.  May container string variables or be <code>null</code>
	 * @return list of feature models found in the location, possible empty 
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem substituting a string variable
	 */
	public TargetFeature[] resolveFeatures(String locationPath, IProgressMonitor monitor) throws CoreException {
		String path = locationPath;
		if (path == null) {
			path = TargetPlatform.getDefaultLocation();
		} else {
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			path = manager.performStringSubstitution(path);
		}

		TargetFeature[] models = null;
		if (fFeaturesInLocation != null) {
			models = (TargetFeature[]) fFeaturesInLocation.get(path);
		}

		if (models != null) {
			return models; /*(IFeatureModel[])models.toArray(new IFeatureModel[models.size()]);*/
		}

		models = ExternalFeatureModelManager.createFeatures(path, new ArrayList(), monitor);
		fFeaturesInLocation.put(path, models);
		return models;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetDefinition#getAllFeatures()
	 */
	public TargetFeature[] getAllFeatures() {
		if (!isResolved()) {
			return null;
		}

		if (fFeatures != null) {
			return fFeatures;
		}

		ITargetLocation[] containers = getTargetLocations();

		// collect up all features from all containers and remove duplicates.
		Map result = new HashMap();
		if (containers != null && containers.length > 0) {
			for (int i = 0; i < containers.length; i++) {
				TargetFeature[] currentFeatures = containers[i].getFeatures();
				if (currentFeatures != null && currentFeatures.length > 0) {
					for (int j = 0; j < currentFeatures.length; j++) {
						TargetFeature feature = currentFeatures[j];
						if (feature.getId() != null) {
							// Don't allow features with null ids, Bug 377563
							NameVersionDescriptor key = new NameVersionDescriptor(feature.getId(), feature.getVersion());
							result.put(key, feature);
						}
					}
				}
			}
		}

		fFeatures = (TargetFeature[]) result.values().toArray(new TargetFeature[result.size()]);
		return fFeatures;
	}

	/**
	 * Returns the set of IResolvedBundle available in this target that are not part of any features, will return a cached copy if available
	 * 
	 * @see #getAllFeatures()
	 * @return set of resolved bundles available in this target that don't belong to any features, possibly empty
	 */
	public TargetBundle[] getOtherBundles() {
		if (!isResolved()) {
			return null;
		}

		if (fOtherBundles != null) {
			return fOtherBundles;
		}

		TargetBundle[] allBundles = getAllBundles();
		Map remaining = new HashMap();
		for (int i = 0; i < allBundles.length; i++) {
			remaining.put(allBundles[i].getBundleInfo().getSymbolicName(), allBundles[i]);
		}

		TargetFeature[] features = getAllFeatures();
		for (int i = 0; i < features.length; i++) {
			NameVersionDescriptor[] plugins = features[i].getPlugins();
			for (int j = 0; j < plugins.length; j++) {
				remaining.remove(plugins[j].getId());
			}
		}

		Collection values = remaining.values();
		fOtherBundles = (TargetBundle[]) values.toArray(new TargetBundle[values.size()]);
		return fOtherBundles;
	}

	/**
	 * Convenience method to return the set of IFeatureModels that are included in this
	 * target as well as any other included plug-ins as IResolvedBundles (that are not part 
	 * of the features). Also returns any bundles with error statuses.  Will return <code>null</code> 
	 * if this target has not been resolved.
	 * 
	 * @see #getAllFeatures()
	 * @see #getOtherBundles()
	 * @return set of IFeatureModels and IResolvedBundles or <code>null</code>
	 */
	public Set getFeaturesAndBundles() {
		if (!isResolved()) {
			return null;
		}

		TargetFeature[] allFeatures = getAllFeatures();
		TargetBundle[] allExtraBundles = getOtherBundles();

		NameVersionDescriptor[] included = getIncluded();

		if (included == null) {
			Set result = new HashSet();
			result.addAll(Arrays.asList(allFeatures));
			result.addAll(Arrays.asList(allExtraBundles));
			return result;
		}

		Set result = new HashSet();
		for (int i = 0; i < included.length; i++) {
			if (included[i].getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				for (int j = 0; j < allExtraBundles.length; j++) {
					if (allExtraBundles[j].getBundleInfo().getSymbolicName().equals(included[i].getId())) {
						result.add(allExtraBundles[j]);
					}
				}
			} else if (included[i].getType() == NameVersionDescriptor.TYPE_FEATURE) {
				for (int j = 0; j < allFeatures.length; j++) {
					if (allFeatures[j].getId().equals(included[i].getId())) {
						result.add(allFeatures[j]);
					}
				}
			}
		}

		return result;
	}

	/**
	 * @return the current UI style one of {@link #MODE_FEATURE} or {@link #MODE_PLUGIN}
	 */
	public int getUIMode() {
		return fUIMode;
	}

	/**
	 * @param mode new UI style to use, one of {@link #MODE_FEATURE} or {@link #MODE_PLUGIN}
	 */
	public void setUIMode(int mode) {
		fUIMode = mode;
	}

	/**
	 * Returns the current sequence number of this target.  Sequence numbers change
	 * whenever something in the target that affects the set of features and bundles that 
	 * would be resolved.
	 * 
	 * @return the current sequence number
	 */
	public int getSequenceNumber() {
		return fSequenceNumber;
	}

	/**
	 * Increases the current sequence number.
	 * @see TargetDefinition#getSequenceNumber()
	 * @return the current sequence number after it has been increased
	 */
	public int incrementSequenceNumber() {
		return ++fSequenceNumber;
	}

	/**
	 * Convenience method to set the sequence number to a specific 
	 * value. Used when loading a target from a persisted file.
	 * @param value value to set the sequence number to
	 */
	void setSequenceNumber(int value) {
		fSequenceNumber = value;
	}
}
