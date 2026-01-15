package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.jupiter.api.BeforeEach;

public abstract class CompilationParticipantTest extends TestBase {

	protected IProject testProject;

	protected abstract String getTestProjectName();

	@BeforeEach
	public void setUp() {
		testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(getTestProjectName());
		assumeTrue(testProject.exists(), "Test project does not exist!");
	}
}
