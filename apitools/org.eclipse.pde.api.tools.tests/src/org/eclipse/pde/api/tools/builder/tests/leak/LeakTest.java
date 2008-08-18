/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;

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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	@Override
	protected void assertProblems(ApiProblem[] problems) {
		super.assertProblems(problems);
		int[] pids = getExpectedProblemIds();
		assertEquals("The number of problems should match the number of specified problem ids", pids.length, problems.length);
		String[][] margs = getExpectedMessageArgs();
		if(margs != null) {
			ArrayList<String[]> args = new ArrayList<String[]>(Arrays.asList(margs));
			String message = null;
			int messageid = -1;
			loop: for(int i = 0; i < problems.length; i++) {
				for(Iterator<String[]> iter = args.iterator(); iter.hasNext();) {
					messageid = ApiProblemFactory.getProblemMessageId(problems[i].getProblemId());
					message = ApiProblemFactory.getLocalizedMessage(messageid, iter.next());
					if(problems[i].getMessage().equals(message)) {
						iter.remove();
						continue loop;
					}
				}
			}
			if(args.size() > 0) {
				fail("There was no problem that matched the arguments: "+Arrays.toString(args.iterator().next()));
			}
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
		Class[] classes = new Class[] {
				ClassExtendsLeak.class,
				ClassImplementsLeak.class,
				InterfaceExtendsLeak.class,
				ConstructorParameterLeak.class,
				MethodParameterLeak.class,
				MethodReturnTypeLeak.class,
				FieldTypeLeak.class
		};
		return classes;
	}
}
