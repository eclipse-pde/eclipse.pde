/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      IBM Corporation - ongoing enhancements
 *      Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathresolver;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupDirector;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

/**
 * Tests {@link IBundleClasspathResolver} API to extend how the classpath and
 * source lookup path is created.
 *
 */
public class ClasspathResolverTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private static IProject project;

	/**
	 * The project name and bundle symbolic name of the test project
	 */
	public static final String bundleName = "classpathresolver";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		project = ProjectUtils.importTestProject("tests/projects/" + bundleName);
	}

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private class _PDESourceLookupQuery extends PDESourceLookupQuery {

		public _PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
			super(director, object);
		}

		@Override // Make super.getSourceContainers() visible
		public ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
			return super.getSourceContainers(location, id);
		}
	}

	/**
	 * Checks that a created dev properties file will recognise the modified
	 * classpath
	 */
	@Test
	public void testGetDevProperties() throws Exception {
		File devProperties = tempFolder.newFile("dev.properties").getCanonicalFile();
		String devPropertiesURL = ClasspathHelper.getDevEntriesProperties(devProperties.getPath(), false);

		Properties properties = loadProperties(devPropertiesURL);

		String expectedDevCP = project.getFolder("cpe").getLocation().toPortableString();
		assertEquals(expectedDevCP, properties.get(bundleName));
	}

	/**
	 * Checks that the source lookup path of a project is updated from the API
	 */
	@Test
	public void testSourceLookupPath() throws Exception {
		PDESourceLookupDirector d = new PDESourceLookupDirector();
		_PDESourceLookupQuery q = new _PDESourceLookupQuery(d, project);

		ISourceContainer[] containers = q.getSourceContainers(project.getLocation().toOSString(), bundleName);

		assertEquals(2, containers.length);
		assertEquals(JavaCore.create(project), ((JavaProjectSourceContainer) containers[0]).getJavaProject());
		assertEquals(project.getFolder("cpe").getLocation().toFile(),
				((DirectorySourceContainer) containers[1]).getDirectory());
	}

	// --- utility methods ---

	private static Properties loadProperties(String devPropertiesURL) throws IOException {
		File propertiesFile = new File(new URL(devPropertiesURL).getPath());
		Properties devProperties = new Properties();
		try (InputStream stream = new FileInputStream(propertiesFile)) {
			devProperties.load(stream);
		}
		return devProperties;
	}
}
