package org.eclipse.pde.core.tests.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.osgi.framework.Version;

public class DependencyManagerTest {

	@Before
	public void ensurePluginModelManagerIsInitialized() {
		PluginModelManager.getInstance().getState();
	}

	@After
	public void clearWorkspace() throws Exception {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			project.delete(false, true, null);
		}
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
	public void testFindRequirementsClosure_RequireBundle2() {
		Map<URI, List<Entry<String, Version>>> locationIUs = Map.of(
				Path.of("tests", "sites", "site.a.b").toAbsolutePath().toUri(),
				List.of(Map.entry("feature.a.feature.group", Version.emptyVersion)));
		PDEState pdeState = createTPState(locationIUs);

		BundleDescription bundle3 = pdeState.getState().getBundle("bundle.a3", null);
		BundleDescription bundle2 = pdeState.getState().getBundle("bundle.a2", null);
		BundleDescription bundle1 = pdeState.getState().getBundle("bundle.a1", null);

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

	private PDEState createTPState(Map<URI, List<Entry<String, Version>>> locationIUs) {
		ITargetPlatformService tps = TargetPlatformService.getDefault();
		List<ITargetLocation> locations = new ArrayList<>();
		locationIUs.forEach((locationURI, ius) -> {
			String[] unitIds = ius.stream().map(Entry::getKey).toArray(String[]::new);
			String[] versions = ius.stream().map(Entry::getValue).map(Version::toString).toArray(String[]::new);
			URI[] repositories = new URI[] { locationURI };
			ITargetLocation location = tps.newIULocation(unitIds, versions, repositories,
					IUBundleContainer.INCLUDE_REQUIRED | IUBundleContainer.INCLUDE_CONFIGURE_PHASE);
			locations.add(location);
		});

		ITargetDefinition target = tps.newTarget();
		target.setTargetLocations(locations.toArray(ITargetLocation[]::new));

		IStatus resolveStatus = target.resolve(null);
		if (!resolveStatus.isOK()) {
			Assertions.fail("Target resolution failed: " + resolveStatus);
		} else if (!target.getStatus().isOK()) {
			Assertions.fail("Target resolution failed: " + target.getStatus());
		}

		TargetBundle[] allBundles = target.getAllBundles();

		List<URI> allLocations = Arrays.stream(allBundles).map(b -> b.getBundleInfo().getLocation())
				.filter(Objects::nonNull).collect(Collectors.toList());

		// Create a PDE State containing all of the target bundles
		PDEState pdeState = new PDEState(allLocations.toArray(URI[]::new), true, true, null);
		pdeState.resolveState(true);
		if (!pdeState.getState().isResolved()) {
			Assertions.fail("PDE state resolution failed");
		}
		State state = pdeState.getState();
		for (BundleDescription bundle : state.getBundles()) {
			ResolverError[] resolverErrors = state.getResolverErrors(bundle);
			if (resolverErrors != null && resolverErrors.length > 0) {
				System.err.println(bundle + " has resolution errors: " + Arrays.toString(resolverErrors));
			}
		}
		return pdeState;
	}
}
