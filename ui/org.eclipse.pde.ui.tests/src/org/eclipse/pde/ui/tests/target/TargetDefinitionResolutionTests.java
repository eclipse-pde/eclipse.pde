package org.eclipse.pde.ui.tests.target;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.impl.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.target.provisional.*;

public class TargetDefinitionResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionResolutionTests.class);
	}
	
	public void testMissingBundles() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		directoryContainer.setIncludedBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		profileContainer.setIncludedBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer});
		definition.resolve(null);
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		assertEquals("Wrong number of included bundles", 4, definition.getBundles().length);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] containerStatuses = definitionStatus.getChildren();
		assertEquals("Wrong number of container status", 2, containerStatuses.length);
		for (int i = 0; i < containerStatuses.length; i++) {
			IStatus[] children = containerStatuses[i].getChildren();
			assertEquals("Wrong number of statuses", 2, children.length);
			assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
			assertEquals(IResolvedBundle.STATUS_DOES_NOT_EXIST, children[0].getCode());
			assertEquals("Wrong severity", IStatus.ERROR, children[1].getSeverity());
			assertEquals(IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, children[1].getCode());
		}
		
		// Check that removing the included bundles and resolving removes the errors.
		directoryContainer.setIncludedBundles(null);
		profileContainer.setIncludedBundles(null);

		assertTrue(definition.isResolved());
		assertTrue(definition.getBundleStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
	public void testMissingOptionalBundles() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		directoryContainer.setOptionalBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		profileContainer.setOptionalBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer});
		definition.resolve(null);
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.INFO, definitionStatus.getSeverity());

		IStatus[] containerStatuses = definitionStatus.getChildren();
		assertEquals("Wrong number of container status", 2, containerStatuses.length);
		for (int i = 0; i < containerStatuses.length; i++) {
			IStatus[] children = containerStatuses[i].getChildren();
			assertEquals("Wrong number of statuses", 2, children.length);
			assertEquals("Wrong severity", IStatus.INFO, children[0].getSeverity());
			assertEquals(IResolvedBundle.STATUS_DOES_NOT_EXIST, children[0].getCode());
			assertEquals("Wrong severity", IStatus.INFO, children[1].getSeverity());
			assertEquals(IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, children[1].getCode());
		}
		
		// Check that removing the optional bundles and resolving removes the errors.
		directoryContainer.setOptionalBundles(null);
		profileContainer.setOptionalBundles(null);
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
		includesContainer.setIncludedBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		includesContainer.setOptionalBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		assertFalse(includesContainer.isResolved());
		assertNull("Bundles available when unresolved", includesContainer.getBundles());
		status = includesContainer.resolve(definition, null);
		assertTrue(includesContainer.isResolved());
		assertNotNull("Bundles not available when resolved", includesContainer.getBundles());
		assertEquals("Incorrect Severity", IStatus.OK, status.getSeverity());
		assertEquals("Incorrect Severity", IStatus.ERROR, includesContainer.getBundleStatus().getSeverity());
		definition.setBundleContainers(new IBundleContainer[]{brokenContainer, profileContainer});
		assertTrue(definition.isResolved());
		assertNotNull("Bundles not available when resolved", definition.getBundles());
		
		// Setting includes, optional, etc. should not unresolve the target
		includesContainer.setIncludedBundles(null);
		includesContainer.setOptionalBundles(null);
		assertTrue(definition.isResolved());
		profileContainer.setIncludedBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		profileContainer.setOptionalBundles(new BundleInfo[]{new BundleInfo("bogus",null,null,BundleInfo.NO_LEVEL,false),new BundleInfo("org.eclipse.platform","666.666.666",null,BundleInfo.NO_LEVEL,false)});
		assertTrue(definition.isResolved());
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());
		BundleInfo[] implicit = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
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
			IStatus status = getTargetService().compareWithTargetPlatform(definition, null);
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
			IStatus status = getTargetService().compareWithTargetPlatform(copy, null);
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
			
			IStatus status = getTargetService().compareWithTargetPlatform(copy, null);
			assertFalse(status.isOK());
			IStatus[] children = status.getChildren();
			assertEquals(1, children.length);
			assertEquals(ITargetPlatformService.STATUS_MISSING_FROM_TARGET_PLATFORM, children[0].getCode());
			assertEquals("bundle.a", children[0].getMessage());
		} finally {
			resetTargetPlatform();
		}
	}	
	
}