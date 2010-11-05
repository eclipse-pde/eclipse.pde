/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import org.eclipse.pde.internal.core.target.P2TargetUtils;

import java.util.Iterator;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;

/**
 * Tests for the IU bundle container
 */
public class IUBundleContainerTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(IUBundleContainerTests.class);
	}	
	
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
		URL url = MacroPlugin.getBundleContext().getBundle().getEntry(relativePath);
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
		IQueryResult result = repository.query(QueryUtil.createIUQuery(id),  null);
		IInstallableUnit[] units  = (IInstallableUnit[]) result.toArray(IInstallableUnit.class);
		if (units.length == 1) {
			return units[0];
		}		
		assertTrue("Did not find IU: " + id, false);
		return null;
	}
	
	public void testResolveUsingProfile() throws Exception {
		String[] bundleIDs = new String[]{"feature.b.feature.group"};
		String[] expectedBundles = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		String[] expectedBundles2 = new String[]{"bundle.b1", "bundle.b2", "bundle.b3"};

		try { 

			IUBundleContainer container = createContainer(bundleIDs);
			ITargetDefinition target = getTargetService().newTarget();
			target.setBundleContainers(new IBundleContainer[]{container});

			List infos = getAllBundleInfos(target);
			Set names = collectAllSymbolicNames(infos);
			assertEquals(expectedBundles.length, infos.size());
			for (int i = 0; i < expectedBundles.length; i++) {
				assertTrue("Missing: " + expectedBundles[i], names.contains(expectedBundles[i]));
			}

			IProfile profile = P2TargetUtils.getProfile(target);

			IProvisioningAgent agent = P2TargetUtils.getAgent();
			IEngine engine = P2TargetUtils.getEngine();
			IProvisioningPlan plan = engine.createPlan(profile, new ProvisioningContext(agent));
			IQueryResult units = profile.query(QueryUtil.ALL_UNITS, null);
			for (Iterator iterator = units.iterator(); iterator.hasNext();) {
				IInstallableUnit unit = (IInstallableUnit) iterator.next();
				if (unit.getId().startsWith("bundle.a")){
					plan.removeInstallableUnit(unit);
				}
			}
			IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE});
			IStatus result = engine.perform(plan, phases, null);

			assertTrue("Problem while provisioning: " + result.getMessage(), result.isOK());
			
			target.resolve(null);  // Force the target to reresolve (hopefully using the modified profile)
			infos = getAllBundleInfos(target);
			names = collectAllSymbolicNames(infos);
			assertEquals(expectedBundles2.length, infos.size());
			for (int i = 0; i < expectedBundles2.length; i++) {
				assertTrue("Missing: " + expectedBundles2[i], names.contains(expectedBundles2[i]));
			}

			List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
			assertEquals(1, profiles.size());
			String id = (String) profiles.get(0);
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
		assertTrue("Contents should be equivalent", c1.isContentEqual(c2));
	}
	
	/**
	 * Tests that contents should be equal.
	 * 
	 * @throws Exception
	 */
	public void testContentNotEqualNonNull() throws Exception {
		IUBundleContainer c1 = createContainer(new String[]{"bundle.a1", "bundle.a2"});
		IUBundleContainer c2 = createContainer(new String[]{"bundle.b1", "bundle.b2"});
		assertFalse("Contents should not be equivalent", c1.isContentEqual(c2));
	}	
	
	/**
	 * Tests that contents should be equal.
	 * 
	 * @throws Exception
	 */
	public void testContentEqualNull() throws Exception {		
		ITargetPlatformService service = getTargetService();
		IUBundleContainer c3 = (IUBundleContainer) service.newIUContainer(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null);
		IUBundleContainer c4 = (IUBundleContainer) service.newIUContainer(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null);
		assertTrue("Contents should be equivalent", c3.isContentEqual(c4));
	}
	
	/**
	 * Tests that contents should not be equal.
	 * 
	 * @throws Exception
	 */
	public void testContentNotEqualNull() throws Exception {		
		ITargetPlatformService service = getTargetService();
		IUBundleContainer c3 = (IUBundleContainer) service.newIUContainer(new String[]{"bundle.a1", "bundle.a2"}, new String[]{"1.0.0", "1.0.0"}, null);
		IUBundleContainer c4 = (IUBundleContainer) service.newIUContainer(new String[]{"bundle.b1", "bundle.b2"}, new String[]{"1.0.0", "1.0.0"}, null);
		assertFalse("Contents should not be equivalent", c3.isContentEqual(c4));
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
			target.setBundleContainers(new IBundleContainer[]{container});
			List infos = getAllBundleInfos(target);
			Set names = collectAllSymbolicNames(infos);
			assertEquals(bundleIds.length, infos.size());

			for (int i = 0; i < bundleIds.length; i++) {
				assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
			}
			List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
			assertEquals(1, profiles.size());
			String id = (String) profiles.get(0);
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
		target.setBundleContainers(new IBundleContainer[]{container});
	
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(target, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		assertTrue("Target content not equal",((TargetDefinition)target).isContentEqual(definitionB));
		
		// resolve the restored target and ensure bundles are correct
		List infos = getAllBundleInfos(definitionB);
		Set names = collectAllSymbolicNames(infos);
		assertEquals(bundleIds.length, infos.size());
		
		for (int i = 0; i < bundleIds.length; i++) {
			assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
		}
		List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(1, profiles.size());
		String id = (String) profiles.get(0);
		assertTrue("Unexpected profile GC'd", id.endsWith(definitionB.getHandle().getMemento()));
	}	
	
	/**
	 * Tests that the external model manager can restore external bundles from the bundle pool
	 * properly. See bug 320583.
	 * 
	 * @throws Exception
	 */
	public void testExternalModelManagerPreferences() throws Exception {		
		// Set the active target to feature b (has 6 bundles)
		String[]unitIds = new String[]{"feature.b.feature.group"};
		IUBundleContainer container = createContainer(unitIds);
		ITargetDefinition targetB = getTargetService().newTarget();
		targetB.setBundleContainers(new IBundleContainer[]{container});
		getTargetService().saveTargetDefinition(targetB);
		setTargetPlatform(targetB);
		
		// Set the active target to feature a (has 3 bundles)
		unitIds = new String[]{"feature.a.feature.group"};
		container = createContainer(unitIds);
		ITargetDefinition targetA = getTargetService().newTarget();
		targetA.setBundleContainers(new IBundleContainer[]{container});
		getTargetService().saveTargetDefinition(targetA);
		setTargetPlatform(targetA);
		
		// ensure the external model manager only knows about bundles in target A
		URL[] paths = PDECore.getDefault().getModelManager().getExternalModelManager().getPluginPaths();
		assertEquals("Wrong number of external bundles", 3, paths.length);
		// expected bundles
		Set expected = new HashSet();
		expected.add("bundle.a1_1.0.0.jar");
		expected.add("bundle.a2_1.0.0.jar");
		expected.add("bundle.a3_1.0.0.jar");
		for (int i = 0; i < paths.length; i++) {
			assertTrue("Unexpected bundle in restored list: " + paths[i].toExternalForm(), expected.remove(URLUtil.toFile(paths[i]).getName()));
		}
		assertTrue(expected.isEmpty());
		resetTargetPlatform();
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
		IMetadataRepository repository = getRepository(uri);
		IInstallableUnit[] units = new IInstallableUnit[unitIds.length];
		for (int i = 0; i < unitIds.length; i++) {
			units[i] = getUnit(unitIds[i], repository);
		}
		return createContainer(units, new URI[]{uri});
	}
	
	/**
	 * Creates and returns an new IU bundle container with the specified IU's and repositories.
	 * 
	 * @param units IU's
	 * @param repositories locations of repositories
	 * @return IU bundle container
	 * @throws Exception
	 */
	protected IUBundleContainer createContainer(IInstallableUnit[] units, URI[] repositories) throws Exception {
		return (IUBundleContainer) getTargetService().newIUContainer(units, repositories);
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
		target.setBundleContainers(new IUBundleContainer[]{c1});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());
		
		getTargetService().saveTargetDefinition(target);
		ITargetHandle handle = target.getHandle();
		// get new unresolved copy of the target
		target = handle.getTargetDefinition();
		IUBundleContainer c2 = createContainer(new String[]{"feature.b.feature.group"});
		target.setBundleContainers(new IUBundleContainer[]{c2});
		
		List infos = getAllBundleInfos(target);
		Set names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3", "bundle.b1", "bundle.b2", "bundle.b3"};
		assertEquals(bundleIds.length, infos.size());
		
		for (int i = 0; i < bundleIds.length; i++) {
			assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
		}
		
		getTargetService().deleteTarget(target.getHandle());
		
		List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
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
		target.setBundleContainers(new IUBundleContainer[]{c1});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());
		
		getTargetService().saveTargetDefinition(target);
		ITargetHandle handle = target.getHandle();
		// get new unresolved copy of the target
		target = handle.getTargetDefinition();
		IUBundleContainer c2 = createContainer(new String[]{"feature.a.feature.group"});
		target.setBundleContainers(new IUBundleContainer[]{c2});
		
		List infos = getAllBundleInfos(target);
		Set names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		assertEquals(bundleIds.length, infos.size());
		
		for (int i = 0; i < bundleIds.length; i++) {
			assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
		}
		
		getTargetService().deleteTarget(target.getHandle());
		
		List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
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
		target.setBundleContainers(new IUBundleContainer[]{c1, c2});
		IStatus resolve = target.resolve(null);
		assertTrue(resolve.isOK());
				
		List infos = getBundleInfos(c1);
		Set names = collectAllSymbolicNames(infos);
		String[] bundleIds = new String[]{"bundle.a1", "bundle.a2", "bundle.a3"};
		assertEquals(bundleIds.length, infos.size());
		
		for (int i = 0; i < bundleIds.length; i++) {
			assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
		}
		
		infos = getBundleInfos(c2);
		names = collectAllSymbolicNames(infos);
		bundleIds = new String[]{"bundle.b1", "bundle.b2", "bundle.b3"};
		assertEquals(bundleIds.length, infos.size());
		
		for (int i = 0; i < bundleIds.length; i++) {
			assertTrue("Missing: " + bundleIds[i], names.contains(bundleIds[i]));
		}
		
		List profiles = P2TargetUtils.cleanOrphanedTargetDefinitionProfiles();
		assertEquals(1, profiles.size());
	}	
}
