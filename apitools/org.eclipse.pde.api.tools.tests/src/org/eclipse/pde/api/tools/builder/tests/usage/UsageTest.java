/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.usage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests usage scanning in source
 * @since 1.0.0 
 */
public abstract class UsageTest extends ApiBuilderTest {

	protected static final String TESTING_PACKAGE = "x.y.z";
	protected static final String REPLACEMENT_PACKAGE = "x.y.z.replace";
	protected static final String REF_PROJECT_NAME = "refproject";
	protected static final String TESTING_PROJECT = "usagetests"; 
	protected static final String INNER_NAME1 = "inner";
	protected static final String OUTER_NAME = "outer";
	protected static final String INNER_NAME2 = "inner2";
	protected static final String OUTER_INAME = "Iouter";
	
	/**
	 * Constructor
	 */
	public UsageTest(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(true);
		enableVersionNumberOptions(false);
	}

	/**
	 * Performs actions in the {@link #setUp()} method
	 * @throws Exception
	 */
	protected void doSetup() throws Exception {
		IProject[] pjs = getEnv().getWorkspace().getRoot().getProjects();
		getEnv().setAutoBuilding(false);
		if(pjs.length == 0) {
			createExistingProjects("usageprojects", true, true, false);
		}
		else {
			//ensureCompliance(new String[] {"usagetests"});
			incrementalBuild();
		}
	}
	
	/**
	 * Makes sure the compliance for the project is what the test says it should be
	 * @param projectnames
	 */
	protected void ensureCompliance(String[] projectnames) {
		IJavaProject project = null;
		String compliance = null;
		for (int i = 0; i < projectnames.length; i++) {
			project = getEnv().getJavaProject(projectnames[i]);
			compliance = getTestCompliance();
			if(!compliance.equals(project.getOption(CompilerOptions.OPTION_Compliance, true))) {
				getEnv().setProjectCompliance(project, compliance);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("usage");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "usagetests";
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(UsageTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/**
	 * Returns the path into the {@link ApiBuilderTest#TEST_SOURCE_ROOT} location 
	 * with the given path ending
	 * @param path
	 * @return the {@link IPath} to {@link ApiBuilderTest#TEST_SOURCE_ROOT} with the given path appended
	 */
	protected IPath getTestSourcePath(String path) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("usageprojects").append(path);
	}
	
	protected IPath getReplacementSourcePath(String path) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(path).addFileExtension("java");
	}
	
	/**
	 * Deploys a usage test
	 * @param typename
	 * @param inc
	 */
	protected void deployTest(String typename, boolean inc) {
		deployUsageTest(TESTING_PACKAGE, 
				typename, 
				true, 
				inc, 
				true);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getEnv().setRevert(true);
		doSetup();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getEnv().setRevert(false);
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
	
	/**
	 * Deploys a standard API usage test with the test project being created and the given source is imported in the testing project
	 * into the given project.
	 * 
	 * This method assumes that the reference and testing project have been imported into the workspace already.
	 * 
	 * @param packagename
	 * @param sourcename
	 * @param expectingproblems
	 * @param buildtype the type of build to perform. One of:
	 * <ol>
	 * <li>IncrementalProjectBuilder#FULL_BUILD</li>
	 * <li>IncrementalProjectBuilder#INCREMENTAL_BUILD</li>
	 * <li>IncrementalProjectBuilder#CLEAN_BUILD</li>
	 * </ol>
	 * @param buildworkspace
	 */
	protected void deployUsageTest(String packagename, String sourcename, boolean expectingproblems, boolean incremental, boolean buildworkspace) {
		try {
			IPath typepath = getProjectRelativePath(packagename, sourcename);
			createWorkspaceFile(typepath, getReplacementSourcePath(sourcename));
			if(incremental) {
				incrementalBuild();
			}
			else {
				fullBuild();
			}
			expectingNoJDTProblems();
			ApiProblem[] problems = getEnv().getProblemsFor(typepath, null);
			assertProblems(problems);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	protected IPath getProjectRelativePath(String packagename, String sourcename) {
		return new Path(getTestingProjectName()).append(SRC_ROOT).append(packagename.replace('.', '/')).append(sourcename).addFileExtension("java");
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
				FieldUsageTests.class,
				Java5FieldUsageTests.class,
				MethodUsageTests.class,
				Java5MethodUsageTests.class,
				ConstructorUsageTests.class,
				ClassUsageTests.class,
				Java5ClassUsageTests.class,
				InterfaceUsageTests.class,
				UnusedApiProblemFilterTests.class,
				DependentUsageTests.class,
				FragmentUsageTests.class
		};
		return classes;
	}
}
