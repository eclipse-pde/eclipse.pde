/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.classpathcontributor;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;

/**
 * Tests {@link IClasspathContributor} API to add additional classpath
 * entries during project classpath computation. Requires {@link TestClasspathContributor}
 * to be installed as an extension.
 */
public class ClasspathContributorTest extends TestCase {

	private static final IProgressMonitor monitor = new NullProgressMonitor();
	private IWorkspace workspace = ResourcesPlugin.getWorkspace();
	private IProject project;

	@Override
	protected void setUp() throws Exception {
		project = importProject(workspace);
	}

	@Override
	protected void tearDown() throws Exception {
		project.delete(true, true, monitor);
	}

	public void testAdditionalClasspathEntries() throws Exception {
		List<IClasspathEntry> expected = new ArrayList<>(TestClasspathContributor.entries);
		expected.addAll(TestClasspathContributor.entries2);
		IJavaProject jProject = JavaCore.create(project);
		IClasspathContainer container = JavaCore.getClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, jProject);
		assertNotNull("Could not find PDE classpath container", container);
		IClasspathEntry[] classpath = container.getClasspathEntries();
		for (IClasspathEntry element : classpath) {
			// Ignore the PDE Core bundle dependency
			if (element.getPath().toPortableString().indexOf("org.eclipse.pde.core") == -1){
				assertTrue("Unexpected classpath entry found: " + element, expected.remove(element));
			}
		}
		assertTrue("Expected classpath entry not found: " + expected.toArray(), expected.isEmpty());
	}

	/**
	 * Imports a project into the test workspace
	 *
	 * @param workspace workspace to import into
	 * @return the created project
	 * @throws IOException
	 * @throws CoreException
	 */
	IProject importProject(IWorkspace workspace) throws IOException, CoreException {
		File rootFile = workspace.getRoot().getLocation().toFile();

		URL srcURL = PDETestsPlugin.getBundleContext().getBundle ().getEntry("tests/projects/" + ClasspathResolverTest.bundleName);
		File srcBasedir = new File(FileLocator.toFileURL(srcURL).getFile());

		File dstBasedir = new File(rootFile, ClasspathResolverTest.bundleName);
		copyFile(srcBasedir, dstBasedir, ".project");
		copyFile(srcBasedir, dstBasedir, ".classpath");
		copyFile(srcBasedir, dstBasedir, "build.properties");
		copyFile(srcBasedir, dstBasedir, "META-INF/MANIFEST.MF");
		copyFile(srcBasedir, dstBasedir, "cpe/some.properties");
		IProject project = workspace.getRoot().getProject(ClasspathResolverTest.bundleName);
		IProjectDescription description = workspace.newProjectDescription(ClasspathResolverTest.bundleName);
		project.create(description, monitor);
		project.open(monitor);
		return project;
	}

	private void copyFile(File srcBasedir, File dstBasedir, String file) throws IOException {
		copyFile(new File(srcBasedir, file), new File(dstBasedir, file));
	}

	// copy&paste from org.eclipse.m2e.tests.common.FileHelpers
	private void copyFile(File src, File dst) throws IOException {
		src.getParentFile().mkdirs();
		dst.getParentFile().mkdirs();

		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {

			byte[] buf = new byte[10240];
			int len;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}

		}
	}

}
