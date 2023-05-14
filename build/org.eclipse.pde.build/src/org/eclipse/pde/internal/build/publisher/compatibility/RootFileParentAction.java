/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAction;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

public class RootFileParentAction extends AbstractPublisherAction {

	private final String flavor;
	private final String version;
	protected final String baseId;

	public RootFileParentAction(ProductFile product, String flavor) {
		this.flavor = flavor;
		this.baseId = product.getId();
		this.version = getVersion(product.getVersion());
	}

	public RootFileParentAction(String rootId, String rootVersion, String flavor) {
		this.flavor = flavor;
		this.baseId = rootId != null ? rootId : "org.eclipse"; //$NON-NLS-1$
		this.version = getVersion(rootVersion);
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		final String idPrefix = baseId + ".rootfiles"; //$NON-NLS-1$
		final String flavorPrefix = flavor + baseId + ".rootfiles"; //$NON-NLS-1$

		HashSet<IInstallableUnit> collector = new HashSet<>();
		Iterator<IInstallableUnit> iter = results.getIUs(null, IPublisherResult.NON_ROOT).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = iter.next();
			String id = iu.getId();
			if (id.startsWith(idPrefix) || id.startsWith(flavorPrefix))
				collector.add(iu);
		}

		InstallableUnitDescription descriptor = createParentIU(collector, RootFilesAction.computeIUId(baseId, flavor), Version.parseVersion(version));
		descriptor.setSingleton(true);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		results.addIU(rootIU, IPublisherResult.ROOT);
		return Status.OK_STATUS;
	}

	private String getVersion(String rootVersion) {
		if (rootVersion != null && !rootVersion.equals(IPDEBuildConstants.GENERIC_VERSION_NUMBER))
			return rootVersion;
		return "1.0.0"; //$NON-NLS-1$
	}
}
