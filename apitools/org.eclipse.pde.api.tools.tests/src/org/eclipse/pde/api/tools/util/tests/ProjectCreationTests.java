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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;

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
