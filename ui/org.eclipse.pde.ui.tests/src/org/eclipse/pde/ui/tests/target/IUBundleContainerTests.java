/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests for the IU bundle container
 */
public class IUBundleContainerTests extends AbstractTargetTest {

	/**
	 * Returns the metadata repository at the specified location.
	 *
	 * @param uri location
	 * @return metadata repository at the specified location
	 * @throws Exception
	 */
	protected IMetadataRepository getRepository(URI uri) throws Exception {
		IMetadataRepositoryManager manager = P2TargetUtils.getRepoManager();
		assertNotNull("Missing metadata repository manager", manager);
		IMetadataRepository repo = manager.loadRepository(uri, null);
		return repo;
	}

	/**
	 * Returns a URI for the the specified test plug-in relative path.
	 *
	 * @param relativePath test plug-in relative path
	 * @return URI
	 * @throws Exception
	 */
	protected URI getURI(String relativePath) throws Exception {
		URL url = PDETestsPlugin.getBundleContext().getBundle().getEntry(relativePath);
		Path path = new Path(new File(FileLocator.toFileURL(url).getFile()).getAbsolutePath());
		return URIUtil.toURI(path);
	}

	/**
	 * Returns an installable unit from the given repository with the specified identifier.
	 *
	 * @param id unit identifier
	 * @param repository repository
	 * @return installable unit
	 */
	protected IInstallableUnit getUnit(String id, IMetadataRepository repository) {
		IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.createIUQuery(id), null);
		IInstallableUnit[] units = result.toArray(IInstallableUnit.class);
		if (units.length == 1) {
			return units[0];
		}
		assertTrue("Did not find IU: " + id, false);
		return null;
	}

	public void testResolveUsingProfile() throws Exception {
		String[] features1 = new String[]{"feature.b.feature.group"};
		String[] features2 = new String[]{"feature.a.feature.group"};
		String[] expectedBundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		String[] expectedBundles2 = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};

		try {
			// create a target that references just a high level root
			IUBundleContainer container = createContainer(features1);
			ITargetDefinition target = getTargetService().newTarget();
			target.setTargetLocations(new ITargetLocation[]{container});
			List<BundleInfo> infos = getAllBundleInfos(target);
			Set<String> names = collectAllSymbolicNames(infos);
			assertEquals(expectedBundles.length, infos.size());
			for (String expectedBundle : expectedBundles) {
				assertTrue("Missing: " + expectedBundle, names.contains(expectedBundle));
			}

			// Now modify the target to have just a lower level root.  The extra higher level stuff should get removed.
			container = createContainer(features2);
			target.setTargetLocations(new ITargetLocation[]{container});
			infos = getAllBundleInfos(target);
			names = collectAllSymbolicNames(infos);
			assertEquals(expectedBundles2.length, infos.size());
			for (String element : expectedBundles2) {
				assertTrue("Missing: " + element, names.contains(element));
			}

			List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
			assertEquals(1, profiles.size());
			String id = profiles.get(0);
			assertTrue("Unexpected profile GC'd", id.endsWith(target.getHandle().getMemento()));

		} finally {
			// Always clean any profiles, even if the test failed to prevent cascading failures
			P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		}
	}

	/**
	 * Tests all bundles are resolved for a feature and its required feature
	 *
	 * @throws Exception
	 */
	public void testResolveRequiredFeatures() throws Exception {
		String[] bundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		doResolutionTest(new String[]{"feature.b.feature.group"}, bundles);
	}

	/**
	 * Tests all bundles are resolved for a single feature
	 *
	 * @throws Exception
	 */
	public void testResolveSingleFeature() throws Exception {
		String[] bundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		doResolutionTest(new String[]{"feature.a.feature.group"}, bundles);
	}

	/**
	 * Tests all bundles are resolved for a bundle and its required bundles
	 *
	 * @throws Exception
	 */
	public void testResolveRequiredBundles() throws Exception {
		String[] bundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1"};
		doResolutionTest(new String[]{"bundle.b1"}, bundles);
	}

	/**
	 * Tests a bundle is resolved (no required bundles)
	 *
	 * @throws Exception
	 */
	public void testResolveSingleBundle() throws Exception {
		String[] bundles = new String[]{"bundle.a1"};
		doResolutionTest(new String[]{"bundle.a1"}, bundles);
	}

	/**
	 * Tests that contents should be equal.
	 *
	 * @throws Exception
	 */
	public void testContentEqualNonNull() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"bundle.a1", "bundle.a2"});
		IUBundleContainer c2 = createContainer(new String[]{"bundle.a1", "bundle.a2"});
		assertTrue("Contents should be equivalent", c1.equals(c2));
	}

	/**
	 * Tests that contents should be equal.
	 *
	 * @throws Exception
	 */
	public void testContentNotEqualNonNull() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"bundle.a1", "bundle.a2"});
		IUBundleContainer c2 = createContainer(new String[]{"bundle.b1", "bundle.b2"});
		assertFalse("Contents should not be equivalent", c1.equals(c2));
	}

	/**
	 * Tests that contents should be equal.
	 *
	 * @throws Exception
	 */
	public void testContentEqualNull() throws Exception {
		ITargetPlatformService service = getTargetService();
		IUBundleContainer c3 = (IUBundleContainer) service.newIULocation(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null, 0);
		IUBundleContainer c4 = (IUBundleContainer) service.newIULocation(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null, 0);
		assertTrue("Contents should be equivalent", c3.equals(c4));
	}

	/**
	 * Tests that contents should not be equal.
	 *
	 * @throws Exception
	 */
	public void testContentNotEqualNull() throws Exception {
		ITargetPlatformService service = getTargetService();
		IUBundleContainer c3 = (IUBundleContainer) service.newIULocation(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null, 1);
		IUBundleContainer c4 = (IUBundleContainer) service.newIULocation(new String[]{"bundle.b1", "bundle.b2"}, new String[]{"1.0.0", "1.0.0"}, null, 0);
		assertFalse("Contents should not be equivalent", c3.equals(c4));
	}

	/**
	 * Creates an IU bundle container with the specified IU's, resolves the
	 * contents and ensures that the specified bundles are present.
	 *
	 * @param unitIds identifiers of IU's to add to the container
	 * @param bundleIds symbolic names of bundles that should be present after resolution
	 * @throws Exception
	 */
	protected void doResolutionTest(String[] unitIds, String[] bundleIds) throws Exception {
		try {
			IUBundleContainer container = createContainer(unitIds);
			ITargetDefinition target = getTargetService().newTarget();
			target.setTargetLocations(new ITargetLocation[]{container});
			List<BundleInfo> infos = getAllBundleInfos(target);
			Set<String> names = collectAllSymbolicNames(infos);
			assertEquals(bundleIds.length, infos.size());

			for (String bundleId : bundleIds) {
				assertTrue("Missing: " + bundleId, names.contains(bundleId));
			}
			List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
			assertEquals(1, profiles.size());
			String id = profiles.get(0);
			assertTrue("Unexpected profile GC'd", id.endsWith(target.getHandle().getMemento()));
		} finally {
			// Always clean any profiles, even if the test failed to prevent cascading failures
			P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		}
	}

	/**
	 * Creates an IU bundle container with the specified IU's, persists and
	 * restores them.
	 *
	 * @param unitIds identifiers of IU's to add to the container
	 * @param bundleIds ids of resolved bundles expected
	 * @throws Exception
	 */
	protected void doPersistanceTest(String[] unitIds, String[] bundleIds) throws Exception {
		IUBundleContainer container = createContainer(unitIds);
		ITargetDefinition target = getTargetService().newTarget();
		target.setTargetLocations(new ITargetLocation[]{container});

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(target, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		assertTrue("Target content not equal",((TargetDefinition)target).isContentEqual(definitionB));

		// resolve the restored target and ensure bundles are correct
		List<BundleInfo> infos = getAllBundleInfos(definitionB);
		Set<String> names = collectAllSymbolicNames(infos);
		assertEquals(bundleIds.length, infos.size());

		for (String bundleId : bundleIds) {
			assertTrue("Missing: " + bundleId, names.contains(bundleId));
		}
		List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(1, profiles.size());
		String id = profiles.get(0);
		assertTrue("Unexpected profile GC'd", id.endsWith(definitionB.getHandle().getMemento()));
	}

	/**
	 * Tests that the external model manager can restore external bundles from the bundle pool
	 * properly. See bug 320583.
	 *
	 * @throws Exception
	 */
	public void testExternalModelManagerPreferences() throws Exception {
		try {
			// Set the active target to feature b (has 6 bundles)
			String[]unitIds = new String[]{"feature.b.feature.group"};
			IUBundleContainer container = createContainer(unitIds);
			ITargetDefinition targetB = getTargetService().newTarget();
			targetB.setTargetLocations(new ITargetLocation[]{container});
			getTargetService().saveTargetDefinition(targetB);
			setTargetPlatform(targetB);

			// Set the active target to feature a (has 3 bundles)
			unitIds = new String[]{"feature.a.feature.group"};
			container = createContainer(unitIds);
			ITargetDefinition targetA = getTargetService().newTarget();
			targetA.setTargetLocations(new ITargetLocation[]{container});
			getTargetService().saveTargetDefinition(targetA);
			setTargetPlatform(targetA);

			// ensure the external model manager only knows about bundles in target A
			IPluginModelBase[] externalBundles = PDECore.getDefault().getModelManager().getExternalModelManager().getAllModels();
			assertEquals("Wrong number of external bundles", 3, externalBundles.length);
			// expected bundles
			Set<String> expected = new HashSet<>();
			expected.add("bundle.a1");
			expected.add("bundle.a2");
			expected.add("bundle.a3");
			for (IPluginModelBase externalBundle : externalBundles) {
				assertTrue("Unexpected bundle in restored list: " + externalBundle.getInstallLocation(), expected.remove(externalBundle.getBundleDescription().getName()));
			}
			assertTrue(expected.isEmpty());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Creates an IU bundle container with the specified IUs from the test repository.
	 *
	 * @param unitIds identifiers of IU's to add to the container
	 * @return bundle container
	 * @throws Exception
	 */
	protected IUBundleContainer createContainer(String[] unitIds) throws Exception {
		URI uri = getURI("/tests/sites/site.a.b");
		IInstallableUnit[] units = getUnits(unitIds, uri);
		return createContainer(units, new URI[]{uri}, IUBundleContainer.INCLUDE_REQUIRED);
	}

	private IInstallableUnit[] getUnits(String[] unitIds, URI uri) throws Exception {
		IMetadataRepository repository = getRepository(uri);
		IInstallableUnit[] units = new IInstallableUnit[unitIds.length];
		for (int i = 0; i < unitIds.length; i++) {
			units[i] = getUnit(unitIds[i], repository);
		}
		return units;
	}

	/**
	 * Creates and returns an new IU bundle container with the specified IU's and repositories.
	 *
	 * @param units IU's
	 * @param repositories locations of repositories
	 * @param flags location flags
	 * @return IU bundle container
	 * @throws Exception
	 */
	protected IUBundleContainer createContainer(IInstallableUnit[] units, URI[] repositories, int flags) throws Exception {
		return (IUBundleContainer) getTargetService().newIULocation(units, repositories, flags);
	}

	/**
	 * Tests that a target definition with IU containers can be serialized to xml, then deserialized without
	 * any loss of data.
	 *
	 * @throws Exception
	 */
	public void testPersistIUDefinition() throws Exception {
		String[] bundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		doPersistanceTest(new String[]{"feature.a.feature.group"}, bundles);
	}

	/**
	 * Tests that a target definition with IU containers can be serialized to xml, then deserialized without
	 * any loss of data.
	 *
	 * @throws Exception
	 */
	public void testPersistMultipleIUDefinition() throws Exception {
		String[] bundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		doPersistanceTest(new String[]{"bundle.a3", "bundle.b3"}, bundles);
	}

	/**
	 * Incrementally adding IUs to a target.
	 *
	 * @throws Exception
	 */
	public void testAddIUs() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"feature.a.feature.group"});
		ITargetDefinition target = getTargetService().newTarget();
		target.setTargetLocations(new IUBundleContainer[]{c1});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());

		getTargetService().saveTargetDefinition(target);
		ITargetHandle handle = target.getHandle();
		// get new unresolved copy of the target
		target = handle.getTargetDefinition();
		IUBundleContainer c2 = createContainer(new String[]{"feature.b.feature.group"});
		target.setTargetLocations(new IUBundleContainer[]{c2});

		List<BundleInfo> infos = getAllBundleInfos(target);
		Set<String> names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		assertEquals(bundleIds.length, infos.size());

		for (String bundleId : bundleIds) {
			assertTrue("Missing: " + bundleId, names.contains(bundleId));
		}

		getTargetService().deleteTarget(target.getHandle());

		List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(0, profiles.size());
	}

	/**
	 * Incrementally removing IUs from a target.
	 *
	 * @throws Exception
	 */
	public void testRemoveIUs() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"feature.b.feature.group"});
		ITargetDefinition target = getTargetService().newTarget();
		target.setTargetLocations(new IUBundleContainer[]{c1});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());

		getTargetService().saveTargetDefinition(target);
		ITargetHandle handle = target.getHandle();
		// get new unresolved copy of the target
		target = handle.getTargetDefinition();
		IUBundleContainer c2 = createContainer(new String[]{"feature.a.feature.group"});
		target.setTargetLocations(new IUBundleContainer[]{c2});

		List<BundleInfo> infos = getAllBundleInfos(target);
		Set<String> names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		assertEquals(bundleIds.length, infos.size());

		for (String bundleId : bundleIds) {
			assertTrue("Missing: " + bundleId, names.contains(bundleId));
		}

		getTargetService().deleteTarget(target.getHandle());

		List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(0, profiles.size());
	}

	/**
	 * Tests overlapping IU containers.
	 *
	 * @throws Exception
	 */
	public void testOverlappingIUContainers() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"feature.a.feature.group"});
		IUBundleContainer c2 = createContainer(new String[]{"feature.b.feature.group"});
		ITargetDefinition target = getTargetService().newTarget();
		target.setTargetLocations(new IUBundleContainer[]{c1, c2});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());

		List<BundleInfo> infos = getBundleInfos(c1);
		Set<String> names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		assertEquals(bundleIds.length, infos.size());

		for (String bundleId : bundleIds) {
			assertTrue("Missing: " + bundleId, names.contains(bundleId));
		}

		infos = getBundleInfos(c2);
		names = collectAllSymbolicNames(infos);
		bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		assertEquals(bundleIds.length, infos.size());

		for (String bundleId : bundleIds) {
			assertTrue("Missing: " + bundleId, names.contains(bundleId));
		}

		List<String> profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(1, profiles.size());
	}

	public void testSerialization1() throws Exception {
		URI uri = getURI("/tests/sites/site.a.b");
		String[] unitIds = new String[]{"feature.a.feature.group"};
		IInstallableUnit[] units = getUnits(unitIds, uri);
		IUBundleContainer location = createContainer(units, new URI[] {uri}, IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS);
		String xml = location.serialize();
		assertIncludeAllPlatform(xml, true);
		assertIncludeMode(xml, "slicer");
		assertIncludeSource(xml, false);
		deserializationTest(location);
	}


	public void testSerialization2() throws Exception {
		IUBundleContainer location = createContainer(new String[]{"bundle.a1", "bundle.a2"});
		String xml = location.serialize();
		assertIncludeAllPlatform(xml, false);
		assertIncludeMode(xml, "planner");
		assertIncludeSource(xml, false);
		deserializationTest(location);
	}

	public void testSerialization3() throws Exception {
		URI uri = getURI("/tests/sites/site.a.b");
		String[] unitIds = new String[]{"feature.b.feature.group"};
		IInstallableUnit[] units = getUnits(unitIds, uri);
		IUBundleContainer location = createContainer(units, new URI[] {uri}, IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE);
		String xml = location.serialize();
		assertIncludeAllPlatform(xml, true);
		assertIncludeMode(xml, "slicer");
		assertIncludeSource(xml, true);
		deserializationTest(location);
	}

	public void deserializationTest(IUBundleContainer location) throws Exception {
		ITargetDefinition td = getTargetService().newTarget();
		td.setTargetLocations(new ITargetLocation[] {location});
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(td, out);
		String xml = new String(out.toByteArray());
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		parser.setErrorHandler(new DefaultHandler());
		Document doc = parser.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));

		ITargetDefinition definition = getTargetService().newTarget();
		Element root = doc.getDocumentElement();
		TargetPersistence38Helper.initFromDoc(definition, root);
		ITargetLocation[] locations = definition.getTargetLocations();
		assertEquals(1, locations.length);
		assertTrue(locations[0] instanceof IUBundleContainer);
		assertTrue(((IUBundleContainer)locations[0]).equals(location));
	}

	private void assertIncludeAllPlatform(String xml, boolean expectedValue) {
		assertToken(xml, "includeAllPlatforms=\"", String.valueOf(expectedValue));
	}

	private void assertIncludeSource(String xml, boolean expectedValue) {
		assertToken(xml, "includeSource=\"", String.valueOf(expectedValue));
	}

	private void assertIncludeMode(String xml, String expectedValue) {
		assertToken(xml, "includeMode=\"", expectedValue);
	}

	private void assertToken(String xml, String token, String expectedValue) {
		int start = xml.indexOf(token) + token.length();
		String actualValue = xml.substring(start, start + String.valueOf(expectedValue).length()) ;
		assertEquals(String.valueOf(expectedValue), actualValue);
	}
}
