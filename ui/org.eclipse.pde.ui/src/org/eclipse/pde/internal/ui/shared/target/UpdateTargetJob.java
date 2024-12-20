/*******************************************************************************
 * Copyright (c) 2010, 2020 EclipseSource Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    EclipseSource Inc. - initial API and implementation
 *    Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *    Christoph Läubrich 	Bug 567506 - TargetLocationsGroup.handleEdit() should activate bundles if necessary
 *    						Bug 568865 - [target] add advanced editing capabilities for custom target platforms
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

/**
 * Updates selected target locations using a list of {@link IJobFunction}s in an
 * asynchronous job
 * <p>
 * If all calls to {@link IJobFunction#run(IProgressMonitor)} return an OK
 * status with {@link ITargetLocationHandler#STATUS_CODE_NO_CHANGE}, the
 * returned status will also have that status code, indicating that no changes
 * were made to the target.
 */
public class UpdateTargetJob extends Job {

	private final IJobFunction action;

	private boolean update;

	private Job cancelJob;

	/**
	 * Schedules a new update job that will update all selected children of the
	 * target location get updated.
	 * <p>
	 * If no changes are performed an OK status with
	 * {@link ITargetLocationHandler#STATUS_CODE_NO_CHANGE}, the returned status
	 * will also have that status code, indicating that no changes were made to
	 * the target.
	 * </p>
	 *
	 * @param definition
	 *            the target definition to update
	 * @param handler
	 *            the handler
	 * @param treePaths
	 *            the actual path to refresh
	 * @param listener
	 *            job change listener that will be added to the created job, can
	 *            be <code>null</code>
	 * @param cancelJob
	 *            a job to cancel when the new one is run, can be
	 *            <code>null</code>
	 * @return the job created and scheduled
	 */
	public static Job update(ITargetDefinition definition, ITargetLocationHandler handler, TreePath[] treePaths,
			IJobChangeListener listener, Job cancelJob) {
		Job job = new UpdateTargetJob(cancelJob, monitor -> handler.update(definition, treePaths, monitor), true);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
		return job;
	}

	/**
	 * Schedules a new update job that will refresh all target locations in the
	 * provided target.
	 *
	 * @param handler
	 *            the handler
	 * @param definition
	 *            the target definition to refresh
	 * @param listener
	 *            job change listener that will be added to the created job, can
	 *            be <code>null</code>
	 * @param cancelJob
	 *            a job to cancel when the new one is run, can be
	 *            <code>null</code>
	 * @return the job created and scheduled
	 */
	public static Job refresh(ITargetDefinition definition, ITargetLocationHandler handler,
			IJobChangeListener listener, Job cancelJob) {
		Job job = new UpdateTargetJob(cancelJob,
				monitor -> handler.reload(definition, definition.getTargetLocations(), monitor),
				false);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
		return job;
	}

	private UpdateTargetJob(Job cancelJob, IJobFunction updateActions, boolean update) {
		super(update ? Messages.UpdateTargetJob_UpdateJobName : Messages.UpdateTargetJob_RefreshJobName);
		this.cancelJob = cancelJob;
		this.action = updateActions;
		this.update = update;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor,
				update ? Messages.UpdateTargetJob_UpdatingTarget : Messages.UpdateTargetJob_RefreshingTarget, 100);
		if (cancelJob != null) {
			cancelJob.cancel();
			try {
				cancelJob.join();
			} catch (InterruptedException e) {
				// we tried our best
			}
		}
		return action.run(progress);
	}
}
