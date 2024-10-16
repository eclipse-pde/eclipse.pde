/*******************************************************************************
 * Copyright (c) 2016, 2024 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.p2;

import java.net.URI;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * A utility class used to fetch IUs from a repository. Used for unit ids
 * completion.
 */
public class P2Fetcher {

	/**
	 * This methods goes 'online' to make contact with a p2 repo and query it.
	 *
	 * @param repositoryLocation
	 *            URL string of a p2 repository
	 * @return List of available installable unit models. See {@link UnitNode}
	 */
	public static Stream<IVersionedId> fetchAvailableUnits(URI repositoryLocation, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 31);
		BundleContext context = FrameworkUtil.getBundle(P2Fetcher.class).getBundleContext();
		ServiceReference<IProvisioningAgentProvider> sr = context.getServiceReference(IProvisioningAgentProvider.class);
		try {
			IProvisioningAgentProvider agentProvider = context.getService(sr);
			IProvisioningAgent agent = agentProvider.createAgent(null);
			IMetadataRepositoryManager manager = agent.getService(IMetadataRepositoryManager.class);
			IMetadataRepository repository = manager.loadRepository(repositoryLocation, subMonitor.split(30));
			IQueryResult<IInstallableUnit> allUnits = repository.query(QueryUtil.ALL_UNITS, subMonitor.split(1));
			return allUnits.stream().map(iu -> new VersionedId(iu.getId(), iu.getVersion()));
		} finally {
			context.ungetService(sr);
		}
	}

}
