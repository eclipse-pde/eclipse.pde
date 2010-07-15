/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that problems with API usage are correctly reported
 * 
 * @since 1.0
 */
public class FieldUsageTests extends UsageTest {
	
	protected static final String FIELD_CLASS_NAME = "FieldUsageClass";
	
	private int pid = -1;
	
	/**
	 * Constructor
	 */
	public FieldUsageTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE,
					IElementDescriptor.FIELD, 
					IApiProblem.ILLEGAL_REFERENCE, 
					IApiProblem.FIELD);
		}
		return pid;
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("field");
	}
	
	/**
	 * @return the test suite for this class
	 */
	public static Test suite() {
		return buildTestSuite(FieldUsageTests.class);
	}

	/**
	 * Tests that field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using a full build
	 */
	public void testFieldUsage1F() {
		x1(false);
	}
	
	/**
	 * Tests that a field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using an incremental build
	 */
	public void testFieldUsage1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testF1";
		setExpectedMessageArgs(new String[][] {
				{FIELD_CLASS_NAME, typename, "f1"},
				{FIELD_CLASS_NAME, typename, "f2"},
				{FIELD_CLASS_NAME, INNER_NAME1, "f1"},
				{FIELD_CLASS_NAME, INNER_NAME1, "f2"},
				{FIELD_CLASS_NAME, INNER_NAME2, "f1"},
				{FIELD_CLASS_NAME, INNER_NAME2, "f2"},
				{FIELD_CLASS_NAME, OUTER_NAME, "f1"},
				{FIELD_CLASS_NAME, OUTER_NAME, "f2"}
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that a field in a static inner class tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using a full build
	 */
	public void testFieldUsage2F() {
		x2(false);
	}
	
	/**
	 * Tests that a field in a static inner class tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using an incremental build
	 */
	public void testFieldUsage2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testF2";
		setExpectedMessageArgs(new String[][] {
				{INNER_NAME1, typename, "f1"},
				{INNER_NAME1, typename, "f2"},
				{INNER_NAME1, INNER_NAME1, "f1"},
				{INNER_NAME1, INNER_NAME1, "f2"},
				{INNER_NAME1, INNER_NAME2, "f1"},
				{INNER_NAME1, INNER_NAME2, "f2"},
				{INNER_NAME1, OUTER_NAME, "f1"},
				{INNER_NAME1, OUTER_NAME, "f2"}
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that a final field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem using a full build
	 */
	public void testFieldUsage3F() {
		x3(false);
	}
	
	/**
	 * Tests that a final field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using an incremental build
	 */
	public void testFieldUsage3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		expectingNoProblems();
		String typename = "testF3";
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that a static final field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem using a full build
	 */
	public void testFieldUsage4F() {
		x4(false);
	}
	
	/**
	 * Tests that a static final field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem using an incremental build
	 */
	public void testFieldUsage4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		expectingNoProblems();
		String typename = "testF4";
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that an interface field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem using a full build
	 */
	public void testFieldUsage5F() {
		x5(false);
	}
	
	/**
	 * Tests that an interface field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is not flagged as a problem using an incremental build
	 */
	public void testFieldUsage5I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		expectingNoProblems();
		String typename = "testF5";
		deployUsageTest(typename, inc);
	}
	/**
	 * Tests that field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using a full build
	 */
	public void testFieldUsage6F() {
		x6(false);
	}
	
	/**
	 * Tests that a field tagged with a noreference tag that is being accessed from a dependent plug-in 
	 * is properly flagged as a problem using an incremental build
	 */
	public void testFieldUsage6I() {
		x6(true);
	}
	
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		String typename = "testF8";
		setExpectedMessageArgs(new String[][] {
				{FIELD_CLASS_NAME, typename, "f1"},
		});
		deployUsageTest(typename, inc);
	}
}
