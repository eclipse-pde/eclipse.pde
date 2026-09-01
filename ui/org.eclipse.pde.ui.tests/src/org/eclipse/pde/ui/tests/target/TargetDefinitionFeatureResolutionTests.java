/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.junit.jupiter.api.Test;

/**
 * Tests whether targets and bundle containers manage features correctly.
 *
 * @since 3.6
 */
public class TargetDefinitionFeatureResolutionTests extends AbstractTargetTest {

	/**
	 * Tests that a directory bundle container provides the correct features to
	 * a target
	 */
	@Test
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
			assertTrue(expectedIDs.contains(currentID), "Extra feature in result: " + currentID); //$NON-NLS-1$
			expectedIDs.remove(currentID);
		}

		assertTrue(expectedIDs.isEmpty(), "Not all expected features returned by the container: " + expectedIDs.toString()); //$NON-NLS-1$
	}

	/**
	 * Tests that a profile (installation) bundle container provides the correct
	 * features to a target
	 */
	@Test
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
			assertTrue(expectedIDs.contains(currentID), "Extra feature in result: " + currentID); //$NON-NLS-1$
			expectedIDs.remove(currentID);
		}

		assertTrue(expectedIDs.isEmpty(), "Not all expected features returned by the container: " + expectedIDs.toString()); //$NON-NLS-1$
	}

	@Test
	public void testExplicitIncludes() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
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
					assertTrue(status.isOK(), "Mac bundle should be present"); //$NON-NLS-1$
				} else {
					assertFalse(status.isOK(), "Mac bundle should be missing"); //$NON-NLS-1$
					assertEquals(TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, status.getCode(), "Mac bundle should be mssing"); //$NON-NLS-1$
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue(expected.isEmpty(), "Wrong bundles in JDT feature"); //$NON-NLS-1$

	}

	@Test
	public void testSingleInclude() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
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
					assertTrue(status.isOK(), "Mac bundle should be present"); //$NON-NLS-1$
				} else {
					assertFalse(status.isOK(), "Mac bundle should be missing"); //$NON-NLS-1$
					assertEquals(TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, status.getCode(), "Mac bundle should be missing"); //$NON-NLS-1$
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue(expected.isEmpty(), "Wrong bundles in JDT feature"); //$NON-NLS-1$
	}

	@Test
	public void testMixedIncludes() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
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
					assertTrue(status.isOK(), "Mac bundle should be present"); //$NON-NLS-1$
				} else {
					assertFalse(status.isOK(), "Mac bundle should be missing"); //$NON-NLS-1$
					assertEquals(TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, status.getCode(), "Mac bundle should be mssing"); //$NON-NLS-1$
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue(expected.isEmpty(), "Wrong bundles in JDT feature"); //$NON-NLS-1$
	}

	@Test
	public void testMissingFeatures() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE), };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		assertNotNull(bundles, "Target didn't resolve"); //$NON-NLS-1$
		assertEquals(1, bundles.length, "Wrong number of included bundles"); //$NON-NLS-1$

		IStatus definitionStatus = definition.getStatus();
		assertEquals(IStatus.ERROR, definitionStatus.getSeverity(), "Wrong severity"); //$NON-NLS-1$

		IStatus[] children = definitionStatus.getChildren();
		assertEquals(1, children.length, "Wrong number of statuses"); //$NON-NLS-1$
		assertEquals(IStatus.ERROR, children[0].getSeverity(), "Wrong severity"); //$NON-NLS-1$
		assertEquals(TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, children[0].getCode());

		// Check that removing the included bundles and resolving removes the
		// errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}

	@Test
	public void testMissingFeatureVersion() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
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
					assertTrue(status.isOK(), "Mac bundle should be present"); //$NON-NLS-1$
				} else {
					assertFalse(status.isOK(), "Mac bundle should be missing"); //$NON-NLS-1$
					assertEquals(TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, status.getCode(), "Mac bundle should be mssing"); //$NON-NLS-1$
				}
			}
		}
		for (String name : expected) {
			System.err.println("Missing: " + name);
		}
		assertTrue(expected.isEmpty(), "Wrong bundles in JDT feature"); //$NON-NLS-1$
	}

	@Test
	public void testMissingMixed() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		Path location = extractModifiedFeatures();

		ITargetLocation container = getTargetService().newDirectoryLocation(location.toString());
		definition.setTargetLocations(new ITargetLocation[] { container });
		definition.resolve(null);

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[] {
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_PLUGIN),
				new NameVersionDescriptor("org.eclipse.jdt", "DOES_NOT_EXIST", NameVersionDescriptor.TYPE_PLUGIN), };
		definition.setIncluded(allFeatures);
		TargetBundle[] bundles = definition.getBundles();

		assertNotNull(bundles, "Target didn't resolve"); //$NON-NLS-1$
		assertEquals(1, bundles.length, "Wrong number of included bundles"); //$NON-NLS-1$

		IStatus definitionStatus = definition.getStatus();
		assertEquals(IStatus.ERROR, definitionStatus.getSeverity(), "Wrong severity"); //$NON-NLS-1$

		IStatus[] children = definitionStatus.getChildren();
		assertEquals(1, children.length, "Wrong number of statuses"); //$NON-NLS-1$
		assertEquals(IStatus.ERROR, children[0].getSeverity(), "Wrong severity"); //$NON-NLS-1$
		assertEquals(TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, children[0].getCode());

		// Check that removing the included bundles and resolving removes the
		// errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}

	/**
	 * Tests that a feature bundle container provides the correct features to a target
	 */
	@Test
	public void testFeatureBundleContainer() throws Exception {
		PDETestCase.assumeRunningInStandaloneEclipseSDK();

		ITargetDefinition definition = getNewTarget();
		ITargetLocation featureContainer = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(), "org.eclipse.pde", null);

		assertNull(featureContainer.getFeatures());

		List<IFeatureModel> possibleFeatures = PDECore.getDefault().getFeatureModelManager().findFeatureModels("org.eclipse.pde");
		assertFalse(possibleFeatures.isEmpty());

		featureContainer.resolve(definition, null);
		TargetFeature[] features = featureContainer.getFeatures();
		assertNotNull(features);
		assertEquals(features.length, 1);
		assertEquals(features[0].getId(), possibleFeatures.get(0).getFeature().getId());
	}

}