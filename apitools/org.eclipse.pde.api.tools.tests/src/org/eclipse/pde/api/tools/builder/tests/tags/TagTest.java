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
package org.eclipse.pde.api.tools.builder.tests.tags;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;


/**
 * Tests the builder to make sure it correctly finds and reports
 * unsupported tag usage.
 * 
 * @since 3.4
 */
public abstract class TagTest extends ApiBuilderTest {

	protected final String TESTING_PACKAGE = "a.b.c";
	
	/**
	 * Constructor
	 */
	public TagTest(String name) {
		super(name);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			InvalidClassTagTests.class,
			ValidClassTagTests.class,
			InvalidInterfaceTagTests.class,
			ValidInterfaceTagTests.class,
			InvalidFieldTagTests.class,
			ValidFieldTagTests.class,
			InvalidMethodTagTests.class,
			ValidMethodTagTests.class,
			InvalidEnumTagTests.class,
			InvalidAnnotationTagTests.class,
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
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TagTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		//only care about unsupported tags
		enableUnsupportedTagOptions(true);
		
		//disable the rest
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}
	
	/**
	 * Returns an array composed only of the specified number of {@link #PROBLEM_ID}
	 * @param problemcount
	 * @return an array of {@link #PROBLEM_ID} of the specified size, or an empty array if the specified
	 * size is < 1
	 */
	protected int[] getDefaultProblemSet(int problemcount) {
		if(problemcount < 1) {
			return new int[0];
		}
		int[] array = new int[problemcount];
		int defaultproblem = getDefaultProblemId();
		for(int i = 0; i < problemcount; i++) {
			array[i] = defaultproblem;
		}
		return array;
	}
	
	/**
	 * @return the default problem id for the given tag test
	 */
	protected abstract int getDefaultProblemId();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("tags");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "tagtest";
	}
}
