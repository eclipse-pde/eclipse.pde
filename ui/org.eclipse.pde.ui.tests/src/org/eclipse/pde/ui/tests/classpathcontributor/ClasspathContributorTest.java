/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathcontributor;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;

/**
 * Tests {@link IClasspathContributor} API to add additional classpath 
 * entries during project classpath computation. Requires {@link TestClasspathContributor}
 * to be installed as an extension.
 */
public class ClasspathContributorTest extends TestCase {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for plugin classpath contributor API");
		suite.addTestSuite(ClasspathContributorTest.class);
		return suite;
	}
	
	private static final IProgressMonitor monitor = new NullProgressMonitor();
	private IWorkspace workspace = ResourcesPlugin.getWorkspace();
	private IProject project;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		project = importProject(workspace);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		project.delete(true, true, monitor);
	}
	
	public void testAdditionalClasspathEntries() throws Exception {
		List<IClasspathEntry> expected = new ArrayList<IClasspathEntry>(TestClasspathContributor.entries);
		expected.addAll(TestClasspathContributor.entries2);
		IJavaProject jProject = JavaCore.create(project);
		IClasspathContainer container = JavaCore.getClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, jProject);
		assertNotNull("Could not find PDE classpath container", container);
		IClasspathEntry[] classpath = container.getClasspathEntries();
		for (int i = 0; i < classpath.length; i++) {
			// Ignore the PDE Core bundle dependency
			if (!classpath[i].getPath().toPortableString().contains("org.eclipse.pde.core")){
				assertTrue("Unexpected classpath entry found: " + classpath[i], expected.remove(classpath[i]));
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

		URL srcURL = MacroPlugin.getBundleContext().getBundle ().getEntry("tests/projects/" + ClasspathResolverTest.bundleName);
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

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

		byte[] buf = new byte[10240];
		int len;
		while ((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
		}

		out.close();
		in.close();
	}

}
