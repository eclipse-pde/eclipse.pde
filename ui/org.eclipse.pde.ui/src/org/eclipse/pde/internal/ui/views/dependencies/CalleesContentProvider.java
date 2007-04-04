/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.HashMap;

import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;

public class CalleesContentProvider extends DependenciesViewPageContentProvider {
	public CalleesContentProvider(DependenciesView view) {
		super(view);
	}
	
	protected Object[] findCallees(BundleDescription desc) {
		if (desc == null)
			return new Object[0];
		HostSpecification spec = desc.getHost();
		if (spec != null) {
			BaseDescription host = spec.getSupplier();
			if (host instanceof BundleDescription) {
				BundleDescription hostDesc = (BundleDescription)host;
				Object[] dependencies = getDependencies(hostDesc);
				Object[] result = new Object[dependencies.length + 1];
				result[0] = hostDesc;
				System.arraycopy(dependencies, 0, result, 1, dependencies.length);
				return result;
			}
		}
		return getDependencies(desc);
	}
	
	private Object[] getDependencies(BundleDescription desc) {
		// use map to store dependencies so if Import-Package is supplied by same BundleDescription as supplier of Require-Bundle, it only shows up once
		// Also, have to use BundleSpecficiation instead of BundleDescroption to show re-exported icon on re-exported Required-Bundles
		HashMap dependencies = new HashMap();
		BundleSpecification[] requiredBundles = desc.getRequiredBundles();
		for (int i = 0; i < requiredBundles.length; i++) {
			BaseDescription bd = requiredBundles[i].getSupplier();
			if (bd != null)
				dependencies.put(bd, requiredBundles[i]);
			else
				dependencies.put(requiredBundles[i], requiredBundles[i]);
		}
		ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
		for (int i = 0; i < importedPkgs.length; i++) {
			BaseDescription bd = importedPkgs[i].getSupplier();
			if (bd != null && bd instanceof ExportPackageDescription) {
				BundleDescription exporter = ((ExportPackageDescription)bd).getExporter();
				if (exporter != null) {
					if (!dependencies.containsKey(exporter)) {
						dependencies.put(exporter, exporter);
						continue;
					}
				}
			}
			// ignore unresolved packages
		}
		return dependencies.values().toArray();
	}

}
