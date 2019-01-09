/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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

import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.progress.UIJob;

/**
 * Provides a default {@link #tearDown()} implementation to delete all
 * projects in the workspace.
 *
 */
public abstract class PDETestCase extends TestCase {

	private static boolean welcomeClosed;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestUtils.log(IStatus.INFO, getName(), "setUp");
		assertWelcomeScreenClosed();
	}

	@Override
	protected void tearDown() {
		TestUtils.log(IStatus.INFO, getName(), "tearDown");
		// Close any editors we opened
		IWorkbenchWindow[] workbenchPages = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow workbenchPage : workbenchPages) {
			IWorkbenchPage page = workbenchPage.getActivePage();
			if (page != null){
				page.closeAllEditors(false);
			}
		}
		TestUtils.processUIEvents();
		TestUtils.cleanUp(getName());
		// Delete any projects that were created
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (IProject project : projects) {
				project.delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
		}
		TestUtils.waitForJobs(getName(), 10, 10000);
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

			UIJob job = new UIJob("close welcome screen for debug test suite") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					closeIntro(wb);
					return Status.OK_STATUS;
				}

			};
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
}
