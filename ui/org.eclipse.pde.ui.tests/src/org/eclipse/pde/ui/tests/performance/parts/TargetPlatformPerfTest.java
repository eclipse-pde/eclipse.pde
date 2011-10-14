/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance.parts;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.pde.ui.tests.target.LocalTargetDefinitionTests;
import org.eclipse.pde.ui.tests.util.TestBundleCreator;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.Version;

/**
 * Tests the time it takes to resolve a target definition and set it as the target platform.
 *
 * These performance tests do many I/O operations so they iterate a large number of times to account for
 * the inconsistency.
 * 
 * The example target used in these tests consists of 1000 simple bundles only containing a manifest.  They
 * were generated using {@link TestBundleCreator} and have increasing numbers of required bundles specified in
 * the manifest (ex TestBundle_100 has required bundles of TestBundle_1 to TestBundle_99).
 *
 */
public class TargetPlatformPerfTest extends PerformanceTestCase {

	private static final String SEARCH_TEST_WORKSPACE_NAME = "ExampleWorkspaceProject";
	private static final String SEARCH_TEST_EXTERNAL_NAME = "TestBundle_500";
	private static final int SEARCH_TEST_EXTERNAL_COUNT = 1000;

	private static final String TEST_PLUGIN_LOCATION = "/tests/performance/target/targetPerfTestPlugins.zip";

	public static Test suite() {
		return new TestSuite(TargetPlatformPerfTest.class);
	}
	
	/**
	 * Resolves an example target definition
	 */
	public void testResolveTargetDefinition() throws Exception {
		tagAsSummary("Resolve target definition", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		IPath testBundles = extractTargetPerfTestPlugins();

		ITargetPlatformService tps = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());

		ITargetDefinition originalTarget = tps.newTarget();
		originalTarget.setBundleContainers(new IBundleContainer[] {tps.newDirectoryContainer(testBundles.toPortableString())});
		tps.saveTargetDefinition(originalTarget);
		ITargetHandle handle = originalTarget.getHandle();

		// Warm-up Iterations
		for (int i = 0; i < 3; i++) {
			// Get the target definition inside the loop so that it is not resolved
			ITargetDefinition target = handle.getTargetDefinition();
			target.resolve(new NullProgressMonitor());
		}
		// Test Iterations
		for (int i = 0; i < 200; i++) {
			// Get the target definition inside the loop so that it is not resolved
			ITargetDefinition target = handle.getTargetDefinition();
			startMeasuring();
			target.resolve(new NullProgressMonitor());
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();

	}

	/**
	 * Searches the model registry for various plug-ins to see how efficient model retrieval is
	 */
	public void testSearchModelRegistry() throws Exception {
		tagAsSummary("Search model registry", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		IPath testBundles = extractTargetPerfTestPlugins();

		IBundleProjectService service = (IBundleProjectService) PDECore.getDefault().acquireService(IBundleProjectService.class.getName());
		ITargetPlatformService tps = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());

		// Create a workspace model
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject proj = ws.getRoot().getProject(SEARCH_TEST_WORKSPACE_NAME);
		assertFalse("Project should not exist", proj.exists());
		IBundleProjectDescription description = service.getDescription(proj);
		description.setSymbolicName(SEARCH_TEST_WORKSPACE_NAME);
		description.setBundleVersion(new Version("1.1.1"));
		description.apply(null);

		// Build the project so it's model is added
		ws.build(IncrementalProjectBuilder.FULL_BUILD, null);

		try {

			// Set the example target as active
			ITargetDefinition target = tps.newTarget();
			target.setBundleContainers(new IBundleContainer[] {tps.newDirectoryContainer(testBundles.toPortableString())});
			target.resolve(null);
			LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
			job.runInWorkspace(new NullProgressMonitor());

			// Warm-up Iterations
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 10; j++) {
					executeSearchTest();
				}
			}
			// Test Iterations
			for (int i = 0; i < 100; i++) {
				startMeasuring();
				for (int j = 0; j < 10; j++) {
					executeSearchTest();
				}
				stopMeasuring();
			}
			commitMeasurements();
			assertPerformance();

		} finally {
			// Delete the created project
			proj.delete(true, true, null);
			// Restore the default target platform
			ITargetDefinition defaultTarget = ((TargetPlatformService) tps).newDefaultTargetDefinition();
			LoadTargetDefinitionJob restoreJob = new LoadTargetDefinitionJob(defaultTarget);
			restoreJob.runInWorkspace(null);
		}
	}

	/**
	 * Loads an example target definition as the active target platform
	 */
	public void testChangeTargetPlatform() throws Exception {
		tagAsSummary("Change target platform", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		IPath testBundles = extractTargetPerfTestPlugins();

		ITargetPlatformService tps = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());

		ITargetDefinition target = tps.newTarget();
		target.setBundleContainers(new IBundleContainer[] {tps.newDirectoryContainer(testBundles.toPortableString())});
		target.resolve(null);
		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);

		try {

			// Warm-up Iterations
			for (int i = 0; i < 2; i++) {
				// Execute test from this thread
				job.runInWorkspace(new NullProgressMonitor());
			}
			// Test Iterations
			for (int i = 0; i < 150; i++) {
				// Execute test from this thread
				startMeasuring();
				job.runInWorkspace(new NullProgressMonitor());
				stopMeasuring();
			}
			commitMeasurements();
			assertPerformance();

		} finally {
			// Restore the default target platform
			ITargetDefinition defaultTarget = ((TargetPlatformService) tps).newDefaultTargetDefinition();
			LoadTargetDefinitionJob restoreJob = new LoadTargetDefinitionJob(defaultTarget);
			restoreJob.runInWorkspace(null);
		}
	}

	private void executeSearchTest() {
		IPluginModelBase[] models;
		models = PluginRegistry.getAllModels();
		assertEquals(SEARCH_TEST_EXTERNAL_COUNT + 1, models.length);
		models = PluginRegistry.getWorkspaceModels();
		assertEquals(1, models.length);
		models = PluginRegistry.getExternalModels();
		assertEquals(SEARCH_TEST_EXTERNAL_COUNT, models.length);

		IPluginModelBase model;
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		model = PluginRegistry.findModel(SEARCH_TEST_WORKSPACE_NAME);
		assertNotNull(model);
		IProject project = ws.getRoot().getProject(SEARCH_TEST_WORKSPACE_NAME);
		model = PluginRegistry.findModel(project);
		assertNotNull(model);

		model = PluginRegistry.findModel(SEARCH_TEST_EXTERNAL_NAME);
		openRequiredBundles(model, new HashSet());

	}

	/**
	 * Checks that the given model exists and that all it's required bundles exist as
	 * models in the plugin registry.
	 * 
	 * @param model model to look up dependencies for
	 * @param allBundleNames set of symbolic names that have been looked up to prevent stack overflows
	 */
	private void openRequiredBundles(IPluginModelBase model, Set allBundleNames) {
		assertNotNull(model);
		BundleSpecification[] required = model.getBundleDescription().getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			if (!allBundleNames.contains(required[i].getName())) {
				allBundleNames.add(required[i].getName());
				model = PluginRegistry.findModel(required[i].getBundle());
				openRequiredBundles(model, allBundleNames);
			}
		}

	}

	private IPath extractTargetPerfTestPlugins() throws Exception {
		IPath stateLocation = MacroPlugin.getDefault().getStateLocation();
		IPath location = stateLocation.append("targetPerfTestPlugins");
		if (!location.toFile().exists()) {
			return doUnZip(location, TEST_PLUGIN_LOCATION);
		}
		return location;
	}

	/**
	 * Unzips the given archive to the specified location.
	 * 
	 * @param location path in the local file system
	 * @param archivePath path to archive relative to the test plug-in
	 * @throws IOException
	 */
	private IPath doUnZip(IPath location, String archivePath) throws IOException {
		URL zipURL = MacroPlugin.getBundleContext().getBundle().getEntry(archivePath);
		Path zipPath = new Path(new File(FileLocator.toFileURL(zipURL).getFile()).getAbsolutePath());
		ZipFile zipFile = new ZipFile(zipPath.toFile());
		Enumeration entries = zipFile.entries();
		IPath parent = location.removeLastSegments(1);
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			if (!entry.isDirectory()) {
				IPath entryPath = parent.append(entry.getName());
				File dir = entryPath.removeLastSegments(1).toFile();
				dir.mkdirs();
				File file = entryPath.toFile();
				file.createNewFile();
				InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
				byte[] bytes = LocalTargetDefinitionTests.getInputStreamAsByteArray(inputStream, -1);
				inputStream.close();
				BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
				outputStream.write(bytes);
				outputStream.close();
			}
		}
		zipFile.close();
		return parent;
	}
}
