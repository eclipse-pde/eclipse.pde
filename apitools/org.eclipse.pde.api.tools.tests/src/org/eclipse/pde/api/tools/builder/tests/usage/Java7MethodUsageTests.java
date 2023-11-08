/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Test class usage for Java 7 code snippets
 *
 * @since 1.0.100
 */
public class Java7MethodUsageTests extends Java7UsageTest {

	/**
	 * Constructor
	 * @param name
	 */
	public Java7MethodUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java7MethodUsageTests.class);
	}

	/**
	 * Returns the problem id with the given kind
	 *
	 * @param kind
	 * @return the problem id
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, kind, flags);
	}

	/**
	 * Tests illegal use of methods inside a string switch block
	 * (full)
	 */
	public void testStringSwitchF() throws Exception {
		IWorkspaceRoot root = getEnv().getWorkspace().getRoot();
		assertEquals("unexpected test project name", "usageprojectjava7", getTestingProjectName()); //$NON-NLS-1$//$NON-NLS-2$
		String[] projectNames = { "refprojectjava7", "usageprojectjava7" }; //$NON-NLS-1$ //$NON-NLS-2$
		logProjectInfos(getClass() + "." + getName() + " logging extra infos before refresh", projectNames); //$NON-NLS-1$ //$NON-NLS-2$
		for (String projectName : projectNames) {
			IProject project = root.getProject(projectName);
			assertTrue("project must exist but does not: " + projectName, project.exists()); //$NON-NLS-1$
			project.open(null);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, null);

		logProjectInfos(getClass() + "." + getName() + " logging extra infos before full build", projectNames); //$NON-NLS-1$ //$NON-NLS-2$
		x1(false);
	}

	/**
	 * Tests illegal use of methods inside a string switch block
	 * (incremental)
	 */
	public void testStringSwitchI() {
		x1(true);
	}


	private void x1(boolean inc) {
		int[] pids = new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		};
		setExpectedProblemIds(pids);
		String typename = "testMStringSwitch"; //$NON-NLS-1$
		String[][] args = new String[][] {
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m1()" }, //$NON-NLS-1$
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m3()" }, //$NON-NLS-1$
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m1()" }, //$NON-NLS-1$
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m3()" }, //$NON-NLS-1$
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m1()" }, //$NON-NLS-1$
				{ MethodUsageTests.METHOD_CLASS_NAME, typename, "m3()" }, //$NON-NLS-1$
		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(25, pids[0], args[0]), new LineMapping(26, pids[1], args[1]),
				new LineMapping(29, pids[2], args[2]), new LineMapping(30, pids[3], args[3]),
				new LineMapping(33, pids[4], args[4]), new LineMapping(34, pids[5], args[5])
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of methods inside a multi catch block
	 * (full)
	 */
	public void testMultiCatchF() {
		x2(false);
	}

	/**
	 * Tests illegal use of methods inside a multi catch block
	 * (incremental)
	 */
	public void testMultiCatchI() {
		x2(true);
	}


	private void x2(boolean inc) {
		int[] pids = new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		};
		setExpectedProblemIds(pids);
		String typename = "testMMultiCatch"; //$NON-NLS-1$
		String[][] args = new String[][] {
				{"MultipleThrowableClass", typename, "m2()"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(26, pids[0], args[0])
		});
		deployUsageTest(typename, inc);
	}

}
