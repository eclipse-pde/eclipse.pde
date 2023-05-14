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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
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

	public static final String JOB_FAMILY_ID = "UpdateTargetJob"; //$NON-NLS-1$

	private List<IJobFunction> toUpdate;

	/**
	 * Schedules a new update job that will update all target locations in the
	 * provided map. A target's selected children can be added as a set to the
	 * values of the map so that only certain portions of the target location
	 * get updated.
	 * <p>
	 * If all calls to {@link IJobFunction#run(IProgressMonitor)} return an OK
	 * status with {@link ITargetLocationHandler#STATUS_CODE_NO_CHANGE}, the
	 * returned status will also have that status code, indicating that no
	 * changes were made to the target.
	 * </p>
	 *
	 * @param target
	 *            the target being updated
	 * @param updateActions
	 *            maps {@link ITargetLocation}s to the {@link Set} of selected
	 *            children items that should be updated. The sets may be empty,
	 *            but not <code>null</code>
	 * @param listener
	 *            job change listener that will be added to the created job, can
	 *            be <code>null</code>
	 */
	public static void update(List<IJobFunction> updateActions, IJobChangeListener listener) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		Job job = new UpdateTargetJob(updateActions);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
	}

	/**
	 * Use {@link #update(ITargetDefinition, Map, IJobChangeListener)} instead
	 */
	private UpdateTargetJob(List<IJobFunction> updateActions) {
		super(Messages.UpdateTargetJob_UpdateJobName);
		this.toUpdate = updateActions;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UpdateTargetJob_UpdatingTarget,
				toUpdate.size() * 100);
		MultiStatus errors = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.UpdateTargetJob_TargetUpdateFailedStatus,
				null);
		boolean noChange = true;
		for (IJobFunction action : toUpdate) {
			IStatus result = action.run(progress.split(100));
			if (result.isOK() && result.getCode() != ITargetLocationHandler.STATUS_CODE_NO_CHANGE) {
				noChange = false;
			} else if (!result.isOK()) {
				noChange = false;
				errors.add(result);
			}
		}
		progress.done();
		if (noChange) {
			return new Status(IStatus.OK, PDECore.PLUGIN_ID, ITargetLocationHandler.STATUS_CODE_NO_CHANGE,
					Messages.UpdateTargetJob_TargetUpdateSuccessStatus, null);
		} else if (!errors.isOK()) {
			return errors;
		} else {
			return Status.OK_STATUS;
		}
	}
}
