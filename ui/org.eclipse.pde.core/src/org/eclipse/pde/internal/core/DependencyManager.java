/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Constants;

public class DependencyManager {

	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getSelfAndDependencies(IPluginModelBase model) {
		return getDependencies(new Object[] {model}, getImplicitDependencies(), TargetPlatformHelper.getState(), false, true);
	}

	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getSelfandDependencies(IPluginModelBase[] models) {
		return getDependencies(models, getImplicitDependencies(), TargetPlatformHelper.getState(), false, true);
	}

	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getDependencies(Object[] selected, String[] implicit, State state) {
		return getDependencies(selected, implicit, state, true, true);
	}

	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	public static Set getDependencies(Object[] selected, boolean includeOptional) {
		return getDependencies(selected, getImplicitDependencies(), TargetPlatformHelper.getState(), true, includeOptional);
	}

	/** 
	 * @return a set of plug-in IDs
	 * 
	 */
	private static Set getDependencies(Object[] selected, String[] implicit, State state, boolean removeSelf, boolean includeOptional) {
		Set set = new TreeSet();
		for (int i = 0; i < selected.length; i++) {
			if (!(selected[i] instanceof IPluginModelBase))
				continue;
			IPluginModelBase model = (IPluginModelBase) selected[i];
			addBundleAndDependencies(model.getBundleDescription(), set, includeOptional);
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null) {
					int dot = point.lastIndexOf('.');
					if (dot != -1) {
						String id = point.substring(0, dot);
						addBundleAndDependencies(state.getBundle(id, null), set, includeOptional);
					}
				}
			}
		}

		for (int i = 0; i < implicit.length; i++) {
			addBundleAndDependencies(state.getBundle(implicit[i], null), set, includeOptional);
		}

		if (removeSelf) {
			for (int i = 0; i < selected.length; i++) {
				if (!(selected[i] instanceof IPluginModelBase))
					continue;
				IPluginModelBase model = (IPluginModelBase) selected[i];
				set.remove(model.getPluginBase().getId());
			}
		}
		return set;
	}

	private static String[] getImplicitDependencies() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String dependencies = preferences.getString(ICoreConstants.IMPLICIT_DEPENDENCIES);
		if (dependencies.length() == 0)
			return new String[0];
		StringTokenizer tokenizer = new StringTokenizer(dependencies, ","); //$NON-NLS-1$
		String[] implicitIds = new String[tokenizer.countTokens()];
		for (int i = 0; i < implicitIds.length; i++)
			implicitIds[i] = tokenizer.nextToken();
		return implicitIds;
	}

	private static void addBundleAndDependencies(BundleDescription desc, Set set, boolean includeOptional) {
		if (desc != null && set.add(desc.getSymbolicName())) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (includeOptional || !required[i].isOptional())
					addBundleAndDependencies((BundleDescription) required[i].getSupplier(), set, includeOptional);
			}
			ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
			for (int i = 0; i < importedPkgs.length; i++) {
				ExportPackageDescription exporter = (ExportPackageDescription) importedPkgs[i].getSupplier();
				// Continue if the Imported Package is unresolved of the package is optional and don't want optional packages
				if (exporter == null || (!includeOptional && Constants.RESOLUTION_OPTIONAL.equals(importedPkgs[i].getDirective(Constants.RESOLUTION_DIRECTIVE))))
					continue;
				addBundleAndDependencies(exporter.getExporter(), set, includeOptional);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved())
					continue;
				String id = fragments[i].getSymbolicName();
				if (!"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
					addBundleAndDependencies(fragments[i], set, includeOptional);
			}
			HostSpecification host = desc.getHost();
			if (host != null)
				addBundleAndDependencies((BundleDescription) host.getSupplier(), set, includeOptional);
		}
	}

}
