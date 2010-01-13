/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher.compatibility;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.RootFilesAction;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.MatchQuery;
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

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		final String idPrefix = baseId + ".rootfiles"; //$NON-NLS-1$
		final String flavorPrefix = flavor + baseId + ".rootfiles"; //$NON-NLS-1$
		//TODO this could be turned into a "name query", a query that checks on some parameters
		MatchQuery query = new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					String id = ((IInstallableUnit) candidate).getId();
					return id.startsWith(idPrefix) || id.startsWith(flavorPrefix);
				}
				return false;
			}
		};

		IQueryResult collector = query.perform(results.getIUs(null, IPublisherResult.NON_ROOT).iterator());
		InstallableUnitDescription descriptor = createParentIU(collector.unmodifiableSet(), RootFilesAction.computeIUId(baseId, flavor), Version.parseVersion(version));
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
