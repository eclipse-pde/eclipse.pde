package org.eclipse.pde.ds.internal.annotations.tests;

import org.junit.Before;

public abstract class TestBase {

	@Before
	public void ensureWorkspaceReady() throws Exception {
		AllDSAnnotationsTests.wsJob.join();
	}
}
