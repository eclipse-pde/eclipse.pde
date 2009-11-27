/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Constants;

/**
 * Utility class to return bundle id collections for a variety of dependency
 * scenarios
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DependencyManager {

	/** 
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * {@link IPluginModelBase}. The set includes the id of the given model base
	 * as well as all computed implicit / optional dependencies. 
	 * 
	 * @param model the {@link IPluginModelBase} to compute dependencies for
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set getSelfAndDependencies(IPluginModelBase model, String[] excludeFragments) {
		return getDependencies(new Object[] {model}, getImplicitDependencies(), TargetPlatformHelper.getState(), false, true, toSet(excludeFragments));
	}

	/** 
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * {@link IPluginModelBase}s. The set includes the ids of the given model bases
	 * as well as all computed implicit / optional dependencies.
	 * 
	 * @param models the array of {@link IPluginModelBase}s to compute dependencies for
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set getSelfandDependencies(IPluginModelBase[] models, String[] excludeFragments) {
		return getDependencies(models, getImplicitDependencies(), TargetPlatformHelper.getState(), false, true, toSet(excludeFragments));
	}

	/** 
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * objects from the given {@link State}. 
	 * The set does not include the ids of the given objects
	 * and only includes the given set of implicit dependencies.
	 * 
	 * @param selected the group of objects to compute dependencies for. Any items
	 * in this array that are not {@link IPluginModelBase}s are ignored.
	 * @param implicit the array of additional implicit dependencies to add to the {@link Set}
	 * @param state the {@link State} to compute the dependencies in
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set getDependencies(Object[] selected, String[] implicit, State state, String[] excludeFragments) {
		return getDependencies(selected, implicit, state, true, true, toSet(excludeFragments));
	}

	/** 
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * objects. The set does not include the ids of the given objects
	 * but does include the computed set of implicit dependencies.
	 * 
	 * @param selected selected the group of objects to compute dependencies for. Any items
	 * in this array that are not {@link IPluginModelBase}s are ignored.
	 * @param includeOptional if optional bundle ids should be included
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 *  or <code>null</code> if none
	 * @return a set of bundle IDs
	 */
	public static Set getDependencies(Object[] selected, boolean includeOptional, String[] excludeFragments) {
		return getDependencies(selected, getImplicitDependencies(), TargetPlatformHelper.getState(), true, includeOptional, toSet(excludeFragments));
	}

	/**
	 * Returns the array as a set or <code>null</code> 
	 * @param array array or <code>null</code>
	 * @return set
	 */
	private static Set toSet(String[] array) {
		Set set = new HashSet();
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				set.add(array[i]);
			}
		}
		return set;
	}

	/** 
	 * Returns a {@link Set} of bundle ids for the dependents of the given
	 * objects from the given {@link State}. 
	 * The set additionally only includes the given set of implicit dependencies.
	 * 
	 * @param selected selected the group of objects to compute dependencies for. Any items
	 * in this array that are not {@link IPluginModelBase}s are ignored.
	 * @param implicit the array of additional implicit dependencies to add to the {@link Set}
	 * @param state the {@link State} to compute the dependencies in
	 * @param removeSelf if the id of one of the bundles were are computing dependencies for should be
	 * included in the result {@link Set} or not
	 * @param includeOptional if optional bundle ids should be included
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 * @return a set of bundle IDs
	 */
	private static Set getDependencies(Object[] selected, String[] implicit, State state, boolean removeSelf, boolean includeOptional, Set excludeFragments) {
		Set set = new TreeSet();
		for (int i = 0; i < selected.length; i++) {
			if (!(selected[i] instanceof IPluginModelBase))
				continue;
			IPluginModelBase model = (IPluginModelBase) selected[i];
			addBundleAndDependencies(model.getBundleDescription(), set, includeOptional, excludeFragments);
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null) {
					int dot = point.lastIndexOf('.');
					if (dot != -1) {
						String id = point.substring(0, dot);
						addBundleAndDependencies(state.getBundle(id, null), set, includeOptional, excludeFragments);
					}
				}
			}
		}

		for (int i = 0; i < implicit.length; i++) {
			addBundleAndDependencies(state.getBundle(implicit[i], null), set, includeOptional, excludeFragments);
		}

		if (removeSelf) {
			for (int i = 0; i < selected.length; i++) {
				if (!(selected[i] instanceof IPluginModelBase)) {
					continue;
				}
				IPluginModelBase model = (IPluginModelBase) selected[i];
				set.remove(model.getPluginBase().getId());
			}
		}
		return set;
	}

	/**
	 * Computes the set of implicit dependencies from the {@link PDEPreferencesManager}
	 * @return a set if bundle ids 
	 */
	private static String[] getImplicitDependencies() {
		PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
		String dependencies = preferences.getString(ICoreConstants.IMPLICIT_DEPENDENCIES);
		if (dependencies.length() == 0) {
			return new String[0];
		}
		StringTokenizer tokenizer = new StringTokenizer(dependencies, ","); //$NON-NLS-1$
		String[] implicitIds = new String[tokenizer.countTokens()];
		for (int i = 0; i < implicitIds.length; i++) {
			implicitIds[i] = tokenizer.nextToken();
		}
		return implicitIds;
	}

	/**
	 * Recursively adds the given {@link BundleDescription} and its dependents to the given 
	 * {@link Set}
	 * @param desc the {@link BundleDescription} to compute dependencies for
	 * @param set the {@link Set} to collect results in
	 * @param includeOptional if optional dependencies should be included
	 * @param excludeFragments a collection of <b>fragment</b> bundle symbolic names to exclude from the dependency resolution
	 */
	private static void addBundleAndDependencies(BundleDescription desc, Set set, boolean includeOptional, Set excludeFragments) {
		if (desc != null && set.add(desc.getSymbolicName())) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (includeOptional || !required[i].isOptional()) {
					addBundleAndDependencies((BundleDescription) required[i].getSupplier(), set, includeOptional, excludeFragments);
				}
			}
			ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
			for (int i = 0; i < importedPkgs.length; i++) {
				ExportPackageDescription exporter = (ExportPackageDescription) importedPkgs[i].getSupplier();
				// Continue if the Imported Package is unresolved of the package is optional and don't want optional packages
				if (exporter == null || (!includeOptional && Constants.RESOLUTION_OPTIONAL.equals(importedPkgs[i].getDirective(Constants.RESOLUTION_DIRECTIVE)))) {
					continue;
				}
				addBundleAndDependencies(exporter.getExporter(), set, includeOptional, excludeFragments);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved()) {
					continue;
				}
				String id = fragments[i].getSymbolicName();
				if (!excludeFragments.contains(id)) {
					addBundleAndDependencies(fragments[i], set, includeOptional, excludeFragments);
				}
			}
			HostSpecification host = desc.getHost();
			if (host != null) {
				addBundleAndDependencies((BundleDescription) host.getSupplier(), set, includeOptional, excludeFragments);
			}
		}
	}

}
