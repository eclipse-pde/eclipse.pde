/*******************************************************************************
 *  Copyright (c) 2021, 2022 Hannes Wellmann and others.
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
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *     Hannes Wellmann - Bug 578336 - DependencyManager: improve method-signatures and tests
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.pde.internal.core.DependencyManager.findRequirementsClosure;
import static org.eclipse.pde.internal.core.DependencyManager.Options.INCLUDE_ALL_FRAGMENTS;
import static org.eclipse.pde.internal.core.DependencyManager.Options.INCLUDE_OPTIONAL_DEPENDENCIES;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.PROVIDE_CAPABILITY;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.REQUIRE_CAPABILITY;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.ui.tests.launcher.AbstractLaunchTest;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;

public class DependencyManagerTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Path tpJarDirectory;

	@Before
	public void setupBefore() throws IOException {
		tpJarDirectory = folder.newFolder("TPJarDirectory").toPath();
		// ensure PluginModelManager is initialized
		PluginModelManager.getInstance().getState();
	}

	@Test
	public void testFindRequirementsClosure_requireBundle() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("1.0.0"))),

				bundle("bundle.requireBundle", "1.0.0", //
						entry(REQUIRE_BUNDLE, "bundle.a" + bundleVersion("1.0.0", "1.1.0"))));

		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundle = bundleDescription("bundle.requireBundle", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundle);
		Set<BundleDescription> closure = findRequirementsClosure(bundles);
		assertThat(closure).isEqualTo(Set.of(bundleA, bundle));
	}

	@Test
	public void testFindRequirementsClosure_requireBundle2() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a1", "1.0.0"),

				bundle("bundle.a2", "1.0.0", //
						entry(REQUIRE_BUNDLE, "bundle.a1")),

				bundle("bundle.a3", "1.0.0", //
						entry(REQUIRE_BUNDLE, "bundle.a2")));

		BundleDescription bundle3 = bundleDescription("bundle.a3", "1.0.0");
		BundleDescription bundle2 = bundleDescription("bundle.a2", "1.0.0");
		BundleDescription bundle1 = bundleDescription("bundle.a1", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundle3);
		Set<BundleDescription> closure = findRequirementsClosure(bundles);
		assertThat(closure).isEqualTo(Set.of(bundle3, bundle2, bundle1));
	}

	@Test
	public void testFindRequirementsClosure_importPackage() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "2.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("2.0.0"))),

				bundle("bundle.importPackage", "1.0.0", //
						entry(IMPORT_PACKAGE, "bundle.a.pack")));

		BundleDescription bundleA = bundleDescription("bundle.a", "2.0.0");
		BundleDescription bundle = bundleDescription("bundle.importPackage", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundle);
		Set<BundleDescription> closure = findRequirementsClosure(bundles);
		assertThat(closure).isEqualTo(Set.of(bundleA, bundle));
	}

	@Test
	public void testFindRequirementsClosure_requiredCapability() throws Exception {

		setTargetPlatform( //
				bundle("capabilities.provider", "1.0.0", //
						entry(PROVIDE_CAPABILITY, "some.test.capability")),

				bundle("capabilities.consumer", "1.0.0", //
						entry(REQUIRE_CAPABILITY, "some.test.capability")));

		BundleDescription consumerDescription = bundleDescription("capabilities.consumer", "1.0.0");
		BundleDescription providerDescription = bundleDescription("capabilities.provider", "1.0.0");

		Set<BundleDescription> bundles = Set.of(consumerDescription);
		Set<BundleDescription> closure = findRequirementsClosure(bundles);
		assertThat(closure).isEqualTo(Set.of(consumerDescription, providerDescription));
	}

	@Test
	public void testFindRequirementsClosure_requireDifferentVersions() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("1.0.0"))),

				bundle("bundle.a", "2.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("2.0.0"))),

				bundle("bundle.importPackage", "1.0.0", //
						entry(IMPORT_PACKAGE, "bundle.a.pack" + version("2.0.0"))),

				bundle("bundle.requireBundle", "1.0.0", //
						entry(REQUIRE_BUNDLE, "bundle.a" + bundleVersion("1.0.0", "1.1.0"))));

		BundleDescription bundleA1 = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleA2 = bundleDescription("bundle.a", "2.0.0");
		BundleDescription bundleImportPackage = bundleDescription("bundle.importPackage", "1.0.0");
		BundleDescription bundleRequireBundle = bundleDescription("bundle.requireBundle", "1.0.0");
		// bundleImportPackage requires bundle.a 2.0.0
		// bundleRequireBundle requires bundle.a 1.0.0

		Set<BundleDescription> bundles = Set.of(bundleImportPackage, bundleRequireBundle);
		Set<BundleDescription> closure = findRequirementsClosure(bundles);
		assertThat(closure).isEqualTo(Set.of(bundleImportPackage, bundleRequireBundle, bundleA1, bundleA2));
	}

	@Test
	public void testFindRequirementsClosure_includeFragments() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("1.0.0"))),

				bundle("bundle.fragment", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.a")));

		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleFragment = bundleDescription("bundle.fragment", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundleA);

		Set<BundleDescription> noFragmentsClosure = findRequirementsClosure(bundles);
		assertThat(noFragmentsClosure).isEqualTo(Set.of(bundleA));

		Set<BundleDescription> allFragmentsClosure = findRequirementsClosure(bundles, INCLUDE_ALL_FRAGMENTS);
		assertThat(allFragmentsClosure).isEqualTo(Set.of(bundleA, bundleFragment));
	}

	@Test
	public void testFindRequirementsClosure_includeOptional() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("1.0.0"))),

				bundle("bundle.a", "2.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("2.0.0"))),

				bundle("capabilities.provider", "1.0.0", //
						entry(PROVIDE_CAPABILITY, "some.test.capability")),

				bundle("bundle.optional", "1.0.0", //
						entry(IMPORT_PACKAGE, "bundle.a.pack" + version("2.0.0") + resolution(OPTIONAL)), //
						entry(REQUIRE_BUNDLE, "bundle.a" + bundleVersion("1.0.0", "1.1.0") + resolution(OPTIONAL)), //
						entry(REQUIRE_CAPABILITY, "some.test.capability" + resolution(OPTIONAL))));

		BundleDescription bundleA1 = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleA2 = bundleDescription("bundle.a", "2.0.0");
		BundleDescription bundleProvider = bundleDescription("capabilities.provider", "1.0.0");
		BundleDescription bundleOptional = bundleDescription("bundle.optional", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundleOptional);

		Set<BundleDescription> noOptionalClosure = findRequirementsClosure(bundles);
		assertThat(noOptionalClosure).isEqualTo(Set.of(bundleOptional));

		Set<BundleDescription> optionalClosure = findRequirementsClosure(bundles, INCLUDE_OPTIONAL_DEPENDENCIES);
		assertThat(optionalClosure).isEqualTo(Set.of(bundleOptional, bundleA1, bundleA2, bundleProvider));
	}

	// --- utility methods ---

	@SafeVarargs
	private void setTargetPlatform(Map.Entry<NameVersionDescriptor, Map<String, String>>... pluginDescriptions)
			throws IOException, InterruptedException {
		TargetPlatformUtil.setDummyBundlesAsTarget(Map.ofEntries(pluginDescriptions), tpJarDirectory);
	}

	@SafeVarargs
	private static Entry<NameVersionDescriptor, Map<String, String>> bundle(String id, String version,
			Entry<String, String>... additionalManifestEntries) {
		return entry(new NameVersionDescriptor(id, version), Map.ofEntries(additionalManifestEntries));
	}

	private static String version(String version) {
		return ";" + Constants.VERSION_ATTRIBUTE + "=\"" + version + "\"";
	}

	private static String bundleVersion(String lowerBound, String upperBound) {
		return ";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"[" + lowerBound + "," + upperBound + ")\"";
	}

	private static String resolution(String resolutionType) {
		return ";" + Constants.RESOLUTION_DIRECTIVE + ":=\"" + resolutionType + "\"";
	}

	private static final String OPTIONAL = Constants.RESOLUTION_OPTIONAL;

	private BundleDescription bundleDescription(String id, String version) {
		return AbstractLaunchTest.findTargetModel(id, version).getBundleDescription();
	}
}
