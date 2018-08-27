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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.util.tests.ResourceEventWaiter;

import junit.framework.Test;

/**
 * Tests that unused
 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
 * s are reported properly on full and incremental builds
 */
public class UnusedApiProblemFilterTests extends UsageTest {

	private static final String BEFORE = "before"; //$NON-NLS-1$
	private static final String AFTER = "after"; //$NON-NLS-1$

	private IPath fRootPath = super.getTestSourcePath().append("filters"); //$NON-NLS-1$
	private IPath fFiltersPath = new Path("/usagetests/.settings/.api_filters"); //$NON-NLS-1$

	public UnusedApiProblemFilterTests(String name) {
		super(name);
	}

	/**
	 * Asserts a stub {@link IApiBaseline} that contains all of the workspace
	 * projects as API components
	 *
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		assertStubBaseline(BASELINE);
	}

	@Override
	protected void tearDown() throws Exception {
		removeBaseline(BASELINE);
		super.tearDown();
	}

	/**
	 * Removes the baseline with the given name
	 *
	 * @param name
	 */
	private void removeBaseline(String name) {
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline baseline = manager.getDefaultApiBaseline();
		if (baseline != null) {
			assertEquals("The given name should be the default baseline name", baseline.getName(), name); //$NON-NLS-1$
			assertTrue("The baseline [" + name + "] should have been removed", manager.removeApiBaseline(name)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	protected void setBuilderOptions() {
		super.setBuilderOptions();
		enableLeakOptions(true);
	}

	@Override
	protected IPath getTestSourcePath() {
		return fRootPath;
	}

	private IPath getBeforePath(String testname, String typename) {
		return getUpdateFilePath(testname).append(BEFORE).append(typename);
	}

	private IPath getAfterPath(String testname, String typename) {
		return getUpdateFilePath(testname).append(AFTER).append(typename);
	}

	private IPath getFilterFilePath(String testname) {
		return getUpdateFilePath(testname).append(".api_filters"); //$NON-NLS-1$
	}

	private IPath getUpdatePath(String path) {
		return new Path("/usagetests/src/x/y/z/").append(path); //$NON-NLS-1$
	}

	/**
	 * @return the test suite for this class
	 */
	public static Test suite() {
		return buildTestSuite(UnusedApiProblemFilterTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.UNUSED_PROBLEM_FILTERS, IApiProblem.NO_FLAGS);
	}

	protected void deployTest(IPath beforepath, IPath afterpath, IPath filterpath, IPath updatepath, boolean inc) throws Exception {
		// add the source
		createWorkspaceFile(updatepath, beforepath);

		// touch the filter store to ensure it is listening...
		IApiBaselineManager mgr = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline baseline = mgr.getWorkspaceBaseline();
		assertNotNull("The workspace baseline should not be null", baseline); //$NON-NLS-1$
		IProject project = getEnv().getProject("usagetests"); //$NON-NLS-1$
		assertNotNull("the testing project 'usagetests' must exist in the testing workspace", project); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(project);
		assertNotNull("The API component for project 'usagetests' must exist", component); //$NON-NLS-1$
		IApiFilterStore store = component.getFilterStore();
		assertNotNull("The filterstore for 'usagetests' must not be null", store); //$NON-NLS-1$
		// wait for the event
		ResourceEventWaiter waiter = new ResourceEventWaiter(fFiltersPath, IResourceChangeEvent.POST_CHANGE, IResourceDelta.CHANGED, 0);
		createWorkspaceFile(fFiltersPath, filterpath);
		Object event = waiter.waitForEvent();
		assertNotNull("the resource changed event for the filter file was not recieved", event); //$NON-NLS-1$

		expectingNoJDTProblems();
		// update the source
		deleteWorkspaceFile(updatepath, false);
		if (afterpath != null) {
			createWorkspaceFile(updatepath, afterpath);
		}
		if (inc) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		expectingNoJDTProblems();
		IProject projectCurrent = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		ApiProblem[] apiProblems = allSortedApiProblems(new IPath[] { projectCurrent.getFullPath() });
		if (apiProblems != null && apiProblems.length > 0) {
			assertProblems(apiProblems);
		} else {
			expectingNoProblemsFor(projectCurrent.getFullPath());
		}
	}

	public void testUnusedFilter1F() throws Exception {
		x1(false);
	}

	public void testUnusedFilter1I() throws Exception {
		x1(true);
	}

	/**
	 * Tests that unused filters are correctly reported. This test adds the
	 * final modifier to a class that has a protected method leaking and
	 * internal type, with a filter for the problem
	 *
	 * @param inc
	 */
	private void x1(boolean inc) throws Exception {
		String testname = "test1"; //$NON-NLS-1$
		String sourcename = "testUF1.java"; //$NON-NLS-1$
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { { "testUF1.m1(internal) has non-API parameter type internal" } }); //$NON-NLS-1$
		deployTest(getBeforePath(testname, sourcename), getAfterPath(testname, sourcename), getFilterFilePath(testname), getUpdatePath(sourcename), inc);
	}

	public void testUnusedFilter2F() throws Exception {
		// TODO straighten out what happens when a type with filters is deleted
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=416937
		// x2(false);
	}

	public void testUnusedFilter2I() throws Exception {
		// TODO straighten out what happens when a type with filters is deleted
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=416937
		// x2(true);
	}

	/**
	 * Tests that there is problem reported for a compilation unit that has been
	 * deleted, which has an API problem filter
	 *
	 * @param inc
	 */
	void x2(boolean inc) throws Exception {
		String testname = "test2"; //$NON-NLS-1$
		String sourcename = "testUF2.java"; //$NON-NLS-1$
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { { "testUF2.m1() has non-API return type internal" } }); //$NON-NLS-1$
		deployTest(getBeforePath(testname, sourcename), null, getFilterFilePath(testname), getUpdatePath(sourcename), inc);
	}

	public void testUnusedFilter3F() throws Exception {
		x3(false);
	}

	public void testUnusedFilter3I() throws Exception {
		x3(true);
	}

	/**
	 * Tests that a compilation unit with more than one problem in it works
	 * correctly when deleting a member that had a filter
	 *
	 * @param inc
	 */
	private void x3(boolean inc) throws Exception {
		String testname = "test3"; //$NON-NLS-1$
		String sourcename = "testUF3.java"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_METHOD_PARAMETER) });
		setExpectedMessageArgs(new String[][] {
				{ "testUF3.m2() has non-API return type internal" }, //$NON-NLS-1$
				{ "internal", "testUF3", "m1(internal[])" } }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		deployTest(getBeforePath(testname, sourcename), getAfterPath(testname, sourcename), getFilterFilePath(testname), getUpdatePath(sourcename), inc);
	}

	public void testUnusedFilter4F() throws Exception {
		x4(false);
	}

	public void testUnusedFilter4I() throws Exception {
		x4(true);
	}

	/**
	 * Tests that unused filters are not reported because the workspace baseline
	 * has not been set - i.e. the build should short-circuit to not even check
	 * for filters. This test adds the final modifier to a class that has a
	 * protected method leaking and internal type, with a filter for the
	 * problem, but no API baseline set
	 *
	 * @param inc
	 */
	private void x4(boolean inc) throws Exception {
		removeBaseline(BASELINE);
		String testname = "test1"; //$NON-NLS-1$
		String sourcename = "testUF1.java"; //$NON-NLS-1$
		IPath path = new Path("usagetest/x/y/z").append(sourcename); //$NON-NLS-1$
		expectingNoProblemsFor(path);
		deployTest(getBeforePath(testname, sourcename), getAfterPath(testname, sourcename), getFilterFilePath(testname), getUpdatePath(sourcename), inc);
	}
}
