/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.performance.parts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.eclipse.pde.ui.tests.util.TestBundleCreator;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.junit.Assert;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

/**
 * Tests the time it takes to resolve a target definition and set it as the target platform.
 *
 * These performance tests do many I/O operations so they iterate a large number of times to account for
 * the inconsistency.
 *
 * The example target used in these tests consists of 1000 simple bundles only containing a manifest.  They
 * were generated using {@link TestBundleCreator} and have increasing numbers of required bundles specified in
 * the manifest (ex TestBundle_100 has required bundles of TestBundle_1 to TestBundle_99).
 */
public class TargetPlatformPerfTest extends PerformanceTestCase {

	private static final String SEARCH_TEST_WORKSPACE_NAME = "ExampleWorkspaceProject";
	private static final String SEARCH_TEST_EXTERNAL_NAME = "TestBundle_500";
	private static final int SEARCH_TEST_EXTERNAL_COUNT = 1000;

	private static final String TEST_PLUGIN_LOCATION = "/tests/performance/target/targetPerfTestPlugins.zip";

	/**
	 * Resolves an example target definition
	 */
	public void testResolveTargetDefinition() throws Exception {
		tagAsSummary("Resolve target definition", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		Path testBundles = extractTargetPerfTestPlugins();

		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);

		ITargetDefinition originalTarget = tps.newTarget();
		originalTarget.setTargetLocations(new ITargetLocation[] { tps.newDirectoryLocation(testBundles.toString()) });
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
		tagAsSummary("Resolve target definition", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		Path testBundles = extractTargetPerfTestPlugins();

		IBundleProjectService service = PDECore.getDefault().acquireService(IBundleProjectService.class);
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);

		// Create a workspace model
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject proj = ws.getRoot().getProject(SEARCH_TEST_WORKSPACE_NAME);
		Assert.assertFalse("Project should not exist", proj.exists());
		IBundleProjectDescription description = service.getDescription(proj);
		description.setSymbolicName(SEARCH_TEST_WORKSPACE_NAME);
		description.setBundleVersion(new Version("1.1.1"));
		description.apply(null);

		// Build the project so it's model is added
		ws.build(IncrementalProjectBuilder.FULL_BUILD, null);

		try {

			// Set the example target as active
			ITargetDefinition target = tps.newTarget();
			target.setTargetLocations(new ITargetLocation[] { tps.newDirectoryLocation(testBundles.toString()) });
			TargetPlatformUtil.loadAndSetTarget(target);

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
			ITargetDefinition defaultTarget = tps.newDefaultTarget();
			LoadTargetDefinitionJob restoreJob = new LoadTargetDefinitionJob(defaultTarget);
			restoreJob.runInWorkspace(null);
		}
	}

	/**
	 * Loads an example target definition as the active target platform
	 */
	//	public void testChangeTargetPlatform() throws Exception {
	//		tagAsSummary("Change target platform", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
	//		IPath testBundles = extractTargetPerfTestPlugins();
	//
	// ITargetPlatformService tps =
	// PDECore.getDefault().acquireService(ITargetPlatformService.class);
	//
	//		ITargetDefinition target = tps.newTarget();
	//		target.setTargetLocations(new ITargetLocation[] {tps.newDirectoryLocation(testBundles.toPortableString())});
	//		target.resolve(null);
	//		LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
	//
	//		try {
	//
	//			// Warm-up Iterations
	//			for (int i = 0; i < 1; i++) {
	//				// Execute test from this thread
	//				job.runInWorkspace(new NullProgressMonitor());
	//			}
	//			// Test Iterations
	//			for (int i = 0; i < 2; i++) {
	//				// Execute test from this thread
	//				startMeasuring();
	//				job.runInWorkspace(new NullProgressMonitor());
	//				stopMeasuring();
	//			}
	//			commitMeasurements();
	//			assertPerformance();
	//
	//		} finally {
	//			// Restore the default target platform
	//			ITargetDefinition defaultTarget = tps.newDefaultTarget();
	//			LoadTargetDefinitionJob restoreJob = new LoadTargetDefinitionJob(defaultTarget);
	//			restoreJob.runInWorkspace(null);
	//		}
	//	}

	private void executeSearchTest() {
		IPluginModelBase[] models;
		models = PluginRegistry.getAllModels();
		Assert.assertEquals(SEARCH_TEST_EXTERNAL_COUNT + 1, models.length);
		models = PluginRegistry.getWorkspaceModels();
		Assert.assertEquals(1, models.length);
		models = PluginRegistry.getExternalModels();
		Assert.assertEquals(SEARCH_TEST_EXTERNAL_COUNT, models.length);

		IPluginModelBase model;
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		model = PluginRegistry.findModel(SEARCH_TEST_WORKSPACE_NAME);
		Assert.assertNotNull(model);
		IProject project = ws.getRoot().getProject(SEARCH_TEST_WORKSPACE_NAME);
		model = PluginRegistry.findModel(project);
		Assert.assertNotNull(model);

		model = PluginRegistry.findModel(SEARCH_TEST_EXTERNAL_NAME);
		openRequiredBundles(model, new HashSet<>());

	}

	/**
	 * Checks that the given model exists and that all it's required bundles exist as
	 * models in the plugin registry.
	 *
	 * @param model model to look up dependencies for
	 * @param allBundleNames set of symbolic names that have been looked up to prevent stack overflows
	 */
	private void openRequiredBundles(IPluginModelBase model, Set<String> allBundleNames) {
		Assert.assertNotNull(model);
		BundleSpecification[] required = model.getBundleDescription().getRequiredBundles();
		for (BundleSpecification element : required) {
			if (!allBundleNames.contains(element.getName())) {
				allBundleNames.add(element.getName());
				model = PluginRegistry.findModel((Resource) element.getBundle());
				openRequiredBundles(model, allBundleNames);
			}
		}

	}

	static Path extractTargetPerfTestPlugins() throws Exception {
		Path stateLocation = PDETestCase.getThisBundlesStateLocation();
		Path location = stateLocation.resolve("targetPerfTestPlugins");
		if (!Files.exists(location)) {
			return PDETestCase.doUnZip(location.getParent(), TEST_PLUGIN_LOCATION);
		}
		return location;
	}

}
