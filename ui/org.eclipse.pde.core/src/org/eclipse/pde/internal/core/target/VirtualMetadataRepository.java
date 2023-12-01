/*******************************************************************************
 * Copyright (c) 2023 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

/**
 * In-Memory representation of a metadata repository based on a non-IU target
 * location. This repository is used during the planner resolution of an IU
 * target location to supply the metadata from other (non-IU) locations.
 */
public class VirtualMetadataRepository extends AbstractMetadataRepository {
	private final List<IInstallableUnit> installableUnits;

	public VirtualMetadataRepository(IProvisioningAgent agent, List<IInstallableUnit> installableUnits) {
		super(agent);
		this.installableUnits = List.copyOf(installableUnits);
	}


	@Override
	public Collection<IRepositoryReference> getReferences() {
		return Collections.emptySet();
	}

	@Override
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return query.perform(installableUnits.iterator());
	}

	@Override
	public void initialize(RepositoryState state) {
		// nothing to do
	}
}
