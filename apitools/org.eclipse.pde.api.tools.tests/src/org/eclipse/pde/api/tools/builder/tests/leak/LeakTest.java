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
package org.eclipse.pde.api.tools.builder.tests.leak;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;

/**
 * Tests the builder to make sure it correctly reports 
 * non-API type leaks
 * 
 * @since 1.0
 */
public abstract class LeakTest extends ApiBuilderTest {

	protected final String TESTING_PACKAGE = "x.y.z";
	protected final String TESTING_PACKAGE_INTERNAL = "internal.x.y.z";
	protected final String TESTING_INTERNAL_CLASS_NAME = "internal";
	protected final String TESTING_INTERNAL_PROTECTED_CLASS_NAME = "internalprotected";
	protected final String TESTING_INTERNAL_PUBLIC_FIELD_CLASS_NAME = "internalpublicfield";
	protected final String TESTING_INTERNAL_PROTECTED_FIELD_CLASS_NAME = "internalprotectedfield";
	protected final String TESTING_INTERNAL_PRIVATE_FIELD_CLASS_NAME = "internalprivatefield";
	protected final String TESTING_INTERNAL_INTERFACE_NAME = "Iinternal";
	
	/**
	 * Constructor
	 */
	public LeakTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(true);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("leak");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "leaktests";
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(LeakTest.class.getName());
		collectTests(suite);
		return suite;
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
				ClassExtendsLeak.class,
				ClassImplementsLeak.class,
				InterfaceExtendsLeak.class,
				/*ConstructorParameterLeak.class,
				MethodParameterLeak.class,
				MethodReturnTypeLeak.class,*/
				FieldTypeLeak.class
		};
		return classes;
	}
	
	/**
	 * Deploys a full build with the given package and source names, where: 
	 * <ol>
	 * <li>the listing of internal package names will set all those packages that exist to be x-internal=true in the manifest</li>
	 * <li>the listing of fully qualified type names will each be checked for set expected problem id</li>
	 * <li>all other packages specified in packagenames that do not appear in internalpnames will be set to exported</li>
	 * </ol>
	 * @param packagenames the names of the packages to create in the testing project
	 * @param sourcenames the names of the source files to create in the testing project. Each source will be placed in the 
	 * corresponding package from the packagnames array, i.e. sourcenames[0] will be placed in packagenames[0]
	 * @param internalpnames the names of packages to mark as x-internal=true in the manifest of the project
	 * @param expectingproblemson the fully qualified names of the types we are expecting to see problems on
	 * @param expectingproblems the problem ids we expect to see on each of the types specified in the expectingproblemson array
	 * @param buildtype the type of build to run. One of:
	 * <ol>
	 * <li>IncrementalProjectBuilder#FULL_BUILD</li>
	 * <li>IncrementalProjectBuilder#INCREMENTAL_BUILD</li>
	 * <li>IncrementalProjectBuilder#CLEAN_BUILD</li>
	 * </ol>
	 * @param buildworkspace true if the entire workspace should be built, false if only the created project should be built
	 */
	protected void deployLeakTest(String[] packagenames, String[] sourcenames, String[] internalpnames, String[] expectingproblemson, boolean expectingproblems, int buildtype, boolean buildworkspace) {
		try {
			IPath path = assertProject(sourcenames, packagenames, internalpnames);
			doBuild(buildtype, (buildworkspace ? null : path));
			// should be no compilation problems
			expectingNoJDTProblems();
			if(expectingproblems || expectingproblemson != null) {
				IJavaProject jproject = getEnv().getJavaProject(path);
				for(int i = 0; i < expectingproblemson.length; i++) {
					IType type = jproject.findType(expectingproblemson[i]);
					assertNotNull("The type "+expectingproblemson[i]+" must exist", type);
					expectingOnlySpecificProblemsFor(type.getPath(), getExpectedProblemIds());
				}
				assertProblems(getEnv().getProblems());
			}
			else {
				expectingNoProblems();
			}
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}
