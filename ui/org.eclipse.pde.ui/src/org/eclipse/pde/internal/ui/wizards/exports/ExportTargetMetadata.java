/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ian Bull <irbull@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class ExportTargetMetadata {

	IProvisioningAgent agent = null;
	public static ExportTargetMetadata instance = null;

	public void start() {
		instance = this;
	}

	public void stop() {
		instance = null;

	}

	public static ExportTargetMetadata getDefault() {
		return instance;
	}

	public synchronized void clearExporedRepository(URI destination) {
		agent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			return;
		if (((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).contains(destination))
			((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).removeRepository(destination);
	}

	public synchronized IStatus exportMetadata(IProfile profile, URI destination, String targetName) {
		if (agent == null)
			return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to mirror the metadata."); //$NON-NLS-1$
		boolean removeRepoAfterLoad = false;
		try {
			IMetadataRepository repository = null;

			try {
				//TODO: There appears to be a small (5 byte) difference from when I do the load vs. the create
				//      The create gives the repo a version 1, while the load gives it 1.0.0
				//      this might be worth looking into
				removeRepoAfterLoad = !((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).contains(destination);
				repository = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).loadRepository(destination, IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, new NullProgressMonitor());
			} catch (ProvisionException e) {
				// The repository cannot be loaded
			}
			if (repository == null) {
				repository = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).createRepository(destination, targetName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			}

			if (repository != null) {
				mirrorMetadata(profile, repository);
			} else {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to mirror the metadata."); //$NON-NLS-1$
			}

		} catch (ProvisionException e) {
			return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to mirror the metadata.", e); //$NON-NLS-1$
		} finally {
			if (removeRepoAfterLoad) {
				((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).removeRepository(destination);
			}
		}

		return Status.OK_STATUS;
	}

	private void mirrorMetadata(IProfile profile, IMetadataRepository repository) {
		IQueryResult results = profile.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		repository.addInstallableUnits(results.toUnmodifiableSet());
	}

}
