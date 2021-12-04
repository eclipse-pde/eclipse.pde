/*******************************************************************************
 *  Copyright (c) 2021, 2021 Hannes Wellmann and others.
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
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.ui.tests.target.IUBundleContainerTests;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.*;
import org.junit.rules.TestRule;
import org.osgi.framework.Version;

public class DependencyManagerTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	@Rule
	public final TestRule restoreTargetDefinition = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	@Before
	public void ensurePluginModelManagerIsInitialized() {
		PluginModelManager.getInstance().getState();
	}

	@Test
	public void testFindRequirementsClosure_RequireBundle() throws IOException, CoreException {
		BundleDescription bundleA = importProjectBundle("tests/projects/requirements/bundle.A-1.0.0");
		BundleDescription bundle = importProjectBundle("tests/projects/requirements/bundle-RequireBundle");

		State state = TargetPlatformHelper.getState();
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null);
		BundleDescription osgiBundleFragment = osgiBundle.getFragments()[0];

		Set<BundleDescription> bundles = Set.of(bundle);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(Set.of(bundleA, bundle, osgiBundle, osgiBundleFragment));
	}

	@Test
	public void testFindRequirementsClosure_RequireBundle2() throws Exception {
		URI locationURI = IUBundleContainerTests.getURI("tests/sites/site.a.b");
		Map<URI, List<Entry<String, Version>>> locationIUs = Map.of(locationURI,
				List.of(Map.entry("feature.a.feature.group", Version.emptyVersion)));
		loadIUTarget(locationIUs);

		State state = TargetPlatformHelper.getState();
		BundleDescription bundle3 = state.getBundle("bundle.a3", null);
		BundleDescription bundle2 = state.getBundle("bundle.a2", null);
		BundleDescription bundle1 = state.getBundle("bundle.a1", null);

		Set<BundleDescription> bundles = Set.of(bundle3);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(Set.of(bundle3, bundle2, bundle1));
	}

	@Test
	public void testFindRequirementsClosure_ImportPackage() throws IOException, CoreException {
		BundleDescription bundleA = importProjectBundle("tests/projects/requirements/bundle.A-2.0.0");
		BundleDescription bundle = importProjectBundle("tests/projects/requirements/bundle-ImportPackage");

		State state = TargetPlatformHelper.getState();
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null);
		BundleDescription osgiBundleFragment = osgiBundle.getFragments()[0];

		Set<BundleDescription> bundles = Set.of(bundle);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(Set.of(bundleA, bundle, osgiBundle, osgiBundleFragment));
	}

	@Test
	public void testFindRequirementsClosure_RequiredCapability() throws IOException, CoreException {
		BundleDescription consumerDescription = importProjectBundle(
				"tests/projects/capabilities/capabilities.consumer");
		BundleDescription providerDescription = importProjectBundle(
				"tests/projects/capabilities/capabilities.provider");

		State state = TargetPlatformHelper.getState();
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null);
		BundleDescription osgiBundleFragment = osgiBundle.getFragments()[0];

		Set<BundleDescription> bundles = Set.of(consumerDescription);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(Set.of(consumerDescription, providerDescription, osgiBundle, osgiBundleFragment));
	}

	@Test
	public void testFindRequirementsClosure_RequireDifferentVersions() throws IOException, CoreException {
		BundleDescription bundleA1 = importProjectBundle("tests/projects/requirements/bundle.A-1.0.0");
		BundleDescription bundleA2 = importProjectBundle("tests/projects/requirements/bundle.A-2.0.0");
		BundleDescription bundleImportPackage = importProjectBundle("tests/projects/requirements/bundle-ImportPackage");
		BundleDescription bundleRequireBundle = importProjectBundle("tests/projects/requirements/bundle-RequireBundle");
		// bundleImportPackage requires bundle.a 2.0.0
		// bundleRequireBundle requires bundle.a 1.0.0

		State state = TargetPlatformHelper.getState();
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null);
		BundleDescription osgiBundleFragment = osgiBundle.getFragments()[0];

		Set<BundleDescription> bundles = Set.of(bundleImportPackage, bundleRequireBundle);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(
				Set.of(bundleImportPackage, bundleRequireBundle, bundleA1, bundleA2, osgiBundle, osgiBundleFragment));
	}

	@Test
	public void testFindRequirementsClosure_IncludeFragments() throws IOException, CoreException {
		BundleDescription bundleA = importProjectBundle("tests/projects/requirements/bundle.A-1.0.0");
		BundleDescription bundleFragment = importProjectBundle("tests/projects/requirements/bundle-Fragment");

		State state = TargetPlatformHelper.getState();
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null);
		BundleDescription osgiBundleFragment = osgiBundle.getFragments()[0];

		Set<BundleDescription> bundles = Set.of(bundleA);
		Set<BundleDescription> closure = DependencyManager.findRequirementsClosure(bundles, false);
		assertThat(closure).isEqualTo(Set.of(bundleA, bundleFragment, osgiBundle, osgiBundleFragment));
	}

	// --- utility methods ---

	private BundleDescription importProjectBundle(String projectPath) throws IOException, CoreException {
		IProject project = ProjectUtils.importTestProject(projectPath);
		return PluginRegistry.findModel(project).getBundleDescription();
	}

	private void loadIUTarget(Map<URI, List<Entry<String, Version>>> locationIUs) throws Exception {
		ITargetPlatformService tps = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		List<ITargetLocation> locations = new ArrayList<>();
		locationIUs.forEach((locationURI, ius) -> {
			String[] unitIds = ius.stream().map(Entry::getKey).toArray(String[]::new);
			String[] versions = ius.stream().map(Entry::getValue).map(Version::toString).toArray(String[]::new);
			URI[] repositories = new URI[] { locationURI };
			ITargetLocation location = tps.newIULocation(unitIds, versions, repositories,
					IUBundleContainer.INCLUDE_REQUIRED | IUBundleContainer.INCLUDE_CONFIGURE_PHASE);
			locations.add(location);
		});
		TargetPlatformUtil.createAndSetTargetForWorkspace(null, locations, null);
	}
}
