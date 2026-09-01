/*******************************************************************************
 *  Copyright (c) 2022, 2023 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *     Hannes Wellmann - Bug 325614 - Support mixed products (features and bundles)
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher.product;

import java.nio.file.Files;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import static java.util.Map.ofEntries;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addIncludedFeature;
import static org.eclipse.pde.ui.tests.util.ProjectUtils.addIncludedPlugin;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProductEditorLaunchingTest {

	@RegisterExtension
	public static final Extension CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@RegisterExtension
	public static final Extension RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	@TempDir
	public static Path folder;
	private static Path tpBundlePool;
	private static IProject project;

	@BeforeAll
	public static void setUpBeforeClass() throws Throwable {
		tpBundlePool = Files.createDirectory(folder.resolve("TPJarDirectory"));
		var targetPlugins = ofEntries( //
				bundle("plugin.a", "1.0.0"), //
				bundle("plugin.b", "1.0.0"), //
				bundle("plugin.c", "1.0.0"), //
				bundle("plugin.d", "1.0.0"), //
				bundle("plugin.in.f.a", "1.0.0"), //
				bundle("plugin.in.f.b", "1.0.0"), //
				bundle("plugin.in.f.c", "1.0.0"), //
				bundle("plugin.in.f.d", "1.0.0"), //
				bundle("plugin.in.f.in.f.b", "1.0.0"));
		TargetPlatformUtil.targetFeature("feature.a", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.in.f.a", "1.0.0");
		}, tpBundlePool);
		TargetPlatformUtil.targetFeature("feature.b", "1.0.0", f -> {
			addIncludedFeature(f, "feature.in.f.b", "1.0.0");
			addIncludedPlugin(f, "plugin.in.f.b", "1.0.0");
		}, tpBundlePool);
		TargetPlatformUtil.targetFeature("feature.in.f.b", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.in.f.in.f.b", "1.0.0");
		}, tpBundlePool);
		TargetPlatformUtil.targetFeature("feature.c", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.in.f.c", "1.0.0");
		}, tpBundlePool);
		TargetPlatformUtil.targetFeature("feature.c", "1.0.0", f -> {
			addIncludedPlugin(f, "plugin.in.f.c", "1.0.0");
		}, tpBundlePool);
		TargetPlatformUtil.setDummyBundlesAsTarget(targetPlugins, List.of(), tpBundlePool);
		project = ProjectUtils.importTestProject("tests/products");
	}

	private static final boolean USE_FEATURES_LAUNCH = true;
	private static final boolean USE_BUNDLE_LAUNCH = false;

	@Test
	public void testProductLaunch_useFeatureFALSE() throws Exception {
		Set<String> features = Set.of();
		Set<String> additionalBundles = Set.of();
		Set<String> bundles = Set.of("plugin.a");
		assertProductLaunchConfig("bundles1", USE_BUNDLE_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_typeBUNDLES() throws Exception {
		Set<String> features = Set.of();
		Set<String> additionalBundles = Set.of();
		Set<String> bundles = Set.of("plugin.a");
		assertProductLaunchConfig("bundles2", USE_BUNDLE_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_useFeatureTRUE() throws Exception {
		Set<String> features = Set.of("feature.a");
		Set<String> additionalBundles = Set.of();
		Set<String> bundles = Set.of("plugin.in.f.a");
		assertProductLaunchConfig("features1", USE_FEATURES_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_typeFEATURES() throws Exception {
		Set<String> features = Set.of("feature.a");
		Set<String> additionalBundles = Set.of();
		Set<String> bundles = Set.of("plugin.in.f.a");
		assertProductLaunchConfig("features2", USE_FEATURES_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_typeMIXED() throws Exception {
		Set<String> features = Set.of("feature.a");
		Set<String> additionalBundles = Set.of("plugin.a");
		Set<String> bundles = Set.of("plugin.a", "plugin.in.f.a");
		assertProductLaunchConfig("mixed", USE_FEATURES_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_bundlesWithVersion() throws Exception {
		Set<String> features = Set.of();
		Set<String> additionalBundles = Set.of();
		// Versions of plug-ins are not yet considered
		Set<String> bundles = Set.of("plugin.a", "plugin.b", "plugin.c");
		assertProductLaunchConfig("bundlesWithVersion", USE_BUNDLE_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_featuresWithVersion() throws Exception {
		Set<String> features = Set.of("feature.a", "feature.b", "feature.c");
		Set<String> additionalBundles = Set.of();
		Set<String> bundles = Set.of("plugin.in.f.a", "plugin.in.f.b", "plugin.in.f.in.f.b", "plugin.in.f.c");
		assertProductLaunchConfig("featuresWithVersion", USE_FEATURES_LAUNCH, features, additionalBundles, bundles);
	}

	@Test
	public void testProductLaunch_programArgumentsWithRepeatedValues() throws Exception {
		IFile file = project.getFile("programArgs.product");
		assertTrue(file.exists());
		WorkspaceProductModel model = new WorkspaceProductModel(file, false);
		model.load();
		IProduct product = model.getProduct();

		ILaunchConfiguration config = new LaunchAction(product, file.getFullPath(), ILaunchManager.RUN_MODE)
				.findLaunchConfiguration();

		String programArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");
		List<String> arguments = List.of(DebugPlugin.splitArguments(programArgs));

		// A repeated value must be preserved. It was wrongly dropped when it
		// matched a value contributed by an earlier user argument, turning
		// "-target testarg -helper testarg" into "-target testarg -helper".
		assertEquals("testarg", argumentValue(arguments, "-target"), "-target testarg expected"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("testarg", argumentValue(arguments, "-helper"), "-helper testarg expected"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(2, Collections.frequency(arguments, "testarg"), "both repeated values must be kept"); //$NON-NLS-1$ //$NON-NLS-2$
		// A user argument already provided by the initial arguments is not duplicated.
		assertEquals(1, Collections.frequency(arguments, "-consoleLog"), "-consoleLog must not be duplicated"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String argumentValue(List<String> arguments, String flag) {
		int index = arguments.indexOf(flag);
		assertTrue(index >= 0 && index + 1 < arguments.size(), flag + " is missing"); //$NON-NLS-1$
		return arguments.get(index + 1);
	}

	private void assertProductLaunchConfig(String productId, boolean useFeatures, Set<String> expectedFeatures,
			Set<String> expectedAdditionalBundles, Set<String> expectedBundles) throws CoreException {

		IFile file = project.getFile(productId + ".product");
		assertTrue(file.exists());
		WorkspaceProductModel model = new WorkspaceProductModel(file, false);
		model.load();
		IProduct product = model.getProduct();

		ILaunchConfiguration config = new LaunchAction(product, file.getFullPath(), ILaunchManager.RUN_MODE)
				.findLaunchConfiguration();

		assertEquals(productId + ".product", config.getName()); //$NON-NLS-1$
		assertEquals(useFeatures, config.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false));

		assertAttributeSet(expectedFeatures, IPDELauncherConstants.SELECTED_FEATURES, config, ":");
		assertAttributeSet(expectedAdditionalBundles, IPDELauncherConstants.ADDITIONAL_PLUGINS, config, ":");
		assertAttributeSet(expectedBundles, IPDELauncherConstants.SELECTED_TARGET_BUNDLES, config, "@");
		assertEquals(0, config.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, Set.of()).size());
	}

	private void assertAttributeSet(Set<String> expected, String attribute, ILaunchConfiguration config,
			String delimiter) throws CoreException {
		Set<String> values = config.getAttribute(attribute, Collections.emptySet());
		Set<String> ids = values.stream().map(f -> f.substring(0, f.indexOf(delimiter))).collect(Collectors.toSet());
		assertEquals(expected, ids);
	}

}
