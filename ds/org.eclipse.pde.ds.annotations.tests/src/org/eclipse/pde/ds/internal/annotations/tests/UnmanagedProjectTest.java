package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
		assertFalse(DSAnnotationCompilationParticipant.isManaged(testProject));
	}
}
