/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.common.IClassContributionProvider;
import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.common.ClassContributionCollector;
import org.eclipse.e4.tools.emf.ui.internal.common.component.dialogs.FilteredContributionDialog;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilteredContributionDialogTest {

	private Display display;
	private Shell parent;
	private IEclipseContext context;

	@BeforeAll
	public static void fillStaticMessages() throws IllegalAccessException {
		// the static texts are only loaded inside a running workbench
		for (Field field : Messages.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class && field.get(null) == null) {
				field.set(null, field.getName());
			}
		}
	}

	@BeforeEach
	public void setUp() {
		display = Display.getDefault();
		parent = new Shell(display);
		context = EclipseContextFactory.create();
		context.set(Display.class, display);
	}

	@AfterEach
	public void tearDown() {
		parent.dispose();
		context.dispose();
	}

	@Test
	public void testCloseDoesNotWaitForRunningSearch() {
		Realm.runWithDefault(DisplayRealm.getRealm(display), () -> {
			try {
				closeWhileSearching();
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
		});
	}

	private void closeWhileSearching() throws InterruptedException {
		CountDownLatch searchStarted = new CountDownLatch(1);
		AtomicBoolean stopped = new AtomicBoolean();
		ClassContributionCollector collector = new ClassContributionCollector();
		collector.addContributor(new IClassContributionProvider() {
			@Override
			public void findContribution(Filter filter, ContributionResultHandler handler) {
				searchStarted.countDown();
				// keep reporting until cancelled, like a large target platform
				while (!filter.getProgressMonitor().isCanceled()) {
					handler.result(new ContributionData("bundle", "a.B", "Java", null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				stopped.set(true);
			}
		});

		TestDialog dialog = new TestDialog(parent, context, collector);
		dialog.setBlockOnOpen(false);
		dialog.open();
		assertTrue(spinUntil(() -> searchStarted.getCount() == 0), "search did not start"); //$NON-NLS-1$

		// a hanging close() would block this thread forever, so interrupt it to fail instead
		Thread uiThread = Thread.currentThread();
		ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
		ScheduledFuture<?> interrupt = watchdog.schedule(uiThread::interrupt, 10, TimeUnit.SECONDS);
		long start = System.nanoTime();
		boolean closed;
		try {
			closed = dialog.close();
		} finally {
			interrupt.cancel(false);
			watchdog.shutdownNow();
			Thread.interrupted();
		}
		long closeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		assertTrue(closed);
		assertTrue(closeMillis < 5_000, "close() waited for the search job"); //$NON-NLS-1$
		assertTrue(spinUntil(stopped::get), "search job was not cancelled"); //$NON-NLS-1$
	}

	private boolean spinUntil(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
			if (!display.readAndDispatch()) {
				Thread.sleep(10);
			}
		}
		return condition.getAsBoolean();
	}

	private static class TestDialog extends FilteredContributionDialog {
		private final ClassContributionCollector collector;

		TestDialog(Shell parentShell, IEclipseContext context, ClassContributionCollector collector) {
			super(parentShell, context);
			this.collector = collector;
		}

		@Override
		protected ClassContributionCollector getCollector() {
			return collector;
		}

		@Override
		protected String getFilterTextMessage() {
			return ""; //$NON-NLS-1$
		}

		@Override
		protected String getResourceNameText() {
			return ""; //$NON-NLS-1$
		}

		@Override
		protected String getDialogMessage() {
			return ""; //$NON-NLS-1$
		}

		@Override
		protected String getDialogTitle() {
			return ""; //$NON-NLS-1$
		}

		@Override
		protected String getShellTitle() {
			return ""; //$NON-NLS-1$
		}
	}
}
