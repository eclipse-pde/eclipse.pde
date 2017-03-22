package org.eclipse.pde.ds.internal.annotations.tests;

import org.junit.Before;

public abstract class TestBase {

	protected static final String DS_PROBLEM_MARKER = "org.eclipse.pde.ds.annotations.problem";

	@Before
	public void ensureWorkspaceReady() throws Exception {
		AllDSAnnotationsTests.wsJob.join();
	}
}
