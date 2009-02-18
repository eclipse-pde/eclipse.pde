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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.internal.util.Util;
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
	
	/**
	 * Deploys a usage test
	 * @param typename
	 * @param inc
	 */
	protected void deployTest(String typename, boolean inc) {
		deployUsageTest(TESTING_PACKAGE, 
				typename, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				getExpectedProblemIds().length > 0);
	}

	protected void deployReplacementTest(IPath before, IPath after, IPath filterpath, String sourcename, boolean inc) {
		try {
			getEnv().setAutoBuilding(false);
			IProject project = getEnv().getProject(getTestingProjectName());
			assertNotNull("the testing project "+getTestingProjectName()+" must be in the workspace", project);
			IPath settings = assertSettingsFolder(project);
			getEnv().addFile(settings, filterpath.lastSegment(), Util.getFileContentAsString(filterpath.toFile()));
			assertSource(before, project, TESTING_PACKAGE, sourcename);
			doBuild((inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), null);
			replaceSource(after, project, TESTING_PACKAGE, sourcename);
			doBuild((inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), null);
			IJavaProject jproject = getEnv().getJavaProject(getTestingProjectName());
			IPath sourcepath = project.getFullPath();
			if(after != null) {
				IType type = jproject.findType(TESTING_PACKAGE, sourcename);
				assertNotNull("The type "+sourcename+" from package "+TESTING_PACKAGE+" must exist", type);
				sourcepath = type.getPath();
			}
			if(getExpectedProblemIds().length > 0) {
				expectingOnlySpecificProblemsFor(sourcepath, getExpectedProblemIds());
				assertProblems(getEnv().getProblems());
			}
			else {
				expectingNoProblemsFor(sourcepath);
			}
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
		finally {
			getEnv().setAutoBuilding(true);
		}
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IProject[] pjs = getEnv().getWorkspace().getRoot().getProjects();
		File file = null;
		if(pjs.length == 0) {
			file = getTestSourcePath("refproject").toFile();
			createExistingProject(file, true, false);
		}
		file = getTestSourcePath("usagetests").toFile();
		createExistingProject(file, true, true);
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
		Class[] classes = new Class[] {
				FieldUsageTests.class,
				Java5FieldUsageTests.class,
				MethodUsageTests.class,
				Java5MethodUsageTests.class,
				ConstructorUsageTests.class,
				ClassUsageTests.class,
				Java5ClassUsageTests.class,
				InterfaceUsageTests.class,
				UnusedApiProblemFilterTests.class
		};
		return classes;
	}
}
