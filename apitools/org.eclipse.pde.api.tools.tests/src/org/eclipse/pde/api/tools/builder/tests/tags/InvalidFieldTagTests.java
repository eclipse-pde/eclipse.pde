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
package org.eclipse.pde.api.tools.builder.tests.tags;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests unsupported javadoc tags on fields in classes, interfaces, enums and
 * annotations
 *
 * @since 1.0
 */
public class InvalidFieldTagTests extends TagTest {

	public InvalidFieldTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(InvalidFieldTagTests.class.getName());
		collectTests(suite);
		return suite;
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("field"); //$NON-NLS-1$
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				InvalidClassFieldTagTests.class,
				InvalidInterfaceFieldTagTests.class,
				InvalidAnnotationFieldTagTests.class,
				InvalidEnumFieldTagTests.class,
				InvalidEnumConstantTagTests.class };
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
		for (Class<?> clazz : classes) {
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
}
