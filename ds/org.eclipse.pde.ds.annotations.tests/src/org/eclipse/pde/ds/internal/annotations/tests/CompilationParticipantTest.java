package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assume.assumeTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.Before;

public abstract class CompilationParticipantTest extends TestBase {

	protected IProject testProject;

	protected abstract String getTestProjectName();

	@Before
	public void setUp() {
		testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(getTestProjectName());
		assumeTrue("Test project does not exist!", testProject.exists());
	}
}
