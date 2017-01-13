/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.target.extension.p2;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.genericeditor.target.extension.model.RepositoryCache;
import org.eclipse.pde.genericeditor.target.extension.model.UnitNode;
import org.osgi.framework.FrameworkUtil;

/**
 * Fetching P2 repository information is a costly operation time-wise. Thus we
 * start a job to do it, as per the guidelines.
 */
public class UpdateJob extends Job {

	private LocationNode node;

	public UpdateJob(LocationNode node) {
		super(Messages.UpdateJob_P2DataFetch + node.getRepositoryLocation());
		this.node = node;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<UnitNode> list = RepositoryCache.getDefault().fetchP2UnitsFromRepo(node.getRepositoryLocation(), true);
		if (list == null) {
			return new Status(IStatus.ERROR, FrameworkUtil.getBundle(UpdateJob.class).getSymbolicName(), Messages.UpdateJob_ErrorMessage);
		}
		return Status.OK_STATUS;
	}

}
