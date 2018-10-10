/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.build.properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tests that the builder for build.properties files generates the correct problems and quickfixes.
 *
 * @since 3.6
 * @see AbstractBuildValidationTest
 *
 */
public class BuildPropertiesValidationTest extends AbstractBuildValidationTest {

	private static boolean fOneTimeSetupComplete = false;

	@Override
	protected void setUp() throws Exception {
		if (fOneTimeSetupComplete)
			return;
		super.setUp();
		fOneTimeSetupComplete = true;
	}

	public void testSourceFolder() throws CoreException, BackingStoreException, IOException {
		for (int i = 1; i <= 5; i++) {
			IProject project = findProject("org.eclipse.pde.tests.build.properties." + i);
			setPreferences(project, CompilerFlags.ERROR);
			if (buildProject(project)) {
				IResource buildProperty = project.findMember("build.properties");
				PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

				verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
				verifyQuickFixes(buildProperty, expectedValues);
			} else {
				fail("Could not build the project '" + project.getName() + "'");
			}
		}
	}

	public void testJavacEntries() throws CoreException, BackingStoreException, IOException {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.6");
		setPreferences(project, CompilerFlags.WARNING);
		// Other preferences are set by a .settings file in the example project, see bug 334241

		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.WARNING);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	public void testJreCompliance() throws CoreException, BackingStoreException, IOException {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.7");
		setPreferences(project, CompilerFlags.ERROR);
		// Other preferences are set by a .settings file in the example project, see bug 334241

		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	public void testSimpleProject() throws CoreException, BackingStoreException, IOException {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.8");
		setPreferences(project, CompilerFlags.ERROR);
		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	//Bug 292763
	public void testSrcExcludeQuickFix() throws CoreException, BackingStoreException, IOException {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.9");
		setPreferences(project, CompilerFlags.ERROR);
		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	// Bug 323774
	public void testOsgiInf() throws Exception {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.10");
		setPreferences(project, CompilerFlags.ERROR);
		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	// Bug 540442
	public void testTestSource() throws CoreException, BackingStoreException, IOException {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.11");
		setPreferences(project, CompilerFlags.ERROR);
		if (buildProject(project)) {
			IResource buildProperty = project.findMember("build.properties");
			PropertyResourceBundle expectedValues = new PropertyResourceBundle(
					new FileInputStream(buildProperty.getLocation().toFile()));

			verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
			verifyQuickFixes(buildProperty, expectedValues);
		} else {
			fail("Could not build the project '" + project.getName() + "'");
		}
	}

	public void testIncrementalMarkers() throws Exception {
		IProject project = findProject("org.eclipse.pde.tests.build.properties.1");
		setPreferences(project, CompilerFlags.ERROR);
		if (!buildProject(project)) {
			fail("Could not build the project '" + project.getName() + "'");
		}

		IResource buildProperty = project.findMember("build.properties");
		IMarker[] initialMarkers = buildProperty.findMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		assertNotEquals(0, initialMarkers.length);

		if (!buildProject(project)) {
			fail("Could not build the project '" + project.getName() + "'");
		}

		IMarker[] markersAfterBuild = buildProperty.findMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		assertArrayEquals("validation should not have recreated unchanged markers", initialMarkers, markersAfterBuild);
	}
}
