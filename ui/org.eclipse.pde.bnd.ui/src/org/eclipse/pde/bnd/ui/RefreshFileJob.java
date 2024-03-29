/*******************************************************************************
 * Copyright (c) 2013, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Carter Smithhart <carter.smithhart@gmail.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class RefreshFileJob extends WorkspaceJob {
	private final boolean		derived;
	private final List<File>	files;

	public RefreshFileJob(File file, boolean derived) throws Exception {
		this(Collections.singletonList(file), derived);
	}

	public RefreshFileJob(List<File> filesToRefresh, boolean derived) {
		super("Refreshing files");
		this.derived = derived;
		this.files = filesToRefresh;
	}

	public boolean needsToSchedule() {
		return !files.isEmpty();
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, getName(), files.size());
		for (File file : files) {
			if (file != null) {
				Central.refreshFile(file, progress.split(1), derived);
			} else {
				progress.worked(1);
			}
		}
		progress.done();
		return Status.OK_STATUS;
	}
}
