/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;

public class CalculateDependenciesAction extends Action {
	
	boolean fIncludeOptional;
	Set fIncludedBundles;
	Collection fDependencies;
	
	/*
	 * Object[] can be IPluginModelBases, BundleDescriptions, or Strings (id's of bundles)
	 */
	public CalculateDependenciesAction(Object[] includedBundles, boolean includeOptional) {
		super();
		fIncludeOptional = includeOptional;
		addIncludedBundles(includedBundles);
	}
	
	
	private void addIncludedBundles(Object[] includedBundles) {
		fIncludedBundles = new HashSet();
		for (int i = 0; i < includedBundles.length; i++) {
			Object obj = includedBundles[i];
			if (obj instanceof IPluginModelBase) {
				IPluginModelBase base = ((IPluginModelBase)obj);
				BundleDescription desc = base.getBundleDescription();
				if (desc != null)
					obj = desc;
			}
			if (obj instanceof BundleDescription)
				fIncludedBundles.add(obj);
		}
	}
	
	public void run() {
		HashMap dependencies = new HashMap();
		Iterator it = fIncludedBundles.iterator();
		while (it.hasNext())
			findDependencies((BundleDescription)it.next(), dependencies);
		fDependencies = dependencies.values();
	}
	
	/*
	 * Returns a Collection of IPluginModelBases
	 */
	public Collection getDependencies() {
		return fDependencies;
	}
	
	protected void findDependencies(BundleDescription desc, HashMap dependencies) {
		if (desc == null)
			return;
		String id = desc.getSymbolicName();
		if (dependencies.containsKey(id))
			return;
		IPluginModelBase model = PluginRegistry.findModel(desc);
		if (model == null)
			return;
		dependencies.put(id, model);

		addRequiredBundles(desc.getRequiredBundles(), dependencies);
		addImportedPackages(desc.getImportPackages(), dependencies);
		
		HostSpecification host = desc.getHost();
		if (host != null) {
			// if current BundleDescription is a fragment, include host bundle
			BaseDescription bd = host.getSupplier();
			if (bd != null && bd instanceof BundleDescription) 
				findDependencies((BundleDescription)bd, dependencies);
		} else {
			// otherwise, include applicable fragments for bundle
			addFragments(desc, dependencies);
		}
	}
	
	protected void addRequiredBundles(BundleSpecification[] requiredBundles, HashMap dependencies) {
		for (int i = 0; i < requiredBundles.length; i++) {
			if (requiredBundles[i].isOptional() && !fIncludeOptional)
				continue;
			BaseDescription bd = requiredBundles[i].getSupplier();
			// only recursively search statisfied require-bundles
			if (bd != null && bd instanceof BundleDescription) 
				findDependencies((BundleDescription)bd, dependencies);
		}
	}
	
	protected void addImportedPackages(ImportPackageSpecification[] packages, HashMap dependencies) {
		for (int i = 0; i < packages.length; i++) {
			if (!fIncludeOptional) 
				if (Constants.RESOLUTION_OPTIONAL.equals(packages[i].getDirective(Constants.RESOLUTION_DIRECTIVE))) {
					continue;
				}
			BaseDescription bd = packages[i].getSupplier();
			// only recursively search statisfied import-packages
			if (bd != null && bd instanceof ExportPackageDescription) {
				BundleDescription exporter = ((ExportPackageDescription)bd).getExporter();
				if (exporter != null)
					findDependencies(exporter, dependencies);
			}
		}
	}
	
	protected void addFragments(BundleDescription desc, HashMap dependencies) {
		BundleDescription[] fragments = desc.getFragments();
		for (int i = 0; i < fragments.length; i++)
			if (fragments[i].isResolved() && !fragments[i].getSymbolicName().equals("org.eclipse.ui.workbench.compatibility")) { //$NON-NLS-1$
				findDependencies(fragments[i], dependencies);
			}
	}

}
