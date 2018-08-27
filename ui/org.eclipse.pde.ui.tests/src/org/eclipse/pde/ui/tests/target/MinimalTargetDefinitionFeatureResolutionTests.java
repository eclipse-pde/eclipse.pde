/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Tests whether targets and bundle containers manage features correctly. Runs
 * on minimal bundles and not the whole eclipse SDK. This class is refactored
 * out of TargetDefinitionFeatureResolutionTests
 */
public class MinimalTargetDefinitionFeatureResolutionTests extends AbstractTargetTest {

	/**
	 * Tests that a directory bundle container provides the correct features to
	 * a target
	 */
	public void testDirectoryBundleContainer() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation directoryContainer = getTargetService()
				.newDirectoryLocation(TargetPlatform.getDefaultLocation());

		assertNull(directoryContainer.getFeatures());

		IFeatureModel[] expectedFeatures = PDECore.getDefault().getFeatureModelManager().getModels();
		Set<String> expectedIDs = new HashSet<>();
		for (IFeatureModel expectedFeature : expectedFeatures) {
			expectedIDs.add(expectedFeature.getFeature().getId());
		}

		directoryContainer.resolve(definition, null);
		TargetFeature[] features = directoryContainer.getFeatures();
		assertNotNull(features);

		for (TargetFeature feature : features) {
			String currentID = feature.getId();
			assertTrue("Extra feature in result: " + currentID, expectedIDs.contains(currentID));
			expectedIDs.remove(currentID);
		}

		assertTrue("Not all expected features returned by the container: " + expectedIDs.toString(),
				expectedIDs.isEmpty());
	}

	/**
	 * Tests that a profile (installation) bundle container provides the correct
	 * features to a target
	 */
	public void testProfileBundleContainer() throws Exception {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				null);

		assertNull(profileContainer.getFeatures());

		IFeatureModel[] expectedFeatures = PDECore.getDefault().getFeatureModelManager().getModels();
		Set<String> expectedIDs = new HashSet<>();
		for (IFeatureModel expectedFeature : expectedFeatures) {
			expectedIDs.add(expectedFeature.getFeature().getId());
		}

		profileContainer.resolve(definition, null);
		TargetFeature[] features = profileContainer.getFeatures();
		assertNotNull(features);

		for (TargetFeature feature : features) {
			String currentID = feature.getId();
			assertTrue("Extra feature in result: " + currentID, expectedIDs.contains(currentID));
			expectedIDs.remove(currentID);
		}

		assertTrue("Not all expected features returned by the container: " + expectedIDs.toString(),
				expectedIDs.isEmpty());
	}

	public void testExplicitIncludes() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		expected.add("org.eclipse.jdt.launching.source");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit.source");
		expected.add("org.junit");
		expected.add("org.junit.source");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
			expected.add("org.eclipse.jdt.launching.macosx.source");
		}

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt", "3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1",
						NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("org.eclipse.jdt.source", "3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1",
						NameVersionDescriptor.TYPE_FEATURE) };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		for (TargetBundle bundle : bundles) {
			String symbolicName = bundle.getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundle.getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST,
							status.getCode());
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());

	}

	public void testSingleInclude() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_FEATURE) };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		for (TargetBundle bundle : bundles) {
			String symbolicName = bundle.getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundle.getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be missing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST,
							status.getCode());
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}

	public void testMixedIncludes() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_PLUGIN) };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		for (TargetBundle bundle : bundles) {
			String symbolicName = bundle.getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundle.getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST,
							status.getCode());
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}

	public void testMissingFeatures() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE), };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		assertNotNull("Target didn't resolve", bundles);
		assertEquals("Wrong number of included bundles", 1, bundles.length);

		IStatus definitionStatus = definition.getStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 1, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, children[0].getCode());

		// Check that removing the included bundles and resolving removes the
		// errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}

	public void testMissingFeatureVersion() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		List<String> expected = new ArrayList<>();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt", "DOES_NOT_EXIST", NameVersionDescriptor.TYPE_FEATURE) };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		for (TargetBundle bundle : bundles) {
			String symbolicName = bundle.getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundle.getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST,
							status.getCode());
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}

	public void testMissingMixed() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toOSString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_PLUGIN),
				new NameVersionDescriptor("org.eclipse.jdt", "DOES_NOT_EXIST", NameVersionDescriptor.TYPE_PLUGIN), };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		assertNotNull("Target didn't resolve", bundles);
		assertEquals("Wrong number of included bundles", 1, bundles.length);

		IStatus definitionStatus = definition.getStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 1, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, children[0].getCode());

		// Check that removing the included bundles and resolving removes the
		// errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}

}