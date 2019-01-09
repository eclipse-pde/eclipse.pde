/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.ee;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;

/**
 * Tests exporting bundles.
 */
public class ExportBundleTests extends PDETestCase {

	private static final IPath EXPORT_PATH = PDETestsPlugin.getDefault().getStateLocation().append(".export");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Path path = Files.createDirectories(EXPORT_PATH.toFile().toPath());
		if (Files.exists(path)) {
			Files.delete(path);
		}
		assertFalse(Files.exists(path));
	}

	/**
	 * Deletes the specified project.
	 *
	 * @param name
	 * @throws CoreException
	 */
	protected void deleteProject(String name) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project.exists()) {
			project.delete(true, null);
		}
	}

	/**
	 * Deletes the specified folder.
	 *
	 * @param dir
	 *            the file to delete
	 * @throws IOException
	 */
	protected void deleteFolder(File dir) throws IOException {
		if (dir.isDirectory()) {
			if (dir.list().length == 0)
				Files.delete(dir.toPath());
			else {
				File[] files = dir.listFiles();
				for (File file : files) {
					deleteFolder(file);
				}
				if (dir.list().length == 0) {
					Files.delete(dir.toPath());
				}
			}
		} else {
			Files.delete(dir.toPath());
		}
	}

	/**
	 * Validates the target level of a generated class file.
	 *
	 * @param zipFileName location of archive file
	 * @param zipEntryName path to class file in archive
	 * @param major expected major class file version
	 */
	protected void validateTargetLevel(String zipFileName, String zipEntryName, int major) {
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(zipFileName, zipEntryName, IClassFileReader.ALL);
		assertEquals("Wrong major version", major, reader.getMajorVersion());
	}

	/**
	 * Exports a plug-in project with a custom execution environment and validates class file
	 * target level.
	 *
	 * @throws Exception
	 */
	public void testExportCustomEnvironment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(EnvironmentAnalyzerDelegate.EE_NO_SOUND);
			IJavaProject project = ProjectUtils.createPluginProject("no.sound.export", env);
			assertTrue("Project was not created", project.exists());

			final FeatureExportInfo info = new FeatureExportInfo();
			info.toDirectory = true;
			info.useJarFormat = true;
			info.exportSource = false;
			info.allowBinaryCycles = false;
			info.useWorkspaceCompiledClasses = false;
			info.destinationDirectory = EXPORT_PATH.toOSString();
			info.zipFileName = null;
			info.items = new Object[]{PluginRegistry.findModel(project.getProject())};
			info.signingInfo = null;
			info.qualifier = "vXYZ";

			PluginExportOperation job = new PluginExportOperation(info, "Test-Export");
			job.schedule();
			job.join();
			if (job.hasAntErrors()){
				fail("Export job had ant errors");
			}
			IStatus result = job.getResult();
			assertTrue("Export job had errors", result.isOK());

			TestUtils.processUIEvents(100);
			TestUtils.waitForJobs(getName(), 100, 10000);

			// verify exported bundle exists
			IPath path = EXPORT_PATH.append("plugins/no.sound.export_1.0.0.jar");

			// The jar file may not have been copied to the file system yet, see Bug 424597
			if (!path.toFile().exists()) {
				TestUtils.waitForJobs(getName(), 100, 30000);
			}

			assertTrue("Missing exported bundle", path.toFile().exists());
			validateTargetLevel(path.toOSString(), "no/sound/export/Activator.class", 47);
		} finally {
			TestUtils.waitForJobs(getName(), 10, 5000);
			deleteProject("no.sound.export");
			deleteFolder(EXPORT_PATH.toFile());
		}
	}

	/**
	 * Exports a plug-in project with a J2SE-1.4 execution environment and validates class file
	 * target level.
	 *
	 * @throws Exception
	 */
	public void testExport14Environment() throws Exception {
		try {
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4");
			IJavaProject project = ProjectUtils.createPluginProject("j2se14.export", env);
			assertTrue("Project was not created", project.exists());

			final FeatureExportInfo info = new FeatureExportInfo();
			info.toDirectory = true;
			info.useJarFormat = true;
			info.exportSource = false;
			info.allowBinaryCycles = false;
			info.useWorkspaceCompiledClasses = false;
			info.destinationDirectory = EXPORT_PATH.toOSString();
			info.zipFileName = null;
			info.items = new Object[]{PluginRegistry.findModel(project.getProject())};
			info.signingInfo = null;
			info.qualifier = "vXYZ";

			long l1 = System.currentTimeMillis();
			PluginExportOperation job = new PluginExportOperation(info, "Test-Export");
			long l2 = System.currentTimeMillis();
			job.schedule();
			long l3 = System.currentTimeMillis();
			job.join();
			long l4 = System.currentTimeMillis();
			if (job.hasAntErrors()){
				fail("Export job had ant errors");
			}
			long l5 = System.currentTimeMillis();
			IStatus result = job.getResult();
			assertTrue("Export job had errors", result.isOK());
			long l6 = System.currentTimeMillis();

			// veriry exported bundle exists
			IPath path = EXPORT_PATH.append("plugins/j2se14.export_1.0.0.jar");
			long l7 = System.currentTimeMillis();

			TestUtils.processUIEvents(100);
			TestUtils.waitForJobs(getName(), 100, 10000);

			boolean didPathExistBeforeSleep = path.toFile().exists();
			/*		give a 30 second delay when the path doesn't exist
					( JUST IN CASE - unlikely to work but worth trying)*/
			if (!path.toFile().exists()) {
				TestUtils.waitForJobs(getName(), 3000, 30000);
			}
			boolean didPathExistAfterSleep = path.toFile().exists();

			long l8 = System.currentTimeMillis();

			/*	 print out the time taken and see if there is a pattern when this test breaks
			     see Bug 424597. Further debug statement may be required in future  */
			if (true) { //print information everytime - in PASS OR FAIL
				System.out.println("BUG 424597\n================================");
				System.out.println("Export path: " + EXPORT_PATH);
				System.out.println("Constructor of PluginExportOperation time: " + (l2 - l1));
				System.out.println("Schedule                             time: " + (l3 - l2));
				System.out.println("Job join                             time: " + (l4 - l3));
				System.out.println("Ant Error                            time: " + (l5 - l4));
				System.out.println("Job result                           time: " + (l6 - l5));
				System.out.println("Append                               time: " + (l7 - l6));
				System.out.println("Thread sleep time if file not pr     time: " + (l8 - l7));

				System.out.println("Did file exist before sleep: " + didPathExistBeforeSleep);
				System.out.println("Did file exist after  sleep: " + didPathExistAfterSleep);

				System.out.println("================================\nEnd of BUG 424597");
			}

			System.out.println("BUG 424597\n================================");
			File exportContents = EXPORT_PATH.toFile();
			if (exportContents.isDirectory()) {
				System.out.println("Exported directory exists: ");

				// Should only have plugin/feature folders
				printContents(exportContents);
			} else {
				IPath stateLocation = PDETestsPlugin.getDefault().getStateLocation();
				if (stateLocation.toFile().exists()) {
					System.out.println("Exported directory is missing, parent: ");
					printContents(stateLocation.toFile());
				} else {
					System.out.println("Neither exported directory not the state location exist!");
				}
			}
			System.out.println("================================\nEnd of BUG 424597");

			assertTrue("Missing exported bundle", path.toFile().exists());
			validateTargetLevel(path.toOSString(), "j2se14/export/Activator.class", 46);
		} finally {
			TestUtils.waitForJobs(getName(), 10, 5000);
			deleteProject("j2se14.export");
			deleteFolder(EXPORT_PATH.toFile());
		}
	}

	private void printContents(File dir) {
		System.out.println("First 2 levels of: " + dir);
		File[] children = dir.listFiles();
		for (File element : children) {
			if (element.isDirectory()) {
				System.out.println("Directory: " + element.getName());
				File[] subChildren = element.listFiles();
				for (File subElement : subChildren) {
					if (subElement.isDirectory()) {
						System.out.println("   Directory: " + subElement.getName());
					} else {
						System.out.println("   File: " + subElement.getName());
					}
				}
			} else {
				System.out.println("File: " + element.getName());
			}
		}
	}

}
