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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.RepositoryCache;

/**
 * Fetching P2 repository information is a costly operation time-wise. Thus we
 * start a job to do it, as per the guidelines.
 */
public class UpdateJob extends Job {

	private final LocationNode node;

	public UpdateJob(LocationNode node) {
		super(Messages.UpdateJob_P2DataFetch + node.getRepositoryLocation());
		this.node = node;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (RepositoryCache.fetchP2UnitsFromRepo(node.getRepositoryLocation()) == null) {
			return Status.error(Messages.UpdateJob_ErrorMessage);
		}
		return Status.OK_STATUS;
	}

}
