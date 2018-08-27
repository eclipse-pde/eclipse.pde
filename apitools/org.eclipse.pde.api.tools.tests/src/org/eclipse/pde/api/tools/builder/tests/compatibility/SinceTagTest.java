/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests that the builder correctly finds and reports missing since tags
 *
 * @since 1.0
 */
public abstract class SinceTagTest extends CompatibilityTest {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/since"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.since."; //$NON-NLS-1$

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				MissingSinceTagTests.class, InvalidSinceTagTests.class,
				MalformedSinceTagTests.class };
		return classes;
	}

	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 *
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder
		// tests...
		Class<?>[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class<?> clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite"); //$NON-NLS-1$
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null);
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
		TestSuite suite = new TestSuite(SinceTagTest.class.getName());
		collectTests(suite);
		return suite;
	}

	public SinceTagTest(String name) {
		super(name);
	}

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(true);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("since"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected String getTestingProjectName() {
		return "enumcompat"; //$NON-NLS-1$
	}

}
