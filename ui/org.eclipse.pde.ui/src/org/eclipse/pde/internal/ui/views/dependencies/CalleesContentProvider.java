/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.HashMap;
import java.util.LinkedHashMap;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Constants;

public class CalleesContentProvider extends DependenciesViewPageContentProvider {
	private BundleDescription fFragmentDescription;

	public CalleesContentProvider(DependenciesView view) {
		super(view);
	}

	protected Object[] findCallees(IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();
		if (desc == null)
			return new Object[0];
		fFragmentDescription = null;
		HostSpecification spec = desc.getHost();
		if (spec != null) {
			fFragmentDescription = desc;
			Object[] fragmentDependencies = getDependencies(desc);
			BaseDescription host = spec.getSupplier();
			if (host instanceof BundleDescription) {
				BundleDescription hostDesc = (BundleDescription) host;
				// check to see if the host is already included as a dependency.  If so, we don't need to include the host manually.
				for (Object fragmentDependency : fragmentDependencies) {
					BundleDescription dependency = null;
					if (fragmentDependency instanceof BundleSpecification)
						dependency = ((BundleSpecification) fragmentDependency).getBundle();
					else if (fragmentDependency instanceof ImportPackageSpecification) {
						ExportPackageDescription epd = (ExportPackageDescription) ((ImportPackageSpecification) fragmentDependency).getSupplier();
						if (epd != null)
							dependency = epd.getSupplier();
					}
					if (dependency != null && dependency.equals(hostDesc))
						return fragmentDependencies;
				}

				// host not included as dependency, include it manually.
				Object[] result = new Object[fragmentDependencies.length + 1];
				result[0] = hostDesc;
				System.arraycopy(fragmentDependencies, 0, result, 1, fragmentDependencies.length);
				return result;
			}
			return fragmentDependencies;
		}
		return getDependencies(desc);
	}

	protected Object[] findCallees(BundleDescription desc) {
		if (desc == null)
			return new Object[0];
		return getDependencies(desc);
	}

	private Object[] getDependencies(BundleDescription desc) {
		// use map to store dependencies so if Import-Package is supplied by same BundleDescription as supplier of Require-Bundle, it only shows up once
		// Also, have to use BundleSpecficiation instead of BundleDescroption to show re-exported icon on re-exported Required-Bundles
		// Have to use ImportPackageSpecification to determine if an import is optional and should be filtered.
		HashMap<Object, Object> dependencies = new LinkedHashMap<>();
		BundleSpecification[] requiredBundles = desc.getRequiredBundles();
		for (BundleSpecification requiredBundle : requiredBundles) {
			BaseDescription bd = requiredBundle.getSupplier();
			if (bd != null)
				dependencies.put(bd, requiredBundle);
			else
				dependencies.put(requiredBundle, requiredBundle);
		}
		ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
		for (int i = 0; i < importedPkgs.length; i++) {
			BaseDescription bd = importedPkgs[i].getSupplier();
			if (bd != null && bd instanceof ExportPackageDescription) {
				BundleDescription exporter = ((ExportPackageDescription) bd).getExporter();
				if (exporter == desc)
					continue;
				if (exporter != null) {
					Object obj = dependencies.get(exporter);
					if (obj == null) {
						dependencies.put(exporter, importedPkgs[i]);
					} else if (!Constants.RESOLUTION_OPTIONAL.equals(importedPkgs[i].getDirective(Constants.RESOLUTION_DIRECTIVE)) && obj instanceof ImportPackageSpecification && Constants.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) obj).getDirective(Constants.RESOLUTION_DIRECTIVE))) {
						// if we have a non-optional Import-Package dependency on a bundle which we already depend on, check to make sure our
						// current dependency is not optional.  If it is, replace the optional dependency with the non-optional one
						dependencies.put(exporter, importedPkgs[i]);
					}
				}
			}
			// ignore unresolved packages
		}
		// include fragments which are "linked" to this bundle
		BundleDescription frags[] = desc.getFragments();
		for (int i = 0; i < frags.length; i++) {
			if (!frags[i].equals(fFragmentDescription))
				dependencies.put(frags[i], frags[i]);
		}
		return dependencies.values().toArray();
	}

}
