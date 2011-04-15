/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * Job that will update a set of installable units from a set of bundle containers.  Containers that 
 * support updating (currently only {@link IUBundleContainer}) will check to see if any updates are 
 * available and if so, update to the newest version. 
 */
public class UpdateTargetJob extends Job {
	/**
	 * Bitmask constant indicating that the effective set of bundles and features indicated by this
	 * container has changed. Typically as the result of an update() operation.
	 */
	public static final int UPDATED = 1 << 1;

	/**
	 * Bitmask constant indicating that the actual definition of this
	 * container has changed. Typically as the result of an update() operation.
	 * This indicates that the target definition containing an updated container
	 * should be saved.
	 */
	public static final int DIRTY = 1 << 2;

	private static final String JOB_FAMILY_ID = "UpdateTargetJob"; //$NON-NLS-1$

	private Map toUpdate;
	private int result = 0;

	/**
	 * Schedules a new update job that will update a set of installable units from a set of containers.  Any
	 * previously running update jobs will be cancelled.  If a listener is provided it will be added to
	 * the scheduled job.
	 * 
	 * @param toUpdate maps {@link IBundleContainer}s that to the {@link Set} of {@link IInstallableUnit}s 
	 * that should be updated.  If the container maps to an empty set, all IUs from the container will be updated.
	 * @param listener job change listener that will be added to the created job, can be <code>null</code>
	 */
	public static void update(Map toUpdate, IJobChangeListener listener) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		Job job = new UpdateTargetJob(toUpdate);
		job.setUser(true);
		if (listener != null) {
			job.addJobChangeListener(listener);
		}
		job.schedule();
	}

	/**
	 * @see UpdateTargetJob#update(Map, IJobChangeListener)
	 */
	private UpdateTargetJob(Map toUpdate) {
		super(Messages.UpdateTargetJob_UpdateJobName);
		this.toUpdate = toUpdate;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UpdateTargetJob_UpdatingTarget, toUpdate.size());
		result = 0;
		try {
			for (Iterator i = toUpdate.entrySet().iterator(); i.hasNext();) {
				try {
					Map.Entry entry = (Map.Entry) i.next();
					IBundleContainer container = (IBundleContainer) entry.getKey();
					monitor.subTask(NLS.bind(Messages.UpdateTargetJob_UpdatingContainer, ((AbstractBundleContainer) container).getLocation(false)));
					if (container instanceof IUBundleContainer)
						result |= ((IUBundleContainer) container).update((Set) entry.getValue(), progress.newChild(1));
				} catch (CoreException e1) {
					return e1.getStatus();
				} finally {
					monitor.worked(1);
				}
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * @return whether the update job ran and resulted in the definition of a container changing and the target definition file needs to be saved 
	 * @see #isUpdated()
	 */
	public boolean isDirty() {
		return (result & DIRTY) != 0;
	}

	/**
	 * @return whether the update job ran and resulted in the bundles or features in a container changed
	 * @see UpdateTargetJob#isDirty()
	 */
	public boolean isUpdated() {
		return (result & UPDATED) != 0;
	}
}
