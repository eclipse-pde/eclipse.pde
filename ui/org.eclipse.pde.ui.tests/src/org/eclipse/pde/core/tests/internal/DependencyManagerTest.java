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
import static org.eclipse.pde.internal.core.DependencyManager.Options.INCLUDE_NON_TEST_FRAGMENTS;
import static org.eclipse.pde.internal.core.DependencyManager.Options.INCLUDE_OPTIONAL_DEPENDENCIES;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundle;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.bundleVersion;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.resolution;
import static org.eclipse.pde.ui.tests.util.TargetPlatformUtil.version;
import static org.osgi.framework.Constants.EXPORT_PACKAGE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.IMPORT_PACKAGE;
import static org.osgi.framework.Constants.PROVIDE_CAPABILITY;
import static org.osgi.framework.Constants.REQUIRE_BUNDLE;
import static org.osgi.framework.Constants.REQUIRE_CAPABILITY;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.ui.tests.launcher.AbstractLaunchTest;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
						entry(FRAGMENT_HOST, "bundle.a")),

				bundle("bundle.fragment.with.dependencies", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.a"), //
						entry(REQUIRE_BUNDLE, "bundle.b"), //
						entry(IMPORT_PACKAGE, "bundle.c.pack")),

				bundle("bundle.b", "1.0.0"), //

				bundle("bundle.c", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.c.pack")),

				bundle("bundle.d", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.d.pack")));

		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleB = bundleDescription("bundle.b", "1.0.0");
		BundleDescription bundleC = bundleDescription("bundle.c", "1.0.0");
		BundleDescription bundleFragment = bundleDescription("bundle.fragment", "1.0.0");
		BundleDescription bundleFragmentWithDeps = bundleDescription("bundle.fragment.with.dependencies", "1.0.0");

		Set<BundleDescription> bundles = Set.of(bundleA);

		Set<BundleDescription> noFragmentsClosure = findRequirementsClosure(bundles);
		assertThat(noFragmentsClosure).isEqualTo(Set.of(bundleA));

		Set<BundleDescription> allFragmentsClosure = findRequirementsClosure(bundles, INCLUDE_ALL_FRAGMENTS);
		assertThat(allFragmentsClosure)
		.isEqualTo(Set.of(bundleA, bundleFragment, bundleFragmentWithDeps, bundleB, bundleC));
	}

	@Test
	public void testFindRequirementsClosure_includeNonTestFragments() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "bundle.a.pack" + version("1.0.0"))),

				bundle("bundle.fragment", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.a")),

				bundle("other.tests", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.a")));

		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleFragment = bundleDescription("bundle.fragment", "1.0.0");
		BundleDescription otherFragmentWithTestName = bundleDescription("other.tests", "1.0.0");

		BundleDescription testFragmentWithTestAttr = createFragmentProject("bundle.a1.tests", "bundle.a", true);
		BundleDescription testFragmentWithOtherName = createFragmentProject("bundle.a.checks", "bundle.a", true);
		BundleDescription testFragmentWithoutTestAttr = createFragmentProject("bundle.a2.tests", "bundle.a", false);

		Set<BundleDescription> bundles = Set.of(bundleA);

		Set<BundleDescription> noFragmentsClosure = findRequirementsClosure(bundles);
		assertThat(noFragmentsClosure).isEqualTo(Set.of(bundleA));

		Set<BundleDescription> allFragmentsClosure = findRequirementsClosure(bundles, INCLUDE_ALL_FRAGMENTS);
		assertThat(allFragmentsClosure).isEqualTo(Set.of(bundleA, bundleFragment, otherFragmentWithTestName,
				testFragmentWithTestAttr, testFragmentWithOtherName, testFragmentWithoutTestAttr));

		Set<BundleDescription> nonTestFragmentsClosure = findRequirementsClosure(bundles, INCLUDE_NON_TEST_FRAGMENTS);
		assertThat(nonTestFragmentsClosure)
		.isEqualTo(Set.of(bundleA, bundleFragment, otherFragmentWithTestName, testFragmentWithoutTestAttr));
	}

	@Test
	public void testFindRequirementsClosure_includeFragmentsProvidingPackages() throws Exception {

		setTargetPlatform( //
				bundle("bundle.a", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.a" + version("1.0.0"))),
				bundle("fragment.a", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.a"), //
						entry(EXPORT_PACKAGE, "pack.a,pack.a.frag")),

				// host that imports package from a fragment
				bundle("bundle.b", "1.0.0", //
						entry(EXPORT_PACKAGE, "pack.b"), //
						entry(IMPORT_PACKAGE, "pack.b.frag")),
				bundle("fragment.b", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.b"), //
						entry(EXPORT_PACKAGE, "pack.b,pack.b.frag")),

				bundle("bundle.z", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.a")),
				bundle("bundle.y", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.a.frag")),
				bundle("bundle.x", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.b")),
				bundle("bundle.w", "1.0.0", //
						entry(IMPORT_PACKAGE, "pack.b.frag")));

		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription fragmentA = bundleDescription("fragment.a", "1.0.0");
		BundleDescription bundleB = bundleDescription("bundle.b", "1.0.0");
		BundleDescription fragmentB = bundleDescription("fragment.b", "1.0.0");

		BundleDescription bundleZ = bundleDescription("bundle.z", "1.0.0");
		BundleDescription bundleY = bundleDescription("bundle.y", "1.0.0");
		BundleDescription bundleX = bundleDescription("bundle.x", "1.0.0");
		BundleDescription bundleW = bundleDescription("bundle.w", "1.0.0");

		Set<BundleDescription> zClosure = findRequirementsClosure(Set.of(bundleZ));
		assertThat(zClosure).isEqualTo(Set.of(bundleZ, bundleA));

		Set<BundleDescription> yClosure = findRequirementsClosure(Set.of(bundleY));
		assertThat(yClosure).isEqualTo(Set.of(bundleY, bundleA, fragmentA));

		Set<BundleDescription> xClosure = findRequirementsClosure(Set.of(bundleX));
		assertThat(xClosure).isEqualTo(Set.of(bundleX, bundleB, fragmentB));

		Set<BundleDescription> wClosure = findRequirementsClosure(Set.of(bundleW));
		assertThat(wClosure).isEqualTo(Set.of(bundleW, bundleB, fragmentB));
	}

	@Test
	public void testFindRequirementsClosure_requirementsOfTransitivlyRequiredFragment() throws Exception {
		setTargetPlatform( //
				bundle("bundle.host", "1.0.0"),

				bundle("bundle.a", "1.0.0", //
						entry(REQUIRE_BUNDLE, "bundle.host"), //
						entry(REQUIRE_CAPABILITY, "some.test.capability")),

				bundle("bundle.fragment", "1.0.0", //
						entry(FRAGMENT_HOST, "bundle.host"), //
						entry(REQUIRE_BUNDLE, "bundle.b"), //
						entry(PROVIDE_CAPABILITY, "some.test.capability")),

				bundle("bundle.b", "1.0.0"));

		BundleDescription bundleHost = bundleDescription("bundle.host", "1.0.0");
		BundleDescription bundleFragment = bundleDescription("bundle.fragment", "1.0.0");
		BundleDescription bundleA = bundleDescription("bundle.a", "1.0.0");
		BundleDescription bundleB = bundleDescription("bundle.b", "1.0.0");

		List<BundleDescription> bundles = List.of(bundleHost, bundleA);
		// It's important that the host is first

		Set<BundleDescription> noFragmentsClosure = findRequirementsClosure(bundles);
		assertThat(noFragmentsClosure).isEqualTo(Set.of(bundleHost, bundleFragment, bundleA, bundleB));
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
			throws Exception {
		TargetPlatformUtil.setDummyBundlesAsTarget(Map.ofEntries(pluginDescriptions), List.of(), tpJarDirectory);
	}

	private static final String OPTIONAL = Constants.RESOLUTION_OPTIONAL;

	private static BundleDescription bundleDescription(String id, String version) {
		return AbstractLaunchTest.findTargetModel(id, version).getBundleDescription();
	}

	private static BundleDescription createFragmentProject(String projectName, String hostName,
			boolean setTestAttribute) throws CoreException {

		IProject project = ProjectUtils.createPluginProject(projectName, projectName, "1.0.0", (d, s) -> {
			d.setHost(s.newHost(hostName, Utils.EMPTY_RANGE));
		});
		IPluginModelBase model = PluginRegistry.findModel(project);
		if (setTestAttribute) { // set test attribute in classpath
			IClasspathEntry[] classpath = ClasspathComputer.getClasspath(project, model, null, false, true);
			var cpEntries = Arrays.stream(classpath).map(e -> ClasspathComputer.updateTestAttribute(true, e));
			JavaCore.create(project).setRawClasspath(cpEntries.toArray(IClasspathEntry[]::new), null);
		}
		return model.getBundleDescription();
	}
}
