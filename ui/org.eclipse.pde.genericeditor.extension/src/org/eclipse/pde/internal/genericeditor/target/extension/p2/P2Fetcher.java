/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc. and others
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
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
	public static List<UnitNode> fetchAvailableUnits(String repositoryLocation) {
		List<UnitNode> units = new ArrayList<>();
		IQueryResult<IInstallableUnit> result = null;
		try {
			URI uri;
			try {
				uri = new URI(repositoryLocation);
			} catch (URISyntaxException e) {
				return units;
			}
			BundleContext context = FrameworkUtil.getBundle(P2Fetcher.class).getBundleContext();
			ServiceReference<IProvisioningAgentProvider> sr = context
					.getServiceReference(IProvisioningAgentProvider.class);
			IProvisioningAgentProvider agentProvider = null;
			agentProvider = context.getService(sr);
			IProvisioningAgent agent = null;

			try {
				agent = agentProvider.createAgent(null);
			} catch (ProvisionException e) {
				e.printStackTrace();
			}
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent
					.getService(IMetadataRepositoryManager.SERVICE_NAME);
			IMetadataRepository repository = manager.loadRepository(uri, null);
			result = repository.query(QueryUtil.createLatestIUQuery(), null);

			Iterator<IInstallableUnit> iterator = result.iterator();
			while (iterator.hasNext()) {
				IInstallableUnit unit = iterator.next();
				UnitNode modelUnit = new UnitNode();
				modelUnit.setId(unit.getId());
				modelUnit.setVersion(unit.getVersion().getOriginal());
				IQueryResult<IInstallableUnit> versions = repository.query(QueryUtil.createIUQuery(unit.getId()), null);
				for (IInstallableUnit version : versions) {
					modelUnit.getAvailableVersions().add(version.getVersion().getOriginal());
				}
				units.add(modelUnit);
			}

			return units;

		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

}
