/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import org.eclipse.pde.core.target.*;

import java.io.FileWriter;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

public class TargetDefinitionResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionResolutionTests.class);
	}
	
	public void testMissingBundles() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		ITargetLocation directoryContainer = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		
		definition.setTargetLocations(new ITargetLocation[]{directoryContainer, profileContainer});
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		definition.resolve(null);
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		assertEquals("Wrong number of included bundles", 2, definition.getBundles().length);
		
		IStatus definitionStatus = definition.getStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 2, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, children[0].getCode());
		assertEquals("Wrong severity", IStatus.ERROR, children[1].getSeverity());
		assertEquals(TargetBundle.STATUS_VERSION_DOES_NOT_EXIST, children[1].getCode());
		
		// Check that removing the included bundles and resolving removes the errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
	
	public void testInvalidBundleContainers() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		ITargetLocation directoryContainer = getTargetService().newDirectoryLocation("***SHOULD NOT EXIST***");
		IStatus status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		ITargetLocation profileContainer = getTargetService().newProfileLocation("***SHOULD NOT EXIST***", null);
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		ITargetLocation featureContainer = getTargetService().newFeatureLocation("***SHOULD NOT EXIST***", "org.eclipse.jdt", "");
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		definition.setTargetLocations(new ITargetLocation[]{directoryContainer, profileContainer, featureContainer});
		status = definition.resolve(null);
		IStatus[] children = status.getChildren();
		assertEquals("Wrong number of children", 3, children.length);
		for (int i = 0; i < children.length; i++) {
			assertEquals("Incorrect severity", IStatus.ERROR, children[i].getSeverity());
			assertFalse("Failed resolution should be single status", children[i].isMultiStatus());
		}
	}
	
	/**
	 * Tests that if we find a bundle with a bad or missing manifest when resolving we create the
	 * correct status.
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
		
		// Resolving with errors should stay resolved, solving a definition should resolve all targets
		ITargetLocation brokenContainer = getTargetService().newDirectoryLocation("***SHOULD NOT EXIST***");
		assertFalse(brokenContainer.isResolved());
		assertNull("Bundles available when unresolved", brokenContainer.getBundles());
		definition.setTargetLocations(new ITargetLocation[]{brokenContainer});
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		IStatus status = definition.resolve(null);
		assertEquals("Incorrect Severity", IStatus.ERROR, status.getSeverity());
		assertTrue(brokenContainer.isResolved());
		assertNotNull("Bundles not available when resolved", brokenContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Adding an unresolved container should make the target unresolved, resolving the container should resolve the target
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		assertFalse(profileContainer.isResolved());
		assertNull("Bundles available when unresolved", profileContainer.getBundles());
		definition.setTargetLocations(new ITargetLocation[]{brokenContainer, profileContainer});
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		status = profileContainer.resolve(definition, null);
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getStatus().getSeverity());
		assertTrue(profileContainer.isResolved());
		assertNotNull("Bundles not available when resolved", profileContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Having a bundle status does not prevent the resolution, adding a resolved container should leave the target resolved
		ITargetLocation includesContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		assertFalse(includesContainer.isResolved());
		assertNull("Bundles available when unresolved", includesContainer.getBundles());
		status = includesContainer.resolve(definition, null);
		assertTrue(includesContainer.isResolved());
		assertNotNull("Bundles not available when resolved", includesContainer.getBundles());
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.OK, includesContainer.getStatus().getSeverity());
		definition.setTargetLocations(new ITargetLocation[]{brokenContainer, profileContainer});
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		assertTrue(definition.isResolved());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getStatus().getSeverity());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Setting includes, etc. should not unresolve the target
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});		assertTrue(definition.isResolved());
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());
		NameVersionDescriptor[] implicit = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null)
		};		
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
	 * Tests that if users have the old preference to append .ini VM arguments,
	 * target definitions are migrated properly with the arguments appended. 
	 */
	public void testVMArgumentsMigrationAppend() throws Exception {
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean original = store.getBoolean(ICoreConstants.VM_LAUNCHER_INI);
		store.setValue(ICoreConstants.VM_LAUNCHER_INI, true);
		try {
			ITargetDefinition target = getNewTarget();
			((TargetPlatformService)getTargetService()).loadTargetDefinitionFromPreferences(target);
			String vmArguments = target.getVMArguments();
			String iniVmArgs = TargetPlatformHelper.getIniVMArgs();
			assertEquals(vmArguments, iniVmArgs);
		} finally {
			store.setValue(ICoreConstants.VM_LAUNCHER_INI, original);
		}
	}
	
	/**
	 * Tests that if users *don't* have the old preference to append .ini VM arguments,
	 * target definitions are migrated properly *without* the arguments appended. 
	 */
	public void testVMArgumentsMigrationNoAppend() throws Exception {
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean original = store.getBoolean(ICoreConstants.VM_LAUNCHER_INI);
		store.setValue(ICoreConstants.VM_LAUNCHER_INI, false);
		try {
			ITargetDefinition target = getNewTarget();
			((TargetPlatformService)getTargetService()).loadTargetDefinitionFromPreferences(target);
			String vmArguments = target.getVMArguments();
			assertNull("Arguments should be empty", vmArguments);
		} finally {
			store.setValue(ICoreConstants.VM_LAUNCHER_INI, original);
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
		definition.setTargetLocations(new ITargetLocation[]{container});
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
	 * Tests that a target definition is in synch with the target platform when there
	 * are duplicates in the target definition (duplicates should be ignored).
	 * 
	 * @throws Exception
	 */
	public void testTargetInSynchWithDuplicates() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		ITargetLocation container2 = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[]{container, container2});
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
	 * Tests that a target definition is not in synch with the target platform when a
	 * bundle is deleted from the underlying files system (target platform).
	 * 
	 * @throws Exception
	 */
	public void testTargetMissingBundle() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[]{container});
		
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
	 * Tests that a target definition is not in synch with the target platform when a
	 * bundle is added to the underlying file system (target platform).
	 * 
	 * @throws Exception
	 */
	public void testTargetPlatformMissingBundle() throws Exception {
		IPath location = extractAbcdePlugins();
		IPath dirPath = location.append("plugins");
		// delete a bundle (by renaming it)
		IPath bundle = dirPath.append("bundle.a_1.0.0.jar");
		File jar = bundle.toFile();
		assertTrue(jar.exists());
		File xxx = new File(jar.getParentFile(), "bundle.a_1.0.0.xxx");
		jar.renameTo(xxx);
		
		ITargetDefinition definition = getTargetService().newTarget();
		ITargetLocation container = getTargetService().newDirectoryLocation(dirPath.toOSString());
		definition.setTargetLocations(new ITargetLocation[]{container});
		definition.resolve(null);
		TargetBundle[] allBundles = definition.getAllBundles();
		assertEquals(9, allBundles.length);
		
		try {
			setTargetPlatform(definition);
			// force definition to re-resolve
			ITargetDefinition copy = getTargetService().newTarget();
			getTargetService().copyTargetDefinition(definition, copy);
			// add the bundle back to the file system
			xxx.renameTo(jar);
			
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
	 * Tests that a pre-p2 installation can be read/parsed properly.
	 * 
	 * @throws Exception
	 */
	public void testClassicInstallResolution() throws Exception {
		// extract the 3.0.2 skeleton
		IPath location = extractClassicPlugins();
		
		// the new way
		ITargetDefinition definition = getNewTarget();
		String home = location.removeLastSegments(1).toOSString();
		ITargetLocation container = getTargetService().newProfileLocation(home, null);
		definition.setTargetLocations(new ITargetLocation[]{container});
		definition.resolve(null);
		TargetBundle[] bundles = definition.getAllBundles();
		
		int source = 0;
		int frag = 0;
		int bin = 0;
		
		for (int i = 0; i < bundles.length; i++) {
			TargetBundle bundle = bundles[i];
			if (bundle.isFragment()) {
				frag++;
				if (bundle.isSourceBundle()) {
					source++; // fragment && source
				} 
			} else if (bundle.isSourceBundle()) {
				source++;
			} else {
				bin++;
			}
		}
		// there should be 80 plug-ins and 4 source plug-ins (win 32)
		assertEquals("Wrong number of bundles", 84, bundles.length);
		assertEquals("Wrong number of binary bundles", 75, bin);
		assertEquals("Wrong number of source bundles", 4, source);
		assertEquals("Wrong number of fragments", 6, frag);
	}
	
	
	/**
	 * Tests that an installation container will recognize linked plug-ins
	 * while a directory container will not
	 * @throws Exception
	 */
	public void testLinkedInstallResolution() throws Exception {
		// extract the 3.0.2 skeleton and extra plugins to link
		IPath location = extractClassicPlugins();
		IPath extraPlugins = extractLinkedPlugins();
		
		// Create the link file
		File linkLocation = new File(location.toFile().getParentFile(),"links");
		linkLocation.mkdirs();
		File linkFile = new File(linkLocation, "test.link");
		linkFile.createNewFile();
		FileWriter writer = new FileWriter(linkFile);
		writer.write("path=" + extraPlugins.removeLastSegments(1).toPortableString());
		writer.flush();
		writer.close();
		
		ITargetDefinition definition = getNewTarget();
		String home = location.removeLastSegments(1).toOSString();
		ITargetLocation container = getTargetService().newProfileLocation(home, null);
		definition.setTargetLocations(new ITargetLocation[]{container});
		definition.resolve(null);
		TargetBundle[] bundles = definition.getAllBundles();
		
		int source = 0;
		int frag = 0;
		int bin = 0;
		
		for (int i = 0; i < bundles.length; i++) {
			TargetBundle bundle = bundles[i];
			if (bundle.isFragment()) {
				frag++;
				if (bundle.isSourceBundle()) {
					source++; // fragment && source
				} 
			} else if (bundle.isSourceBundle()) {
				source++;
			} else {
				bin++;
			}
		}
		// there should be 80 plug-ins and 4 source plug-ins (win 32) + 10 extra links plug-ins (5 of which are source)
		assertEquals("Wrong number of bundles", 94, bundles.length);
		assertEquals("Wrong number of binary bundles", 80, bin);
		assertEquals("Wrong number of source bundles", 9, source);
		assertEquals("Wrong number of fragments", 6, frag);
		
		// Check that the directory container doesn't find any linked plugins
		definition = getNewTarget();
		container = getTargetService().newDirectoryLocation(home);
		definition.setTargetLocations(new ITargetLocation[]{container});
		definition.resolve(null);
		bundles = definition.getAllBundles();
		
		source = 0;
		frag = 0;
		bin = 0;
		
		for (int i = 0; i < bundles.length; i++) {
			TargetBundle bundle = bundles[i];
			if (bundle.isFragment()) {
				frag++;
				if (bundle.isSourceBundle()) {
					source++; // fragment && source
				} 
			} else if (bundle.isSourceBundle()) {
				source++;
			} else {
				bin++;
			}
		}
		// there should be 80 plug-ins and 4 source plug-ins (win 32)
		assertEquals("Wrong number of bundles", 84, bundles.length);
		assertEquals("Wrong number of source bundles", 4, source);
		assertEquals("Wrong number of fragments", 6, frag);
		
	}
	/**
	 * Tests that when resolving a set of bundles that include source bundles, the source bundles
	 * are able to determine the bundle their source is for.
	 * @throws Exception
	 */
	public void testSourceBundleRecognition() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		ITargetLocation directoryContainer = getTargetService().newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(), null);
		
		ITargetLocation featureContainer = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt", null);
		
		ITargetLocation featureContainer2 = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt.source", null);
	
		definition.setTargetLocations(new ITargetLocation[]{directoryContainer, profileContainer, featureContainer, featureContainer2});
		definition.resolve(null);
		
		TargetBundle[] bundles = definition.getBundles();
		
		assertNotNull("Target didn't resolve",bundles);
		
		IStatus definitionStatus = definition.getStatus();
		assertEquals("Wrong severity", IStatus.OK, definitionStatus.getSeverity());
		
		// Ensure that all source bundles know what they provide source for.
		for (int i = 0; i < bundles.length; i++) {
			TargetBundle bundle = bundles[i];
			if (bundle.isSourceBundle()){
				BundleInfo info = bundle.getSourceTarget();
				assertNotNull("Missing source target for " + bundle,info);
			} else {
				assertNull(bundle.getSourceTarget());
			}
		}
		
		// Everything in the JDT feature has an equivalent named source bundle
		bundles = featureContainer2.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getBundleInfo().getSymbolicName().indexOf("doc") == -1){
				assertTrue("Non-source bundle in source feature", bundles[i].isSourceBundle());
				assertEquals("Incorrect source target", bundles[i].getBundleInfo().getSymbolicName(),bundles[i].getSourceTarget().getSymbolicName()+".source");
			}
		}
	}
	
	/**
	 * Tests the two options on IU bundle containers for controlling how the site will be resolved
	 * @throws Exception
	 */
	public void testSiteContainerIncludeSettings() throws Exception{
		ITargetDefinition target = getNewTarget();
		IUBundleContainer containerA = (IUBundleContainer)getTargetService().newIULocation(new IInstallableUnit[0], null, IUBundleContainer.INCLUDE_REQUIRED);
		IUBundleContainer containerB = (IUBundleContainer)getTargetService().newIULocation(new String[]{"unit1", "unit2"}, new String[]{"1.0","2.0"}, null, IUBundleContainer.INCLUDE_REQUIRED);
		target.setTargetLocations(new ITargetLocation[]{containerA, containerB});
		
		// Check default settings
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertFalse(containerA.getIncludeSource());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		assertFalse(containerB.getIncludeSource());
		
		// Check basic changes
		int flags = IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE;
		containerA = (IUBundleContainer)getTargetService().newIULocation(new IInstallableUnit[0], null, flags);
		containerB = (IUBundleContainer)getTargetService().newIULocation(new String[]{"unit1", "unit2"}, new String[]{"1.0","2.0"}, null, flags);
		target.setTargetLocations(new ITargetLocation[]{containerA, containerB});
		assertFalse(containerA.getIncludeAllRequired());
		assertTrue(containerA.getIncludeAllEnvironments());
		assertTrue(containerA.getIncludeSource());
		
		// Check that all containers are updated in the target if target is passed as argument
		flags = IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS | IUBundleContainer.INCLUDE_SOURCE;
		containerA = (IUBundleContainer)getTargetService().newIULocation(new IInstallableUnit[0], null, flags);
		flags = IUBundleContainer.INCLUDE_REQUIRED;
		containerB = (IUBundleContainer)getTargetService().newIULocation(new String[]{"unit1", "unit2"}, new String[]{"1.0","2.0"}, null, flags);

		target.setTargetLocations(new ITargetLocation[]{containerA, containerB});
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertFalse(containerA.getIncludeSource());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		assertFalse(containerB.getIncludeSource());

		// now switch the order of A and B
		target.setTargetLocations(new ITargetLocation[]{containerB, containerA});
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