/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.junit.Test;

/**
 * Tests for target definitions. The tested targets will be created in the
 * metadata.
 *
 * @see WorkspaceTargetDefinitionTests
 *
 * @since 3.5
 */
public class LocalTargetDefinitionTests extends AbstractTargetTest {

	public static final NameVersionDescriptor MULTI_VERSION_LOW_DESCRIPTION = new NameVersionDescriptor(
			"a.typical.bundle", "1.0.0.200907071058");
	public static final NameVersionDescriptor MULTI_VERSION_HIGH_DESCRIPTION = new NameVersionDescriptor(
			"a.typical.bundle", "1.1.0.200907071100");

	/**
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a set of URIs.
	 *
	 * @param target
	 *            target definition
	 * @return all bundle URIs
	 */
	private static Set<URI> getAllBundleURIs(ITargetDefinition target) throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		return Arrays.stream(target.getBundles()).map(b -> b.getBundleInfo().getLocation()).collect(Collectors.toSet());
	}

	/**
	 * Tests that resetting the target platform should work OK (i.e. is
	 * equivalent to the models in the default target platform).
	 */
	@Test
	public void testResetTargetPlatform() throws Exception {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		Set<URI> uris = getAllBundleURIs(definition);
		Set<URI> fragments = Arrays.stream(definition.getBundles()) //
				.filter(TargetBundle::isFragment).map(b -> b.getBundleInfo().getLocation())
				.collect(Collectors.toCollection(HashSet::new));

		// current platform
		IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();

		// should be equivalent
		assertEquals("Should have same number of bundles", uris.size(), models.length);
		for (IPluginModelBase model : models) {
			Path location = Path.of(model.getInstallLocation());
			URI uri = location.toUri();
			assertTrue("Missing plug-in " + location, uris.contains(uri));
			if (model.isFragmentModel()) {
				assertTrue("Missing fragment", fragments.remove(uri));
			}
		}
		assertTrue("Different number of fragments", fragments.isEmpty());
	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving).
	 */
	@Test
	public void testDefaultTargetPlatform() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		definition.setTargetLocations(new ITargetLocation[] { container });
		assertTargetContentIsEqualToRuntimeBundles(definition);
	}

	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundles contains the right set.
	 */
	@Test
	public void testRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null) };
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.setIncluded(restrictions);
		List<BundleInfo> infos = getAllBundleInfos(definition);

		assertEquals("Wrong number of bundles", 2, infos.size());
		Set<String> set = collectAllSymbolicNames(infos);
		for (NameVersionDescriptor info : restrictions) {
			set.remove(info.getId());
		}
		assertEquals("Wrong bundles", 0, set.size());
	}

	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundle versions contains the right set.
	 */
	@Test
	public void testVersionRestrictedDefaultTargetPlatform() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		definition.setTargetLocations(new ITargetLocation[] { container });
		List<BundleInfo> infos = getAllBundleInfos(definition);
		// find right versions
		String v1 = infos.stream().filter(info -> info.getSymbolicName().equals("org.eclipse.jdt.launching"))
				.map(info -> info.getVersion()).findFirst().orElse(null);
		String v2 = infos.stream().filter(info -> info.getSymbolicName().equals("org.eclipse.jdt.debug"))
				.map(info -> info.getVersion()).findFirst().orElse(null);
		assertNotNull("Missing bundle 'org.eclipse.jdt.launching'", v1);
		assertNotEquals(BundleInfo.EMPTY_VERSION, v1);
		assertNotNull("Missing bundle 'org.eclipse.jdt.debug'", v2);
		assertNotEquals(BundleInfo.EMPTY_VERSION, v2);

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", v1),
				new NameVersionDescriptor("org.eclipse.jdt.debug", v2) };
		definition.setIncluded(restrictions);
		infos = getAllBundleInfos(definition);

		assertEquals("Wrong number of bundles", 2, infos.size());
		for (BundleInfo info : infos) {
			if (info.getSymbolicName().equals("org.eclipse.jdt.launching")) {
				assertEquals(v1, info.getVersion());
			} else if (info.getSymbolicName().equals("org.eclipse.jdt.debug")) {
				assertEquals(v2, info.getVersion());
			}
		}
	}

	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundles contains the right set. In this case
	 * empty, since the versions specified are bogus.
	 */
	@Test
	public void testMissingVersionRestrictedDefaultTargetPlatform() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", "xyz"),
				new NameVersionDescriptor("org.eclipse.jdt.debug", "abc") };
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.setIncluded(restrictions);
		definition.resolve(null);
		TargetBundle[] bundles = definition.getBundles();

		assertEquals("Wrong number of bundles", 2, bundles.length);
		for (TargetBundle rb : bundles) {
			assertEquals("Should be a missing bundle version", TargetBundle.STATUS_VERSION_DOES_NOT_EXIST,
					rb.getStatus().getCode());
			assertEquals("Should be an error", IStatus.ERROR, rb.getStatus().getSeverity());
		}
	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}
	 */
	@Test
	public void testEclipseHomeTargetPlatform() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation("${eclipse_home}", null);
		definition.setTargetLocations(new ITargetLocation[] { container });
		assertTargetContentIsEqualToRuntimeBundles(definition);
	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}.
	 */
	@Test
	public void testEclipseHomeTargetPlatformAndConfigurationArea() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation("${eclipse_home}",
				"${eclipse_home}/configuration");
		definition.setTargetLocations(new ITargetLocation[] { container });
		assertTargetContentIsEqualToRuntimeBundles(definition);
	}

	/**
	 * Tests that a bundle directory container is equivalent to scanning
	 * locations.
	 */
	@Test
	public void testDirectoryBundleContainer() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService()
				.newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		definition.setTargetLocations(new ITargetLocation[] { container });
		Set<URI> uris = getAllBundleURIs(definition);

		assertEquals(getAllBundleURIs(TargetPlatformService.getDefault().newDefaultTarget()), uris);
	}

	/**
	 * Tests that a bundle directory container is equivalent to scanning
	 * locations when it uses a variable to specify its location.
	 */
	@Test
	public void testVariableDirectoryBundleContainer() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation("${eclipse_home}/plugins");
		definition.setTargetLocations(new ITargetLocation[] { container });
		Set<URI> uris = getAllBundleURIs(definition);

		assertEquals(getAllBundleURIs(TargetPlatformService.getDefault().newDefaultTarget()), uris);
	}

	/**
	 * Tests a JDT feature bundle container contains the appropriate bundles
	 */
	@Test
	public void testFeatureBundleContainer() throws Exception {
		// extract the feature
		Path location = extractModifiedFeatures();

		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toString(), "org.eclipse.jdt", null);
		container.resolve(definition, null);
		TargetBundle[] bundles = container.getBundles();

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}

		assertEquals("Wrong number of bundles in test JDT feature", expected.size(), bundles.length);
		for (TargetBundle bundle : bundles) {
			expected.remove(bundle.getBundleInfo().getSymbolicName());
		}
		assertTrue("Wrong bundles in JDT feature. Missing: " + expected, expected.isEmpty());

		// should be no source bundles
		for (TargetBundle bundle : bundles) {
			assertFalse("Should be no source bundles", bundle.isSourceBundle());
		}
	}

	/**
	 * Tests a JDT feature bundle container contains the appropriate bundles for
	 * a specific OS.
	 */
	@Test
	public void testMacOSFeatureBundleContainer() throws Exception {
		// extract the feature
		Path location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		definition.setOS(Platform.OS_MACOSX);
		ITargetLocation container = getTargetService().newFeatureLocation(location.toString(), "org.eclipse.jdt", null);
		container.resolve(definition, null);
		TargetBundle[] bundles = container.getBundles();

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		expected.add("org.eclipse.jdt.launching.macosx");

		assertEquals("Wrong number of bundles in JDT feature", expected.size(), bundles.length);
		for (TargetBundle bundle : bundles) {
			String symbolicName = bundle.getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundle.getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST,
							status.getCode());
				}
			}
		}
		assertTrue("Wrong bundles in JDT feature. Missing: " + expected, expected.isEmpty());

		// should be no source bundles
		for (TargetBundle bundle : bundles) {
			assertFalse("Should be no source bundles", bundle.isSourceBundle());
		}
	}

	/**
	 * Tests that a target definition based on the JDT feature restricted to a
	 * subset of bundles contains the right set.
	 */
	@Test
	public void testRestrictedFeatureBundleContainer() throws Exception {
		// extract the feature
		Path location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toString(), "org.eclipse.jdt", null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt", null),
				new NameVersionDescriptor("org.junit", "3.8.2.v20090203-1005") };
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.setIncluded(restrictions);
		List<BundleInfo> infos = getAllBundleInfos(definition);

		assertEquals("Wrong number of bundles", 2, infos.size());
		Set<String> set = collectAllSymbolicNames(infos);
		for (NameVersionDescriptor info : restrictions) {
			set.remove(info.getId());
		}
		assertEquals("Wrong bundles", 0, set.size());
	}

	/**
	 * Tests a JDT source feature bundle container contains the appropriate
	 * bundles
	 */
	@Test
	public void testSourceFeatureBundleContainer() throws Exception {
		// extract the feature
		Path location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toString(), "org.eclipse.jdt.source",
				null);
		container.resolve(definition, null);
		TargetBundle[] bundles = container.getBundles();

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt.source");
		expected.add("org.eclipse.jdt.launching.source");
		// There are two versions of junit available, each with source
		expected.add("org.junit.source");
		expected.add("org.junit.source");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx.source");
		}

		assertEquals("Wrong number of bundles", expected.size(), bundles.length);
		for (TargetBundle bundle : bundles) {
			if (bundle.getBundleInfo().getSymbolicName().equals("org.eclipse.jdt.doc.isv")) {
				assertFalse("Should not be a source bundle", bundle.isSourceBundle());
			} else {
				assertTrue(expected.remove(bundle.getBundleInfo().getSymbolicName()));
				assertTrue("Should be a source bundle", bundle.isSourceBundle());
			}
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}

	/**
	 * Tests setting the target platform to the stored JDT feature test data
	 */
	@Test
	public void testSetTargetPlatformToJdtFeature() throws Exception {
		try {
			// extract the feature
			Path location = extractModifiedFeatures();
			// org.eclipse.jdt_3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1

			ITargetDefinition target = getNewTarget();
			ITargetLocation container = getTargetService().newFeatureLocation(location.toString(), "org.eclipse.jdt",
					"3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1");

			target.setTargetLocations(new ITargetLocation[] { container });

			setTargetPlatform(target);

			List<String> expected = new ArrayList<>();
			expected.add("org.eclipse.jdt");
			expected.add("org.eclipse.jdt.launching");
			// 2 versions of JUnit
			expected.add("org.junit");
			expected.add("org.junit");
			expected.add("org.junit4");
			if (Platform.getOS().equals(Platform.OS_MACOSX)) {
				expected.add("org.eclipse.jdt.launching.macosx");
			}

			// current platform
			IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();

			assertEquals("Wrong number of bundles in JDT feature", expected.size(), models.length);
			for (IPluginModelBase model : models) {
				expected.remove(model.getPluginBase().getId());
				assertTrue(model.isEnabled());
			}
			assertTrue("Wrong bundles in target platform. Missing: " + expected, expected.isEmpty());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests setting the target platform to empty.
	 */
	@Test
	public void testSetEmptyTargetPlatform() throws CoreException {
		try {
			setTargetPlatform(null);

			// current platform
			IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();

			assertEquals("Wrong number of bundles in empty target", 0, models.length);

		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * A directory of bundles should not have VM arguments.
	 */
	@Test
	public void testArgumentsPluginsDirectory() throws Exception {
		// test bundle containers for known arguments
		ITargetLocation directoryContainer = getTargetService()
				.newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		assertNull("Plugins directory containers should not have arguments", directoryContainer.getVMArguments());
	}

	/**
	 * A directory that points to an installation should have VM arguments.
	 */
	@Test
	public void testArgumentsInstallDirectory() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetLocation installDirectory = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation());
		String[] installArgs = installDirectory.getVMArguments();
		assertNotNull("Install directory should have arguments", installArgs);
		assertTrue("Install directory should have arguments", installArgs.length > 0);
	}

	/**
	 * A feature container should not have VM arguments.
	 */
	@Test
	public void testArgumentsFeatureContainer() throws Exception {
		ITargetLocation featureContainer = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(),
				"DOES NOT EXIST", "DOES NOT EXIST");
		assertNull("Feature containers should not have arguments", featureContainer.getVMArguments());
	}

	/**
	 * A profile container should have VM arguments.
	 */
	@Test
	public void testArgumentsProfileContainer() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				null);
		String[] arguments = profileContainer.getVMArguments();
		assertNotNull("Profile containers should have arguments", arguments);
		assertTrue("Profile containers should have arguments", arguments.length > 0);
	}

	/**
	 * Tests the ability to add arguments to a target platform and have them
	 * show up on new configs
	 */
	@Test
	public void testArguments() throws Exception {
		ITargetDefinition definition = getNewTarget();

		// Add program arguments
		String programArgs = "-testProgramArgument -testProgramArgument2";
		definition.setProgramArguments(programArgs);
		assertEquals(programArgs, definition.getProgramArguments());

		// Add VM arguments
		String vmArgs = "-testVMArgument -testVMArgument2";
		definition.setVMArguments(vmArgs);
		assertEquals(vmArgs, definition.getVMArguments());

		PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
		try {
			getTargetService().saveTargetDefinition(definition);
			setTargetPlatform(definition);

			// Check that new launch configs will be prepopulated from target
			// along with the default preference values
			assertEquals(vmArgs + " -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true",
					LaunchArgumentsHelper.getInitialVMArguments());
			assertEquals("-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog "
					.concat(programArgs), LaunchArgumentsHelper.getInitialProgramArguments());

			// Check that new launch configs will be prepopulated from target
			// along with ADD_SWT_NON_DISPOSAL_REPORTING == false
			prefs.setValue(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING, false);
			assertEquals(vmArgs, LaunchArgumentsHelper.getInitialVMArguments());

			// Check that new launch configs will be prepopulated from target
			// along with ADD_SWT_NON_DISPOSAL_REPORTING == true
			prefs.setValue(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING, true);
			assertEquals(vmArgs + " -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true",
					LaunchArgumentsHelper.getInitialVMArguments());

			// Check that new launch configs will be prepopulated from target
			// along with ADD_SWT_NON_DISPOSAL_REPORTING == true but the define
			// is already set in the target platform to false
			prefs.setValue(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING, true);
			vmArgs = "-testVMArgument -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=false -testVMArgument2";
			definition.setVMArguments(vmArgs);
			assertEquals(vmArgs, LaunchArgumentsHelper.getInitialVMArguments());
		} finally {
			prefs.setToDefault(ICoreConstants.ADD_SWT_NON_DISPOSAL_REPORTING);
			getTargetService().deleteTarget(definition.getHandle());
			resetTargetPlatform();
		}

	}

	/**
	 * Test for https://github.com/eclipse-pde/eclipse.pde/issues/1246
	 */
	@Test
	public void testDeleteCleansCaches() throws Exception {
		ITargetDefinition definition = getNewTarget();
		// dummy content to ensure the target is cached
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		definition.setTargetLocations(new ITargetLocation[] { container });
		try {
			assertFalse(TargetPlatformHelper.getTargetDefinitionMap().containsKey(definition.getHandle()));
			definition.resolve(null);
			assertTrue(TargetPlatformHelper.getTargetDefinitionMap().containsKey(definition.getHandle()));
		} finally {
			getTargetService().deleteTarget(definition.getHandle());
			assertFalse(TargetPlatformHelper.getTargetDefinitionMap().containsKey(definition.getHandle()));
		}
	}

	/**
	 * Tests that a single (lower) version of a bundle can be included in the
	 * target platform.
	 */
	@Test
	public void testLowerVersionOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[] { MULTI_VERSION_LOW_DESCRIPTION });
	}

	/**
	 * Tests that a single (higher) version of a bundle can be included in the
	 * target platform.
	 */
	@Test
	public void testHigherVersionOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[] { MULTI_VERSION_HIGH_DESCRIPTION });
	}

	/**
	 * Tests all versions of a bundle can be excluded.
	 */
	@Test
	public void testNoVersionsOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[0]);
	}

	/**
	 * Tests all versions of a bundle can be included.
	 */
	@Test
	public void testAllVersionsOfBundle() throws Exception {
		doIncludeVersions(null);
	}

	/**
	 * Tests all versions of a bundle can be included.
	 */
	@Test
	public void testAllVersionsOfBundleExplicit() throws Exception {
		doIncludeVersions(
				new NameVersionDescriptor[] { MULTI_VERSION_LOW_DESCRIPTION, MULTI_VERSION_HIGH_DESCRIPTION });
	}

	private void doIncludeVersions(NameVersionDescriptor[] descriptions) throws Exception {
		String bsn = MULTI_VERSION_LOW_DESCRIPTION.getId();

		Path extras = extractMultiVersionPlugins();
		ITargetDefinition target = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(extras.toString());
		target.setTargetLocations(new ITargetLocation[] { container });
		target.setIncluded(descriptions);
		try {
			getTargetService().saveTargetDefinition(target);
			setTargetPlatform(target);
			IPluginModelBase[] models = PluginRegistry.getExternalModels();
			Set<NameVersionDescriptor> enabled = new HashSet<>();
			for (IPluginModelBase pm : models) {
				if (pm.getBundleDescription().getSymbolicName().equals(bsn)) {
					NameVersionDescriptor desc = new NameVersionDescriptor(pm.getPluginBase().getId(),
							pm.getPluginBase().getVersion());
					if (pm.isEnabled()) {
						enabled.add(desc);
					}
				}
			}
			if (descriptions == null) {

			} else {
				assertEquals("Wrong number of enabled bundles", descriptions.length, enabled.size());
				for (NameVersionDescriptor description : descriptions) {
					assertTrue("Missing bundle", enabled.contains(description));
				}
			}
		} finally {
			getTargetService().deleteTarget(target.getHandle());
			resetTargetPlatform();
		}
	}

	private void assertTargetContentIsEqualToRuntimeBundles(ITargetDefinition definition) throws Exception {
		Path home = Path.of(TargetPlatform.getDefaultLocation());
		Path configuration = home.resolve("configuration");
		Set<URI> pluginPaths = readBundlesTxt(home, configuration);
		// pluginPaths will be empty when self-hosting and the target platform
		// is not a real installation
		assertFalse(pluginPaths.isEmpty());
		assertEquals(pluginPaths, getAllBundleURIs(definition));
	}

	private static Set<URI> readBundlesTxt(Path platformHome, Path configurationArea) {
		BundleInfo[] bundles = P2Utils.readBundles(platformHome.toString(), configurationArea.toFile());
		if (bundles == null) {
			return Set.of();
		}
		BundleInfo[] srcBundles = P2Utils.readSourceBundles(platformHome.toString(), configurationArea.toFile());
		Stream<BundleInfo> allBundles = srcBundles == null || srcBundles.length == 0 ? Arrays.stream(bundles)
				: (Stream.of(bundles, srcBundles).flatMap(Arrays::stream));
		return allBundles.map(info -> new File(info.getLocation()).toURI())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
