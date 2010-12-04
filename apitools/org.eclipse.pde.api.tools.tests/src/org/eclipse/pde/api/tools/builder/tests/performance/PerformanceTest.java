/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.performance;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.ApiTestsPlugin;

/**
 * Base class for performance tests
 * 
 * @since 1.0
 */
public abstract class PerformanceTest extends ApiBuilderTest {	

	/**
	 * Constructor
	 * @param name
	 */
	public PerformanceTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("perf");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(true);
		enableBaselineOptions(true);
		enableCompatibilityOptions(true);
		enableLeakOptions(true);
		enableSinceTagOptions(true);
		enableUsageOptions(true);
		enableVersionNumberOptions(true);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			FullSourceBuildTests.class,
			ApiDescriptionTests.class,
			IncrementalBuildTests.class,
			ExternalDependencyPerfTests.class
		};
		return classes;
	}
	
	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder tests...
		Class[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite", new Class[0]);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null, new Object[0]);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				continue;
			}
			suite.addTest((Test) test);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "dummy";
	}
	
	/* (non-Javadoc)
	 * 
	 * Ensure a baseline has been created to compare against.
	 * 
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// populate the workspace with initial plug-ins/projects
		createInitialWorkspace();
		createBaseline();
	}	
	
	/**
	 * Performs a clean and full build on the projects in the workspace computed ordering
	 * @throws CoreException
	 */
	protected void orderedBuild(IProject[] projects) throws CoreException {
		for (int i = 0; i < projects.length; i++) {
			projects[i].build(IncrementalProjectBuilder.CLEAN_BUILD, null);
			projects[i].build(IncrementalProjectBuilder.FULL_BUILD, null);
		}
	}
	
	/**
	 * Creates the API baseline for this test.
	 * 
	 * @throws Exception
	 */
	protected void createBaseline() throws Exception {
		String zipPath = getBaselineLocation();
		if (zipPath != null) {
			IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
			IPath path = new Path(zipPath);
			String id = path.lastSegment();
			IApiBaseline perfline = manager.getApiBaseline(id);
			if (perfline == null) {
				// create the API baseline
				IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append(id);
				long start = System.currentTimeMillis();
				System.out.println("Unzipping baseline: "+zipPath);
				System.out.print("	in "+baselineLocation.toOSString()+"...");
				Util.unzip(zipPath, baselineLocation.toOSString());
				System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");	
					
				perfline = ApiModelFactory.newApiBaseline(id);
				File[] files = baselineLocation.toFile().listFiles();
				IApiComponent[] components = new IApiComponent[files.length];
				for (int i = 0; i < files.length; i++) {
					IPath location = baselineLocation.append(files[i].getName());
					components[i] = ApiModelFactory.newApiComponent(perfline, location.toOSString());
				}
				perfline.addApiComponents(components);
				manager.addApiBaseline(perfline);
				System.out.println("Setting default baseline to be: "+perfline.getName());
				manager.setDefaultApiBaseline(perfline.getName());			
			}
			IApiBaseline baseline = manager.getDefaultApiBaseline();
			if (baseline != perfline) {
				manager.setDefaultApiBaseline(perfline.getName());
			}
		}
	}
	
	/**
	 * Creates the workspace by importing projects from the source zip. This is the 
	 * initial state of the workspace.
	 *  
	 * @throws Exception
	 */
	protected void createInitialWorkspace() throws Exception {
		// Get workspace info
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// Modify resources workspace preferences to avoid disturbing tests while running them
		IEclipsePreferences resourcesPreferences = new InstanceScope().getNode(ResourcesPlugin.PI_RESOURCES);
		resourcesPreferences.put(ResourcesPlugin.PREF_AUTO_REFRESH, "false");
		
		// do not show the dialog if a build fails...will lock the workspace
		IEclipsePreferences antuiprefs = new InstanceScope().getNode("org.eclipse.ant.ui");
		antuiprefs.put("errorDialog", "false");
		
		workspace.getDescription().setSnapshotInterval(Long.MAX_VALUE);
		workspace.getDescription().setAutoBuilding(false);

		
		
		// Get projects directories
		long start = System.currentTimeMillis();
		String fullSourceZipPath = getWorkspaceLocation();
		IPath path = new Path(fullSourceZipPath);
		String dirName = path.lastSegment();
		String fileExtension = path.getFileExtension();
		dirName = dirName.substring(0, dirName.length() - fileExtension.length());
		IPath location = ApiTestsPlugin.getDefault().getStateLocation().append(dirName);
		File dir = location.toFile();
		if (dir.exists()) {
			deleteDir(dir);
		}
		dir.mkdirs();
		String targetWorkspacePath = location.toOSString();
		System.out.println("Unzipping "+fullSourceZipPath);
		System.out.print("	in "+targetWorkspacePath+"...");
		Util.unzip(fullSourceZipPath, targetWorkspacePath);
		System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");
		
		start = System.currentTimeMillis();
		System.out.println("Importing projects... ");
		File root = dir;
		File[] projects = root.listFiles();
		for (int i = 0; i < projects.length; i++) {
			System.out.println("\t" + projects[i].getName());
			createExistingProject(projects[i], true, false);
		}
		System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");
	}		
	
	/**
	 * Recursively deletes directories and all files in it.
	 * 
	 * @param dir
	 */
	protected void deleteDir(File dir) {
		File[] listFiles = dir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			File file = listFiles[i];
			if (file.isDirectory()) {
				deleteDir(file);
			}
			file.delete();
		}
	}
	
	/**
	 * Returns the a string of the absolute path in the local file system to an archive of the
	 * API baseline to use for this test or <code>null</code> if none. Subclasses must override
	 * if they need a baseline.
	 * 
	 * @return absolute path in the local file system to an archive of the
	 * 	API baseline to use for this test or <code>null</code>
	 */
	protected String getBaselineLocation() {
		return null;
	}
	
	/**
	 * Returns the a string of the absolute path in the local file system to an archive of the
	 * source workspace to use for this test or <code>null</code> if none. Subclasses must override
	 * if they need to populate a workspace.
	 * 
	 * @return absolute path in the local file system to an archive of the
	 * 	source workspace to use for this test or <code>null</code>
	 */	
	protected String getWorkspaceLocation() {
		return null;
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(PerformanceTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is updated with a corresponding file from test data. A build is performed
	 * and problems are compared against the expected problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
			updateWorkspaceFile(
					workspaceFile,
					getUpdateFilePath(workspaceFile.lastSegment()));
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			ApiProblem[] problems = getEnv().getProblemsFor(workspaceFile, null);
			assertProblems(problems);
	}
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is updated with a corresponding file from test data. A build is performed
	 * and problems are compared against the expected problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performVersionTest(IPath workspaceFile, boolean incremental) throws Exception {
			updateWorkspaceFile(
					workspaceFile,
					getUpdateFilePath(workspaceFile.lastSegment()));
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			ApiProblem[] problems = getEnv().getProblemsFor(new Path(workspaceFile.segment(0)).append(JarFile.MANIFEST_NAME), null);
			assertProblems(problems);
	}	
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is deleted. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performDeletionCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
			deleteWorkspaceFile(workspaceFile, true);
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			ApiProblem[] problems = getEnv().getProblems();
			assertProblems(problems);
	}	
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is created. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCreationCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
		createWorkspaceFile(
				workspaceFile,
				getUpdateFilePath(workspaceFile.lastSegment()));
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblems();
		assertProblems(problems);
	}
}
