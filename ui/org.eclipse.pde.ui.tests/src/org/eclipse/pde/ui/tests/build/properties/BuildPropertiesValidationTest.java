/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.build.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tests that the builder for build.properties files generates the correct problems and quickfixes.
 * 
 * @since 3.6
 * @see AbstractBuildValidationTest
 *
 */
public class BuildPropertiesValidationTest extends AbstractBuildValidationTest {
	
	public static Test suite() {
		return new TestSuite(BuildPropertiesValidationTest.class);
	}

	private static boolean fOneTimeSetupComplete = false;

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
}
