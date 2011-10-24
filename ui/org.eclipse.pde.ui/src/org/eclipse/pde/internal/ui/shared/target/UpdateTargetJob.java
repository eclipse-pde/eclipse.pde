/*******************************************************************************
 * Copyright (c) 2010, 2011 EclipseSource Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

/**
 * Updates selected target locations using {@link ITargetLocationUpdater}s in an asynchronous job
 * <p>
 * If all calls to {@link ITargetLocationUpdater#update(org.eclipse.pde.core.target.ITargetDefinition, ITargetLocation, IProgressMonitor)}
 * return an OK status with {@link ITargetLocationUpdater#STATUS_CODE_NO_CHANGE}, the returned status will also have that status code,
 * indicating that no changes were made to the target.
 */
public class UpdateTargetJob extends Job {

	public static final String JOB_FAMILY_ID = "UpdateTargetJob"; //$NON-NLS-1$

	private Map toUpdate;
	private ITargetDefinition fTarget;

	/**
	 * Schedules a new update job that will update all target locations in the provided map.  A target's selected 
	 * children can be added as a set to the values of the map so that only certain portions of the target
	 * location get updated.
	 * <p>
	 * TODO The {@link ITargetLocationUpdater} does not currently support updating children
	 * </p><p>
	 *  If all calls to {@link ITargetLocationUpdater#update(org.eclipse.pde.core.target.ITargetDefinition, ITargetLocation, IProgressMonitor)}
	 * return an OK status with {@link ITargetLocationUpdater#STATUS_CODE_NO_CHANGE}, the returned status will also have that status code,
	 * indicating that no changes were made to the target.
	 * </p>
	 * @param target the target being updated
	 * @param toUpdate maps {@link ITargetLocation}s to the {@link Set} of selected children items 
	 * that should be updated.  The sets may be empty, but not <code>null</code>
	 * @param listener job change listener that will be added to the created job, can be <code>null</code>
	 */
	public static void update(ITargetDefinition target, Map toUpdate, IJobChangeListener listener) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		Job job = new UpdateTargetJob(toUpdate, target);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
	}

	/**
	 * Use {@link #update(Map, IJobChangeListener)} instead
	 */
	private UpdateTargetJob(Map toUpdate, ITargetDefinition target) {
		super(Messages.UpdateTargetJob_UpdateJobName);
		this.toUpdate = toUpdate;
		fTarget = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UpdateTargetJob_UpdatingTarget, toUpdate.size() * 100);
		MultiStatus errors = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.UpdateTargetJob_TargetUpdateFailedStatus, null);
		boolean noChange = true;
		for (Iterator iterator = toUpdate.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			ITargetLocation location = (ITargetLocation) entry.getKey();
			Set children = (Set) entry.getValue();

			String path = null;
			try {
				path = location.getLocation(false);
			} catch (CoreException e1) {
				// Ignore as this is just for the subtask
			}
			progress.subTask(NLS.bind(Messages.UpdateTargetJob_UpdatingContainer, path));

			// TODO Custom code for IUBundleContainers with children selected
			if (location instanceof IUBundleContainer && !children.isEmpty()) {
				try {
					boolean result = ((IUBundleContainer) location).update((Set) entry.getValue(), progress.newChild(100));
					if (result) {
						noChange = false;
					}
				} catch (CoreException e) {
					errors.add(e.getStatus());
				}
			} else {
				ITargetLocationUpdater provider = (ITargetLocationUpdater) Platform.getAdapterManager().getAdapter(location, ITargetLocationUpdater.class);
				if (provider != null) {
					if (provider.canUpdate(fTarget, location)) {
						IStatus result = provider.update(fTarget, location, progress.newChild(100));
						if (result.isOK() && result.getCode() != ITargetLocationUpdater.STATUS_CODE_NO_CHANGE) {
							noChange = false;
						} else if (!result.isOK()) {
							noChange = false;
							errors.add(result);
						}
					}
				} else {
					// If the button enablement is correct, this should not get hit
					progress.worked(100);
				}
			}
		}
		progress.done();
		if (noChange) {
			return new Status(IStatus.OK, PDECore.PLUGIN_ID, ITargetLocationUpdater.STATUS_CODE_NO_CHANGE, Messages.UpdateTargetJob_TargetUpdateSuccessStatus, null);
		} else if (!errors.isOK()) {
			return errors;
		} else {
			return Status.OK_STATUS;
		}
	}
}
