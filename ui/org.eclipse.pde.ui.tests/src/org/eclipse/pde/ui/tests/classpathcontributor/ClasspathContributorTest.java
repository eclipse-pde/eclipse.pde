/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathcontributor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.junit.rules.TestRule;

/**
 * Tests {@link IClasspathContributor} API to add additional classpath
 * entries during project classpath computation. Requires {@link TestClasspathContributor}
 * to be installed as an extension.
 */
public class ClasspathContributorTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	@Test
	public void testAdditionalClasspathEntries() throws Exception {
		IProject project = ProjectUtils.importTestProject("tests/projects/" + ClasspathResolverTest.bundleName);
		List<IClasspathEntry> expected = new ArrayList<>(TestClasspathContributor.entries);
		expected.addAll(TestClasspathContributor.entries2);
		IJavaProject jProject = JavaCore.create(project);
		IClasspathContainer container = JavaCore.getClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, jProject);
		assertNotNull("Could not find PDE classpath container", container);
		IClasspathEntry[] classpath = container.getClasspathEntries();
		for (IClasspathEntry element : classpath) {
			if (!isPdeDependency(element)) {
				assertTrue("Unexpected classpath entry found: " + element, expected.remove(element));
			}
		}
		assertTrue("Expected classpath entry not found: " + Arrays.toString(expected.toArray()), expected.isEmpty());
	}

	private boolean isPdeDependency(IClasspathEntry element) {
		String portableString = element.getPath().toPortableString();
		if (portableString.indexOf("org.eclipse.pde.core") > -1) {
			// The PDE Core bundle dependency
			return true;
		}
		if (portableString.contains("org.osgi.annotation.versioning")) {
			// osgi versioning annotations
			return true;
		}
		if (portableString.contains("org.osgi.annotation.bundle")) {
			// osgi bundle annotations
			return true;
		}
		if (portableString.contains("org.osgi.service.component.annotations")) {
			// osgi ds annotations
			return true;
		}
		if (portableString.contains("org.osgi.service.metatype.annotations")) {
			// osgi configuration as code annotations
			return true;
		}
		return false;
	}
}
