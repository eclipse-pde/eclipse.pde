package org.eclipse.pde.ds.internal.annotations.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class TestBase {

	protected static final String DS_PROBLEM_MARKER = "org.eclipse.pde.ds.annotations.problem";

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		AllDSAnnotationsTests.setUpBeforeClass();
	}

	@BeforeEach
	public void ensureWorkspaceReady() throws Exception {
		AllDSAnnotationsTests.wsJob.join();
	}
	
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		AllDSAnnotationsTests.tearDownAfterClass();
	}
}
