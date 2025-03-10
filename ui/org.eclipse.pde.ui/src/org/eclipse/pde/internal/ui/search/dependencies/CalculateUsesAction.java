/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

public class CalculateUsesAction extends Action {

	private final IProject fProject;
	private final IBundlePluginModelBase fModel;

	public CalculateUsesAction(IProject project, IBundlePluginModelBase model) {
		fProject = project;
		fModel = model;
	}

	@Override
	public void run() {
		Job job = createJob();
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PSEARCH_OBJ);
		job.schedule();
	}

	protected Job createJob() {
		return new WorkspaceJob(PDEUIMessages.CalculateUsesAction_jobName) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				CalculateUsesOperation op = getOperation();
				try {
					op.run(monitor);
				} catch (InvocationTargetException | InterruptedException e) {
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
	}

	protected CalculateUsesOperation getOperation() {
		return new CalculateUsesOperation(fProject, fModel) {

			@Override
			protected void handleSetUsesDirectives(final Map<String, HashSet<String>> pkgsAndUses) {
				Display.getDefault().asyncExec(() -> {
					if (pkgsAndUses.isEmpty()) {
						return;
					}
					setUsesDirectives(pkgsAndUses);
				});
			}

		};
	}

}
