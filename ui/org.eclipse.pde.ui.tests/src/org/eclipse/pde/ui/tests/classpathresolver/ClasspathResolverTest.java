/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathresolver;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import junit.framework.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.pde.core.IBundleClasspathResolver;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupDirector;
import org.eclipse.pde.internal.launching.sourcelookup.PDESourceLookupQuery;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;

/**
 * Tests {@link IBundleClasspathResolver} API to extend how the classpath and source lookup path
 * is created.
 *
 */
public class ClasspathResolverTest extends TestCase {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for bundle classpath resolver API");
		suite.addTestSuite(ClasspathResolverTest.class);
		return suite;
	}

	private static final IProgressMonitor monitor = new NullProgressMonitor();

	private IWorkspace workspace = ResourcesPlugin.getWorkspace();

	private IProject project;

	protected void setUp() throws Exception {
		project = importProject(workspace);
	}

	protected void tearDown() throws Exception {
		project.delete(true, true, monitor);
	}

	private class _PDESourceLookupQuery extends PDESourceLookupQuery {

		public _PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
			super(director, object);
		}

		public ISourceContainer[] getSourceContainers(String location, String id) throws CoreException {
			return super.getSourceContainers(location, id);
		}
	}

	/**
	 * Checks that a created dev properties file will recognise the modified classpath
	 * @throws Exception
	 */
	public void testDevProperties() throws Exception {
		File devProperties = File.createTempFile("dev", ".properties");
		ClasspathHelper.getDevEntriesProperties(devProperties.getCanonicalPath(), false);

		Properties properties = new Properties();
		InputStream is = new FileInputStream(devProperties);
		try {
			properties.load(is);
		} finally {
			is.close();
		}

		assertEquals(project.getFolder("cpe").getLocation().toPortableString(), properties.get("classpathresolver"));
	}

	/**
	 * Checks that the source lookup path of a project is updated from the API
	 * @throws Exception
	 */
	public void testSourceLookupPath() throws Exception {
		PDESourceLookupDirector d = new PDESourceLookupDirector();
		_PDESourceLookupQuery q = new _PDESourceLookupQuery(d, project);

		ISourceContainer[] containers = q.getSourceContainers(project.getLocation().toOSString(), "classpathresolver");

		assertEquals(2, containers.length);
		assertEquals(JavaCore.create(project), ((JavaProjectSourceContainer) containers[0]).getJavaProject());
		assertEquals(project.getFolder("cpe").getLocation().toFile(), ((DirectorySourceContainer) containers[1]).getDirectory());
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
		String prjName = "classpathresolver";
		File rootFile = workspace.getRoot().getLocation().toFile();

		URL srcURL = MacroPlugin.getBundleContext().getBundle().getEntry("tests/projects/classpathresolver");
		File srcBasedir = new File(FileLocator.toFileURL(srcURL).getFile());
		
		File dstBasedir = new File(rootFile, prjName);
		copyFile(srcBasedir, dstBasedir, ".project");
		copyFile(srcBasedir, dstBasedir, ".classpath");
		copyFile(srcBasedir, dstBasedir, "build.properties");
		copyFile(srcBasedir, dstBasedir, "META-INF/MANIFEST.MF");
		copyFile(srcBasedir, dstBasedir, "cpe/some.properties");
		IProject project = workspace.getRoot().getProject(prjName);
		IProjectDescription description = workspace.newProjectDescription(prjName);
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
