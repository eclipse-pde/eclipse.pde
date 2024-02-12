/*******************************************************************************
 * Copyright (c) 2016, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gregory Amerson <gregory.amerson@liferay.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package bndtools.central;

import java.util.concurrent.atomic.AtomicReference;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.service.progress.ProgressPlugin;
import bndtools.Plugin;

public class JobProgress implements ProgressPlugin {
	static final ILogger logger = Logger.getLogger(JobProgress.class);

	@Override
	public Task startTask(String name, int size) {
		TaskJob taskjob = new TaskJob(name, size);
		taskjob.schedule();
		return taskjob;
	}

	private static class TaskJob extends Job implements Task {
		private final String					name;
		private final int						size;
		private final AtomicReference<IStatus>	status	= new AtomicReference<>();
		private volatile IProgressMonitor		monitor;

		TaskJob(String name, int size) {
			super(name);
			this.name = name;
			this.size = size;
			logger.logInfo(name, null);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			this.monitor = monitor;

			monitor.beginTask(name, size);
			while (status.get() == null) {
				if (!isCanceled(monitor)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						monitor.setCanceled(true);
						status.compareAndSet(null,
							new Status(IStatus.CANCEL, Plugin.PLUGIN_ID, "InterruptedException", e));
						Thread.currentThread()
							.interrupt();
					}
				}
			}
			monitor.done();

			return status.get();
		}

		/**
		 * Must ensure task is done if true is returned.
		 */
		private boolean isCanceled(IProgressMonitor m) {
			boolean canceled = m.isCanceled();
			if (canceled) {
				status.compareAndSet(null, new Status(IStatus.CANCEL, Plugin.PLUGIN_ID, "Canceled"));
			}
			return canceled;
		}

		@Override
		public void worked(int units) {
			IProgressMonitor m = monitor;
			if (m == null || (status.get() != null)) {
				return;
			}
			m.worked(units);
		}

		@Override
		public void done(String message, Throwable error) {
			status.compareAndSet(null,
				new Status(error == null ? IStatus.OK : IStatus.ERROR, Plugin.PLUGIN_ID, message, error));
		}

		@Override
		public boolean isCanceled() {
			IProgressMonitor m = monitor;
			if (m == null) {
				return false;
			}
			return isCanceled(m);
		}
	}
}
