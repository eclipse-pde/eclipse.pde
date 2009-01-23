/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.impl;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * Common function for bundle containers.
 * 
 * @since 3.5
 */
public abstract class AbstractBundleContainer implements IBundleContainer {

	/**
	 * Bundle restrictions (subset) this container is restricted to or <code>null</code> if
	 * no restrictions.
	 */
	private BundleInfo[] fRestrictions;

	/**
	 * Resolves any string substitution variables in the given text returning
	 * the result.
	 * 
	 * @param text text to resolve
	 * @return result of the resolution
	 * @throws CoreException if unable to resolve 
	 */
	protected String resolveVariables(String text) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#resolveBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final BundleInfo[] resolveBundles(IProgressMonitor monitor) throws CoreException {
		BundleInfo[] all = resolveAllBundles(monitor);
		return getMatchingBundles(all, getRestrictions());
	}

	/**
	 * Resolves all executable bundles in this container regardless of any bundle restrictions.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 * @param monitor progress monitor
	 * @return all executable bundles in this container regardless of any bundle restrictions
	 * @throws CoreException if an error occurs
	 */
	protected abstract BundleInfo[] resolveAllBundles(IProgressMonitor monitor) throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#resolveSourceBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final BundleInfo[] resolveSourceBundles(IProgressMonitor monitor) throws CoreException {
		BundleInfo[] all = resolveAllSourceBundles(monitor);
		return getMatchingBundles(all, getRestrictions());
	}

	/**
	 * Resolves all source bundles in this container regardless of any bundle restrictions.
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 * @param monitor progress monitor
	 * @return all source bundles in this container regardless of any bundle restrictions
	 * @throws CoreException if an error occurs
	 */
	protected abstract BundleInfo[] resolveAllSourceBundles(IProgressMonitor monitor) throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRestrictions()
	 */
	public BundleInfo[] getRestrictions() {
		return fRestrictions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#setRestrictions(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void setRestrictions(BundleInfo[] bundles) {
		fRestrictions = bundles;
	}

	/**
	 * Returns bundles from the specified collection that match the symbolic names
	 * and/or version in the specified criteria. When no version is specified
	 * the newest version (if any) is selected.
	 * 
	 * @param collection bundles to resolve against match criteria
	 * @param criteria bundles to select or <code>null</code> if no restrictions
	 * @return bundles that match this container's restrictions
	 */
	static BundleInfo[] getMatchingBundles(BundleInfo[] collection, BundleInfo[] criteria) {
		if (criteria == null) {
			return collection;
		}
		// map bundles names to available versions
		Map bundleMap = new HashMap(collection.length);
		for (int i = 0; i < collection.length; i++) {
			BundleInfo info = collection[i];
			List list = (List) bundleMap.get(info.getSymbolicName());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(info.getSymbolicName(), list);
			}
			list.add(info);
		}
		List subset = new ArrayList(criteria.length);
		for (int i = 0; i < criteria.length; i++) {
			BundleInfo info = criteria[i];
			List list = (List) bundleMap.get(info.getSymbolicName());
			if (list != null) {
				String version = info.getVersion();
				if (version == null) {
					// select newest
					if (list.size() > 1) {
						// sort the list
						Collections.sort(list, new Comparator() {
							public int compare(Object o1, Object o2) {
								return ((BundleInfo) o1).getVersion().compareTo(((BundleInfo) o2).getVersion());
							}
						});
					}
					// select the last one
					subset.add(list.get(list.size() - 1));
				} else {
					Iterator iterator = list.iterator();
					boolean found = false;
					while (iterator.hasNext() && !found) {
						BundleInfo bundle = (BundleInfo) iterator.next();
						if (bundle.getVersion().equals(version)) {
							subset.add(bundle);
							found = true;
						}
					}
					if (!found) {
						// TODO: report not found? exception?
					}
				}
			} else {
				// TODO: report not found? exception?
			}
		}
		return (BundleInfo[]) subset.toArray(new BundleInfo[subset.size()]);
	}

	/**
	 * Returns a string that identifies the type of bundle container.  This type is persisted to xml
	 * so that the correct bundle container is created when deserializing the xml.  This type is also
	 * used to alter how the containers are presented to the user in the UI.
	 * 
	 * @return string identifier for the type of bundle container.
	 */
	public abstract String getType();

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * <p>
	 * TODO: Ideally we won't need this method. Currently the PDE target platform preferences are
	 * based on a home location and additional locations, so we need the information.
	 * </p>
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public abstract String getLocation(boolean resolve) throws CoreException;

	/**
	 * Returns whether restrictions are equivalent. Subclasses should override for other data.
	 * 
	 * @param container bundle container
	 * @return whether content is equivalent
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (fRestrictions == null) {
			return container.fRestrictions == null;
		}
		if (container.fRestrictions == null) {
			return false;
		}
		if (fRestrictions.length == container.fRestrictions.length) {
			for (int i = 0; i < fRestrictions.length; i++) {
				if (!fRestrictions[i].equals(container.fRestrictions[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
