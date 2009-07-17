/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.usage;

import junit.framework.Test;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Tests field usage to Java 5 fields elements
 * 
 * @since 1.0.1
 */
public class Java5FieldUsageTests extends FieldUsageTests {

	protected static final String FIELD_ENUM_NAME = "FieldUsageEnum";
	
	/**
	 * Constructor
	 * @param name
	 */
	public Java5FieldUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java5FieldUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}

	public void testFieldUsage1F() {
		x1(false);
	}
	
	
	public void testFieldUsage1I() {
		x1(true);
	}
	
	/**
	 * Tests that an enum field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is flagged as a problem
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testF6";
		setExpectedMessageArgs(new String[][] {
				{FIELD_ENUM_NAME, typename, "f3"},
				{FIELD_ENUM_NAME, typename, "f2"},
				{FIELD_ENUM_NAME, INNER_NAME1, "f3"},
				{FIELD_ENUM_NAME, INNER_NAME1, "f2"},
				{FIELD_ENUM_NAME, INNER_NAME2, "f3"},
				{FIELD_ENUM_NAME, INNER_NAME2, "f2"},
				{FIELD_ENUM_NAME, OUTER_NAME, "f3"},
				{FIELD_ENUM_NAME, OUTER_NAME, "f2"}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testFieldUsage2F() {
		x2(false);
	}
	
	
	public void testFieldUsage2I() {
		x2(true);
	}
	
	/**
	 * Tests that a static final and final enum field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem
	 */
	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "testF7";
		deployUsageTest(typename, inc);
	}
}
