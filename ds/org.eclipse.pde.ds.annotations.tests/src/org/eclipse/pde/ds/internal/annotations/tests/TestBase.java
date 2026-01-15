package org.eclipse.pde.ds.internal.annotations.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceSetupExtension.class)
public abstract class TestBase {

	protected static final String DS_PROBLEM_MARKER = "org.eclipse.pde.ds.annotations.problem";

	@BeforeEach
	public void ensureWorkspaceReady() throws Exception {
		WorkspaceSetupExtension.getWorkspaceJob().join();
	}
}
