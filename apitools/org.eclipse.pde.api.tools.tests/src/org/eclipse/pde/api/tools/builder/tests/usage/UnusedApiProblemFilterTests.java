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

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests that unused {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}s
 * are reported properly on full and incremental builds
 */
public class UnusedApiProblemFilterTests extends UsageTest {

	private static final String BEFORE = "before";
	private static final String AFTER = "after";
	
	private IPath fRootPath = super.getTestSourcePath().append("filters");
	
	/**
	 * Constructor
	 * @param name
	 */
	public UnusedApiProblemFilterTests(String name) {
		super(name);
	}

	/**
	 * Asserts a stub {@link IApiBaseline} that contains all of the workspace projects
	 * as API components
	 * @param name the name for the baseline
	 */
	protected void assertStubBaseline(String name) {
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline baseline = manager.getDefaultApiBaseline();
		if (baseline == null) {
			baseline = ApiModelFactory.newApiBaseline(name);
			manager.addApiBaseline(baseline);
			manager.setDefaultApiBaseline(baseline.getName());
		}
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		assertStubBaseline(BASELINE);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		removeBaseline(BASELINE);
		super.tearDown();
	}
	
	/**
	 * Removes the baseline with the given name
	 * @param name
	 */
	private void removeBaseline(String name) {
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline baseline = manager.getDefaultApiBaseline();
		if (baseline != null) {
			manager.removeApiBaseline(name);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#setBuilderOptions()
	 */
	@Override
	protected void setBuilderOptions() {
		super.setBuilderOptions();
		enableLeakOptions(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return fRootPath; 
	}
	
	private IPath getBeforePath(String testname) {
		return fRootPath.append(testname).append(BEFORE);
	}
	
	private IPath getAfterPath(String testname) {
		return fRootPath.append(testname).append(AFTER);
	}
	
	private IPath getFilterFilePath(String testname) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(fRootPath).append(testname).append(".api_filters");
	}
	
	/**
	 * @return the test suite for this class
	 */
	public static Test suite() {
		return buildTestSuite(UnusedApiProblemFilterTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE, 
				IElementDescriptor.METHOD, 
				IApiProblem.UNUSED_PROBLEM_FILTERS, 
				IApiProblem.NO_FLAGS);
	}
	
	public void testUnusedFilter1F() {
		x1(false);
	}
	
	public void testUnusedFilter1I() {
		x1(true);
	}
	
	/**
	 * Tests that unused filters are correctly reported. This test adds the final modifier
	 * to a class that has a protected method leaking and internal type, with a filter for the problem
	 * @param inc
	 */
	private void x1(boolean inc) {
		String testname = "test1";
		String sourcename = "testUF1";
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {{"testUF1.m1(internal) has non-API parameter type internal"}});
		deployReplacementTest(
				getBeforePath(testname), 
				getAfterPath(testname), 
				getFilterFilePath(testname), 
				sourcename, 
				inc);
	}
	
	public void testUnusedFilter2F() {
		x2(false);
	}
	
	public void testUnusedFilter2I() {
//		x2(true);
	}
	
	/**
	 * Tests that there is no problem reported for a compilation unit that has been deleted, which has an api 
	 * problem filter
	 * @param inc
	 */
	private void x2(boolean inc) {
		String testname = "test2";
		String sourcename = "testUF2";
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {{"testUF2.m1() has non-API return type internal"}});
		deployReplacementTest(
				getBeforePath(testname),
				null,
				getFilterFilePath(testname),
				sourcename,
				inc);
	}

	public void testUnusedFilter3F() {
		x3(false);
	}
	
	public void testUnusedFilter3I() {
		x3(true);
	}
	
	/**
	 * Tests that a compilation unit with more than one problem in it works correctly when 
	 * deleting a member that had a filter
	 * @param inc
	 */
	private void x3(boolean inc) {
		String testname = "test3";
		String sourcename = "testUF3";
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.API_LEAK, 
						IApiProblem.LEAK_METHOD_PARAMETER)}
		);
		setExpectedMessageArgs(new String[][] {{"testUF3.m2() has non-API return type internal"},
				{"internal", "testUF3", "m1(internal[])"}});
		deployReplacementTest(
				getBeforePath(testname), 
				getAfterPath(testname), 
				getFilterFilePath(testname), 
				sourcename, 
				inc);
	}
	
	public void testUnusedFilter4F() {
		x4(false);
	}
	
	public void testUnusedFilter4I() {
		x4(true);
	}
	
	/**
	 * Tests that unused filters are not reported. This test adds the final modifier
	 * to a class that has a protected method leaking and internal type, with a filter for the problem, 
	 * but no API baseline set
	 * @param inc
	 */
	private void x4(boolean inc) {
		removeBaseline(BASELINE);
		String testname = "test1";
		String sourcename = "testUF1";
		expectingNoProblems();
		deployReplacementTest(
				getBeforePath(testname), 
				getAfterPath(testname), 
				getFilterFilePath(testname), 
				sourcename, 
				inc);
	}
}
