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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Provides a default {@link #tearDown()} implementation to delete all
 * projects in the workspace.
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
		ILog.get().log(Status.info("[" + name.getMethodName() + "] " + "setUp"));
		assertWelcomeScreenClosed();
	}

	@After
	public void tearDown() throws Exception {
		ILog.get().log(Status.info("[" + name.getMethodName() + "] " + "tearDown"));
		// Close any editors we opened
		IWorkbenchWindow[] workbenchPages = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow workbenchPage : workbenchPages) {
			IWorkbenchPage page = workbenchPage.getActivePage();
			if (page != null) {
				page.closeAllEditors(false);
			}
		}
		TestUtils.processUIEvents();
		TestUtils.cleanUp(name.getMethodName());
		// Delete any projects that were created
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		AssertionError error = null;
		for (IProject project : projects) {
			try {
				deleteProject(project);
			} catch (CoreException e) {
				String message = "Failed to delete " + project;
				if (error == null) {
					error = new AssertionError(message);
					error.initCause(e);
				} else {
					error.addSuppressed(e);
				}
				ILog.get().error(message, e);
			}
		}
		TestUtils.waitForJobs(name.getMethodName(), 10, 10000);
		FreezeMonitor.done();
		if (error != null) {
			throw error;
		}
	}

	private void deleteProject(IProject project) throws CoreException {
		try {
			project.delete(true, new NullProgressMonitor());
		} catch (CoreException e) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
			}
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			if (project.exists()) {
				project.delete(true, new NullProgressMonitor());
			}
		}
	}

	/**
	 * Ensure the welcome screen is closed because in 4.x the debug perspective
	 * opens a giant fast-view causing issues
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

	/**
	 * Copies the content of the specified directory from the callers bundle to
	 * the specified specified target directory and restores the relative
	 * structure within the given directory.
	 * <p>
	 * This method works for bundles in directory and jar shape and is therefore
	 * suitable for tests that run in Eclipse I-builds too (where the
	 * Test-Plugins are executed as jars).
	 * </p>
	 *
	 * @param rootPath
	 *            the path of the directory to copy from this bundle
	 * @param targetRoot
	 *            the target directory
	 */
	public static void copyFromThisBundleInto(String rootPath, Path targetRoot)
			throws IOException, URISyntaxException {
		Bundle bundle = FrameworkUtil.getBundle(PDETestCase.class);
		URI rootEntry = bundle.getEntry(rootPath).toURI();
		List<URL> entries = Collections.list(bundle.findEntries(rootPath, null, true));
		for (URL entry : entries) {
			String relativePath = URIUtil.makeRelative(entry.toURI(), rootEntry).toString();
			if (!relativePath.endsWith("/")) {
				Path targetPath = targetRoot.resolve(relativePath);
				Files.createDirectories(targetPath.getParent());
				try (InputStream is = entry.openStream()) {
					Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	public static Path getThisBundlesStateLocation() {
		Bundle bundle = FrameworkUtil.getBundle(PDETestCase.class);
		return Platform.getStateLocation(bundle).toPath();
	}

	public static Path doUnZip(Path targetDirectory, String archivePath) throws IOException {
		URL zipURL = FrameworkUtil.getBundle(PDETestCase.class).getEntry(archivePath);
		assertNotNull("Zip file not found at path " + archivePath, zipURL);
		try (ZipInputStream zipStream = new ZipInputStream(zipURL.openStream())) {
			for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
				if (!entry.isDirectory()) {
					Path file = targetDirectory.resolve(entry.getName());
					Files.createDirectories(file.getParent());
					Files.copy(zipStream, file, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return targetDirectory;
		}
	}

	/**
	 * Recursively deletes the directory and files within.
	 *
	 * @param dir
	 *            directory to delete
	 */
	public static void delete(File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				delete(file);
			} else {
				file.delete();
			}
		}
		dir.delete();
	}
}
