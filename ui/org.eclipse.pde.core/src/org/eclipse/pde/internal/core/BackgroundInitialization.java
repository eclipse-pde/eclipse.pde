/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * Helper for model managers whose initialization resolves the target platform
 * and therefore must never run on the UI thread.
 */
final class BackgroundInitialization {

	private BackgroundInitialization() {
	}

	/**
	 * Schedules the given job unless {@code initialized} already reports success,
	 * and runs {@code callback} exactly once, either right away or once the job
	 * has finished, successfully or not.
	 */
	static void whenInitialized(Job job, BooleanSupplier initialized, Runnable callback) {
		if (initialized.getAsBoolean()) {
			callback.run();
			return;
		}
		AtomicBoolean notified = new AtomicBoolean();
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				event.getJob().removeJobChangeListener(this);
				if (notified.compareAndSet(false, true)) {
					callback.run();
				}
			}
		});
		job.schedule();
		// the job may already have finished before the listener was attached
		if (initialized.getAsBoolean() && notified.compareAndSet(false, true)) {
			callback.run();
		}
	}
}
