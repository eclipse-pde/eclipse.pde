/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype, Inc. and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathresolver;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import junit.framework.TestCase;
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
import org.eclipse.pde.ui.tests.PDETestsPlugin;

/**
 * Tests {@link IBundleClasspathResolver} API to extend how the classpath and source lookup path
 * is created.
 *
 */
public class ClasspathResolverTest extends TestCase {

	private static final IProgressMonitor monitor = new NullProgressMonitor();

	private IWorkspace workspace = ResourcesPlugin.getWorkspace();

	private IProject project;

	/**
	 * The project name and bundle symbolic name of the test project
	 */
	public static final String bundleName = "classpathresolver";

	@Override
	protected void setUp() throws Exception {
		project = importProject(workspace);
	}

	@Override
	protected void tearDown() throws Exception {
		project.delete(true, true, monitor);
	}

	private class _PDESourceLookupQuery extends PDESourceLookupQuery {

		public _PDESourceLookupQuery(PDESourceLookupDirector director, Object object) {
			super(director, object);
		}

		@Override
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
		try (InputStream is = new FileInputStream(devProperties)) {
			properties.load(is);
		}

		assertEquals(project.getFolder("cpe").getLocation().toPortableString(), properties.get(bundleName));
	}

	/**
	 * Checks that the source lookup path of a project is updated from the API
	 * @throws Exception
	 */
	public void testSourceLookupPath() throws Exception {
		PDESourceLookupDirector d = new PDESourceLookupDirector();
		_PDESourceLookupQuery q = new _PDESourceLookupQuery(d, project);

		ISourceContainer[] containers = q.getSourceContainers(project.getLocation().toOSString(), bundleName);

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
		File rootFile = workspace.getRoot().getLocation().toFile();

		URL srcURL = PDETestsPlugin.getBundleContext().getBundle().getEntry("tests/projects/" + bundleName);
		File srcBasedir = new File(FileLocator.toFileURL(srcURL).getFile());

		File dstBasedir = new File(rootFile, bundleName);
		copyFile(srcBasedir, dstBasedir, ".project");
		copyFile(srcBasedir, dstBasedir, ".classpath");
		copyFile(srcBasedir, dstBasedir, "build.properties");
		copyFile(srcBasedir, dstBasedir, "META-INF/MANIFEST.MF");
		copyFile(srcBasedir, dstBasedir, "cpe/some.properties");
		IProject project = workspace.getRoot().getProject(bundleName);
		IProjectDescription description = workspace.newProjectDescription(bundleName);
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
