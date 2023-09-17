/*******************************************************************************
 *  Copyright (c) 2005, 2023 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.FreezeMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.progress.UIJob;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Provides a default {@link #tearDown()} implementation to delete all
 * projects in the workspace.
 *
 */
public abstract class PDETestCase {

	private static boolean welcomeClosed;
	@Rule
	public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
		MessageDialog.AUTOMATED_MODE = true;
		ErrorDialog.AUTOMATED_MODE = true;
		FreezeMonitor.expectCompletionInAMinute();
		TestUtils.log(IStatus.INFO, name.getMethodName(), "setUp");
		assertWelcomeScreenClosed();
	}

	@After
	public void tearDown() throws Exception {
		TestUtils.log(IStatus.INFO, name.getMethodName(), "tearDown");
		// Close any editors we opened
		IWorkbenchWindow[] workbenchPages = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow workbenchPage : workbenchPages) {
			IWorkbenchPage page = workbenchPage.getActivePage();
			if (page != null){
				page.closeAllEditors(false);
			}
		}
		TestUtils.processUIEvents();
		TestUtils.cleanUp(name.getMethodName());
		// Delete any projects that were created
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		String firstFailureMessage = null;
		for (IProject project : projects) {
			try {
				project.delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				String message = "Failed to delete " + project;
				if (firstFailureMessage == null) {
					firstFailureMessage = message;
				}
				PDETestsPlugin.getDefault().getLog().error(message, e);
			}
		}
		TestUtils.waitForJobs(name.getMethodName(), 10, 10000);
		FreezeMonitor.done();
		if (firstFailureMessage != null) {
			fail(firstFailureMessage);
		}
	}

	/**
	 * Ensure the welcome screen is closed because in 4.x the debug perspective
	 * opens a giant fast-view causing issues
	 *
	 * @throws Exception
	 */
	protected final void assertWelcomeScreenClosed() throws Exception {
		if (!welcomeClosed && PlatformUI.isWorkbenchRunning()) {
			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb == null) {
				return;
			}
			// In UI thread we don't need to run a job
			if (Display.getCurrent() != null) {
				closeIntro(wb);
				return;
			}

			UIJob job = UIJob.create("close welcome screen for debug test suite",
					(ICoreRunnable) monitor -> closeIntro(wb));
			job.setPriority(Job.INTERACTIVE);
			job.setSystem(true);
			job.schedule();
		}
	}

	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im = wb.getIntroManager();
			IIntroPart intro = im.getIntro();
			if (intro != null) {
				welcomeClosed = im.closeIntro(intro);
			}
		}
	}

	public static void assumeRunningInStandaloneEclipseSDK() {
		try {
			Path location = Path.of(Platform.getInstallLocation().getURL().toURI());
			for (String directory : List.of("plugins", "features")) {
				assumeTrue("Not running in a standalone Eclipse SDK", Files.isDirectory(location.resolve(directory)));
			}
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
}
