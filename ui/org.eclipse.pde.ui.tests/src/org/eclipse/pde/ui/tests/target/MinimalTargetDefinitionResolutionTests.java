/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

/**
 * Runs on minimal bundles and don't require full eclipse SDK.This class is
 * refactored out of TargetDefinitionResolutionTests
 *
 */
public class MinimalTargetDefinitionResolutionTests extends AbstractTargetTest {

	public void testInvalidBundleContainers() throws Exception {
		ITargetDefinition definition = getNewTarget();

		ITargetLocation directoryContainer = getTargetService().newDirectoryLocation("***SHOULD NOT EXIST***");
		IStatus status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());

		ITargetLocation profileContainer = getTargetService().newProfileLocation("***SHOULD NOT EXIST***", null);
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());

		ITargetLocation featureContainer = getTargetService().newFeatureLocation("***SHOULD NOT EXIST***",
				"org.eclipse.jdt", "");
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());

		definition.setTargetLocations(new ITargetLocation[] { directoryContainer, profileContainer, featureContainer });
		status = definition.resolve(null);
		IStatus[] children = status.getChildren();
		assertEquals("Wrong number of children", 3, children.length);
		for (IStatus element : children) {
			assertEquals("Incorrect severity", IStatus.ERROR, element.getSeverity());
			assertFalse("Failed resolution should be single status", element.isMultiStatus());
		}
	}

	/**
	 * Tests that if we find a bundle with a bad or missing manifest when
	 * resolving we create the correct status.
	 *
	 * @see TargetBundle.STATUS_INVALID_MANIFEST
	 * @throws Exception
	 */
	public void testInvalidManifest() throws Exception {
		// TODO Should we have tests for this?
	}

	public void testResolutionCaching() throws Exception {
		ITargetDefinition definition = getNewTarget();
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		assertEquals("Wrong number of bundles", 0, definition.getBundles().length);

		// Resolving with errors should stay resolved, solving a definition
		// should resolve all targets
		ITargetLocation brokenContainer = getTargetService().newDirectoryLocation("***SHOULD NOT EXIST***");
		assertFalse(brokenContainer.isResolved());
		assertNull("Bundles available when unresolved", brokenContainer.getBundles());
		definition.setTargetLocations(new ITargetLocation[] { brokenContainer });
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		IStatus status = definition.resolve(null);
		assertEquals("Incorrect Severity", IStatus.ERROR, status.getSeverity());
		assertTrue(brokenContainer.isResolved());
		assertNotNull("Bundles not available when resolved", brokenContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());

		// Adding an unresolved container should make the target unresolved,
		// resolving the container should resolve the target
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				null);
		assertFalse(profileContainer.isResolved());
		assertNull("Bundles available when unresolved", profileContainer.getBundles());
		definition.setTargetLocations(new ITargetLocation[] { brokenContainer, profileContainer });
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		status = profileContainer.resolve(definition, null);
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getStatus().getSeverity());
		assertTrue(profileContainer.isResolved());
		assertNotNull("Bundles not available when resolved", profileContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());

		// Having a bundle status does not prevent the resolution, adding a
		// resolved container should leave the target resolved
		ITargetLocation includesContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				null);
		assertFalse(includesContainer.isResolved());
		assertNull("Bundles available when unresolved", includesContainer.getBundles());
		status = includesContainer.resolve(definition, null);
		assertTrue(includesContainer.isResolved());
		assertNotNull("Bundles not available when resolved", includesContainer.getBundles());
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.OK, includesContainer.getStatus().getSeverity());
		definition.setTargetLocations(new ITargetLocation[] { brokenContainer, profileContainer });
		definition.setIncluded(new NameVersionDescriptor[] { new NameVersionDescriptor("bogus", null),
				new NameVersionDescriptor("org.eclipse.platform", "666.666.666") });
		assertTrue(definition.isResolved());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getStatus().getSeverity());
		assertNotNull("Bundles not available when resolved", definition.getBundles());

		// Setting includes, etc. should not unresolve the target
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		definition.setIncluded(new NameVersionDescriptor[] { new NameVersionDescriptor("bogus", null),
				new NameVersionDescriptor("org.eclipse.platform", "666.666.666") });
		assertTrue(definition.isResolved());
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
		assertTrue(definition.isResolved());
		definition.setName(null);
		definition.setOS(null);
		definition.setWS(null);
		definition.setArch(null);
		definition.setNL(null);
		definition.setProgramArguments(null);
		definition.setVMArguments(null);
		definition.setJREContainer(null);
		definition.setImplicitDependencies(null);
		assertTrue(definition.isResolved());

		definition.setTargetLocations(null);
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());

	}


	/**
	 * Tests that if users *don't* have the old preference to append .ini VM
	 * arguments, target definitions are migrated properly *without* the
	 * arguments appended.
	 */
	@SuppressWarnings("deprecation")
	public void testVMArgumentsMigrationNoAppend() throws Exception {
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean original = store.getBoolean(ICoreConstants.VM_LAUNCHER_INI);
		store.setValue(ICoreConstants.VM_LAUNCHER_INI, false);
		String originalTarget = store.getString(ICoreConstants.TARGET_MODE);
		store.setValue(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		try {
			ITargetDefinition target = ((TargetPlatformService) getTargetService()).newTargetFromPreferences();
			assertNotNull("No target was created from old preferences", target);
			String vmArguments = target.getVMArguments();
			assertNull("Arguments should be empty", vmArguments);
		} finally {
			store.setValue(ICoreConstants.VM_LAUNCHER_INI, original);
			store.setValue(ICoreConstants.TARGET_MODE, originalTarget);
		}
	}

	/**
	 * Tests that a target definition is in synch with the target platform.
	 *
	 * @throws Exception
	 */
	public void testTargetInSynch() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);
		TargetBundle[] allBundles = definition.getAllBundles();
		assertEquals(10, allBundles.length);

		try {
			setTargetPlatform(definition);
			IStatus status = getTargetService().compareWithTargetPlatform(definition);
			assertTrue(status.isOK());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests that a target definition is in synch with the target platform when
	 * there are duplicates in the target definition (duplicates should be
	 * ignored).
	 *
	 * @throws Exception
	 */
	public void testTargetInSynchWithDuplicates() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		ITargetLocation container2 = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container, container2 });
		definition.resolve(null);
		TargetBundle[] allBundles = definition.getAllBundles();
		assertEquals(20, allBundles.length);

		try {
			setTargetPlatform(definition);
			IStatus status = getTargetService().compareWithTargetPlatform(definition);
			assertTrue(status.isOK());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests that a target definition is not in synch with the target platform
	 * when a bundle is deleted from the underlying files system (target
	 * platform).
	 *
	 * @throws Exception
	 */
	public void testTargetMissingBundle() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });

		try {
			setTargetPlatform(definition);
			// delete a bundle
			IPath bundle = dirPath.append("bundle.a_1.0.0.jar");
			assertTrue(bundle.toFile().exists());
			bundle.toFile().delete();
			// force definition to re-resolve
			ITargetDefinition copy = getTargetService().newTarget();
			getTargetService().copyTargetDefinition(definition, copy);
			copy.resolve(null);
			IStatus status = getTargetService().compareWithTargetPlatform(copy);
			assertNotNull(status);
			assertFalse(status.isOK());
			IStatus[] children = status.getChildren();
			assertEquals(1, children.length);
			assertEquals(ITargetPlatformService.STATUS_MISSING_FROM_TARGET_DEFINITION, children[0].getCode());
			assertEquals("bundle.a", children[0].getMessage());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests that a target definition will warn if an expected bundle does not
	 * exist on the file system.
	 *
	 * @throws Exception
	 */
	public void testTargetPlatformMissingBundle() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		// delete a bundle
		IPath bundle = dirPath.append("bundle.a_1.0.0.jar");
		File jar = bundle.toFile();
		assertTrue(jar.exists());
		jar.delete();

		ITargetDefinition definition = getTargetService().newTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);
		TargetBundle[] allBundles = definition.getAllBundles();
		assertEquals(9, allBundles.length);

		try {
			setTargetPlatform(definition);
			// force definition to re-resolve
			ITargetDefinition copy = getTargetService().newTarget();
			getTargetService().copyTargetDefinition(definition, copy);
			// add the bundle back to the file system
			extractAbcdePlugins();

			copy.resolve(null);
			IStatus status = getTargetService().compareWithTargetPlatform(copy);
			assertNotNull(status);
			assertFalse(status.isOK());
			IStatus[] children = status.getChildren();
			assertEquals(1, children.length);
			assertEquals(ITargetPlatformService.STATUS_MISSING_FROM_TARGET_PLATFORM, children[0].getCode());
			assertEquals("bundle.a", children[0].getMessage());
		} finally {
			resetTargetPlatform();
		}
	}

	/**
	 * Tests the two options on IU bundle containers for controlling how the
	 * site will be resolved
	 *
	 * @throws Exception
	 */
	public void testSiteContainerIncludeSettings() throws Exception {
		ITargetDefinition target = getNewTarget();
		IUBundleContainer containerA = (IUBundleContainer) getTargetService().newIULocation(new IInstallableUnit[0],
				null, IUBundleContainer.INCLUDE_REQUIRED);
		IUBundleContainer containerB = (IUBundleContainer) getTargetService().newIULocation(
				new String[] { "unit1", "unit2" }, new String[] { "1.0", "2.0" }, null,
				IUBundleContainer.INCLUDE_REQUIRED);
		target.setTargetLocations(new ITargetLocation[] { containerA, containerB });

		// Check default settings
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertFalse(containerA.getIncludeSource());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		assertFalse(containerB.getIncludeSource());

		// Check basic changes
		int flags = IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE;
		containerA = (IUBundleContainer) getTargetService().newIULocation(new IInstallableUnit[0], null, flags);
		containerB = (IUBundleContainer) getTargetService().newIULocation(new String[] { "unit1", "unit2" },
				new String[] { "1.0", "2.0" }, null, flags);
		target.setTargetLocations(new ITargetLocation[] { containerA, containerB });
		assertFalse(containerA.getIncludeAllRequired());
		assertTrue(containerA.getIncludeAllEnvironments());
		assertTrue(containerA.getIncludeSource());

		// Check that all containers are updated in the target if target is
		// passed as argument
		flags = IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE;
		containerA = (IUBundleContainer) getTargetService().newIULocation(new IInstallableUnit[0], null, flags);
		flags = IUBundleContainer.INCLUDE_REQUIRED;
		containerB = (IUBundleContainer) getTargetService().newIULocation(new String[] { "unit1", "unit2" },
				new String[] { "1.0", "2.0" }, null, flags);

		target.setTargetLocations(new ITargetLocation[] { containerA, containerB });
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertFalse(containerA.getIncludeSource());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		assertFalse(containerB.getIncludeSource());

		// now switch the order of A and B
		target.setTargetLocations(new ITargetLocation[] { containerB, containerA });
		assertFalse(containerA.getIncludeAllRequired());
		assertTrue(containerA.getIncludeAllEnvironments());
		assertTrue(containerA.getIncludeSource());
		assertFalse(containerB.getIncludeAllRequired());
		assertTrue(containerB.getIncludeAllEnvironments());
		assertTrue(containerB.getIncludeSource());
	}

	public void testNameVersionDescriptor() {
		NameVersionDescriptor d1 = new NameVersionDescriptor("a.b.c", "1.0.0");
		NameVersionDescriptor d2 = new NameVersionDescriptor("a.b.c", "1.0.0");
		assertEquals(d1, d2);

		d1 = new NameVersionDescriptor("a.b.c", "1.0.0");
		d2 = new NameVersionDescriptor("a.b.c", "1.0.1");
		assertFalse(d1.equals(d2));

		d1 = new NameVersionDescriptor("a.b.c", "1.0.0");
		d2 = new NameVersionDescriptor("a.b.e", "1.0.0");
		assertFalse(d1.equals(d2));

		d1 = new NameVersionDescriptor("a.b.c", null);
		d2 = new NameVersionDescriptor("a.b.c", null);
		assertEquals(d1, d2);

		d1 = new NameVersionDescriptor("a.b.c", null);
		d2 = new NameVersionDescriptor("a.b.e", null);
		assertFalse(d1.equals(d2));

		d1 = new NameVersionDescriptor("a.b.c", null);
		d2 = new NameVersionDescriptor("a.b.c", "1.0.0");
		assertFalse(d1.equals(d2));

		d1 = new NameVersionDescriptor("a.b.c", "1.0.0");
		d2 = new NameVersionDescriptor("a.b.c", null);
		assertFalse(d1.equals(d2));
	}
}