/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stefan Xenos (Google) - Initial implementation
 *     Andrey Loskutov (loskutov@gmx.de) - many different extensions
 *     Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.runtime;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Utility methods for JUnit tests.
 */
public class TestUtils {

	/**
	 * Call this in the tearDown method of every test to clean up state that can
	 * otherwise leak through SWT between tests.
	 */
	public static void cleanUp(String owner) {
		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());

		// Wait for any outstanding jobs to finish. Protect against deadlock by
		// terminating the wait after a timeout.
		boolean timedOut = waitForJobs(owner, 5, 5000);
		if (timedOut) {
			// We don't expect any extra jobs run during the test: try to cancel
			// them
			ILog.get().log(Status
					.info("[" + owner + "] Trying to cancel running jobs: " + getRunningOrWaitingJobs(null)));
			getRunningOrWaitingJobs(null).forEach(job -> job.cancel());
			waitForJobs(owner, 5, 1000);
		}

		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());
	}

	/**
	 * Process all queued UI events. If called from background thread, does
	 * nothing.
	 */
	public static void processUIEvents() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Keep pumping events until the queue is empty
			}
		}
	}

	/**
	 * Process all queued UI events. If called from background thread, just
	 * waits
	 *
	 * @param millis
	 *            max wait time to process events
	 */
	public static void processUIEvents(final long millis) throws Exception {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < millis) {
			Display display = Display.getCurrent();
			if (display != null && !display.isDisposed()) {
				while (display.readAndDispatch()) {
					// loop until the queue is empty
				}
			} else {
				Thread.sleep(10);
			}
		}
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has
	 * finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events
	 * are processed.
	 *
	 * @param owner
	 *            name of the caller which will be logged as prefix if the wait
	 *            times out
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @return true if the method timed out, false if all the jobs terminated
	 *         before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs) {
		return waitForJobs(owner, minTimeMs, maxTimeMs, (Object[]) null);
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has
	 * finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events
	 * are processed.
	 *
	 * @param owner
	 *            name of the caller which will be logged as prefix if the wait
	 *            times out
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @param excludedFamilies
	 *            optional list of job families to NOT wait for
	 *
	 * @return true if the method timed out, false if all the jobs terminated
	 *         before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs, Object... excludedFamilies) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException("Max time is smaller as min time!");
		}
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			processUIEvents();
			try {
				Thread.sleep(Math.min(10, minTimeMs));
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		while (!Job.getJobManager().isIdle()) {
			List<Job> jobs = getRunningOrWaitingJobs(null, excludedFamilies);
			if (jobs.isEmpty()) {
				// only uninteresting jobs running
				break;
			}

			if (!Collections.disjoint(runningJobs, jobs)) {
				// There is a job which runs already quite some time, don't wait
				// for it to avoid test timeouts
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}

			if (System.currentTimeMillis() - start >= maxTimeMs) {
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}
			processUIEvents();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		runningJobs.clear();
		return false;
	}

	private static Set<Job> runningJobs = new LinkedHashSet<>();

	private static void dumpRunningOrWaitingJobs(String owner, List<Job> jobs) {
		String message = "[" + owner + "] Some job is still running or waiting to run: "
				+ dumpRunningOrWaitingJobs(jobs);
		ILog.get().log(Status.error(message));
	}

	private static String dumpRunningOrWaitingJobs(List<Job> jobs) {
		if (jobs.isEmpty()) {
			return "";
		}
		// clear "old" running jobs, we only remember most recent
		runningJobs.clear();
		StringBuilder sb = new StringBuilder();
		for (Job job : jobs) {
			runningJobs.add(job);
			sb.append("\n'").append(job.toString()).append("'/");
			sb.append(job.getClass().getName());
			Thread thread = job.getThread();
			if (thread != null) {
				ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean()
						.getThreadInfo(new long[] { thread.threadId() }, true, true);
				if (threadInfos[0] != null) {
					sb.append("\nthread info: ").append(threadInfos[0]);
				}
			}
			sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public static List<Job> getRunningOrWaitingJobs(Object jobFamily, Object... excludedFamilies) {
		List<Job> running = new ArrayList<>();
		Job[] jobs = Job.getJobManager().find(jobFamily);
		for (Job job : jobs) {
			if (isRunningOrWaitingJob(job) && !belongsToFamilies(job, excludedFamilies)) {
				running.add(job);
			}
		}
		return running;
	}

	private static boolean isRunningOrWaitingJob(Job job) {
		int state = job.getState();
		return (state == Job.RUNNING || state == Job.WAITING);
	}

	private static boolean belongsToFamilies(Job job, Object... excludedFamilies) {
		if (excludedFamilies == null || excludedFamilies.length == 0) {
			return false;
		}
		for (Object family : excludedFamilies) {
			if (job.belongsTo(family)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a TestRule similar to {@link org.junit.rules.ExternalResource}
	 * but allows throwing unchecked exception in its
	 * {@link org.junit.rules.ExternalResource#after()} method. Furthermore
	 * {@code before} and {@code after} are expressed by the specified actions
	 * that may throw exceptions and can share a state.
	 *
	 * @param before
	 *            the action performed before the evaluation
	 * @param after
	 *            the action performed after the evaluation (only called if the
	 *            {@code before} action did not throw)
	 * @return a rule performing the given before respectively after action
	 *         before/after the evaluation of the base
	 * @param <S>
	 *            the type of state shared between before and after action
	 */
	public static <S> TestRule getThrowingTestRule(ThrowingSupplier<S> before, ThrowingConsumer<S> after) {
		return (base, description) -> new Statement() {
			@Override
			public void evaluate() throws Throwable {
				S state = before.get();
				List<Throwable> errors = new ArrayList<>();
				try {
					base.evaluate();
				} catch (Throwable t) {
					errors.add(t);
				} finally {
					try {
						after.accept(state);
					} catch (Throwable t) {
						errors.add(t);
					}
				}
				MultipleFailureException.assertEmpty(errors);
			}
		};
	}
}
