/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;

public class TargetDefinitionResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionResolutionTests.class);
	}
	
	public void testTargetBasicResolution() throws Exception {
		ITargetDefinition definition = getNewTarget();
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer});
		
		assertNull(definition.getResolveStatus());
		assertNull(definition.getAvailableUnits());
		assertNull(definition.getIncludedUnits());
		assertNull(definition.getProvisionStatus());
		assertNull(definition.getProvisionedBundles());
		
		IStatus status = definition.resolve(null);
		assertTrue("Resolve status: " + status,status.isOK());
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		assertEquals(definition.getIncludedUnits().length,definition.getAvailableUnits().length);
		assertNull(definition.getProvisionStatus());
		assertNull(definition.getProvisionedBundles());
		
		status = definition.provision(null);
		assertTrue("Resolve status: " + status,status.isOK());
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		assertEquals(definition.getIncludedUnits().length,definition.getAvailableUnits().length);
		assertTrue(definition.getProvisionStatus().isOK());
		assertEquals(definition.getAvailableUnits().length,definition.getProvisionedBundles().length);
		
		definition.setIncluded(new NameVersionDescriptor[]{new NameVersionDescriptor("org.eclipse.platform"), new NameVersionDescriptor("does.not.exist")});
		definition.setOptional(new NameVersionDescriptor[]{new NameVersionDescriptor("org.eclipse.platform"), new NameVersionDescriptor("does.not.exist")});
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		assertEquals(1,definition.getIncludedUnits().length);
		assertNull(definition.getProvisionStatus());
		assertNull(definition.getProvisionedBundles());
		
		status = definition.provision(null);
		assertTrue("Resolve status: " + status,status.isOK());
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		assertTrue(definition.getProvisionStatus().isOK());
		assertEquals(definition.getIncludedUnits().length,definition.getProvisionedBundles().length);
		
		assertTrue(definition.isResolved());
		assertTrue(definition.isProvisioned());
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());
		definition.setImplicitDependencies(new NameVersionDescriptor[]{new NameVersionDescriptor("org.eclipse.jdt.launching")});
		assertTrue(definition.isResolved());
		assertTrue(definition.isProvisioned());
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
		assertTrue(definition.isProvisioned());
		
		definition.setBundleContainers(null);
		assertFalse(definition.isResolved());
		assertNull("Bundles not available when resolved", definition.getAvailableUnits());
		assertNull("Bundles not available when resolved", definition.getIncludedUnits());
		assertNull("Bundles not available when resolved", definition.getProvisionedBundles());
	}
	
	public void testInvalidBundleContainers() throws Exception {
		ITargetDefinition definition = getNewTarget();
		
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer("***SHOULD NOT EXIST***");
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer});
		IStatus status = definition.resolve(null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		IBundleContainer profileContainer = getTargetService().newProfileContainer("***SHOULD NOT EXIST***", null);
		definition.setBundleContainers(new IBundleContainer[]{profileContainer});
		status = definition.resolve(null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		IBundleContainer featureContainer = getTargetService().newFeatureContainer("***SHOULD NOT EXIST***", "org.eclipse.jdt", "");
		definition.setBundleContainers(new IBundleContainer[]{featureContainer});
		status = definition.resolve(null);
		assertEquals("Incorrect severity", IStatus.ERROR, status.getSeverity());
		
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer, featureContainer});
		status = definition.resolve(null);
		assertTrue(status.isMultiStatus());
		IStatus[] children = status.getChildren();
		assertEquals(1,children.length);
		assertTrue(children[0].isMultiStatus());
		children = children[0].getChildren();
		assertEquals("Wrong number of children", 3, children.length);
		for (int i = 0; i < children.length; i++) {
			assertEquals("Incorrect severity", IStatus.ERROR, children[i].getSeverity());
			assertFalse("Failed resolution should be single status", children[i].isMultiStatus());
		}
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
		definition.provision(null);
		BundleInfo[] bundles = definition.getProvisionedBundles();
		assertEquals("Wrong number of bundles", 84, bundles.length);
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