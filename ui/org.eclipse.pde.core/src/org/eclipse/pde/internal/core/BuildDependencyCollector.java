/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;

import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;

public class BuildDependencyCollector {
	private final Collection<BundleDescription> fDependencies = new HashSet<>();
	private final Queue<BundleDescription> fQueue = new ArrayDeque<>();

	private BuildDependencyCollector(Collection<BundleDescription> roots) {
		fQueue.addAll(roots);
		fDependencies.addAll(roots);
	}

	/**
	 * Returns all transitive dependencies that are relevant for building
	 * {@code roots}, including {@code roots} themselves.
	 */
	public static Collection<BundleDescription> collectBuildRelevantDependencies(Collection<BundleDescription> roots) {
		BuildDependencyCollector collector = new BuildDependencyCollector(roots);
		collector.collect();
		return collector.fDependencies;
	}

	private void collect() {
		while (!fQueue.isEmpty()) {
			BundleDescription bundleDescription = fQueue.remove();

			collectRequiredBundles(bundleDescription);
			collectImportedPackages(bundleDescription);
			collectFragmentHost(bundleDescription);
		}
	}

	private void collectRequiredBundles(BundleDescription bundle) {
		for (BundleSpecification required : bundle.getRequiredBundles()) {
			BundleDescription supplier = (BundleDescription) required.getSupplier();
			enqueueDependency(supplier);
		}
	}

	private void collectImportedPackages(BundleDescription bundle) {
		for (ImportPackageSpecification importPackage : bundle.getImportPackages()) {
			BaseDescription supplier = importPackage.getSupplier();
			if (supplier instanceof ExportPackageDescription) {
				enqueueDependency(((ExportPackageDescription) supplier).getExporter());
			}
		}
	}

	private void collectFragmentHost(BundleDescription bundle) {
		HostSpecification host = bundle.getHost();
		if (host != null) {
			BaseDescription supplier = host.getSupplier();
			if (supplier instanceof BundleDescription) {
				enqueueDependency((BundleDescription) supplier);
			}
		}
	}

	private void enqueueDependency(BundleDescription dependency) {
		if (dependency != null && fDependencies.add(dependency)) {
			fQueue.add(dependency);
		}
	}
}