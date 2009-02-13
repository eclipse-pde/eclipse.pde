package org.eclipse.pde.ui.tests.target;

import org.eclipse.jdt.launching.JavaRuntime;

import junit.framework.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.ServiceReference;

public class TargetDefinitionResolutionTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionResolutionTests.class);
	}
	
	/**
	 * Returns the target platform service or <code>null</code> if none
	 * 
	 * @return target platform service
	 */
	protected ITargetPlatformService getTargetService() {
		ServiceReference reference = MacroPlugin.getBundleContext().getServiceReference(ITargetPlatformService.class.getName());
		assertNotNull("Missing target platform service", reference);
		if (reference == null)
			return null;
		return (ITargetPlatformService) MacroPlugin.getBundleContext().getService(reference);
	}
	
	public void testMissingBundles() throws Exception {
		ITargetDefinition definition = getTargetService().newTarget();
		
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
		ITargetDefinition definition = getTargetService().newTarget();
		
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
		ITargetDefinition definition = getTargetService().newTarget();
		
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
		ITargetDefinition definition = getTargetService().newTarget();
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
	
}