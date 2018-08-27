/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

public class TargetDefinitionResolutionTests extends MinimalTargetDefinitionResolutionTests {

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
		for (TargetBundle bundle : bundles) {
			if (bundle.isSourceBundle()){
				BundleInfo info = bundle.getSourceTarget();
				assertNotNull("Missing source target for " + bundle,info);
			} else {
				assertNull(bundle.getSourceTarget());
			}
		}

		// Everything in the JDT feature has an equivalent named source bundle
		bundles = featureContainer2.getBundles();
		for (TargetBundle bundle : bundles) {
			if (bundle.getBundleInfo().getSymbolicName().indexOf("doc") == -1){
				assertTrue("Non-source bundle in source feature", bundle.isSourceBundle());
				assertEquals("Incorrect source target", bundle.getBundleInfo().getSymbolicName(),bundle.getSourceTarget().getSymbolicName()+".source");
			}
		}
	}

	/**
	 * Tests that if users have the old preference to append .ini VM arguments,
	 * target definitions are migrated properly with the arguments appended.
	 */
	@SuppressWarnings("deprecation")
	public void testVMArgumentsMigrationAppend() throws Exception {
		IEclipsePreferences store = InstanceScope.INSTANCE.getNode(PDECore.PLUGIN_ID);
		boolean original = store.getBoolean(ICoreConstants.VM_LAUNCHER_INI, false);
		store.putBoolean(ICoreConstants.VM_LAUNCHER_INI, true);
		String originalTarget = store.get(ICoreConstants.TARGET_MODE, "");
		store.put(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		try {
			ITargetDefinition target = ((TargetPlatformService) getTargetService()).newTargetFromPreferences();
			assertNotNull("No target was created from old preferences", target);
			String vmArguments = target.getVMArguments();
			String iniVmArgs = TargetPlatformHelper.getIniVMArgs();
			assertEquals(vmArguments, iniVmArgs);
		} finally {
			store.putBoolean(ICoreConstants.VM_LAUNCHER_INI, original);
			store.put(ICoreConstants.TARGET_MODE, originalTarget);
		}
	}

}