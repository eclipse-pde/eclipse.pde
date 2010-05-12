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
import org.eclipse.pde.internal.core.target.provisional.*;

public class TargetDefinitionResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionResolutionTests.class);
	}
	
	public void testMissingBundles() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer});
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		definition.resolve(null);
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		assertEquals("Wrong number of included bundles", 2, definition.getBundles().length);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 2, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(IResolvedBundle.STATUS_PLUGIN_DOES_NOT_EXIST, children[0].getCode());
		assertEquals("Wrong severity", IStatus.ERROR, children[1].getSeverity());
		assertEquals(IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, children[1].getCode());
		
		// Check that removing the included bundles and resolving removes the errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getBundleStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
	public void testMissingOptionalBundles() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer});
		definition.setOptional(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		definition.resolve(null);
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.INFO, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 2, children.length);
		assertEquals("Wrong severity", IStatus.INFO, children[0].getSeverity());
		assertEquals(IResolvedBundle.STATUS_PLUGIN_DOES_NOT_EXIST, children[0].getCode());
		assertEquals("Wrong severity", IStatus.INFO, children[1].getSeverity());
		assertEquals(IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, children[1].getCode());
		
		// Check that removing the optional bundles and resolving removes the errors.
		definition.setOptional(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getBundleStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
	public void testInvalidBundleContainers() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer("***SHOULD NOT EXIST***");
		IStatus status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer("***SHOULD NOT EXIST***", null);
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		IBundleContainer featureContainer = getTargetService().newFeatureContainer("***SHOULD NOT EXIST***", "org.eclipse.jdt", "");
		status = directoryContainer.resolve(definition, null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer, featureContainer});
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
	 * @see IResolvedBundle.STATUS_INVALID_MANIFEST
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
		IBundleContainer brokenContainer = getTargetService().newDirectoryContainer("***SHOULD NOT EXIST***");
		assertFalse(brokenContainer.isResolved());
		assertNull("Bundles available when unresolved", brokenContainer.getBundles());
		definition.setBundleContainers(new IBundleContainer[]{brokenContainer});
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		IStatus status = definition.resolve(null);
		assertEquals("Incorrect Severity", IStatus.ERROR, status.getSeverity());
		assertTrue(brokenContainer.isResolved());
		assertNotNull("Bundles not available when resolved", brokenContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Adding an unresolved container should make the target unresolved, resolving the container should resolve the target
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		assertFalse(profileContainer.isResolved());
		assertNull("Bundles available when unresolved", profileContainer.getBundles());
		definition.setBundleContainers(new IBundleContainer[]{brokenContainer, profileContainer});
		assertFalse(definition.isResolved());
		assertNull("Bundles available when unresolved", definition.getBundles());
		status = profileContainer.resolve(definition, null);
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getBundleStatus().getSeverity());
		assertTrue(profileContainer.isResolved());
		assertNotNull("Bundles not available when resolved", profileContainer.getBundles());
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Having a bundle status does not prevent the resolution, adding a resolved container should leave the target resolved
		IBundleContainer includesContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		assertFalse(includesContainer.isResolved());
		assertNull("Bundles available when unresolved", includesContainer.getBundles());
		status = includesContainer.resolve(definition, null);
		assertTrue(includesContainer.isResolved());
		assertNotNull("Bundles not available when resolved", includesContainer.getBundles());
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.OK, includesContainer.getStatus().getSeverity());
		definition.setBundleContainers(new IBundleContainer[]{brokenContainer, profileContainer});
		definition.setOptional(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
		assertTrue(definition.isResolved());
		assertEquals("Incorrect Severity", IStatus.ERROR, definition.getBundleStatus().getSeverity());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Setting includes, optional, etc. should not unresolve the target
		definition.setOptional(null);
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		definition.setOptional(new NameVersionDescriptor[]{new NameVersionDescriptor("bogus",null),new NameVersionDescriptor("org.eclipse.platform","666.666.666")});
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
		
		definition.setBundleContainers(null);
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
		IBundleContainer container = getTargetService().newDirectoryContainer(dirPath.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		IResolvedBundle[] allBundles = definition.getAllBundles();
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
		IBundleContainer container = getTargetService().newDirectoryContainer(dirPath.toOSString());
		IBundleContainer container2 = getTargetService().newDirectoryContainer(dirPath.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container, container2});
		definition.resolve(null);
		IResolvedBundle[] allBundles = definition.getAllBundles();
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
		IBundleContainer container = getTargetService().newDirectoryContainer(dirPath.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		
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
		IBundleContainer container = getTargetService().newDirectoryContainer(dirPath.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		IResolvedBundle[] allBundles = definition.getAllBundles();
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
		IBundleContainer container = getTargetService().newProfileContainer(home, null);
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		IResolvedBundle[] bundles = definition.getAllBundles();
		
		int source = 0;
		int frag = 0;
		int bin = 0;
		
		for (int i = 0; i < bundles.length; i++) {
			IResolvedBundle bundle = bundles[i];
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
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		
		IBundleContainer featureContainer = getTargetService().newFeatureContainer(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt", null);
		
		IBundleContainer featureContainer2 = getTargetService().newFeatureContainer(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt.source", null);
	
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer, featureContainer, featureContainer2});
		definition.resolve(null);
		
		IResolvedBundle[] bundles = definition.getBundles();
		
		assertNotNull("Target didn't resolve",bundles);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.OK, definitionStatus.getSeverity());
		
		// Ensure that all source bundles know what they provide source for.
		for (int i = 0; i < bundles.length; i++) {
			IResolvedBundle bundle = bundles[i];
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
	 * Tests that resolved bundles know what their parent container is
	 * @throws Exception
	 */
	public void testGetParentContainer() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		
		IBundleContainer featureContainer = getTargetService().newFeatureContainer(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt", null);
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer, featureContainer});
		definition.resolve(null);
		
		IResolvedBundle[] bundles = definition.getBundles();
		
		assertNotNull("Target didn't resolve",bundles);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.OK, definitionStatus.getSeverity());
		
		bundles = directoryContainer.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			assertEquals("Wrong parent", directoryContainer, bundles[i].getParentContainer());
		}
		
		bundles = profileContainer.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			assertEquals("Wrong parent", profileContainer, bundles[i].getParentContainer());
		}
		
		bundles = featureContainer.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			assertEquals("Wrong parent", featureContainer, bundles[i].getParentContainer());
		}
	}
	
	/**
	 * Tests the two options on IU bundle containers for controlling how the site will be resolved
	 * @throws Exception
	 */
	public void testSiteContainerIncludeSettings() throws Exception{
		ITargetDefinition target = getNewTarget();
		IUBundleContainer containerA = (IUBundleContainer)getTargetService().newIUContainer(new IInstallableUnit[0], null);
		IUBundleContainer containerB = (IUBundleContainer)getTargetService().newIUContainer(new String[]{"unit1", "unit2"}, new String[]{"1.0","2.0"}, null);
		target.setBundleContainers(new IBundleContainer[]{containerA, containerB});
		
		// Check default settings
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		
		// Check basic changes
		containerA.setIncludeAllRequired(false, null);
		containerA.setIncludeAllEnvironments(true, null);
		assertFalse(containerA.getIncludeAllRequired());
		assertTrue(containerA.getIncludeAllEnvironments());
		containerA.setIncludeAllEnvironments(false, null);
		containerA.setIncludeAllRequired(true, null);
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		
		// Check that all containers are updated in the target if target is passed as argument
		containerA.setIncludeAllRequired(false, target);
		assertFalse(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertFalse(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		containerB.setIncludeAllRequired(true, target);
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
		containerA.setIncludeAllEnvironments(true, target);
		assertTrue(containerA.getIncludeAllRequired());
		assertTrue(containerA.getIncludeAllEnvironments());
		assertTrue(containerB.getIncludeAllRequired());
		assertTrue(containerB.getIncludeAllEnvironments());
		containerB.setIncludeAllEnvironments(false, target);
		assertTrue(containerA.getIncludeAllRequired());
		assertFalse(containerA.getIncludeAllEnvironments());
		assertTrue(containerB.getIncludeAllRequired());
		assertFalse(containerB.getIncludeAllEnvironments());
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