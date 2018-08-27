/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.natures.PDE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Creates the {@link IJavaProject} used for testing in the target workspace
 *
 * @since 1.0.0
 */
public class ProjectCreationTests extends AbstractApiTest {

	/**
	 * The source directory for the javadoc updating test source
	 */
	private static String JAVADOC_SRC_DIR = null;
	/**
	 * The source directory for the javadoc reading test source
	 */
	private static String JAVADOC_READ_SRC_DIR = null;

	static {
		JAVADOC_SRC_DIR = getSourceDirectory("javadoc"); //$NON-NLS-1$
		JAVADOC_READ_SRC_DIR = getSourceDirectory(new Path("a").append("b").append("c")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Before
	public void setUp() throws Exception {
		createProject(TESTING_PROJECT_NAME, null);
		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		assertNotNull("The java project must have been created", project); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		deleteProject(TESTING_PROJECT_NAME);
	}

	/**
	 * Tests importing the java source for the Javadoc tag update tests
	 *
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws JavaModelException
	 */
	@Test
	public void testImportJavadocTestSource() throws InvocationTargetException, IOException, JavaModelException {
		File dest = new File(JAVADOC_SRC_DIR);
		assertTrue("the source dir must exist", dest.exists()); //$NON-NLS-1$
		assertTrue("the source dir must be a directory", dest.isDirectory()); //$NON-NLS-1$
		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		IPackageFragmentRoot srcroot = project.getPackageFragmentRoot(ProjectUtils.SRC_FOLDER);
		assertNotNull("the srcroot for the test java project must not be null", srcroot); //$NON-NLS-1$
		FileUtils.importFilesFromDirectory(dest, project.getPath().append(srcroot.getPath()).append("javadoc"), //$NON-NLS-1$
				new NullProgressMonitor());
		// try to look up a file to test if it worked
		IType type = project.findType("javadoc.JavadocTestClass1", new NullProgressMonitor()); //$NON-NLS-1$
		assertNotNull("the JavadocTestClass1 type should exist in the javadoc package", type); //$NON-NLS-1$
	}

	/**
	 * Tests importing the java source for the javadoc tag reading tests
	 *
	 * @throws IOException
	 * @throws InvocationTargetException
	 */
	@Test
	public void testImportClassesTestSource() throws InvocationTargetException, IOException {
		File dest = new File(JAVADOC_READ_SRC_DIR);
		assertTrue("the source dir must exist", dest.exists()); //$NON-NLS-1$
		assertTrue("the source dir must be a directory", dest.isDirectory()); //$NON-NLS-1$
		IJavaProject project = getTestingJavaProject(TESTING_PROJECT_NAME);
		IPackageFragmentRoot srcroot = project.getPackageFragmentRoot(ProjectUtils.SRC_FOLDER);
		assertNotNull("the srcroot for the test java project must not be null", srcroot); //$NON-NLS-1$
		FileUtils.importFilesFromDirectory(dest,
				project.getPath().append(srcroot.getPath()).append("a").append("b").append("c"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new NullProgressMonitor());
	}

	/**
	 * Tests the creation of a plugin project
	 *
	 * @throws CoreException
	 */
	@Test
	public void testCreatePluginProject() throws CoreException {
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		assertTrue("project must have the PDE nature", project.hasNature(PDE.PLUGIN_NATURE)); //$NON-NLS-1$
		assertTrue("project must have the java nature", project.hasNature(JavaCore.NATURE_ID)); //$NON-NLS-1$
		assertTrue("project must have additional nature for API Tools", project.hasNature(ApiPlugin.NATURE_ID)); //$NON-NLS-1$
		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
		assertTrue("the build.properties file must exist", file.exists()); //$NON-NLS-1$
		file = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		assertTrue("the MANIFEST.MF file must exist", file.exists()); //$NON-NLS-1$
	}

	/**
	 * Finds the specified package export.
	 *
	 * @param exports export descriptions to search
	 * @param packageName what to search for
	 * @return package export description or <code>null</code>
	 */
	private IPackageExportDescription getExport(IPackageExportDescription[] exports, String packageName) {
		if (exports != null) {
			for (int i = 0; i < exports.length; i++) {
				IPackageExportDescription export = exports[i];
				if (export.getName().equals(packageName)) {
					return export;
				}
			}
		}
		return null;
	}

	/**
	 * Asserts the common values of an exported package object
	 *
	 * @param export the package description to test
	 * @param internalstate the desired state of the 'internal' directive
	 * @param friendcount the desired friend count
	 */
	private void assertExportedPackage(IPackageExportDescription export, boolean internalstate, int friendcount) {
		String packagename = export.getName();
		assertTrue("the package " + packagename + " must not be internal", export.isApi() == !internalstate); //$NON-NLS-1$ //$NON-NLS-2$
		if (friendcount == 0) {
			assertNull("the package should not have any friends", export.getFriends()); //$NON-NLS-1$
		} else {
			assertEquals("the package " + packagename + " must not have friends", friendcount, export.getFriends().length); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Tests adding an exported package to a plugin project
	 */
	@Test
	public void testAddRawExportedPackage() throws CoreException {
		String packagename = "org.eclipse.apitools.test"; //$NON-NLS-1$
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, packagename, false, null);
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, packagename), false, 0);
	}

	/**
	 * Tests adding an exported package that has the x-internal directive set
	 */
	@Test
	public void testAddInternalExportedPackage() throws CoreException {
		String packagename = "org.eclipse.apitools.test.internal"; //$NON-NLS-1$
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, packagename, true, null);
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, packagename), true, 0);
	}

	/**
	 * Tests adding an exported package with 4 friends (x-friends directive)
	 */
	@Test
	public void testAddExternalPackageWithFriends() throws CoreException {
		String packagename = "org.eclipse.apitools.test.4friends"; //$NON-NLS-1$
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, packagename, false, new String[] {
				"F1", "F2", "F3", "F4" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, packagename), true, 4);
	}

	/**
	 * Tests adding more than one exported package
	 */
	@Test
	public void testAddMultipleExportedPackages() throws CoreException {
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.multi.friends", false, new String[] { "F1", "F2", "F3", "F4" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.multi.internal", true, null); //$NON-NLS-1$
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.multi.friends"), true, 4); //$NON-NLS-1$
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.multi.internal"), true, 0); //$NON-NLS-1$
	}

	/**
	 * Tests removing an exported package
	 */
	@Test
	public void testRemoveExistingExportedPackage() throws CoreException {
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.remove1", false, new String[] { "F1" }); //$NON-NLS-1$ //$NON-NLS-2$
		ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.remove2", true, null); //$NON-NLS-1$
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.remove1"), true, 1); //$NON-NLS-1$
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.remove2"), true, 0); //$NON-NLS-1$
		ProjectUtils.removeExportedPackage(project, "org.eclipse.apitools.test.remove1"); //$NON-NLS-1$
		exports = ProjectUtils.getExportedPackages(project);
		assertNull("the package should have been removed from the header", getExport(exports, "org.eclipse.apitools.test.remove1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.remove2"), true, 0); //$NON-NLS-1$
	}

	/**
	 * Tests trying to remove a package that does not exist in the header
	 */
	@Test
	public void testRemoveNonExistingExportedPackage() throws CoreException {
		IJavaProject jproject = getTestingJavaProject(TESTING_PROJECT_NAME);
		IProject project = jproject.getProject();
		ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.removeA", false, new String[] { "F1" }); //$NON-NLS-1$ //$NON-NLS-2$
		IPackageExportDescription[] exports = ProjectUtils.getExportedPackages(project);
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.removeA"), true, 1); //$NON-NLS-1$
		ProjectUtils.removeExportedPackage(project, "org.eclipse.apitools.test.dont.exist"); //$NON-NLS-1$
		assertExportedPackage(getExport(exports, "org.eclipse.apitools.test.removeA"), true, 1); //$NON-NLS-1$
	}

	/**
	 * Returns the source path to load the test source files from into the
	 * testing project
	 *
	 * @param dirname the name of the directory the source is contained in
	 * @return the complete path of the source directory
	 */
	private static String getSourceDirectory(IPath dirname) {
		return TestSuiteHelper.getPluginDirectoryPath().append("test-source").append(dirname).toOSString(); //$NON-NLS-1$
	}

	/**
	 * Returns the source path to load the test source files from into the
	 * testing project
	 *
	 * @param dirname the name of the directory the source is contained in
	 * @return the complete path of the source directory
	 */
	private static String getSourceDirectory(String dirname) {
		return TestSuiteHelper.getPluginDirectoryPath().append("test-source").append(dirname).toOSString(); //$NON-NLS-1$
	}
}
