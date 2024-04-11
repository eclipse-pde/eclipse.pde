/*******************************************************************************
 * Copyright (c) 2016, 2024 bndtools project and others.
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
 *     Christoph Läubrich - adapt to pde codebase
*******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import aQute.bnd.service.progress.ProgressPlugin;

public class JobProgress implements ProgressPlugin {

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
						status.compareAndSet(null, Status.CANCEL_STATUS);
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
				status.compareAndSet(null, Status.CANCEL_STATUS);
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
			if (error == null) {
				status.compareAndSet(null, Status.OK_STATUS);
			} else {
				status.compareAndSet(null, Status.error(message, error));
			}
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
