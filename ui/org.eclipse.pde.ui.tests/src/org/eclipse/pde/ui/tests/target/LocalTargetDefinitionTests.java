/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.osgi.framework.ServiceReference;

/**
 * Tests for target definitions.  The tested targets will be created in the metadata.
 * @see WorkspaceTargetDefinitionTests
 *
 * @since 3.5
 */
public class LocalTargetDefinitionTests extends AbstractTargetTest {

	public static final NameVersionDescriptor MULTI_VERSION_LOW_DESCRIPTION = new NameVersionDescriptor("a.typical.bundle", "1.0.0.200907071058");
	public static final NameVersionDescriptor MULTI_VERSION_HIGH_DESCRIPTION = new NameVersionDescriptor("a.typical.bundle", "1.1.0.200907071100");

	/**
	 * Returns the target platform service or <code>null</code> if none
	 *
	 * @return target platform service
	 */
	@Override
	protected ITargetPlatformService getTargetService() {
		ServiceReference<ITargetPlatformService> reference = PDETestsPlugin.getBundleContext()
				.getServiceReference(ITargetPlatformService.class);
		assertNotNull("Missing target platform service", reference);
		if (reference == null)
			return null;
		return PDETestsPlugin.getBundleContext().getService(reference);
	}

	/**
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a set of URLs.
	 *
	 * @param target target definition
	 * @return all bundle URLs
	 */
	protected Set<URL> getAllBundleURLs(ITargetDefinition target) throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		TargetBundle[] bundles = target.getBundles();
		Set<URL> urls = new HashSet<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			urls.add(new File(bundle.getBundleInfo().getLocation()).toURL());
		}
		return urls;
	}

	/**
	 * Tests that resetting the target platform should work OK (i.e. is equivalent to the
	 * models in the default target platform).
	 *
	 * @throws CoreException
	 */
	public void testResetTargetPlatform() throws Exception {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		Set<URL> urls = getAllBundleURLs(definition);
		Set<URL> fragments = new HashSet<>();
		TargetBundle[] bundles = definition.getBundles();
		for (TargetBundle bundle : bundles) {
			if (bundle.isFragment()) {
				fragments.add(new File(bundle.getBundleInfo().getLocation()).toURL());
			}
		}

		// current platform
		IPluginModelBase[] models = TargetPlatformHelper.getPDEState().getTargetModels();

		// should be equivalent
		assertEquals("Should have same number of bundles", urls.size(), models.length);
		for (IPluginModelBase model : models) {
			String location = model.getInstallLocation();
			URL url = new File(location).toURL();
			assertTrue("Missing plug-in " + location, urls.contains(url));
			if (model.isFragmentModel()) {
				assertTrue("Missing fragmnet", fragments.remove(url));
			}
		}
		assertTrue("Different number of fragments", fragments.isEmpty());
	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving).
	 *
	 * @throws Exception
	 */
	public void testDefaultTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		// pluginPaths will be null (and NPE) when self-hosting and the target platform is not a real installation
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}

	}

	/**
	 * Tests that a target definition based on the default target platform
	 * restricted to a subset of bundles contains the right set.
	 *
	 * @throws Exception
	 */
	public void testRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null)
		};
		definition.setTargetLocations(new ITargetLocation[]{container});
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
	 *
	 * @throws Exception
	 */
	public void testVersionRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		definition.setTargetLocations(new ITargetLocation[]{container});
		List<BundleInfo> infos = getAllBundleInfos(definition);
		// find right versions
		String v1 = null;
		String v2 = null;
		Iterator<BundleInfo> iterator = infos.iterator();
		while (iterator.hasNext() && (v2 == null || v1 == null)) {
			BundleInfo info = iterator.next();
			if (info.getSymbolicName().equals("org.eclipse.jdt.launching")) {
				v1 = info.getVersion();
			} else if (info.getSymbolicName().equals("org.eclipse.jdt.debug")) {
				v2 = info.getVersion();
			}
		}
		assertNotNull(v1);
		assertFalse(v1.equals(BundleInfo.EMPTY_VERSION));
		assertNotNull(v2);
		assertFalse(v2.equals(BundleInfo.EMPTY_VERSION));

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt.launching", v1),
				new NameVersionDescriptor("org.eclipse.jdt.debug", v2)
		};
		definition.setIncluded(restrictions);
		infos = getAllBundleInfos(definition);

		assertEquals("Wrong number of bundles", 2, infos.size());
		iterator = infos.iterator();
		while (iterator.hasNext()) {
			BundleInfo info = iterator.next();
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
	 *
	 * @throws Exception
	 */
	public void testMissingVersionRestrictedDefaultTargetPlatform() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt.launching", "xyz"),
				new NameVersionDescriptor("org.eclipse.jdt.debug", "abc")
		};
		definition.setTargetLocations(new ITargetLocation[]{container});
		definition.setIncluded(restrictions);
		definition.resolve(null);
		TargetBundle[] bundles = definition.getBundles();

		assertEquals("Wrong number of bundles", 2, bundles.length);
		for (TargetBundle rb : bundles) {
			assertEquals("Should be a missing bundle version", TargetBundle.STATUS_VERSION_DOES_NOT_EXIST, rb.getStatus().getCode());
			assertEquals("Should be an error", IStatus.ERROR, rb.getStatus().getSeverity());
		}
	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}
	 *
	 * @throws Exception
	 */
	public void testEclipseHomeTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation("${eclipse_home}", null);
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		// pluginPaths will be null (and NPE) when self-hosting and the target platform is not a real installation
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}

	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform (this is an
	 * explicit location with no target weaving), when created with a variable
	 * referencing ${eclipse_home}.
	 *
	 * @throws Exception
	 */
	public void testEclipseHomeTargetPlatformAndConfigurationArea() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation("${eclipse_home}", "${eclipse_home}/configuration");
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		IPath location = new Path(TargetPlatform.getDefaultLocation());
		URL[] pluginPaths = P2Utils.readBundlesTxt(location.toOSString(), location.append("configuration").toFile().toURL());
		// pluginPaths will be null (and NPE) when self-hosting and the target platform is not a real installation
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}

	}

	/**
	 * Tests that a target definition equivalent to the default target platform
	 * contains the same bundles as the default target platform using the
	 * platform's configuration location (which will do target weaving). This
	 * is really only tested when run as a JUnit plug-in test suite from
	 * within Eclipse.
	 *
	 * @throws Exception
	 */
	public void testWovenTargetPlatform() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation(), true);
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());

	}

	/**
	 * Tests that a bundle directory container is equivalent to scanning locations.
	 *
	 * @throws Exception
	 */
	public void testDirectoryBundleContainer() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation(), false);
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
	}

	/**
	 * Tests that a bundle directory container is equivalent to scanning locations
	 * when it uses a variable to specify its location.
	 *
	 * @throws Exception
	 */
	public void testVariableDirectoryBundleContainer() throws Exception {
		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation("${eclipse_home}/plugins");
		definition.setTargetLocations(new ITargetLocation[]{container});
		Set<URL> urls = getAllBundleURLs(definition);

		// the old way
		URL[] pluginPaths = PluginPathFinder.getPluginPaths(TargetPlatform.getDefaultLocation(), false);
		assertEquals("Should have same number of bundles", pluginPaths.length, urls.size());
		for (URL url : pluginPaths) {
			assertTrue("Missing plug-in " + url.toString(), urls.contains(url));
		}
	}

	/**
	 * Returns the given input stream as a byte array
	 * @param stream the stream to get as a byte array
	 * @param length the length to read from the stream or -1 for unknown
	 * @return the given input stream as a byte array
	 * @throws IOException
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				// read at least 8K
				int amountRequested = Math.max(stream.available(), 8192);
				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents,
							0,
							contents = new byte[contentsLength + amountRequested],
							0,
							contentsLength);
				}
				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);
				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);
			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case length is the actual
				// read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}
		return contents;
	}

	/**
	 * Returns the location of the JDT feature in the running host as
	 * a path in the local file system.
	 *
	 * @return path to JDT feature
	 */
	protected IPath getJdtFeatureLocation() {
		IPath path = new Path(TargetPlatform.getDefaultLocation());
		path = path.append("features");
		File dir = path.toFile();
		assertTrue("Missing features directory", dir.exists() && !dir.isFile());
		String[] files = dir.list();
		String location = null;
		for (String file : files) {
			if (file.startsWith("org.eclipse.jdt_")) {
				location = path.append(file).toOSString();
				break;
			}
		}
		assertNotNull("Missing JDT feature", location);
		return new Path(location);
	}

	/**
	 * Tests a JDT feature bundle container contains the appropriate bundles
	 * @throws Exception
	 */
	public void testFeatureBundleContainer() throws Exception {
		// extract the feature
		IPath location = extractModifiedFeatures();

		// the new way
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toOSString(), "org.eclipse.jdt", null);
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
		Iterator<String> iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());

		// should be no source bundles
		for (TargetBundle bundle : bundles) {
			assertFalse("Should be no source bundles", bundle.isSourceBundle());
		}
	}

	/**
	 * Tests a JDT feature bundle container contains the appropriate bundles for a specific OS.
	 *
	 * @throws Exception
	 */
	public void testMacOSFeatureBundleContainer() throws Exception {
		// extract the feature
		IPath location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		definition.setOS(Platform.OS_MACOSX);
		ITargetLocation container = getTargetService().newFeatureLocation(location.toOSString(), "org.eclipse.jdt", null);
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
					assertEquals("Mac bundle should be mssing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, status.getCode());
				}
			}
		}
		Iterator<String> iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());


		// should be no source bundles
		for (TargetBundle bundle : bundles) {
			assertFalse("Should be no source bundles", bundle.isSourceBundle());
		}
	}
	/**
	 * Tests that a target definition based on the JDT feature
	 * restricted to a subset of bundles contains the right set.
	 *
	 * @throws Exception
	 */
	public void testRestrictedFeatureBundleContainer() throws Exception {
		// extract the feature
		IPath location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toOSString(), "org.eclipse.jdt", null);
		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt", null),
				new NameVersionDescriptor("org.junit", "3.8.2.v20090203-1005")
		};
		definition.setTargetLocations(new ITargetLocation[]{container});
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
	 * Tests a JDT source feature bundle container contains the appropriate bundles
	 * @throws Exception
	 */
	public void testSourceFeatureBundleContainer() throws Exception {
		// extract the feature
		IPath location = extractModifiedFeatures();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newFeatureLocation(location.toOSString(), "org.eclipse.jdt.source", null);
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
	 *
	 * @throws Exception
	 */
	public void testSetTargetPlatformToJdtFeature() throws Exception {
		try {
			// extract the feature
			IPath location = extractModifiedFeatures();
			//org.eclipse.jdt_3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1

			ITargetDefinition target = getNewTarget();
			ITargetLocation container = getTargetService().newFeatureLocation(location.toOSString(), "org.eclipse.jdt", "3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1");

			target.setTargetLocations(new ITargetLocation[]{container});

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
			Iterator<String> iterator = expected.iterator();
			while (iterator.hasNext()) {
				String name = iterator.next();
				System.err.println("Missing: " + name);
			}
			assertTrue("Wrong bundles in target platform", expected.isEmpty());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests setting the target platform to empty.
	 * @throws CoreException
	 */
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

	protected void assertTargetDefinitionsEqual(ITargetDefinition targetA, ITargetDefinition targetB) {
		assertTrue("Target content not equal",((TargetDefinition)targetA).isContentEqual(targetB));
	}


	/**
	 * Reads a target definition file from the tests/targets/target-files location
	 * with the given name. Note that ".target" will be appended.
	 *
	 * @param name
	 * @return target definition
	 * @throws Exception
	 */
	protected ITargetDefinition readOldTarget(String name) throws Exception {
		URL url = PDETestsPlugin.getBundleContext().getBundle().getEntry("/tests/targets/target-files/" + name + ".target");
		File file = new File(FileLocator.toFileURL(url).getFile());
		ITargetDefinition target = getNewTarget();
		try (FileInputStream stream = new FileInputStream(file)) {
			TargetDefinitionPersistenceHelper.initFromXML(target, stream);
		}
		return target;
	}

	/**
	 * A directory of bundles should not have VM arguments.
	 *
	 * @throws Exception
	 */
	public void testArgumentsPluginsDirectory() throws Exception {
		// test bundle containers for known arguments
		ITargetLocation directoryContainer = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		assertNull("Plugins directory containers should not have arguments", directoryContainer.getVMArguments());
	}

	/**
	 * A directory that points to an installation should have VM arguments.
	 *
	 * @throws Exception
	 */
	public void testArgumentsInstallDirectory() throws Exception {
		ITargetLocation installDirectory = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation());
		String[] installArgs = installDirectory.getVMArguments();
		assertNotNull("Install directory should have arguments", installArgs);
		assertTrue("Install directory should have arguments", installArgs.length > 0);
	}

	/**
	 * A feature container should not have VM arguments.
	 *
	 * @throws Exception
	 */
	public void testArgumentsFeatureContainer() throws Exception {
		ITargetLocation featureContainer = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(), "DOES NOT EXIST", "DOES NOT EXIST");
		assertNull("Feature containers should not have arguments", featureContainer.getVMArguments());
	}

	/**
	 * A profile container should have VM arguments.
	 *
	 * @throws Exception
	 */
	public void testArgumentsProfileContainer() throws Exception {
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		String[] arguments = profileContainer.getVMArguments();
		assertNotNull("Profile containers should have arguments", arguments);
		assertTrue("Profile containers should have arguments", arguments.length > 0);
	}

	/**
	 * Tests the ability to add arguments to a target platform and have them show up on new configs
	 *
	 * @throws Exception
	 */
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

		try {
			getTargetService().saveTargetDefinition(definition);
			setTargetPlatform(definition);

			// Check that new launch configs will be prepopulated from target
			assertEquals(vmArgs, LaunchArgumentsHelper.getInitialVMArguments());
			assertEquals("-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog ".concat(programArgs), LaunchArgumentsHelper.getInitialProgramArguments());

		} finally {
			getTargetService().deleteTarget(definition.getHandle());
			resetTargetPlatform();
		}

	}

	/**
	 * Tests that a single (lower) version of a bundle can be included in the target platform.
	 *
	 * @throws Exception
	 */
	public void testLowerVersionOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[]{MULTI_VERSION_LOW_DESCRIPTION});
	}

	/**
	 * Tests that a single (higher) version of a bundle can be included in the target platform.
	 *
	 * @throws Exception
	 */
	public void testHigherVersionOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[]{MULTI_VERSION_HIGH_DESCRIPTION});
	}

	/**
	 * Tests all versions of a bundle can be excluded.
	 *
	 * @throws Exception
	 */
	public void testNoVersionsOfBundle() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[0]);
	}

	/**
	 * Tests all versions of a bundle can be included.
	 *
	 * @throws Exception
	 */
	public void testAllVersionsOfBundle() throws Exception {
		doIncludeVersions(null);
	}

	/**
	 * Tests all versions of a bundle can be included.
	 *
	 * @throws Exception
	 */
	public void testAllVersionsOfBundleExplicit() throws Exception {
		doIncludeVersions(new NameVersionDescriptor[]{MULTI_VERSION_LOW_DESCRIPTION, MULTI_VERSION_HIGH_DESCRIPTION});
	}

	protected void doIncludeVersions(NameVersionDescriptor[] descriptions) throws Exception {
		String bsn = MULTI_VERSION_LOW_DESCRIPTION.getId();

		IPath extras = extractMultiVersionPlugins();
		ITargetDefinition target = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(extras.toOSString());
		target.setTargetLocations(new ITargetLocation[]{container});
		target.setIncluded(descriptions);
		try {
			getTargetService().saveTargetDefinition(target);
			setTargetPlatform(target);
			IPluginModelBase[] models = PluginRegistry.getExternalModels();
			Set<NameVersionDescriptor> enabled = new HashSet<>();
			for (IPluginModelBase pm : models) {
				if (pm.getBundleDescription().getSymbolicName().equals(bsn)) {
					NameVersionDescriptor desc = new NameVersionDescriptor(pm.getPluginBase().getId(), pm.getPluginBase().getVersion());
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
}
