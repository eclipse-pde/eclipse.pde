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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.FeatureBundleContainer;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests the persistence of target definitions. Tests memento creation, reading
 * of old target files, and writing of the model.
 *
 * @since 3.5
 */
public class TargetDefinitionPersistenceTests {

	/** Tests restoration of a handle to target definition in an IFile. */
	@Test
	public void testWorkspaceTargetHandleMemento() throws CoreException {
		ITargetPlatformService service = getTargetService();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(IPath.fromOSString("does/not/exist"));
		ITargetHandle handle = service.getTarget(file);
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(IPath.fromOSString("does/not/exist/either"));
		ITargetHandle handle3 = service.getTarget(file2);
		assertFalse("Should be different targets", handle.equals(handle3));
	}

	/** Tests restoration of a handle to target definition in local metadata. */
	@Test
	public void testLocalTargetHandleMemento() throws CoreException {
		ITargetPlatformService service = getTargetService();
		ITargetHandle handle = service.newTarget().getHandle();
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		ITargetHandle handle3 = service.newTarget().getHandle();
		assertFalse("Should be different targets", handle.equals(handle3));
	}

	/** Tests restoration of a handle to target definition in external URI. */
	@Test
	public void testExternalFileTargetHandleMemento() throws CoreException {
		ITargetPlatformService service = getTargetService();
		URI uri = null;
		try {
			uri = new URI("file:///does/not/exist");
		} catch (URISyntaxException e) {
		}
		// IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new
		// Path("does/not/exist"));
		ITargetHandle handle = service.getTarget(uri);
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		// IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new
		// Path("does/not/exist/either"));
		URI uri2 = null;
		try {
			uri2 = new URI("file://even/this/file/does/not/exist");
		} catch (URISyntaxException e) {
		}
		ITargetHandle handle3 = service.getTarget(uri2);
		assertFalse("Should be different targets", handle.equals(handle3));
	}

	/**
	 * Tests that an empty target definition can be serialized to xml, then
	 * deserialized without any loss of data.
	 */
	@Test
	public void testPersistEmptyDefinition() throws Exception {
		ITargetDefinition definitionA = getTargetService().newTarget();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		assertTargetDefinitionsEqual(definitionA, definitionB);
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldBasicTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("basic");

		assertEquals("Wrong name", "Basic", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a profile container", containers[0] instanceof ProfileBundleContainer);
		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()),
				IPath.fromOSString(getResolvedLocation(containers[0])));
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldBasicDirectoryTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("directory");

		assertEquals("Wrong name", "Directory", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("plugins"),
				IPath.fromOSString(getResolvedLocation(containers[0])));
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldSpecificTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("specific");

		assertEquals("Wrong name", "Specific Settings", target.getName());
		assertEquals("x86", target.getArch());
		assertEquals("linux", target.getOS());
		assertEquals("en_US", target.getNL());
		assertEquals("gtk", target.getWS());
		assertEquals("pgm1 pgm2", target.getProgramArguments());
		assertEquals("-Dfoo=\"bar\"", target.getVMArguments());
		assertEquals(
				JavaRuntime
				.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4")),
				target.getJREContainer());

		NameVersionDescriptor[] infos = target.getImplicitDependencies();
		assertEquals("Wrong number of implicit dependencies", 2, infos.length);
		Set<String> set = new HashSet<>();
		for (NameVersionDescriptor info : infos) {
			set.add(info.getId());
		}
		assertTrue("Missing ", set.remove("org.eclipse.jdt.debug"));
		assertTrue("Missing ", set.remove("org.eclipse.debug.core"));
		assertTrue(set.isEmpty());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("plugins"),
				IPath.fromOSString(getResolvedLocation(containers[0])));
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldAdditionLocationsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("additionalLocations");

		assertEquals("Wrong name", "Additional Locations", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof DirectoryBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);

		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()),
				IPath.fromOSString(getResolvedLocation(containers[0])));

		String string = VariablesPlugin.getDefault().getStringVariableManager()
				.performStringSubstitution("${workspace_loc}");
		assertEquals("Wrong 1st additional location", IPath.fromOSString(string).append("stuff"),
				IPath.fromOSString(getResolvedLocation(containers[1])));

		assertEquals("Wrong 2nd additional location",
				IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("dropins"),
				IPath.fromOSString(getResolvedLocation(containers[2])));
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldFeaturesTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("featureLocations");

		assertEquals("Wrong name", "Features", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 2, containers.length);
		assertTrue(containers[0] instanceof FeatureBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);

		assertEquals("Wrong feature location", "org.eclipse.jdt",
				((FeatureBundleContainer) containers[0]).getFeatureId());
		assertEquals("Wrong feature location", "org.eclipse.platform",
				((FeatureBundleContainer) containers[1]).getFeatureId());
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents.
	 */
	@Test
	public void testReadOldRestrictionsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("restrictions");

		assertEquals("Wrong name", "Restrictions", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of containers", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);

		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()),
				IPath.fromOSString(getResolvedLocation(containers[0])));
		assertEquals("Wrong 1st additional location", "org.eclipse.jdt",
				((FeatureBundleContainer) containers[1]).getFeatureId());
		assertEquals("Wrong 2nd additional location",
				IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("dropins"),
				IPath.fromOSString(getResolvedLocation(containers[2])));

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.debug.core", null),
				new NameVersionDescriptor("org.eclipse.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.launching", null) };

		NameVersionDescriptor[] actual = target.getIncluded();
		assertNotNull(actual);
		assertEquals("Wrong number of restrictions", restrictions.length, actual.length);
		for (int j = 0; j < actual.length; j++) {
			assertEquals("Wrong restriction", restrictions[j], actual[j]);
		}
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) that has extra/unsupported tags and retrieve the correct
	 * contents.
	 */
	@Test
	public void testReadOldTargetFileWithUnknownTags() throws Exception {
		ITargetDefinition target = readOldTarget("extratags");

		assertEquals("Wrong name", "Restrictions", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);

		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()),
				IPath.fromOSString(getResolvedLocation(containers[0])));
		assertEquals("Wrong 1st additional location", "org.eclipse.jdt",
				((FeatureBundleContainer) containers[1]).getFeatureId());
		assertEquals("Wrong 2nd additional location",
				IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("dropins"),
				IPath.fromOSString(getResolvedLocation(containers[2])));

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.debug.core", null),
				new NameVersionDescriptor("org.eclipse.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.launching", null) };

		NameVersionDescriptor[] actual = target.getIncluded();
		assertNotNull(actual);
		assertEquals("Wrong number of restrictions", restrictions.length, actual.length);
		for (int j = 0; j < actual.length; j++) {
			assertEquals("Wrong restriction", restrictions[j], actual[j]);
		}
	}

	/**
	 * Tests that we can de-serialize an old style target definition file
	 * (version 3.2) and retrieve the correct contents with optional bundles.
	 */
	@Test
	public void testReadOldOptionalTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("optional");

		assertEquals("Wrong name", "Optional", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 2, containers.length);
		assertTrue("Container should be a profile container", containers[0] instanceof ProfileBundleContainer);
		assertTrue("Container should be a profile container", containers[1] instanceof FeatureBundleContainer);
		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()),
				IPath.fromOSString(getResolvedLocation(containers[0])));
		assertEquals("Wrong feature location", "org.eclipse.jdt",
				((FeatureBundleContainer) containers[1]).getFeatureId());

		// Old optional settings are added to includes
		NameVersionDescriptor[] included = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.debug.core", null),
				new NameVersionDescriptor("org.eclipse.debug.examples.core", null),
				new NameVersionDescriptor("org.eclipse.debug.examples.ui", null),
				new NameVersionDescriptor("org.eclipse.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug.ui", null),
				new NameVersionDescriptor("org.eclipse.jdt.launching", null) };

		NameVersionDescriptor[] actual = target.getIncluded();
		assertNotNull(actual);
		assertEquals("Wrong number of inclusions", included.length, actual.length);
		for (int j = 0; j < actual.length; j++) {
			assertEquals("Wrong restriction", included[j], actual[j]);
		}
	}

	/**
	 * Test for bug 268709, if the content section is included in the xml, but
	 * there are no specific plug-ins or features as well as no
	 * useAllPlugins=true setting, treat the file as though it did include all
	 * plug-ins from the directory.
	 */
	@Test
	public void testEmptyContentSection() throws Exception {
		ITargetDefinition target = readOldTarget("emptycontent");

		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", IPath.fromOSString(TargetPlatform.getDefaultLocation()).append("plugins"),
				IPath.fromOSString(getResolvedLocation(containers[0])));

		target.resolve(null);

		assertTrue("Should have resolved bundles", target.getBundles().length > 0);

	}

	/**
	 * Test for bug 264139. Tests that when a target definition specifies
	 * "useAllPlugins=true" that we ignore specific plug-ins/features specified
	 * in the file during migration.
	 */
	@Test
	public void testMigrationOfUseAllWithRestrictions() throws Exception {
		ITargetDefinition target = readOldTarget("eclipse-serverside");
		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals(6, containers.length);
		validateTypeAndLocation(containers[0], ProfileBundleContainer.class,
				"${resource_loc:/target-platforms/eclipse-equinox-SDK-3.5M5/eclipse}");
		validateTypeAndLocation(containers[1], DirectoryBundleContainer.class,
				"${resource_loc:/target-platforms/eclipse-3.5M5-delta-pack/eclipse}");
		validateTypeAndLocation(containers[2], DirectoryBundleContainer.class,
				"${resource_loc:/target-platforms/eclipse-pde-headless-3.5M5}");
		validateTypeAndLocation(containers[3], DirectoryBundleContainer.class,
				"${resource_loc:/target-platforms/eclipse-test-framework-3.5M5/eclipse}");
		validateTypeAndLocation(containers[4], DirectoryBundleContainer.class,
				"${resource_loc:/target-platforms/eclipse-core-plugins-3.5M5}");
		validateTypeAndLocation(containers[5], DirectoryBundleContainer.class,
				"${resource_loc:/target-platforms/3rdparty-bundles}");
	}

	/** Validates the type and location of a bundle container. */
	private static void validateTypeAndLocation(ITargetLocation container, Class<?> clazz, String rawLocation)
			throws CoreException {
		assertTrue(clazz.isInstance(container));
		assertEquals(rawLocation, container.getLocation(false));
	}

	/**
	 * Tests that we increment the sequence number correctly when target is
	 * modified contents.
	 */
	@Test
	public void testSequenceNumberChange() throws Exception {
		ITargetDefinition target = readOldTarget("featureLocations");

		assertEquals("Wrong name", "Features", target.getName());
		TargetDefinition targetDef = (TargetDefinition) target;
		int currentSeqNo = targetDef.getSequenceNumber();
		target.setArch("x86");
		asssertSequenceNumber("Arch", currentSeqNo, targetDef);

		currentSeqNo = targetDef.getSequenceNumber();
		target.setOS("win32");
		asssertSequenceNumber("OS", currentSeqNo, targetDef);

		currentSeqNo = targetDef.getSequenceNumber();
		target.setNL("hi_IN");
		asssertSequenceNumber("NL", currentSeqNo, targetDef);

		currentSeqNo = targetDef.getSequenceNumber();
		target.setWS("carbon");
		asssertSequenceNumber("WS", currentSeqNo, targetDef);

		ITargetLocation[] newContainers = new ITargetLocation[1];
		newContainers[0] = new DirectoryBundleContainer("Path");
		currentSeqNo = targetDef.getSequenceNumber();
		target.setTargetLocations(newContainers);
		asssertSequenceNumber("Bundle Containers", currentSeqNo, targetDef);
	}

	@Test
	public void testIncludeSource() throws Exception {
		ITargetDefinition target = readOldTarget("SoftwareSiteTarget");
		ITargetLocation[] containers = target.getTargetLocations();
		assertEquals(1, containers.length);
		assertTrue(containers[0] instanceof IUBundleContainer);
		IUBundleContainer iubc = (IUBundleContainer) containers[0];
		assertTrue(iubc.getIncludeSource());
	}

	/**
	 * Tests that a complex metadata based target definition can be serialized
	 * to xml, then deserialized without any loss of data.
	 */
	@Test
	public void testPersistComplexMetadataDefinition() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();

		ITargetDefinition definitionA = getTargetService().newTarget();
		initComplexDefiniton(definitionA);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);

		assertTargetDefinitionsEqual(definitionA, definitionB);
	}

	/**
	 * Tests that a complex workspace file based target definition can be
	 * serialized to xml, then deserialized without any loss of data.
	 */
	@Test
	public void testPersistComplexWorkspaceDefinition() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("TargetDefinitionPersistenceTests");
		try {
			if (!project.exists()) {
				project.create(null);
			}
			assertTrue("Could not create test project", project.exists());
			project.open(null);
			assertTrue("Could not open test project", project.isOpen());

			IFile target = project.getFile(Long.toString(System.currentTimeMillis()) + "A.target");
			ITargetDefinition definitionA = getTargetService().getTarget(target).getTargetDefinition();
			initComplexDefiniton(definitionA);
			getTargetService().saveTargetDefinition(definitionA);
			ITargetDefinition definitionB = getTargetService().getTarget(target).getTargetDefinition();

			assertTargetDefinitionsEqual(definitionA, definitionB);
		} finally {
			if (project.exists()) {
				project.delete(true, null);
			}
			assertFalse("Could not delete test project", project.exists());
		}
	}

	/**
	 * Returns the target platform service or <code>null</code> if none
	 *
	 * @return target platform service
	 */
	private ITargetPlatformService getTargetService() {
		ITargetPlatformService service = AbstractTargetTest.getTargetService();
		assertNotNull("Missing target platform service", service);
		return service;
	}

	/**
	 * Reads a target definition file from the tests/targets/target-files
	 * location with the given name. Note that ".target" will be appended.
	 */
	private ITargetDefinition readOldTarget(String name) throws Exception {
		URL url = FrameworkUtil.getBundle(TargetDefinitionPersistenceTests.class)
				.getEntry("/tests/targets/target-files/" + name + ".trgt");
		File file = new File(FileLocator.toFileURL(url).getFile());
		ITargetDefinition target = getTargetService().newTarget();
		try (FileInputStream stream = new FileInputStream(file)) {
			TargetDefinitionPersistenceHelper.initFromXML(target, stream);
		}
		return target;
	}

	/** Returns the resolved location of the specified bundle container. */
	private String getResolvedLocation(ITargetLocation container) throws CoreException {
		return container.getLocation(true);
	}

	/**
	 * Returns the location of the JDT feature in the running host as a path in
	 * the local file system.
	 *
	 * @return path to JDT feature
	 */
	private IPath getJdtFeatureLocation() {
		IPath path = IPath.fromOSString(TargetPlatform.getDefaultLocation());
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
		return IPath.fromOSString(location);
	}

	private void initComplexDefiniton(ITargetDefinition definition) throws URISyntaxException {
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());

		NameVersionDescriptor[] implicit = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null) };
		definition.setImplicitDependencies(implicit);

		// Directory container
		ITargetLocation dirContainer = getTargetService()
				.newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		// Profile container with specific config area
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		// Feature container with specific version
		IPath location = getJdtFeatureLocation();
		String segment = location.lastSegment();
		int index = segment.indexOf('_');
		assertTrue("Missing version id", index > 0);
		String version = segment.substring(index + 1);
		ITargetLocation featureContainer = getTargetService().newFeatureLocation("${eclipse_home}", "org.eclipse.jdt",
				version);
		// Profile container restricted to just two bundles
		ITargetLocation restrictedProfileContainer = getTargetService()
				.newProfileLocation(TargetPlatform.getDefaultLocation(), null);

		// Site bundle containers with different settings
		IUBundleContainer siteContainer = (IUBundleContainer) getTargetService().newIULocation(
				new IInstallableUnit[] {}, new URI[] { new URI("TESTURI"), new URI("TESTURI2") },
				IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS);
		IUBundleContainer siteContainer2 = (IUBundleContainer) getTargetService().newIULocation(
				new String[] { "unit1", "unit2" }, new String[] { "1.0", "2.0" },
				new URI[] { new URI("TESTURI"), new URI("TESTURI2") }, IUBundleContainer.INCLUDE_REQUIRED);

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null) };
		definition.setIncluded(restrictions);
		definition.setTargetLocations(new ITargetLocation[] { dirContainer, profileContainer, featureContainer,
				restrictedProfileContainer, siteContainer, siteContainer2 });
	}

	private void assertTargetDefinitionsEqual(ITargetDefinition targetA, ITargetDefinition targetB) {
		assertTrue("Target content not equal", ((TargetDefinition) targetA).isContentEqual(targetB));
	}

	private void asssertSequenceNumber(String name, int currentSeqNo, TargetDefinition targetDef) {
		assertEquals("Sequence number did not increment after updating '" + name + "'", currentSeqNo + 1,
				targetDef.getSequenceNumber());
	}

}