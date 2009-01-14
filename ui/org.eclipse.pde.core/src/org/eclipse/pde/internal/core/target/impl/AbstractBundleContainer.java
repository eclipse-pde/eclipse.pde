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
		return getMatchingBundles(all);
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
		return getMatchingBundles(all);
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
	 * Returns bundles from the specified collection that match restrictions on this
	 * container.
	 * 
	 * @param all bundles to choose from
	 * @return bundles that match this container's restrictions
	 */
	private BundleInfo[] getMatchingBundles(BundleInfo[] all) {
		BundleInfo[] restrictions = getRestrictions();
		if (restrictions == null) {
			return all;
		}
		// map bundles names to available versions
		Map bundleMap = new HashMap(all.length);
		for (int i = 0; i < all.length; i++) {
			BundleInfo info = all[i];
			List list = (List) bundleMap.get(info.getSymbolicName());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(info.getSymbolicName(), list);
			}
			list.add(info);
		}
		List subset = new ArrayList(restrictions.length);
		for (int i = 0; i < restrictions.length; i++) {
			BundleInfo info = restrictions[i];
			List list = (List) bundleMap.get(info.getSymbolicName());
			if (list != null) {
				String version = info.getVersion();
				if (version == null) {
					// select newest
					if (list.size() > 1) {
						// sort the list
						Collections.sort(list, new Comparator() {
							public int compare(Object o1, Object o2) {
								return ((BundleInfo) o1).getVersion().compareTo(o2);
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
}
