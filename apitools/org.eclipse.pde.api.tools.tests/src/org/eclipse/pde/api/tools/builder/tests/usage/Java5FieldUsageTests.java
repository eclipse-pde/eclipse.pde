/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;

/**
 * Tests field usage to Java 5 fields elements
 *
 * @since 1.0.1
 */
public class Java5FieldUsageTests extends FieldUsageTests {

	protected static final String FIELD_ENUM_NAME = "FieldUsageEnum"; //$NON-NLS-1$

	public Java5FieldUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java5FieldUsageTests.class);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	@Override
	public void testFieldUsage1F() {
		x1(false);
	}


	@Override
	public void testFieldUsage1I() {
		x1(true);
	}

	/**
	 * Tests that an enum field tagged with a noreference tag that is being accessed from a dependent plug-in
	 * is flagged as a problem
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testF6"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{FIELD_ENUM_NAME, typename, "f3"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, typename, "f2"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, INNER_NAME1, "f3"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, INNER_NAME1, "f2"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, INNER_NAME2, "f3"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, INNER_NAME2, "f2"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, OUTER_NAME, "f3"}, //$NON-NLS-1$
				{FIELD_ENUM_NAME, OUTER_NAME, "f2"} //$NON-NLS-1$
		});
		deployUsageTest(typename, inc);
	}

	@Override
	public void testFieldUsage2F() {
		x2(false);
	}


	@Override
	public void testFieldUsage2I() {
		x2(true);
	}

	/**
	 * Tests that a static final and final enum field tagged with a noreference tag that is being accessed from a dependent plug-in
	 * is not flagged as a problem
	 */
	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "testF7"; //$NON-NLS-1$
		deployUsageTest(typename, inc);
	}
}
