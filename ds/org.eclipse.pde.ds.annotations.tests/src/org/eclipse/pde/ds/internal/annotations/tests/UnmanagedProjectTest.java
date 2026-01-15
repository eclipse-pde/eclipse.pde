package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.pde.ds.internal.annotations.DSAnnotationCompilationParticipant;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class UnmanagedProjectTest extends CompilationParticipantTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test0";
	}

	@Test
	public void managedProject() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("ds.annotations.test0");
		assumeTrue(project.exists());
		assertFalse(DSAnnotationCompilationParticipant.isManaged(project));
	}
}
