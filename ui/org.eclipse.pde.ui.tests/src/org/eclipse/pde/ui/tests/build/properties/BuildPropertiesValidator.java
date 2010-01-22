/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.build.properties;

import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.FileInputStream;
import java.util.PropertyResourceBundle;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.internal.core.builders.CompilerFlags;

public class BuildPropertiesValidator extends BaseValidator {

	
	public static Test suite() {
		return new TestSuite(BuildPropertiesValidator.class);
	}

	private static boolean fOneTimeSetupComplete = false;

	protected void setUp() throws Exception {
		if (fOneTimeSetupComplete)
			return;
		super.setUp();
		fOneTimeSetupComplete = true;
	}

	public void testBuildPropertiesOne() {
		try {
			IProject project = findProject("org.eclipse.pde.tests.build.properties.one");
			project.open(new NullProgressMonitor());
			setPreferences(project, CompilerFlags.ERROR);
			for (int i = 1; i <= 4; i++) {
				if (buildProject(project, i)) {
					IResource buildProperty = project.findMember("build.properties");
					PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

					verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
					verifyQuickFixes(buildProperty, expectedValues);
				} else {
					fail("Could not build the project '" + project.getName() + "'");
				}
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testBuildPropertiesTwo() {
		try {
			IProject project = findProject("org.eclipse.pde.tests.build.properties.two");
			project.open(new NullProgressMonitor());
			setPreferences(project, CompilerFlags.WARNING);
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_SOURCE, "1.3");
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.3");
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_COMPLIANCE, "1.5");

			if (buildProject(project, 1)) {
				IResource buildProperty = project.findMember("build.properties");
				PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

				verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.WARNING);
				verifyQuickFixes(buildProperty, expectedValues);
			} else {
				fail("Could not build the project '" + project.getName() + "'");
			}

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public void testBuildPropertiesTwo_JreCompliance() {
		try {
			IProject project = findProject("org.eclipse.pde.tests.build.properties.two");
			project.open(new NullProgressMonitor());
			setPreferences(project, CompilerFlags.ERROR);
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_SOURCE, "1.3");
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.2");
			setPreference(project, JavaCore.PLUGIN_ID, JavaCore.COMPILER_COMPLIANCE, "1.5");

			if (buildProject(project, 2)) {
				IResource buildProperty = project.findMember("build.properties");
				PropertyResourceBundle expectedValues = new PropertyResourceBundle(new FileInputStream(buildProperty.getLocation().toFile()));

				verifyBuildPropertiesMarkers(buildProperty, expectedValues, CompilerFlags.ERROR);
				verifyQuickFixes(buildProperty, expectedValues);
			} else {
				fail("Could not build the project '" + project.getName() + "'");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
