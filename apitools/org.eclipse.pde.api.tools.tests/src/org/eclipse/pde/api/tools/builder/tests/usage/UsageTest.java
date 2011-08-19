/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
import java.util.ArrayList;

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
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;

/**
 * Tests usage scanning in source
 * @since 1.0.0 
 */
public abstract class UsageTest extends ApiBuilderTest {

	private static final String USAGE = "usage";
	protected static final String TESTING_PACKAGE = "x.y.z";
	protected static final String REPLACEMENT_PACKAGE = "x.y.z.replace";
	protected static final String REF_PROJECT_NAME = "refproject";
	protected static final String TESTING_PROJECT = "usagetests"; 
	protected static final String INNER_NAME1 = "inner";
	protected static final String OUTER_NAME = "outer";
	protected static final String INNER_NAME2 = "inner2";
	protected static final String OUTER_INAME = "Iouter";
	
	protected static IPath SOURCE_PATH = new Path("src/x/y/z");
	
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
		return new Path(USAGE);
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
	 * Deploys a standard API usage test with the test project being created and the given source is imported in the testing project
	 * into the given project.
	 * 
	 * This method assumes that the reference and testing project have been imported into the workspace already.
	 * 
	 * @param sourcename
	 * @param inc if an incremental build should be done
	 */
	protected void deployUsageTest(String typename, boolean inc) {
		try {
			IPath typepath = new Path(getTestingProjectName()).append(SOURCE_PATH).append(typename).addFileExtension("java");
			createWorkspaceFile(typepath, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(typename).addFileExtension("java"));
			if(inc) {
				incrementalBuild();
			}
			else {
				fullBuild();
			}
			expectingNoJDTProblemsFor(typepath);
			ApiProblem[] problems = getEnv().getProblemsFor(typepath, null);
			assertProblems(problems);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		// If we have an existing environment, set it to revert rather than delete the workspace to improve performance
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
			env.setRevertSourcePath(null);
		}
		super.setUp();
	
		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		if (!project.exists()) {
			// populate the workspace with initial plug-ins/projects
			createExistingProjects("usageprojects", true, true, false);
		}
		ensureCompliance(new String[] {getTestingProjectName()});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(false);
		}
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
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		ArrayList<Class> classes = new ArrayList<Class>();
		classes.add(FieldUsageTests.class);
		classes.add(MethodUsageTests.class);
		classes.add(ConstructorUsageTests.class);
		classes.add(ClassUsageTests.class);
		classes.add(InterfaceUsageTests.class);
		classes.add(UnusedApiProblemFilterTests.class);
		classes.add(DependentUsageTests.class);
		classes.add(FragmentUsageTests.class);
		if(ProjectUtils.isJava5Compatible()) {
			classes.add(Java5FieldUsageTests.class);
			classes.add(Java5MethodUsageTests.class);
			classes.add(Java5ClassUsageTests.class);
		}
		if(ProjectUtils.isJava7Compatible()) {
			classes.add(Java7MethodUsageTests.class);
			classes.add(Java7ClassUsageTests.class);
			
		}
		return classes.toArray(new Class[classes.size()]);
	}
}
