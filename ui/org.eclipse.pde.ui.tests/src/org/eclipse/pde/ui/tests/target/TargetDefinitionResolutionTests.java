/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
import java.io.FileWriter;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

public class TargetDefinitionResolutionTests extends MinimalTargetDefinitionResolutionTests {

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

		for (TargetBundle bundle : bundles) {
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
		File linkFile = new File(linkLocation, "test.link");
		try {
			linkLocation.mkdirs();
			linkFile.createNewFile();
			FileWriter writer = new FileWriter(linkFile);
			writer.write("path=" + extraPlugins.removeLastSegments(1).toPortableString());
			writer.flush();
			writer.close();

			ITargetDefinition definition = getNewTarget();
			String home = location.removeLastSegments(1).toOSString();
			ITargetLocation container = getTargetService().newProfileLocation(home, null);
			ITargetLocation container2 = getTargetService().newProfileLocation(linkLocation.getAbsolutePath(), null);
			definition.setTargetLocations(new ITargetLocation[]{container, container2});
			definition.resolve(null);
			TargetBundle[] bundles = definition.getAllBundles();

			int source = 0;
			int frag = 0;
			int bin = 0;

			for (TargetBundle bundle : bundles) {
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

			for (TargetBundle bundle : bundles) {
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

		} finally {
			// Important to delete the link files as they can affect other tests (Bug 381428)
			linkFile.delete();
			linkLocation.delete();
		}

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
		Preferences store = PDECore.getDefault().getPluginPreferences();
		boolean original = store.getBoolean(ICoreConstants.VM_LAUNCHER_INI);
		store.setValue(ICoreConstants.VM_LAUNCHER_INI, true);
		String originalTarget = store.getString(ICoreConstants.TARGET_MODE);
		store.setValue(ICoreConstants.TARGET_MODE, ICoreConstants.VALUE_USE_THIS);
		try {
			ITargetDefinition target = ((TargetPlatformService) getTargetService()).newTargetFromPreferences();
			assertNotNull("No target was created from old preferences", target);
			String vmArguments = target.getVMArguments();
			String iniVmArgs = TargetPlatformHelper.getIniVMArgs();
			assertEquals(vmArguments, iniVmArgs);
		} finally {
			store.setValue(ICoreConstants.VM_LAUNCHER_INI, original);
			store.setValue(ICoreConstants.TARGET_MODE, originalTarget);
		}
	}

}