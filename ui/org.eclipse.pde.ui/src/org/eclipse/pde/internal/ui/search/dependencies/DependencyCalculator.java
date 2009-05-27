/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
	protected HashMap fDependencies;

	/*
	 * Object[] can be IPluginModelBases, BundleDescriptions, or Strings (id's of bundles)
	 */
	public DependencyCalculator(boolean includeOptional) {
		super();
		fIncludeOptional = includeOptional;
	}

	public void findDependencies(Object[] includedBundles) {
		if (fDependencies == null)
			fDependencies = new HashMap();
		for (int i = 0; i < includedBundles.length; i++) {
			findObjectDependencies(includedBundles[i]);
		}
	}

	public void findDependency(Object bundle) {
		if (fDependencies == null)
			fDependencies = new HashMap();
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
	public Set getBundleIDs() {
		Set temp = fDependencies.keySet();
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
		for (int i = 0; i < requiredBundles.length; i++) {
			if (requiredBundles[i].isOptional() && !fIncludeOptional)
				continue;
			BaseDescription bd = requiredBundles[i].getSupplier();
			// only recursively search statisfied require-bundles
			if (bd != null && bd instanceof BundleDescription)
				findDependencies((BundleDescription) bd);
		}
	}

	protected void addImportedPackages(ImportPackageSpecification[] packages) {
		for (int i = 0; i < packages.length; i++) {
			if (!fIncludeOptional)
				if (Constants.RESOLUTION_OPTIONAL.equals(packages[i].getDirective(Constants.RESOLUTION_DIRECTIVE))) {
					continue;
				}
			BaseDescription bd = packages[i].getSupplier();
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
		for (int i = 0; i < fragments.length; i++)
			if (fragments[i].isResolved() && !fragments[i].getSymbolicName().equals("org.eclipse.ui.workbench.compatibility")) { //$NON-NLS-1$
				findDependencies(fragments[i]);
			}
	}

	public boolean containsPluginId(String id) {
		return fDependencies.containsKey(id);
	}

}
