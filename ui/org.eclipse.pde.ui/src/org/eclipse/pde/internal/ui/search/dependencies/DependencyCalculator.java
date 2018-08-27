/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.HashMap;
import java.util.Set;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;

public class DependencyCalculator {

	boolean fIncludeOptional;
	protected HashMap<String, IPluginModelBase> fDependencies;

	/*
	 * Object[] can be IPluginModelBases, BundleDescriptions, or Strings (id's of bundles)
	 */
	public DependencyCalculator(boolean includeOptional) {
		super();
		fIncludeOptional = includeOptional;
	}

	public void findDependencies(Object[] includedBundles) {
		if (fDependencies == null)
			fDependencies = new HashMap<>();
		for (Object bundle : includedBundles) {
			findObjectDependencies(bundle);
		}
	}

	public void findDependency(Object bundle) {
		if (fDependencies == null)
			fDependencies = new HashMap<>();
		findObjectDependencies(bundle);
	}

	private void findObjectDependencies(Object obj) {
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase base = ((IPluginModelBase) obj);
			BundleDescription desc = base.getBundleDescription();
			if (desc != null)
				obj = desc;
		}
		if (obj instanceof BundleDescription)
			findDependencies((BundleDescription) obj);
	}

	/*
	 * Returns a Set of Bundle Ids
	 */
	public Set<String> getBundleIDs() {
		Set<String> temp = fDependencies.keySet();
		fDependencies = null;
		return temp;
	}

	protected void findDependencies(BundleDescription desc) {
		if (desc == null)
			return;
		String id = desc.getSymbolicName();
		if (fDependencies.containsKey(id))
			return;
		IPluginModelBase model = PluginRegistry.findModel(desc);
		if (model == null)
			return;
		fDependencies.put(id, model);

		addRequiredBundles(desc.getRequiredBundles());
		addImportedPackages(desc.getImportPackages());

		HostSpecification host = desc.getHost();
		if (host != null) {
			// if current BundleDescription is a fragment, include host bundle
			BaseDescription bd = host.getSupplier();
			if (bd != null && bd instanceof BundleDescription)
				findDependencies((BundleDescription) bd);
		} else {
			// otherwise, include applicable fragments for bundle
			addFragments(desc);
		}
	}

	protected void addRequiredBundles(BundleSpecification[] requiredBundles) {
		for (BundleSpecification bundle : requiredBundles) {
			if (bundle.isOptional() && !fIncludeOptional)
				continue;
			BaseDescription bd = bundle.getSupplier();
			// only recursively search statisfied require-bundles
			if (bd != null && bd instanceof BundleDescription)
				findDependencies((BundleDescription) bd);
		}
	}

	protected void addImportedPackages(ImportPackageSpecification[] packages) {
		for (ImportPackageSpecification pkg : packages) {
			if (!fIncludeOptional)
				if (Constants.RESOLUTION_OPTIONAL.equals(pkg.getDirective(Constants.RESOLUTION_DIRECTIVE))) {
					continue;
				}
			BaseDescription bd = pkg.getSupplier();
			// only recursively search statisfied import-packages
			if (bd != null && bd instanceof ExportPackageDescription) {
				BundleDescription exporter = ((ExportPackageDescription) bd).getExporter();
				if (exporter != null)
					findDependencies(exporter);
			}
		}
	}

	protected void addFragments(BundleDescription desc) {
		BundleDescription[] fragments = desc.getFragments();
		for (BundleDescription fragment : fragments)
			if (fragment.isResolved()) {
				findDependencies(fragment);
			}
	}

	public boolean containsPluginId(String id) {
		return fDependencies.containsKey(id);
	}

}
