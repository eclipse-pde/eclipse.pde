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
import org.eclipse.jdt.core.tests.junit.extension.TestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests that the builder correctly finds and reports interface compatibility
 * problems
 *
 * @since 1.0
 */
public class InterfaceCompatibilityTests extends CompatibilityTest {

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
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				InterfaceCompatibilityMemberTests.class,
				InterfaceCompatibilityRestrictionTests.class,
				InterfaceCompatibilityTypeParameterTests.class };
		return classes;
	}

	public InterfaceCompatibilityTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(InterfaceCompatibilityTests.class.getName());
		collectTests(suite);
		return suite;
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected String getTestingProjectName() {
		return "intercompat"; //$NON-NLS-1$
	}
}
