package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.pde.ds.internal.annotations.DSAnnotationCompilationParticipant;
import org.junit.Test;

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
