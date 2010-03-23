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

import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.provisional.*;

/**
 * Tests whether targets and bundle containers manage features correctly.
 * 
 * @since 3.6
 */
public class TargetDefinitionFeatureResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionFeatureResolutionTests.class);
	}
	
	/**
	 * Tests that a directory bundle container provides the correct features to a target 
	 */
	public void testDirectoryBundleContainer() throws Exception{
		ITargetDefinition definition = getNewTarget();
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation());
		
		assertNull(directoryContainer.getFeatures());
		
		IFeatureModel[] expectedFeatures = PDECore.getDefault().getFeatureModelManager().getModels();
		Set expectedIDs = new HashSet();
		for (int i = 0; i < expectedFeatures.length; i++) {
			expectedIDs.add(expectedFeatures[i].getFeature().getId());
		}
				
		directoryContainer.resolve(definition, null);
		IFeatureModel[] features = directoryContainer.getFeatures();
		assertNotNull(features);
		
		for (int i = 0; i < features.length; i++) {
			String currentID = features[i].getFeature().getId();
			assertTrue("Extra feature in result: " + currentID, expectedIDs.contains(currentID));
			expectedIDs.remove(currentID);
		}
		
		assertTrue("Not all expected features returned by the container: " + expectedIDs.toString(), expectedIDs.isEmpty());
	}
	
	/**
	 * Tests that a profile (installation) bundle container provides the correct features to a target 
	 */
	public void testProfileBundleContainer() throws Exception{
		ITargetDefinition definition = getNewTarget();
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		
		assertNull(profileContainer.getFeatures());
		
		IFeatureModel[] expectedFeatures = PDECore.getDefault().getFeatureModelManager().getModels();
		Set expectedIDs = new HashSet();
		for (int i = 0; i < expectedFeatures.length; i++) {
			expectedIDs.add(expectedFeatures[i].getFeature().getId());
		}
				
		profileContainer.resolve(definition, null);
		IFeatureModel[] features = profileContainer.getFeatures();
		assertNotNull(features);
		
		for (int i = 0; i < features.length; i++) {
			String currentID = features[i].getFeature().getId();
			assertTrue("Extra feature in result: " + currentID, expectedIDs.contains(currentID));
			expectedIDs.remove(currentID);
		}
		
		assertTrue("Not all expected features returned by the container: " + expectedIDs.toString(), expectedIDs.isEmpty());
	}
	
	/**
	 * Tests that a feature bundle container provides the correct features to a target 
	 */
	public void testFeatureBundleContainer() throws Exception{
		ITargetDefinition definition = getNewTarget();
		IBundleContainer featureContainer = getTargetService().newFeatureContainer(TargetPlatform.getDefaultLocation(), "org.eclipse.pde", null);
		
		assertNull(featureContainer.getFeatures());
		
		IFeatureModel[] possibleFeatures = PDECore.getDefault().getFeatureModelManager().findFeatureModels("org.eclipse.pde");
		assertTrue(possibleFeatures.length > 0);
				
		featureContainer.resolve(definition, null);
		IFeatureModel[] features = featureContainer.getFeatures();
		assertNotNull(features);
		assertEquals(features.length, 1);
		assertEquals(features[0].getFeature().getId(),possibleFeatures[0].getFeature().getId());
	}
	
	public void testExplicitIncludes() throws Exception{
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		
		List expected = new ArrayList();
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
		
		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt", "3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1", NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("org.eclipse.jdt.source", "3.6.0.v20100105-0800-7z8VFR9FMTb52_pOyKHhoek1", NameVersionDescriptor.TYPE_FEATURE)
			};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		for (int i = 0; i < bundles.length; i++) {
			String symbolicName = bundles[i].getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundles[i].getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", IResolvedBundle.STATUS_DOES_NOT_EXIST, status.getCode());
				}
			}
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
		
	}
	
	public void testSingleInclude() throws Exception{
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		
		List expected = new ArrayList();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}
		
		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_FEATURE)
		};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		for (int i = 0; i < bundles.length; i++) {
			String symbolicName = bundles[i].getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundles[i].getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", IResolvedBundle.STATUS_DOES_NOT_EXIST, status.getCode());
				}
			}
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}
	
	public void testMixedIncludes() throws Exception{
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		
		List expected = new ArrayList();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}
		
		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("org.eclipse.jdt", null, NameVersionDescriptor.TYPE_PLUGIN)
		};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		for (int i = 0; i < bundles.length; i++) {
			String symbolicName = bundles[i].getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundles[i].getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", IResolvedBundle.STATUS_DOES_NOT_EXIST, status.getCode());
				}
			}
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}
	
	public void testMissingFeatures() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		
		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE),
		};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		assertNotNull("Target didn't resolve",definition.getBundles());
		assertEquals("Wrong number of included bundles", 1, definition.getBundles().length);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 1, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(IResolvedBundle.STATUS_DOES_NOT_EXIST, children[0].getCode());
		
		// Check that removing the included bundles and resolving removes the errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getBundleStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
	public void testMissingFeatureVersion() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);
		
		List expected = new ArrayList();
		expected.add("org.eclipse.jdt");
		expected.add("org.eclipse.jdt.launching");
		// 2 versions of JUnit
		expected.add("org.junit");
		expected.add("org.junit");
		expected.add("org.junit4");
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			expected.add("org.eclipse.jdt.launching.macosx");
		}
		
		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("org.eclipse.jdt", "DOES_NOT_EXIST", NameVersionDescriptor.TYPE_FEATURE)
		};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		for (int i = 0; i < bundles.length; i++) {
			String symbolicName = bundles[i].getBundleInfo().getSymbolicName();
			expected.remove(symbolicName);
			if (symbolicName.equals("org.eclipse.jdt.launching.macosx")) {
				// the bundle should be missing unless on Mac
				IStatus status = bundles[i].getStatus();
				if (Platform.getOS().equals(Platform.OS_MACOSX)) {
					assertTrue("Mac bundle should be present", status.isOK());
				} else {
					assertFalse("Mac bundle should be missing", status.isOK());
					assertEquals("Mac bundle should be mssing", IResolvedBundle.STATUS_DOES_NOT_EXIST, status.getCode());
				}
			}
		}
		Iterator iterator = expected.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			System.err.println("Missing: " + name);
		}
		assertTrue("Wrong bundles in JDT feature", expected.isEmpty());
	}
	
	public void testMissingMixed() throws Exception {
		// Use the modified JDT features as we know their versions
		ITargetDefinition definition = getNewTarget();
		IPath location = extractModifiedFeatures();
		
		IBundleContainer container = getTargetService().newDirectoryContainer(location.toOSString());
		definition.setBundleContainers(new IBundleContainer[]{container});
		definition.resolve(null);

		NameVersionDescriptor[] allFeatures = new NameVersionDescriptor[]{
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_FEATURE),
				new NameVersionDescriptor("DOES_NOT_EXIST", null, NameVersionDescriptor.TYPE_PLUGIN),
				new NameVersionDescriptor("org.eclipse.jdt", "DOES_NOT_EXIST", NameVersionDescriptor.TYPE_PLUGIN),
		};
		definition.setIncluded(allFeatures);
		IResolvedBundle[] bundles = definition.getBundles();
		
		assertNotNull("Target didn't resolve",bundles);
		assertEquals("Wrong number of included bundles", 1, bundles.length);
		
		IStatus definitionStatus = definition.getBundleStatus();
		assertEquals("Wrong severity", IStatus.ERROR, definitionStatus.getSeverity());

		IStatus[] children = definitionStatus.getChildren();
		assertEquals("Wrong number of statuses", 1, children.length);
		assertEquals("Wrong severity", IStatus.ERROR, children[0].getSeverity());
		assertEquals(IResolvedBundle.STATUS_DOES_NOT_EXIST, children[0].getCode());
		
		// Check that removing the included bundles and resolving removes the errors.
		definition.setIncluded(null);
		assertTrue(definition.isResolved());
		assertTrue(definition.getBundleStatus().isOK());
		assertTrue(definition.getBundles().length > 4);
	}
	
}