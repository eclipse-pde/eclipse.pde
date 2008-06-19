/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;

/**
 * Creates the {@link IJavaProject} used for testing in the target workspace
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
		JAVADOC_SRC_DIR = getSourceDirectory("javadoc");
		JAVADOC_READ_SRC_DIR = getSourceDirectory(new Path("a").append("b").append("c"));
	}
	
	private static IJavaProject project = null;
	private static IPackageFragmentRoot srcroot = null;
	
	/**
	 * Tests creating a new {@link IJavaProject} for the plugin test suite
	 * 
     * @throws Exception
     */
    public void testProjectCreation() throws Exception {
        // delete any pre-existing project
        IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(TESTING_PROJECT_NAME);
        if (pro.exists()) {
            pro.delete(true, true, null);
        }
        // create project and import source
        project = ProjectUtils.createJavaProject(TESTING_PROJECT_NAME, null);
        assertNotNull("The java project must have been created", project);
        srcroot = ProjectUtils.addSourceContainer(project, ProjectUtils.SRC_FOLDER);
        assertNotNull("the src root must have been created", srcroot);

        // add rt.jar
        IVMInstall vm = JavaRuntime.getDefaultVMInstall();
        assertNotNull("No default JRE", vm);
        ProjectUtils.addContainerEntry(project, new Path(JavaRuntime.JRE_CONTAINER));
    }

    /**
     * Tests importing the java source for the Javadoc tag update tests
     */
    public void testImportJavadocTestSource() {
    	try {
    		File dest = new File(JAVADOC_SRC_DIR);
    		assertTrue("the source dir must exist", dest.exists());
    		assertTrue("the source dir must be a directory", dest.isDirectory());
    		assertNotNull("the srcroot for the test java project must not be null", srcroot);
    		FileUtils.importFilesFromDirectory(dest, srcroot.getPath().append("javadoc"), new NullProgressMonitor());
    		//try to look up a file to test if it worked
    		IType type = project.findType("javadoc.JavadocTestClass1", new NullProgressMonitor());
    		assertNotNull("the JavadocTestClass1 type should exist in the javadoc package", type);
    	}
    	catch (Exception e) {
    		fail(e.getMessage());
		}
    }
    
    /**
     * Tests importing the java source for the Javadoc tag update tests to compare
     * against. These source files are copies of originals prior to tag updating used for verification
     * that tags have been updated correctly.
     */
    public void testImportJavadocTestSourceOriginal() {
    	try {
    		File dest = new File(JAVADOC_SRC_DIR);
    		assertTrue("the original source dir must exist", dest.exists());
    		assertTrue("the original source dir must be a directory", dest.isDirectory());
    		assertNotNull("the srcroot for the test java project must not be null", srcroot);
    		FileUtils.importFilesFromDirectory(dest, srcroot.getPath().append("javadoc").append("orig"), new NullProgressMonitor());
    		//try to look up a file to test if it worked
    		IType type = project.findType("javadoc.JavadocTestClass1", new NullProgressMonitor());
    		assertNotNull("the JavadocTestClass1 type should exist in the javadoc package", type);
    	}
    	catch (Exception e) {
    		fail(e.getMessage());
		}
    }
    
    /**
     * Tests importing the java source for the javadoc tag reading tests
     */
    public void testImportClassesTestSource() {
    	try {
    		File dest = new File(JAVADOC_READ_SRC_DIR);
    		assertTrue("the source dir must exist", dest.exists());
    		assertTrue("the source dir must be a directory", dest.isDirectory());
    		assertNotNull("the srcroot for the test java project must not be null", srcroot);
    		FileUtils.importFilesFromDirectory(dest, srcroot.getPath().append("a").append("b").append("c"), new NullProgressMonitor());
    	}
    	catch (Exception e) {
    		fail(e.getMessage());
		}
    }
    
    /**
     * Tests the creation of a plugin project
     */
    public void testCreatePluginProject() {
    	try {
    		IJavaProject jproject = createPluginProject("test_plugin_project");
    		IProject project = jproject.getProject();
    		assertTrue("project must have the PDE nature", project.hasNature(PDE.PLUGIN_NATURE));
    		assertTrue("project must have the java nature", project.hasNature(JavaCore.NATURE_ID));
    		assertTrue("project must have additional nature for API tooling", project.hasNature(ApiPlugin.NATURE_ID));
    		IFile file = project.getFile("build.properties"); //$NON-NLS-1$
    		assertTrue("the build.properties file must exist", file.exists());
    		file = project.getFile(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
    		assertTrue("the MANIFEST.MF file must exist", file.exists());
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Proxy to creating a plugin project, which deletes any existing projects with the same name first
     * @param name
     * @return a new {@link IJavaProject} with the given name
     */
    private IJavaProject createPluginProject(String name) {
    	IJavaProject jproject = null;
    	try {
	    	// delete any pre-existing project
	        IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	        if (pro.exists()) {
	            pro.delete(true, true, new NullProgressMonitor());
	        }
	        jproject = ProjectUtils.createPluginProject(name, new String[] {ApiPlugin.NATURE_ID});
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    	return jproject;
    }
    
    /**
     * Asserts the common values of an exported package object
     * @param pack the package object to assert
     * @param internalstate the desired state of the 'internal' directive
     * @param friendcount the desired friend count
     */
    private void assertExportedPackage(ExportPackageObject pack, boolean internalstate, int friendcount) {
    	String packagename = pack.getName();
    	assertTrue("the package "+packagename+" must be in the header", pack != null);
		assertTrue("the package "+packagename+" must not be internal", pack.isInternal() == internalstate);
		assertTrue("the package "+packagename+" must not have any friends", pack.getFriends().length == friendcount);
    }
    
    /**
     * Tests adding an exported package to a plugin project
     */
    public void testAddRawExportedPackage() {
    	try {
    		String packagename = "org.eclipse.apitools.test";
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, packagename, false, null);
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage(packagename), false, 0);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Tests adding an exported package that has the x-internal directive set
     */
    public void testAddInternalExportedPackage() {
    	try {
    		String packagename = "org.eclipse.apitools.test.internal";
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, packagename, true, null);
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage(packagename), true, 0);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Tests adding an exported package with 4 friends (x-friends directive)
     */
    public void testAddExternalPackageWithFriends() {
    	try {
    		String packagename = "org.eclipse.apitools.test.4friends";
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, packagename, false, new String[] {"F1", "F2", "F3", "F4"});
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage(packagename), true, 4);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Tests adding more than one exported package
     */
    public void testAddMultipleExportedPackages() {
    	try {
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.multi.friends", false, new String[] {"F1", "F2", "F3", "F4"});
			ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.multi.internal", true, null);
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.multi.friends"), true, 4);
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.multi.internal"), true, 0);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Tests removing an exported package 
     */
    public void testRemoveExistingExportedPackage() {
    	try {
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.remove1", false, new String[] {"F1"});
			ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.remove2", true, null);
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.remove1"), true, 1);
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.remove2"), true, 0);
			ProjectUtils.removeExportedPackage(project, "org.eclipse.apitools.test.remove1");
			bundle = ProjectUtils.getBundle(project);
			header = ProjectUtils.getExportedPackageHeader(bundle);
			assertNull("the package should have been removed from the header", header.getPackage("org.eclipse.apitools.test.remove1"));
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.remove2"), true, 0);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Tests trying to remove a package that does not exist in the header
     */
    public void testRemoveNonExistingExportedPackage() {
    	try {
    		IJavaProject jproject = createPluginProject("test_plugin_project");
			IProject project = jproject.getProject();
			ProjectUtils.addExportedPackage(project, "org.eclipse.apitools.test.removeA", false, new String[] {"F1"});
			IBundle bundle = ProjectUtils.getBundle(project);
			assertNotNull("the bundle must not be null for the project", bundle);
			ExportPackageHeader header = ProjectUtils.getExportedPackageHeader(bundle);
			String value = header.getValue();
			assertNotNull("The export package header value must not be null", value);
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.removeA"), true, 1);
			ProjectUtils.removeExportedPackage(project, "org.eclipse.apitools.test.dont.exist");
			assertExportedPackage(header.getPackage("org.eclipse.apitools.test.removeA"), true, 1);
    	}
    	catch(Exception e) {
    		fail(e.getMessage());
    	}
    }
    
    /**
     * Returns the source path to load the test source files from into the testing project
     * @param dirname the name of the directory the source is contained in
     * @return the complete path of the source directory
     */
    private static String getSourceDirectory(IPath dirname) {
		return TestSuiteHelper.getPluginDirectoryPath().append("test-source").append(dirname).toOSString(); 
    }
    
    /**
     * Returns the source path to load the test source files from into the testing project
     * @param dirname the name of the directory the source is contained in
     * @return the complete path of the source directory
     */
    private static String getSourceDirectory(String dirname) {
		return TestSuiteHelper.getPluginDirectoryPath().append("test-source").append(dirname).toOSString(); 
    }
}
